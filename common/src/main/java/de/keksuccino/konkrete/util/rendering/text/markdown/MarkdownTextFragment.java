package de.keksuccino.konkrete.util.rendering.text.markdown;

import de.keksuccino.konkrete.util.ListUtils;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.konkrete.util.resource.ResourceSupplier;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.window.WindowHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Represents one styled or structural fragment of a Markdown document. */
public class MarkdownTextFragment implements Renderable, GuiEventListener {

    /** Fixed visual gap between a bullet marker and item text. */
    protected static final int BULLET_LIST_SPACE_AFTER_INDENT = 5;

    /** Renderer that owns, measures, and styles this fragment. */
    public final MarkdownRenderer parent;
    /** Line assigned during layout preparation. */
    public MarkdownTextLine parentLine;
    /** Visible literal text, or a structural sentinel for synthetic fragments. */
    public String text;
    /** Absolute left edge assigned by line layout. */
    public float x;
    /** Absolute top edge assigned by line layout. */
    public float y;
    /** Measured text width before fragment scaling. */
    public float unscaledTextWidth;
    /** Measured text height before fragment scaling. */
    public float unscaledTextHeight;
    /** Whether this fragment is positioned first on its rendered line. */
    public boolean startOfRenderLine = false;
    /** Whether source structure forces a line break after this fragment. */
    public boolean naturalLineBreakAfter;
    /** Whether automatic wrapping ends the current line after this fragment. */
    public boolean autoLineBreakAfter;
    /** Whether the parser marked the following boundary as eligible for wrapping. */
    public boolean endOfWord;
    /** Optional image rendered in place of this fragment's text. */
    public ResourceSupplier<ITexture> imageSupplier = null;
    /** Whether this fragment represents a horizontal separator rule. */
    public boolean separationLine;
    /** Optional explicit text color overriding the renderer base color. */
    public DrawableColor textColor = null;
    /** Whether Markdown bold styling applies. */
    public boolean bold;
    /** Whether Markdown italic styling applies. */
    public boolean italic;
    /** Whether Markdown strikethrough styling applies. */
    public boolean strikethrough;
    /** Vanilla formatting state active at the start of this fragment. */
    @NotNull
    MinecraftFormattingState minecraftFormatting = MinecraftFormattingState.EMPTY;
    /** Whether this fragment starts a bullet-list item. */
    public boolean bulletListItemStart = false;
    /** Zero-based nesting depth of the active bullet-list item. */
    public int bulletListLevel = 0;
    /** Block alignment inherited by a line when this is its first fragment. */
    @NotNull
    public MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
    /** Hyperlink context shared with adjacent participating fragments. */
    public Hyperlink hyperlink = null;
    /** Custom click-event context shared with participating fragments. */
    public TextClickEvent clickEvent = null;
    /** Custom hover-event context shared with participating fragments. */
    public TextHoverEvent hoverEvent = null;
    /** Headline level controlling fragment scale and underline behavior. */
    @NotNull
    public HeadlineType headlineType = HeadlineType.NONE;
    /** Quote context shared by the fragments in one quoted block. */
    public QuoteContext quoteContext = null;
    /** Inline or fenced code context shared by participating fragments. */
    public CodeBlockContext codeBlockContext = null;
    /** Whether Markdown-derived styling and code context are suppressed. */
    public boolean plainText = false;
    /** Optional vanilla font identifier applied through the component style. */
    public Identifier font = null;
    /** Result of this fragment's most recent hit test. */
    public boolean hovered = false;
    /** Table data owned by a synthetic table placeholder fragment. */
    public TableContext tableContext = null;

    /** Creates a text fragment and initializes its height from the renderer's text backend. */
    public MarkdownTextFragment(@NotNull MarkdownRenderer parent, @NotNull String text) {
        this.parent = parent;
        this.text = text;
        this.unscaledTextHeight = this.parent.getUnscaledTextHeight();
    }

    /** {@inheritDoc} */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.hovered = this.isMouseOver(mouseX, mouseY);

        if (this.isLinkLike() && this.hovered) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }

        // Handle table rendering
        if (this.isTable()) {
            renderTable(graphics, mouseX, mouseY, partial);
            return;
        }

        if (this.imageSupplier != null) {
            this.imageSupplier.forRenderable((iTexture, location) -> {
                RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.parent.textOpacity);
                RenderingUtils.blitF(graphics, location, this.x, this.y, 0.0F, 0.0F, this.getRenderWidth(), this.getRenderHeight(), this.getRenderWidth(), this.getRenderHeight());
                RenderingUtils.resetShaderColor(graphics);
            });
        } else if (this.separationLine) {

            RenderingUtils.fillF(graphics, this.parent.x + this.parent.border, this.y, this.parent.x + this.parent.getRealWidth() - this.parent.border, this.y + this.getRenderHeight(), this.parent.separationLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            RenderingUtils.resetShaderColor(graphics);

        } else {

            float scale = this.getScale();
            if (scale <= 0.0F) return;

            this.renderCodeBlock(graphics);

            graphics.pose().pushMatrix();
            try {
                // Vanilla text extraction only accepts integer draw coordinates. Translate to the exact text origin and draw at local zero so a fractional scale never requires dividing and truncating its absolute position, which would move glyphs into the parent's scissor area.
                graphics.pose().translate(this.getTextX(), this.getTextY());
                graphics.pose().scale(scale, scale);
                this.parent.renderText(graphics, this.buildRenderComponent(false), 0.0F, 0.0F, this.parent.textBaseColor.getColorIntWithAlpha(this.parent.textOpacity), this.parent.textShadow && (this.codeBlockContext == null));
            } finally {
                graphics.pose().popMatrix();
            }
            RenderingUtils.resetShaderColor(graphics);

            this.renderQuoteLine(graphics);

            this.renderBulletListDot(graphics);

            this.renderHeadlineUnderline(graphics);

        }

    }

    /** Measures and renders table fills, cell fragments, interaction state, and border lines. */
    protected void renderTable(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (this.tableContext == null) return;

        // Calculate column widths
        this.tableContext.calculateColumnWidths(this.parent);

        // Use the fragment's x position which already includes alignment offset
        float tableX = this.x;
        float tableY = this.y + this.parent.tableMargin; // Add margin before table
        float currentY = tableY;

        // Use the actual table width for rendering (not the full area width)
        float actualTableWidth = this.tableContext.totalWidth;
        // Render rows
        int dataRowIndex = 0; // Track data rows separately for alternating colors
        for (int rowIndex = 0; rowIndex < this.tableContext.rows.size(); rowIndex++) {
            TableRow row = this.tableContext.rows.get(rowIndex);
            float rowHeight = this.tableContext.getRowHeight(row);
            float currentX = tableX;

            // Draw row background
            if (row.isHeader && this.parent.tableShowHeader) {
                // Header background
                RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, currentY + rowHeight, this.parent.tableHeaderBackgroundColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else if (!row.isHeader) {
                // Regular row background - alternate between two colors
                if (this.parent.tableAlternateRowColors && dataRowIndex % 2 == 1) {
                    // Alternate rows
                    RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, currentY + rowHeight, this.parent.tableAlternateRowColor.getColorIntWithAlpha(this.parent.textOpacity));
                } else {
                    // Base rows
                    RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, currentY + rowHeight, this.parent.tableRowBackgroundColor.getColorIntWithAlpha(this.parent.textOpacity));
                }
                dataRowIndex++;
            }

            // Render cells
            for (int cellIndex = 0; cellIndex < row.cells.size(); cellIndex++) {
                if (cellIndex >= this.tableContext.columnWidths.size()) break;

                TableCell cell = row.cells.get(cellIndex);
                float cellWidth = this.tableContext.columnWidths.get(cellIndex);

                // Calculate text position based on alignment
                float textX = currentX + this.parent.tableCellPadding;
                float textY = currentY + this.parent.tableCellPadding;

                // Calculate alignment offset once for all fragments
                float totalTextWidth = 0;
                for (MarkdownTextFragment f : cell.fragments) {
                    totalTextWidth += f.getTextRenderWidth();
                }

                float alignmentOffset = 0;
                if (cell.alignment == TableCell.TableCellAlignment.CENTER) {
                    alignmentOffset = (cellWidth - this.parent.tableCellPadding * 2 - totalTextWidth) / 2;
                } else if (cell.alignment == TableCell.TableCellAlignment.RIGHT) {
                    alignmentOffset = cellWidth - this.parent.tableCellPadding * 2 - totalTextWidth;
                }

                // First pass: position fragments and render code block backgrounds
                float fragmentX = textX + alignmentOffset;
                float codeBlockStartX = -1;
                MarkdownTextFragment.CodeBlockContext currentCodeBlock = null;

                for (int fragIndex = 0; fragIndex < cell.fragments.size(); fragIndex++) {
                    MarkdownTextFragment fragment = cell.fragments.get(fragIndex);

                    fragment.x = fragmentX;
                    fragment.y = textY;

                    // Track and render code block backgrounds
                    if (fragment.codeBlockContext != null && fragment.codeBlockContext.singleLine) {
                        if (currentCodeBlock == null || currentCodeBlock != fragment.codeBlockContext) {
                            // Start of a new code block
                            currentCodeBlock = fragment.codeBlockContext;
                            codeBlockStartX = fragment.x - 1;
                        }

                        // Check if this is the last fragment of the code block
                        boolean isLastFragment = (fragIndex == cell.fragments.size() - 1) ||
                                                (fragIndex + 1 < cell.fragments.size() &&
                                                 cell.fragments.get(fragIndex + 1).codeBlockContext != currentCodeBlock);

                        if (isLastFragment) {
                            // Render the code block background
                            float endX = fragment.x + fragment.getRenderWidth() + 1;
                            if (fragment.text.endsWith(" ")) {
                                endX -= (this.parent.getUnscaledTextWidth(" ") * fragment.getScale());
                            }
                            renderCodeBlockBackground(graphics, codeBlockStartX, fragment.y - 2, endX, fragment.y + fragment.getTextRenderHeight(), this.parent.codeBlockSingleLineColor.getColorIntWithAlpha(this.parent.textOpacity));
                            currentCodeBlock = null;
                        }
                    } else {
                        currentCodeBlock = null;
                    }

                    fragmentX += fragment.getTextRenderWidth();
                }

                // Second pass: render the fragments
                for (MarkdownTextFragment fragment : cell.fragments) {
                    fragment.extractRenderState(graphics, mouseX, mouseY, partial); // Pass actual mouse coords for hover/click detection

                    // Check if any hyperlink in the table is hovered
                    if (fragment.isLinkLike() && fragment.hovered) {
                        CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
                    }
                }

                currentX += cellWidth;
            }

            currentY += rowHeight;
        }

        // Draw table borders
        int lineColor = this.parent.tableLineColor.getColorIntWithAlpha(this.parent.textOpacity);
        float lineThickness = this.parent.tableLineThickness;

        // Horizontal lines
        float y = tableY;
        for (int i = 0; i <= this.tableContext.rows.size(); i++) {
            RenderingUtils.fillF(graphics, tableX, y, tableX + actualTableWidth, y + lineThickness, lineColor);
            if (i < this.tableContext.rows.size()) {
                y += this.tableContext.getRowHeight(this.tableContext.rows.get(i));
            }
        }

        // Draw thicker line under header
        if (this.tableContext.hasHeader && this.parent.tableShowHeader && !this.tableContext.rows.isEmpty()) {
            float headerY = tableY + this.tableContext.getRowHeight(this.tableContext.rows.get(0));
            RenderingUtils.fillF(graphics, tableX, headerY, tableX + actualTableWidth, headerY + lineThickness * 2, lineColor);
        }

        // Vertical lines
        float x = tableX;
        for (int i = 0; i <= this.tableContext.columnWidths.size(); i++) {
            RenderingUtils.fillF(graphics, x, tableY, x + lineThickness, currentY + lineThickness, lineColor);
            if (i < this.tableContext.columnWidths.size()) {
                x += this.tableContext.columnWidths.get(i);
            }
        }

        RenderingUtils.resetShaderColor(graphics);
    }

    /** Renders this context's code background once from its first visible fragment. */
    protected void renderCodeBlock(GuiGraphicsExtractor graphics) {
        if (this.codeBlockContext == null) return;

        // Check if this fragment is inside a table cell (has tableContext but is not the table itself)
        boolean isInTableCell = this.tableContext != null && !this.isTable();

        // Skip code block background rendering for fragments inside table cells
        // (handled in renderTable method)
        if (isInTableCell) {
            return;
        }

        // Normal code block rendering (outside tables)
        if (this.parentLine != null) {
            MarkdownTextFragment start = this.codeBlockContext.getBlockStart();
            MarkdownTextFragment end = this.codeBlockContext.getBlockEnd();
            if (this.codeBlockContext.singleLine) {
                MarkdownTextLine.SingleLineCodeBlockPart part = this.parentLine.singleLineCodeBlockStartEndPairs.get(this.codeBlockContext);
                if (part == null) return;
                start = part.start;
                end = part.end;
            }
            if (start != this) return;
            if (end == null) return;
            if (this.codeBlockContext.singleLine) {
                float xEnd = end.x + end.getRenderWidth();
                if (end.text.endsWith(" ")) {
                    xEnd -= (this.parent.getUnscaledTextWidth(" ") * this.getScale());
                }
                renderCodeBlockBackground(graphics, this.x, this.y - 2, xEnd, this.y + this.getTextRenderHeight(), this.parent.codeBlockSingleLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else {
                renderCodeBlockBackground(graphics, this.parent.x + this.parent.border, this.y, this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, end.y + end.getRenderHeight() - 1, this.parent.codeBlockMultiLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            }
        }
    }

    /** Draws a one-pixel clipped-corner rectangle for an inline or fenced code background. */
    protected void renderCodeBlockBackground(GuiGraphicsExtractor graphics, float minX, float minY, float maxX, float maxY, int color) {
        RenderingUtils.fillF(graphics, minX+1, minY, maxX-1, minY+1, color);
        RenderingUtils.fillF(graphics, minX, minY+1, maxX, maxY-1, color);
        RenderingUtils.fillF(graphics, minX+1, maxY-1, maxX-1, maxY, color);
        RenderingUtils.resetShaderColor(graphics);
    }

    /** Draws the full-width rule used by level-one and level-two headlines. */
    protected void renderHeadlineUnderline(GuiGraphicsExtractor graphics) {
        if (this.startOfRenderLine && ((this.headlineType == HeadlineType.BIGGER) || (this.headlineType == HeadlineType.BIGGEST))) {
            float scale = (this.parent.parentRenderScale != null) ? this.parent.parentRenderScale : (float)WindowHandler.getGuiScale();
            float lineThickness = (scale > 1) ? 0.5f : 1f;
            float lineY = this.y + this.getTextRenderHeight() + 1;
            RenderingUtils.fillF(graphics, this.parent.x + this.parent.border, lineY, this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, lineY + lineThickness, this.parent.headlineUnderlineColor.getColorIntWithAlpha(this.parent.textOpacity));
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    /** Draws one vertical quote indicator after the final fragment has established its bounds. */
    protected void renderQuoteLine(GuiGraphicsExtractor graphics) {
        if ((this.quoteContext != null) && (this.quoteContext.getQuoteEnd() != null) && (this.quoteContext.getQuoteEnd() == this)) {
            float yStart = Objects.requireNonNull(this.quoteContext.getQuoteStart()).y - 2;
            float yEnd = this.y + this.getRenderHeight() + 1;
            if (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) {
                RenderingUtils.fillF(graphics, this.parent.x, yStart, this.parent.x + 2, yEnd, this.parent.quoteColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else if (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) {
                RenderingUtils.fillF(graphics, this.parent.x + this.parent.getRealWidth() - this.parent.border - 2, yStart, this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, yEnd, this.parent.quoteColor.getColorIntWithAlpha(this.parent.textOpacity));
            }
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    /** Draws a scaled bullet marker for fragments that start list items. */
    protected void renderBulletListDot(GuiGraphicsExtractor graphics) {
        if ((this.bulletListLevel > 0) && this.bulletListItemStart) {
            final float scale = this.getScale();

            // Calculate dimensions using scale
            final float bulletSize = 3 * scale;

            // Shift bullet dot one level to the right:
            final float bulletX = this.x - (5 * scale) + (this.parent.bulletListIndent * (this.bulletListLevel) * scale);

            // Vertical centering using text baseline
            final float textBaselineY = this.getTextY() + (this.parent.getUnscaledTextHeight() * scale * 0.5f) - (bulletSize * 0.5f);

            RenderingUtils.fillF(graphics, bulletX, textBaselineY, bulletX + bulletSize, textBaselineY + bulletSize, this.parent.bulletListDotColor.getColorIntWithAlpha(this.parent.textOpacity));
        }
    }

    /** Builds visible text and style, optionally omitting width-neutral decoration styling. */
    @NotNull
    protected Component buildRenderComponent(boolean forWidthCalculation) {
        Style style = Style.EMPTY;
        if (this.font != null) {
            style = style.withFont(new FontDescription.Resource(this.font));
        }
        if (this.italic) {
            style = style.withItalic(true);
        }
        if (this.bold) {
            style = style.withBold(true);
        }
        if (this.strikethrough && !forWidthCalculation) {
            style = style.withStrikethrough(true);
        }
        if (this.quoteContext != null) {
            style = style.withColor(this.parent.quoteColor.getColorInt());
            if (this.parent.quoteItalic) {
                style = style.withItalic(true);
            }
        }
        if (this.textColor != null) {
            style = style.withColor(this.textColor.getColorInt());
        }
        boolean addSpaceComponentAtEnd = false;
        String t = this.text;
        if (this.isLinkLike() && (this.naturalLineBreakAfter || this.autoLineBreakAfter) && t.endsWith(" ")) {
            //Remove spaces at line end that would look ugly when underlined
            t = t.substring(0, t.length()-1);
        } else if (this.isLinkLike() && this.isLastLinkLikeFragment() && t.endsWith(" ")) {
            //Make space at the end not underlined without removing it completely
            t = t.substring(0, t.length()-1);
            addSpaceComponentAtEnd = true;
        }
        if (this.codeBlockContext != null) {
            style = Style.EMPTY;
        }
        if (this.plainText) {
            style = Style.EMPTY;
        }
        style = this.minecraftFormatting.applyTo(style);
        if (this.hyperlink != null) {
            style = style.withColor(this.parent.hyperlinkColor.getColorInt());
            if (this.hyperlink.isHovered()) {
                style = style.withUnderlined(true);
            }
        }
        if (this.clickEvent != null) {
            style = style.withColor(this.parent.getTextClickEventColor().getColorInt());
            if (this.clickEvent.isHovered()) {
                style = style.withUnderlined(true);
            }
        }
        if (this.hoverEvent != null) {
            style = style.withColor(this.parent.getTextHoverEventColor().getColorInt());
            if (this.hoverEvent.isHovered()) {
                style = style.withUnderlined(true);
            }
        }
        if (this.parent.textCase == MarkdownRenderer.TextCase.ALL_UPPER) {
            t = t.toUpperCase();
        }
        if (this.parent.textCase == MarkdownRenderer.TextCase.ALL_LOWER) {
            t = t.toLowerCase();
        }
        MutableComponent comp = Component.literal(t).setStyle(style);
        if (addSpaceComponentAtEnd) {
            comp.append(Component.literal(" ").setStyle(Style.EMPTY.withUnderlined(false)));
        }
        return comp;
    }

    /** Returns whether this fragment participates in a hyperlink or custom pointer event. */
    protected boolean isLinkLike() {
        return (this.hyperlink != null) || (this.clickEvent != null) || (this.hoverEvent != null);
    }

    /** Returns whether this is the final fragment in its active link-like context. */
    protected boolean isLastLinkLikeFragment() {
        if (this.hyperlink != null) {
            return ListUtils.getLast(this.hyperlink.hyperlinkFragments) == this;
        }
        if (this.clickEvent != null) {
            return ListUtils.getLast(this.clickEvent.eventFragments) == this;
        }
        if (this.hoverEvent != null) {
            return ListUtils.getLast(this.hoverEvent.eventFragments) == this;
        }
        return false;
    }

    /** Remeasures the styled render component through the renderer's text backend. */
    protected void updateWidth() {
        this.unscaledTextWidth = this.parent.getUnscaledTextWidth(this.buildRenderComponent(true));
    }

    /** Returns the text origin converted into pre-scale render coordinates. */
    public float getTextRenderX() {
        float scale = this.getScale();
        if (scale <= 0.0F) return 0.0F;
        return this.getTextX() / scale;
    }

    /** Returns the unscaled horizontal inset contributed by quote, list, and code contexts. */
    protected float getTextRenderOffsetX() {
        float offsetX = 0.0F;

        if ((this.quoteContext != null) && this.startOfRenderLine && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT)) {
            offsetX += this.parent.quoteIndent;
        }

        if (this.bulletListLevel > 0 && this.startOfRenderLine) {
            // Now apply the full bullet indent for the first fragment.
            float bulletIndent = (this.parent.bulletListIndent * this.bulletListLevel) + BULLET_LIST_SPACE_AFTER_INDENT;
            offsetX += bulletIndent;
        }

        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.startOfRenderLine) {
            offsetX += 10;
        }

        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() == this)) {
            offsetX += 1;
        }

        return offsetX;
    }

    /** Returns the text baseline origin converted into pre-scale render coordinates. */
    public float getTextRenderY() {
        float scale = this.getScale();
        if (scale <= 0.0F) return 0.0F;
        return this.getTextY() / scale;
    }

    /** Returns the unscaled vertical inset contributed by fenced code and list spacing. */
    protected float getTextRenderOffsetY() {
        float offsetY = 0.0F;
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() != null) && (this.codeBlockContext.getBlockStart().y == this.y)) {
            offsetY += 10;
        }
        if ((this.bulletListLevel > 0) && (this.parentLine != null) && this.parentLine.bulletListItemStartLine) {
            offsetY += this.parent.bulletListSpacing;
        }
        return offsetY;
    }

    /** Returns the full layout width, including structural insets or replacement-image sizing. */
    public float getRenderWidth() {

        // Handle table width - return actual table width for proper alignment
        if (this.isTable()) {
            this.tableContext.calculateColumnWidths(this.parent);
            return this.tableContext.totalWidth;
        }

        if (this.imageSupplier != null) {
            ITexture t = this.imageSupplier.get();
            if (t == null) return 10;
            if (t.getWidth() <= (this.parent.getRealWidth() - this.parent.border - this.parent.border)) {
                return t.getWidth();
            }
            return this.parent.getRealWidth() - this.parent.border - this.parent.border;
        }

        float f = this.getTextRenderWidth();
        if ((this.quoteContext != null) && this.startOfRenderLine && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT)) {
            f += this.parent.quoteIndent * this.getScale();
        }
        if ((this.quoteContext != null) && (this.naturalLineBreakAfter || this.autoLineBreakAfter) && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT)) {
            f += this.parent.quoteIndent;
        }
        if (this.bulletListLevel > 0 && this.startOfRenderLine) {
            float bulletSpace = (this.parent.bulletListIndent * this.bulletListLevel * this.getScale()) + (BULLET_LIST_SPACE_AFTER_INDENT * this.getScale());
            f += bulletSpace;
        }
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.startOfRenderLine) {
            f += 10;
        }
        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() == this)) {
            f += 1;
        }
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.autoLineBreakAfter || this.naturalLineBreakAfter)) {
            f += 10;
        }
        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockEnd() == this)) {
            f += 1;
        }
        return f;

    }

    /** Returns the full layout height, including decorations, table margins, or image aspect ratio. */
    public float getRenderHeight() {

        // Handle table height
        if (this.isTable()) {
            float totalHeight = 0;
            for (TableRow row : this.tableContext.rows) {
                totalHeight += this.tableContext.getRowHeight(row);
            }
            // Add line thickness for borders
            totalHeight += this.parent.tableLineThickness * (this.tableContext.rows.size() + 1);
            // Add margin above and below tables
            totalHeight += this.parent.tableMargin * 2;
            return totalHeight;
        }

        if (this.imageSupplier != null) {
            ITexture t = this.imageSupplier.get();
            if (t == null) return 10;
            return t.getAspectRatio().getAspectRatioHeight((int)this.getRenderWidth());
        }

        float f = this.getTextRenderHeight();
        if ((this.headlineType == HeadlineType.BIGGER) || (this.headlineType == HeadlineType.BIGGEST)) {
            f += 8;
        }
        if (this.headlineType == HeadlineType.BIG) {
            f += 6;
        }
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() != null) && (this.codeBlockContext.getBlockStart().y == this.y)) {
            f += 10;
        }
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockEnd() != null) && (this.codeBlockContext.getBlockEnd().y == this.y)) {
            f += 10;
        }
        if ((this.bulletListLevel > 0) && this.bulletListItemStart) {
            f += this.parent.bulletListSpacing;
        }
        return f;
    }

    /** Returns the measured text width after fragment scaling. */
    public float getTextRenderWidth() {
        return this.unscaledTextWidth * this.getScale();
    }

    /** Returns the measured text height after fragment scaling. */
    public float getTextRenderHeight() {
        return this.unscaledTextHeight * this.getScale();
    }

    /** Returns the absolute text origin after applying scaled horizontal context insets. */
    public float getTextX() {
        return this.x + (this.getTextRenderOffsetX() * this.getScale());
    }

    /** Returns the absolute text origin after applying scaled vertical context insets. */
    public float getTextY() {
        return this.y + (this.getTextRenderOffsetY() * this.getScale());
    }

    /** Returns the scaled text hit-box width. */
    public float getTextWidth() {
        return this.getTextRenderWidth();
    }

    /** Returns the scaled text hit-box height. */
    public float getTextHeight() {
        return this.getTextRenderHeight();
    }

    /** Returns the positive finite product of headline and renderer scale, or zero if invalid. */
    public float getScale() {
        float f = 1.0f;
        if (this.headlineType == HeadlineType.BIG) f = 1.2f;
        if (this.headlineType == HeadlineType.BIGGER) f = 1.6f;
        if (this.headlineType == HeadlineType.BIGGEST) f = 2.0f;
        float scale = f * this.parent.textBaseScale;
        return Float.isFinite(scale) && scale > 0.0F ? scale : 0.0F;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.isTable()) {
            // For tables, use the actual table rendering area
            this.tableContext.calculateColumnWidths(this.parent);
            float tableX = this.x;
            float tableY = this.y + this.parent.tableMargin;
            float tableWidth = this.tableContext.totalWidth;
            float tableHeight = this.getRenderHeight() - (this.parent.tableMargin * 2); // Subtract the margin
            return RenderingUtils.isXYInArea(mouseX, mouseY, tableX, tableY, tableWidth, tableHeight);
        }
        if ((this.imageSupplier != null) && (this.imageSupplier.get() != null)) {
            return RenderingUtils.isXYInArea(mouseX, mouseY, this.x, this.y, this.getRenderWidth(), this.getRenderHeight());
        }
        return RenderingUtils.isXYInArea(mouseX, mouseY, this.getTextX(), this.getTextY(), this.getTextWidth(), this.getTextHeight());
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Dispatches a coordinate-based click to table cells, custom events, or hyperlinks. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle clicks on table cells
        if (this.isTable() && this.tableContext != null) {
            // Check all cell fragments for clicks
            for (TableRow row : this.tableContext.rows) {
                for (TableCell cell : row.cells) {
                    for (MarkdownTextFragment fragment : cell.fragments) {
                        if (fragment.isMouseOver(mouseX, mouseY) && fragment.mouseClicked(mouseX, mouseY, button)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        if ((this.clickEvent != null) && this.hovered) {
            this.parent.fireTextClickEvent(this.clickEvent.identifier);
            return true;
        }

        // Handle regular hyperlink clicks
        if ((this.hyperlink != null) && this.hovered) {
            WebUtils.openWebLink(this.hyperlink.link);
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void setFocused(boolean var1) {
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFocused() {
        return false;
    }

    /** Returns whether this is the synthetic placeholder responsible for rendering a table. */
    public boolean isTable() {
        return (this.tableContext != null) && this.text.equals("[TABLE]");
    }

    /** Groups fragments that share a web hyperlink. */
    public static class Hyperlink {

        /** Destination opened when any participating fragment is clicked. */
        public String link = null;
        /** Ordered fragments participating in this hyperlink. */
        public final List<MarkdownTextFragment> hyperlinkFragments = new ArrayList<>();

        /** Creates an empty hyperlink context. */
        public Hyperlink() {
        }

        /** Returns whether any participating fragment is currently hovered. */
        public boolean isHovered() {
            for (MarkdownTextFragment f : this.hyperlinkFragments) {
                if (f.hovered) return true;
            }
            return false;
        }

    }

    /** Groups fragments that emit the same click event identifier. */
    public static class TextClickEvent {

        /** Identifier delivered to the renderer's event handler. */
        public final String identifier;
        /** Ordered fragments participating in this click event. */
        public final List<MarkdownTextFragment> eventFragments = new ArrayList<>();

        /** Creates a click context that will emit the supplied identifier. */
        public TextClickEvent(@NotNull String identifier) {
            this.identifier = identifier;
        }

        /** Returns whether any participating fragment is currently hovered. */
        public boolean isHovered() {
            for (MarkdownTextFragment f : this.eventFragments) {
                if (f.hovered) return true;
            }
            return false;
        }

    }

    /** Groups fragments that emit the same hover event identifier. */
    public static class TextHoverEvent {

        /** Identifier delivered to the renderer's event handler. */
        public final String identifier;
        /** Ordered fragments participating in this hover event. */
        public final List<MarkdownTextFragment> eventFragments = new ArrayList<>();
        /** Compatibility flag cleared with fragment hover state. */
        public boolean wasHovered = false;

        /** Creates a hover context that will emit the supplied identifier on entry. */
        public TextHoverEvent(@NotNull String identifier) {
            this.identifier = identifier;
        }

        /** Returns whether any participating fragment is currently hovered. */
        public boolean isHovered() {
            for (MarkdownTextFragment f : this.eventFragments) {
                if (f.hovered) return true;
            }
            return false;
        }

    }

    /** Tracks all fragments belonging to one quote. */
    public static class QuoteContext {

        /** Ordered fragments belonging to this quoted block. */
        public final List<MarkdownTextFragment> quoteFragments = new ArrayList<>();

        /** Creates an empty quote context. */
        public QuoteContext() {
        }

        /** Returns the first quoted fragment, or {@code null} while the context is empty. */
        @Nullable
        public MarkdownTextFragment getQuoteStart() {
            if (!quoteFragments.isEmpty()) return quoteFragments.get(0);
            return null;
        }

        /** Returns the final quoted fragment, or {@code null} while the context is empty. */
        @Nullable
        public MarkdownTextFragment getQuoteEnd() {
            if (!quoteFragments.isEmpty()) return quoteFragments.get(quoteFragments.size()-1);
            return null;
        }

    }

    /** Tracks all fragments belonging to one code block. */
    public static class CodeBlockContext {

        /** Ordered fragments belonging to this code span or fenced block. */
        public final List<MarkdownTextFragment> codeBlockFragments = new ArrayList<>();
        /** Whether this context represents an inline code span rather than a fenced block. */
        public boolean singleLine = true;

        /** Creates an empty code-block context. */
        public CodeBlockContext() {
        }

        /** Returns the first code fragment, or {@code null} while the context is empty. */
        @Nullable
        public MarkdownTextFragment getBlockStart() {
            if (!codeBlockFragments.isEmpty()) return codeBlockFragments.get(0);
            return null;
        }

        /** Returns the final code fragment, or {@code null} while the context is empty. */
        @Nullable
        public MarkdownTextFragment getBlockEnd() {
            if (!codeBlockFragments.isEmpty()) return codeBlockFragments.get(codeBlockFragments.size()-1);
            return null;
        }

    }

    /** Maps Markdown headline levels to renderer scale and underline presets. */
    public enum HeadlineType {

        /** Represents ordinary non-headline text. */
        NONE,
        /** Represents a level-three headline. */
        BIG, // ###
        /** Represents a level-two headline. */
        BIGGER, // ##
        /** Represents a level-one headline. */
        BIGGEST // #

    }

    /** Stores the rows and calculated layout of one table. */
    public static class TableContext {

        /** Parsed rows in source order. */
        public final List<TableRow> rows = new ArrayList<>();
        /** Measured maximum width of each column, including cell padding. */
        public final List<Float> columnWidths = new ArrayList<>();
        /** Whether the first row precedes a valid separator row. */
        public boolean hasHeader = false;
        /** Sum of the most recently calculated column widths. */
        public float totalWidth = 0;
        /** Reserved table-origin coordinate retained for API compatibility. */
        public float x = 0;
        /** Reserved table-origin coordinate retained for API compatibility. */
        public float y = 0;

        /** Creates an empty table context. */
        public TableContext() {
        }

        /** Remeasures each column from its widest cell and updates {@link #totalWidth}. */
        public void calculateColumnWidths(MarkdownRenderer renderer) {
            columnWidths.clear();
            if (rows.isEmpty()) return;

            // Initialize column widths
            int columnCount = rows.get(0).cells.size();
            for (int i = 0; i < columnCount; i++) {
                columnWidths.add(0f);
            }

            // Find maximum width for each column
            for (TableRow row : rows) {
                for (int i = 0; i < Math.min(row.cells.size(), columnCount); i++) {
                    TableCell cell = row.cells.get(i);
                    float cellWidth = 0;
                    for (MarkdownTextFragment fragment : cell.fragments) {
                        cellWidth += fragment.getTextRenderWidth();
                    }
                    cellWidth += renderer.tableCellPadding * 2;
                    if (cellWidth > columnWidths.get(i)) {
                        columnWidths.set(i, cellWidth);
                    }
                }
            }

            // Calculate total width
            totalWidth = 0;
            for (Float width : columnWidths) {
                totalWidth += width;
            }
        }

        /** Returns the tallest fragment height in a row plus vertical cell padding. */
        public float getRowHeight(TableRow row) {
            float maxHeight = 0;
            for (TableCell cell : row.cells) {
                float cellHeight = 0;
                for (MarkdownTextFragment fragment : cell.fragments) {
                    float fragmentHeight = fragment.getTextRenderHeight();
                    if (fragmentHeight > cellHeight) {
                        cellHeight = fragmentHeight;
                    }
                }
                if (cellHeight > maxHeight) {
                    maxHeight = cellHeight;
                }
            }
            return maxHeight + (row.parent.tableCellPadding * 2);
        }

    }

    /** Stores the cells and metadata of one table row. */
    public static class TableRow {

        /** Cells in source-column order. */
        public final List<TableCell> cells = new ArrayList<>();
        /** Whether this row receives header styling when headers are visible. */
        public boolean isHeader = false;
        /** Renderer providing table metrics and theme values. */
        public MarkdownRenderer parent;

        /** Creates an empty body row using the supplied renderer for table metrics. */
        public TableRow(MarkdownRenderer parent) {
            this.parent = parent;
        }

    }

    /** Stores the fragments and alignment of one table cell. */
    public static class TableCell {

        /** Parsed inline fragments contained in this cell. */
        public final List<MarkdownTextFragment> fragments = new ArrayList<>();
        /** Horizontal placement of cell content within its measured column. */
        public TableCellAlignment alignment = TableCellAlignment.LEFT;

        /** Creates an empty left-aligned table cell. */
        public TableCell() {
        }

        /** Controls text placement inside a computed table column. */
        public enum TableCellAlignment {

            /** Aligns cell content to the left. */
            LEFT,
            /** Centers cell content. */
            CENTER,
            /** Aligns cell content to the right. */
            RIGHT

        }

    }

}
