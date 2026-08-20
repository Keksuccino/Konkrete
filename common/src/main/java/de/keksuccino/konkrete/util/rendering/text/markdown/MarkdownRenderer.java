package de.keksuccino.konkrete.util.rendering.text.markdown;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.FocuslessContainerEventHandler;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

/** Parses, lays out, styles, and renders a Markdown document. */
@SuppressWarnings("unused")
public class MarkdownRenderer implements Renderable, FocuslessContainerEventHandler, NarratableEntry, NavigatableWidget {

    private static final int MAX_TEXT_LENGTH = 45000;
    private static final String TEXT_TOO_LONG_ERROR = "Markdown text exceeds the maximum supported length of " + MAX_TEXT_LENGTH + " characters.";
    private static final String NEWLINE_PERCENT = "%n%";
    private static final String NEWLINE = "\n";
    private static final String NEWLINE_R = "\r";
    private static final String NEWLINE_ESCAPED = "\\n";
    private static final String EMPTY_STRING = "";
    private static final String HTML_BREAK = "<br>";

    /** Whether Markdown syntax is interpreted instead of rendered literally. */
    protected boolean parseMarkdown = true;
    /** Original document source supplied by the caller. */
    @NotNull
    protected String text = "";
    /** Transforms source text before Markdown parsing and is reevaluated on every tick. */
    @NotNull
    protected UnaryOperator<String> textPreprocessor;
    /** Cached preprocessed and newline-normalized document source. */
    @Nullable
    protected String renderText;
    /** Absolute left edge of the document layout. */
    protected float x;
    /** Absolute top edge of the document layout. */
    protected float y;
    /** Preferred outer width used as the automatic wrapping boundary. */
    protected float optimalWidth;
    /** Measured outer width of the current line layout. */
    protected float realWidth;
    /** Measured outer height of the current line layout. */
    protected float realHeight;
    /** Fill color behind inline code spans. */
    @NotNull
    protected DrawableColor codeBlockSingleLineColor = DrawableColor.of(new Color(115, 115, 115));
    /** Fill color behind fenced code blocks. */
    @NotNull
    protected DrawableColor codeBlockMultiLineColor = DrawableColor.of(new Color(86, 86, 86));
    /** Underline color for headline levels that use a rule. */
    @NotNull
    protected DrawableColor headlineUnderlineColor = DrawableColor.of(new Color(169, 169, 169));
    /** Color of horizontal separator rules. */
    @NotNull
    protected DrawableColor separationLineColor = DrawableColor.of(new Color(169, 169, 169));
    /** Text color for hyperlinks. */
    @NotNull
    protected DrawableColor hyperlinkColor = DrawableColor.of(new Color(7, 113, 252));
    /** Text color for custom click-event spans. */
    @NotNull
    protected DrawableColor textClickEventColor = DrawableColor.of(new Color(7, 113, 252));
    /** Text color for custom hover-event spans. */
    @NotNull
    protected DrawableColor textHoverEventColor = DrawableColor.of(new Color(7, 113, 252));
    /** Color of quote text and quote indicator lines. */
    @NotNull
    protected DrawableColor quoteColor = DrawableColor.of(new Color(129, 129, 129));
    /** Horizontal indent applied to each quoted line. */
    protected float quoteIndent = 8;
    /** Whether quoted text is forced to italic. */
    protected boolean quoteItalic = false;
    /** Color of bullet-list markers. */
    @NotNull
    protected DrawableColor bulletListDotColor = DrawableColor.of(new Color(169, 169, 169));
    /** Horizontal indent added for each bullet-list nesting level. */
    protected float bulletListIndent = 8;
    /** Vertical gap added around the start of a bullet-list item. */
    protected float bulletListSpacing = 3;
    /** Default color for unstyled text. */
    @NotNull
    protected DrawableColor textBaseColor = DrawableColor.WHITE;
    /** Case conversion applied immediately before component creation. */
    @NotNull
    protected TextCase textCase = TextCase.NORMAL;
    /** Base scale multiplied by fragment-specific headline scaling. */
    protected float textBaseScale = 1.0f;
    /** Whether content wraps at word boundaries when it exceeds the preferred width. */
    protected boolean autoLineBreaks = true;
    /** Whether literal {@code <br>} tokens are removed during preprocessing. */
    protected boolean removeHtmlBreaks = true;
    /** Whether text is drawn with Minecraft's shadow pass. */
    protected boolean textShadow = true;
    /** Global text and decoration opacity in the inclusive range {@code 0.0F..1.0F}. */
    protected float textOpacity = 1.0F;
    /** Vertical gap inserted after each laid-out line. */
    protected float lineSpacing = 2;
    /** Minimum inset between the layout bounds and document content. */
    protected float border = 2;
    /** Whether explicit refresh requests retain the current fragments and lines. */
    public boolean skipRefresh = false;
    /** Optional enclosing UI scale used to keep thin decorations pixel-aligned. */
    @Nullable
    protected Float parentRenderScale = null;
    /** Measures and draws text without coupling Markdown to a specific UI font system. */
    @NotNull
    protected TextRenderer textRenderer;
    /** Color of table borders and cell separators. */
    @NotNull
    protected DrawableColor tableLineColor = DrawableColor.of(new Color(120, 120, 120));
    /** Fill color for visible table header rows. */
    @NotNull
    protected DrawableColor tableHeaderBackgroundColor = DrawableColor.of(new Color(50, 50, 50));
    /** Fill color for ordinary table rows. */
    @NotNull
    protected DrawableColor tableRowBackgroundColor = DrawableColor.of(new Color(40, 40, 40));
    /** Fill color for alternating table rows. */
    @NotNull
    protected DrawableColor tableAlternateRowColor = DrawableColor.of(new Color(60, 60, 60));
    /** Thickness of table borders and cell separators. */
    protected float tableLineThickness = 1.0f;
    /** Horizontal and vertical inset inside each table cell. */
    protected float tableCellPadding = 8.0f;
    /** Vertical space reserved above and below a table. */
    protected float tableMargin = 4.0f;
    /** Whether body rows alternate between the two configured fills. */
    protected boolean tableAlternateRowColors = true;
    /** Whether a parsed header row receives header styling. */
    protected boolean tableShowHeader = true;
    /** Drag state required by the container event-handler contract. */
    protected boolean dragging;
    /** Optional receiver for custom click and first-hover event identifiers. */
    @Nullable
    protected TextEventHandler textEventHandler = null;
    /** Event identifiers hovered during the preceding render-state update. */
    protected final Set<String> hoveredTextEventIds = new HashSet<>();
    /** Current positioned lines built from {@link #fragments}. */
    protected final List<MarkdownTextLine> lines = new ArrayList<>();
    /** Current parser output for {@link #renderText}. */
    protected final List<MarkdownTextFragment> fragments = new ArrayList<>();
    /** Predicates consulted before a positioned line contributes render state. */
    protected final List<ConsumingSupplier<MarkdownTextLine, Boolean>> lineRenderValidators = new ArrayList<>();

    /** Creates a renderer using Minecraft's current vanilla font and no text preprocessing. */
    public MarkdownRenderer() {
        this(Minecraft.getInstance().font);
    }

    /** Creates a renderer using the supplied vanilla font and no text preprocessing. */
    public MarkdownRenderer(@NotNull Font font) {
        this(new VanillaTextRenderer(font));
    }

    /** Creates a renderer using the supplied text backend and no text preprocessing. */
    public MarkdownRenderer(@NotNull TextRenderer textRenderer) {
        this(textRenderer, UnaryOperator.identity());
    }

    /** Creates a renderer using a vanilla font and a document preprocessing hook. */
    public MarkdownRenderer(@NotNull Font font, @NotNull UnaryOperator<String> textPreprocessor) {
        this(new VanillaTextRenderer(font), textPreprocessor);
    }

    /** Creates a renderer using fully configurable text rendering and preprocessing hooks. */
    public MarkdownRenderer(@NotNull TextRenderer textRenderer, @NotNull UnaryOperator<String> textPreprocessor) {
        this.textRenderer = Objects.requireNonNull(textRenderer);
        this.textPreprocessor = Objects.requireNonNull(textPreprocessor);
    }

    /** {@inheritDoc} */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.tick();
        this.onRender(graphics, mouseX, mouseY, partial, true);
        this.updateTextHoverEvents();
    }

    /** Positions every line and optionally extracts render state for lines accepted by all validators. */
    protected void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, boolean shouldRender) {

        float lineOffsetY = this.border;
        for (MarkdownTextLine line : this.lines) {
            float lineAlignmentOffsetX = 0;
            if (line.isAlignmentAllowed(line.alignment)) {
                float realInnerWidth = this.getRealWidth() - this.border - this.border;
                if (line.alignment == MarkdownLineAlignment.CENTERED) {
                    lineAlignmentOffsetX = Math.max(0, realInnerWidth - line.getLineWidth());
                    if (lineAlignmentOffsetX > 0) lineAlignmentOffsetX = lineAlignmentOffsetX / 2f;
                }
                if (line.alignment == MarkdownLineAlignment.RIGHT) {
                    lineAlignmentOffsetX = Math.max(0, realInnerWidth - line.getLineWidth());
                }
            }
            line.offsetX = this.border + lineAlignmentOffsetX;
            line.offsetY = lineOffsetY;
            if (shouldRender && this.isLineRenderingAllowedByValidators(line)) {
                line.extractRenderState(graphics, mouseX, mouseY, partial);
            }
            lineOffsetY += line.getLineHeight() + this.lineSpacing;
        }

    }

    /** Reevaluates dynamic preprocessing, rebuilds changed content, and updates layout size. */
    public void tick() {

        String preprocessedText = this.buildRenderText();
        // Compare the actual content: dynamic preprocessors can legitimately produce distinct strings with the same hash.
        if (!Objects.equals(this.renderText, preprocessedText)) {
            this.renderText = preprocessedText;
            this.refreshRenderer();
        }

        this.updateSize();

    }

    /** Recomputes the document bounds from the current line layout. */
    public void updateSize() {
        this.realWidth = 0;
        this.realHeight = 0;
        for (MarkdownTextLine l : this.lines) {
            float lw = l.getLineWidth();
            if (lw > this.realWidth) {
                this.realWidth = lw;
            }
            this.realHeight += l.getLineHeight() + this.lineSpacing;
        }
        this.realWidth += this.border + this.border;
        this.realHeight += this.border + this.border;
    }

    /** Reparses the current render text and rebuilds its line layout unless refreshes are suppressed. */
    public void refreshRenderer() {
        if (this.skipRefresh) return;
        if (this.renderText == null) this.renderText = this.buildRenderText();
        this.rebuildFragments();
        this.rebuildLines();
        this.onRender(null, 0, 0, 0, false);
    }

    /** Replaces all fragments by parsing the current normalized render text. */
    protected void rebuildFragments() {
        this.fragments.clear();
        if (this.renderText != null) {
            this.fragments.addAll(MarkdownParser.parse(this, this.renderText, this.parseMarkdown));
        }
    }

    /** Wraps parsed fragments into positioned render lines and isolates tables on dedicated lines. */
    protected void rebuildLines() {

        this.lines.clear();

        boolean queueNewLine = true;
        float totalWidth = 20;
        float totalHeight = this.border;
        float currentLineWidth = this.border;
        float currentLineHeight = 0;
        MarkdownTextFragment lastFragment = null;
        MarkdownTextLine line = new MarkdownTextLine(this);

        for (MarkdownTextFragment f : this.fragments) {

            boolean isStartOfLine = queueNewLine;
            queueNewLine = false;

            f.autoLineBreakAfter = false;

            // Tables should always be on their own line
            if (f.isTable()) {
                // If there's content on the current line, finish it
                if (!isStartOfLine && !line.fragments.isEmpty()) {
                    line.prepareLine();
                    this.lines.add(line);
                    line = new MarkdownTextLine(this);
                    totalHeight += currentLineHeight + this.lineSpacing;
                    currentLineHeight = 0;
                    currentLineWidth = this.border;
                }

                // Add table as its own line
                line.fragments.add(f);
                f.startOfRenderLine = true;
                line.offsetX = this.border;
                line.offsetY = totalHeight;
                line.prepareLine();
                this.lines.add(line);

                // Update dimensions
                float tableWidth = f.getRenderWidth() + this.border + this.border;
                if (totalWidth < tableWidth) {
                    totalWidth = tableWidth;
                }
                float tableHeight = f.getRenderHeight();
                totalHeight += tableHeight + this.lineSpacing;

                // Reset for next line
                line = new MarkdownTextLine(this);
                currentLineHeight = 0;
                currentLineWidth = this.border;
                queueNewLine = true;
                continue;
            }

            //Handle Auto Line Break
            if (!isStartOfLine && this.isAutoLineBreakingEnabled()) {
                f.startOfRenderLine = false;
                if (lastFragment.endOfWord && ((lastFragment.codeBlockContext == null) || lastFragment.codeBlockContext.singleLine) && ((currentLineWidth + f.getRenderWidth() + this.border) > this.optimalWidth)) {
                    if (totalWidth < currentLineWidth) {
                        totalWidth = currentLineWidth;
                    }
                    currentLineWidth = this.border;
                    line.offsetX = this.border;
                    line.offsetY = totalHeight;
                    totalHeight += currentLineHeight + this.lineSpacing;
                    currentLineHeight = 0;
                    isStartOfLine = true;
                    line.prepareLine();
                    this.lines.add(line);
                    line = new MarkdownTextLine(this);
                }
            }

            f.startOfRenderLine = isStartOfLine;

            line.fragments.add(f);

            float fw = f.getRenderWidth();
            float fh = f.getRenderHeight();
            currentLineWidth += fw;
            if (currentLineHeight < fh) {
                currentLineHeight = fh;
            }

            //Handle Natural Line Break
            if (f.naturalLineBreakAfter) {
                if (totalWidth < currentLineWidth) {
                    totalWidth = currentLineWidth;
                }
                line.offsetX = this.border;
                line.offsetY = totalHeight;
                currentLineWidth = this.border;
                totalHeight += currentLineHeight + this.lineSpacing;
                currentLineHeight = 0;
                queueNewLine = true;
                line.prepareLine();
                this.lines.add(line);
                line = new MarkdownTextLine(this);
            }

            lastFragment = f;

        }

        if (this.lines.isEmpty() || !line.fragments.isEmpty()) {
            line.prepareLine();
            this.lines.add(line);
        }

    }

    /** Applies preprocessing and normalizes supported newline and HTML-break forms. */
    @NotNull
    protected String buildRenderText() {
        String t = Objects.requireNonNull(this.textPreprocessor.apply(this.text), "The Markdown text preprocessor returned null");
        t = StringUtils.replace(t, NEWLINE_PERCENT, NEWLINE);
        t = StringUtils.replace(t, NEWLINE_R, NEWLINE);
        t = StringUtils.replace(t, NEWLINE_ESCAPED, NEWLINE);
        if (this.removeHtmlBreaks) t = StringUtils.replace(t, HTML_BREAK, EMPTY_STRING);
        return t;
    }

    /** Adds a predicate that can suppress individual lines during render-state extraction. */
    public MarkdownRenderer addLineRenderValidator(@NotNull ConsumingSupplier<MarkdownTextLine, Boolean> validator) {
        this.lineRenderValidators.add(validator);
        return this;
    }

    /** Returns whether every registered validator accepts the supplied line. */
    protected boolean isLineRenderingAllowedByValidators(@NotNull MarkdownTextLine line) {
        for (ConsumingSupplier<MarkdownTextLine, Boolean> validator : this.lineRenderValidators) {
            if (!validator.get(line)) return false;
        }
        return true;
    }

    /** Returns whether Markdown syntax is currently interpreted. */
    public boolean isParseMarkdown() {
        return this.parseMarkdown;
    }

    /** Enables or disables Markdown interpretation and refreshes the document. */
    public void setParseMarkdown(boolean parseMarkdown) {
        this.parseMarkdown = parseMarkdown;
        this.refreshRenderer();
    }

    /** Returns the original source document before preprocessing and newline normalization. */
    @NotNull
    public String getText() {
        return this.text;
    }

    /** Replaces the source document, substituting an error message when it exceeds the safety limit. */
    public void setText(@NotNull String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            this.text = TEXT_TOO_LONG_ERROR;
        } else {
            this.text = Objects.requireNonNull(text);
        }
        this.renderText = null;
    }

    /** Returns the hook applied to source text before parsing. */
    @NotNull
    public UnaryOperator<String> getTextPreprocessor() {
        return this.textPreprocessor;
    }

    /** Sets the non-null hook applied before parsing; identity behavior is the default. */
    public MarkdownRenderer setTextPreprocessor(@NotNull UnaryOperator<String> textPreprocessor) {
        this.textPreprocessor = Objects.requireNonNull(textPreprocessor);
        this.renderText = null;
        return this;
    }

    /** Returns the backend used to measure and draw text. */
    @NotNull
    public TextRenderer getTextRenderer() {
        return this.textRenderer;
    }

    /** Replaces the text backend and rebuilds fragment metrics. */
    public MarkdownRenderer setTextRenderer(@NotNull TextRenderer textRenderer) {
        this.textRenderer = Objects.requireNonNull(textRenderer);
        this.refreshRenderer();
        return this;
    }

    /** Returns the absolute left edge of the document layout. */
    public float getX() {
        return this.x;
    }

    /** Moves the document's absolute left edge without rebuilding its relative layout. */
    public MarkdownRenderer setX(float x) {
        this.x = x;
        return this;
    }

    /** Returns the absolute top edge of the document layout. */
    public float getY() {
        return this.y;
    }

    /** Moves the document's absolute top edge without rebuilding its relative layout. */
    public MarkdownRenderer setY(float y) {
        this.y = y;
        return this;
    }

    /** Changes the preferred outer width and rebuilds wrapping when the value differs. */
    public MarkdownRenderer setOptimalWidth(float width) {
        float oldOptimalWidth = this.optimalWidth;
        this.optimalWidth = width;
        if (oldOptimalWidth != this.optimalWidth) {
            this.refreshRenderer();
        }
        return this;
    }

    /** Returns the preferred outer width used as the wrapping boundary. */
    public float getOptimalWidth() {
        return this.optimalWidth;
    }

    /** Returns the greater of measured content width and preferred outer width. */
    public float getRealWidth() {
        //If real width smaller than optimal, return optimal
        return Math.max(this.realWidth, this.optimalWidth);
    }

    /** Returns the measured outer height of the current line layout. */
    public float getRealHeight() {
        return this.realHeight;
    }

    /** Returns the optional enclosing UI scale used for pixel-aligned decorations. */
    @Nullable
    public Float getParentRenderScale() {
        return this.parentRenderScale;
    }

    /** Replaces the enclosing UI-scale override and refreshes decoration layout. */
    public MarkdownRenderer setParentRenderScale(@Nullable Float parentRenderScale) {
        this.parentRenderScale = parentRenderScale;
        this.refreshRenderer();
        return this;
    }

    /** Returns whether content wraps automatically at word boundaries. */
    public boolean isAutoLineBreakingEnabled() {
        return this.autoLineBreaks;
    }

    /** Enables or disables automatic word wrapping and rebuilds the line layout. */
    public MarkdownRenderer setAutoLineBreakingEnabled(boolean enabled) {
        this.autoLineBreaks = enabled;
        this.rebuildLines();
        return this;
    }

    /** Returns whether literal {@code <br>} tokens are removed before parsing. */
    public boolean isRemoveHtmlBreaks() {
        return this.removeHtmlBreaks;
    }

    /** Controls whether literal {@code <br>} tokens are removed on the next preprocessing pass. */
    public MarkdownRenderer setRemoveHtmlBreaks(boolean removeHtmlBreaks) {
        this.removeHtmlBreaks = removeHtmlBreaks;
        return this;
    }

    /** Returns the color used for horizontal separator rules. */
    @NotNull
    public DrawableColor getSeparationLineColor() {
        return this.separationLineColor;
    }

    /** Changes the color used for horizontal separator rules. */
    public MarkdownRenderer setSeparationLineColor(@NotNull DrawableColor separationLineColor) {
        this.separationLineColor = separationLineColor;
        return this;
    }

    /** Returns the fill color used behind inline code spans. */
    @NotNull
    public DrawableColor getCodeBlockSingleLineColor() {
        return this.codeBlockSingleLineColor;
    }

    /** Changes the fill color used behind inline code spans. */
    public MarkdownRenderer setCodeBlockSingleLineColor(@NotNull DrawableColor codeBlockSingleLineColor) {
        this.codeBlockSingleLineColor = codeBlockSingleLineColor;
        return this;
    }

    /** Returns the fill color used behind fenced code blocks. */
    @NotNull
    public DrawableColor getCodeBlockMultiLineColor() {
        return this.codeBlockMultiLineColor;
    }

    /** Changes the fill color used behind fenced code blocks. */
    public MarkdownRenderer setCodeBlockMultiLineColor(@NotNull DrawableColor codeBlockMultiLineColor) {
        this.codeBlockMultiLineColor = codeBlockMultiLineColor;
        return this;
    }

    /** Returns the rule color used below large headlines. */
    @NotNull
    public DrawableColor getHeadlineUnderlineColor() {
        return this.headlineUnderlineColor;
    }

    /** Changes the rule color used below large headlines. */
    public MarkdownRenderer setHeadlineLineColor(@NotNull DrawableColor headlineUnderlineColor) {
        this.headlineUnderlineColor = headlineUnderlineColor;
        return this;
    }

    /** Returns the text color used for hyperlinks. */
    @NotNull
    public DrawableColor getHyperlinkColor() {
        return this.hyperlinkColor;
    }

    /** Changes the text color used for hyperlinks. */
    public MarkdownRenderer setHyperlinkColor(@NotNull DrawableColor hyperlinkColor) {
        this.hyperlinkColor = Objects.requireNonNull(hyperlinkColor);
        return this;
    }

    /** Returns the text color used for custom click-event spans. */
    @NotNull
    public DrawableColor getTextClickEventColor() {
        return this.textClickEventColor;
    }

    /** Changes the text color used for custom click-event spans. */
    public MarkdownRenderer setTextClickEventColor(@NotNull DrawableColor textClickEventColor) {
        this.textClickEventColor = Objects.requireNonNull(textClickEventColor);
        return this;
    }

    /** Returns the text color used for custom hover-event spans. */
    @NotNull
    public DrawableColor getTextHoverEventColor() {
        return this.textHoverEventColor;
    }

    /** Changes the text color used for custom hover-event spans. */
    public MarkdownRenderer setTextHoverEventColor(@NotNull DrawableColor textHoverEventColor) {
        this.textHoverEventColor = Objects.requireNonNull(textHoverEventColor);
        return this;
    }

    /** Returns the color used for quoted text and indicator lines. */
    @NotNull
    public DrawableColor getQuoteColor() {
        return this.quoteColor;
    }

    /** Changes the color used for quoted text and indicator lines. */
    public MarkdownRenderer setQuoteColor(@NotNull DrawableColor quoteColor) {
        this.quoteColor = Objects.requireNonNull(quoteColor);
        return this;
    }

    /** Returns the case conversion applied before component creation. */
    @NotNull
    public TextCase getTextCase() {
        return this.textCase;
    }

    /** Changes the case conversion applied before component creation. */
    public MarkdownRenderer setTextCase(@NotNull TextCase textCase) {
        this.textCase = Objects.requireNonNull(textCase);
        return this;
    }

    /** Returns the base multiplier applied to every fragment scale. */
    public float getTextBaseScale() {
        return this.textBaseScale;
    }

    /** Changes the base scale multiplier and refreshes fragment metrics. */
    public MarkdownRenderer setTextBaseScale(float textBaseScale) {
        this.textBaseScale = textBaseScale;
        this.refreshRenderer();
        return this;
    }

    /** Returns the default color used by otherwise unstyled text. */
    @NotNull
    public DrawableColor getTextBaseColor() {
        return this.textBaseColor;
    }

    /** Changes the default color used by otherwise unstyled text. */
    public MarkdownRenderer setTextBaseColor(@NotNull DrawableColor textBaseColor) {
        this.textBaseColor = textBaseColor;
        return this;
    }

    /** Returns global text and decoration opacity in the inclusive range {@code 0.0F..1.0F}. */
    public float getTextOpacity() {
        return this.textOpacity;
    }

    /** Sets global text and decoration opacity, clamped to {@code 0.0F..1.0F}. */
    public MarkdownRenderer setTextOpacity(float opacity) {
        if (opacity > 1.0F) opacity = 1.0F;
        if (opacity < 0.0F) opacity = 0.0F;
        this.textOpacity = opacity;
        return this;
    }

    /** Returns the color used for bullet-list markers. */
    @NotNull
    public DrawableColor getBulletListDotColor() {
        return this.bulletListDotColor;
    }

    /** Changes the color used for bullet-list markers. */
    public MarkdownRenderer setBulletListDotColor(@NotNull DrawableColor bulletListDotColor) {
        this.bulletListDotColor = bulletListDotColor;
        return this;
    }

    /** Returns the horizontal indent added per bullet-list nesting level. */
    public float getBulletListIndent() {
        return this.bulletListIndent;
    }

    /** Changes per-level bullet-list indentation and refreshes the layout. */
    public MarkdownRenderer setBulletListIndent(float bulletListIndent) {
        this.bulletListIndent = bulletListIndent;
        this.refreshRenderer();
        return this;
    }

    /** Returns the vertical gap reserved at the start of a bullet-list item. */
    public float getBulletListSpacing() {
        return this.bulletListSpacing;
    }

    /** Changes list-item spacing and refreshes the layout. */
    public MarkdownRenderer setBulletListSpacing(float bulletListSpacing) {
        this.bulletListSpacing = bulletListSpacing;
        this.refreshRenderer();
        return this;
    }

    /** Returns whether quoted text is forced to italic. */
    public boolean isQuoteItalic() {
        return this.quoteItalic;
    }

    /** Controls whether quoted text is forced to italic. */
    public MarkdownRenderer setQuoteItalic(boolean quoteItalic) {
        this.quoteItalic = quoteItalic;
        return this;
    }

    /** Returns the horizontal inset applied to quoted lines. */
    public float getQuoteIndent() {
        return this.quoteIndent;
    }

    /** Changes quoted-line indentation and refreshes the layout. */
    public MarkdownRenderer setQuoteIndent(float quoteIndent) {
        this.quoteIndent = quoteIndent;
        this.refreshRenderer();
        return this;
    }

    /** Returns whether text is drawn with Minecraft's shadow pass. */
    public boolean isTextShadow() {
        return this.textShadow;
    }

    /** Controls whether text is drawn with Minecraft's shadow pass. */
    public MarkdownRenderer setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    /** Returns the vertical gap inserted after each line. */
    public float getLineSpacing() {
        return this.lineSpacing;
    }

    /** Changes the vertical line gap and refreshes the layout. */
    public MarkdownRenderer setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
        this.refreshRenderer();
        return this;
    }

    /** Returns the outer content inset, never less than two pixels. */
    public float getBorder() {
        return Math.max(2.0F, this.border); // Cap min border size at 2, so text doesn't get cut off
    }

    /** Changes the outer content inset, clamps it to at least two pixels, and refreshes layout. */
    public MarkdownRenderer setBorder(float border) {
        this.border = Math.max(2.0F, border); // Cap min border size at 2, so text doesn't get cut off
        this.refreshRenderer();
        return this;
    }

    /** Returns the color used for table borders and cell separators. */
    @NotNull
    public DrawableColor getTableLineColor() {
        return this.tableLineColor;
    }

    /** Changes the color used for table borders and cell separators. */
    public MarkdownRenderer setTableLineColor(@NotNull DrawableColor tableLineColor) {
        this.tableLineColor = tableLineColor;
        return this;
    }

    /** Returns the fill color used for visible table headers. */
    @NotNull
    public DrawableColor getTableHeaderBackgroundColor() {
        return this.tableHeaderBackgroundColor;
    }

    /** Changes the fill color used for visible table headers. */
    public MarkdownRenderer setTableHeaderBackgroundColor(@NotNull DrawableColor tableHeaderBackgroundColor) {
        this.tableHeaderBackgroundColor = tableHeaderBackgroundColor;
        return this;
    }

    /** Returns the primary fill color used for table body rows. */
    @NotNull
    public DrawableColor getTableRowBackgroundColor() {
        return this.tableRowBackgroundColor;
    }

    /** Changes the primary fill color used for table body rows. */
    public MarkdownRenderer setTableRowBackgroundColor(@NotNull DrawableColor tableRowBackgroundColor) {
        this.tableRowBackgroundColor = tableRowBackgroundColor;
        return this;
    }

    /** Returns the alternating fill color used for table body rows. */
    @NotNull
    public DrawableColor getTableAlternateRowColor() {
        return this.tableAlternateRowColor;
    }

    /** Changes the alternating fill color used for table body rows. */
    public MarkdownRenderer setTableAlternateRowColor(@NotNull DrawableColor tableAlternateRowColor) {
        this.tableAlternateRowColor = tableAlternateRowColor;
        return this;
    }

    /** Returns the thickness of table borders and cell separators. */
    public float getTableLineThickness() {
        return this.tableLineThickness;
    }

    /** Changes the thickness of table borders and cell separators. */
    public MarkdownRenderer setTableLineThickness(float tableLineThickness) {
        this.tableLineThickness = tableLineThickness;
        return this;
    }

    /** Returns the horizontal and vertical inset inside each table cell. */
    public float getTableCellPadding() {
        return this.tableCellPadding;
    }

    /** Changes table cell padding and refreshes measured column widths. */
    public MarkdownRenderer setTableCellPadding(float tableCellPadding) {
        this.tableCellPadding = tableCellPadding;
        this.refreshRenderer();
        return this;
    }

    /** Returns whether table body rows use alternating fills. */
    public boolean isTableAlternateRowColors() {
        return this.tableAlternateRowColors;
    }

    /** Controls whether table body rows use alternating fills. */
    public MarkdownRenderer setTableAlternateRowColors(boolean tableAlternateRowColors) {
        this.tableAlternateRowColors = tableAlternateRowColors;
        return this;
    }

    /** Returns whether parsed header rows receive header styling. */
    public boolean isTableShowHeader() {
        return this.tableShowHeader;
    }

    /** Controls whether parsed header rows receive header styling. */
    public MarkdownRenderer setTableShowHeader(boolean tableShowHeader) {
        this.tableShowHeader = tableShowHeader;
        return this;
    }

    /** Returns the vertical space reserved above and below each table. */
    public float getTableMargin() {
        return this.tableMargin;
    }

    /** Changes the vertical space reserved above and below each table. */
    public MarkdownRenderer setTableMargin(float tableMargin) {
        this.tableMargin = tableMargin;
        return this;
    }

    /** Measures an unscaled styled component through the configured text backend. */
    protected float getUnscaledTextWidth(@NotNull Component text) {
        return this.textRenderer.getWidth(text);
    }

    /** Measures an unscaled string through the configured text backend. */
    protected float getUnscaledTextWidth(@NotNull String text) {
        return this.textRenderer.getWidth(text);
    }

    /** Returns the configured text backend's unscaled line height. */
    protected float getUnscaledTextHeight() {
        return this.textRenderer.getHeight();
    }

    /** Delegates component render-state extraction to the configured text backend. */
    protected void renderText(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, float x, float y, int color, boolean shadow) {
        this.textRenderer.render(graphics, text, x, y, color, shadow);
    }

    /** Returns a color with quarter-strength RGB channels while preserving alpha. */
    protected int darkenColor(int color) {
        int alpha = ARGB.alpha(color);
        int red = (int)(ARGB.red(color) * 0.25F);
        int green = (int)(ARGB.green(color) * 0.25F);
        int blue = (int)(ARGB.blue(color) * 0.25F);
        return ARGB.color(alpha, red, green, blue);
    }

    /** Clears hover state throughout the document and forgets active hover-event identifiers. */
    public void resetHovered() {
        this.resetHovered(this.fragments);
        this.hoveredTextEventIds.clear();
    }

    /** Recursively clears hover state for fragments and nested table-cell content. */
    protected void resetHovered(@NotNull List<MarkdownTextFragment> fragments) {
        for (MarkdownTextFragment fragment : fragments) {
            fragment.hovered = false;
            if (fragment.hoverEvent != null) {
                fragment.hoverEvent.wasHovered = false;
            }
            if (fragment.isTable() && fragment.tableContext != null) {
                for (MarkdownTextFragment.TableRow row : fragment.tableContext.rows) {
                    for (MarkdownTextFragment.TableCell cell : row.cells) {
                        this.resetHovered(cell.fragments);
                    }
                }
            }
        }
    }

    /** Emits first-hover callbacks and records the identifiers still hovered this frame. */
    protected void updateTextHoverEvents() {
        if (this.textEventHandler == null) {
            this.hoveredTextEventIds.clear();
            return;
        }
        List<MarkdownTextFragment.TextHoverEvent> hoverEvents = new ArrayList<>();
        this.collectHoverEvents(this.fragments, hoverEvents);
        Set<String> currentlyHovered = new HashSet<>();
        for (MarkdownTextFragment.TextHoverEvent hoverEvent : hoverEvents) {
            if (hoverEvent.isHovered()) {
                currentlyHovered.add(hoverEvent.identifier);
            }
        }
        for (String eventId : currentlyHovered) {
            if (!this.hoveredTextEventIds.contains(eventId)) {
                this.fireTextHoverEvent(eventId);
            }
        }
        this.hoveredTextEventIds.clear();
        this.hoveredTextEventIds.addAll(currentlyHovered);
    }

    /** Recursively collects unique hover contexts from fragments and nested table cells. */
    protected void collectHoverEvents(@NotNull List<MarkdownTextFragment> fragments, @NotNull List<MarkdownTextFragment.TextHoverEvent> events) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.hoverEvent != null && !events.contains(fragment.hoverEvent)) {
                events.add(fragment.hoverEvent);
            }
            if (fragment.isTable() && fragment.tableContext != null) {
                for (MarkdownTextFragment.TableRow row : fragment.tableContext.rows) {
                    for (MarkdownTextFragment.TableCell cell : row.cells) {
                        this.collectHoverEvents(cell.fragments, events);
                    }
                }
            }
        }
    }

    /** Replaces the receiver for custom click and first-hover event identifiers. */
    public MarkdownRenderer setTextEventHandler(@Nullable TextEventHandler textEventHandler) {
        this.textEventHandler = textEventHandler;
        return this;
    }

    /** Delivers a custom click identifier when an event handler is present. */
    protected void fireTextClickEvent(@NotNull String eventId) {
        if (this.textEventHandler != null) {
            this.textEventHandler.onTextClickEvent(eventId);
        }
    }

    /** Delivers a custom first-hover identifier when an event handler is present. */
    protected void fireTextHoverEvent(@NotNull String eventId) {
        if (this.textEventHandler != null) {
            this.textEventHandler.onTextHoverEvent(eventId);
        }
    }

    /** {@inheritDoc} */
    @Override
    @NotNull
    public List<MarkdownTextFragment> children() {
        return this.fragments;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    /** {@inheritDoc} */
    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
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

    /** {@inheritDoc} */
    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    /** {@inheritDoc} */
    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFocusable() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("MarkdownRenderers are not focusable.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNavigatable() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("MarkdownRenderers are not navigatable.");
    }

    /** Receives Markdown-specific click and hover events. */
    public interface TextEventHandler {

        /** Handles a click on fragments carrying the supplied event identifier. */
        void onTextClickEvent(@NotNull String eventId);
        /** Handles the initial hover of fragments carrying the supplied event identifier. */
        void onTextHoverEvent(@NotNull String eventId);

    }

    /** Measures and draws text for a Markdown renderer. */
    public interface TextRenderer {

        /** Returns the rendered width of a component. */
        float getWidth(@NotNull Component text);
        /** Returns the rendered width of a string. */
        float getWidth(@NotNull String text);
        /** Returns the rendered line height. */
        float getHeight();
        /** Draws a component with the requested visual options. */
        void render(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, float x, float y, int color, boolean shadow);

    }

    /** Renders Markdown text with Minecraft's vanilla font renderer. */
    public static class VanillaTextRenderer implements TextRenderer {

        /** Vanilla font used for measurement and render-state extraction. */
        protected final Font font;

        /** Creates a text backend that delegates measurement and drawing to the supplied font. */
        public VanillaTextRenderer(@NotNull Font font) {
            this.font = Objects.requireNonNull(font);
        }

        /** {@inheritDoc} */
        @Override
        public float getWidth(@NotNull Component text) {
            return this.font.width(text);
        }

        /** {@inheritDoc} */
        @Override
        public float getWidth(@NotNull String text) {
            return this.font.width(text);
        }

        /** {@inheritDoc} */
        @Override
        public float getHeight() {
            return this.font.lineHeight;
        }

        /** {@inheritDoc} */
        @Override
        public void render(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, float x, float y, int color, boolean shadow) {
            graphics.text(this.font, text, (int)x, (int)y, color, shadow);
        }

    }

    /** Controls case conversion applied to visible text before component creation. */
    public enum TextCase {

        /** Leaves source casing unchanged. */
        NORMAL,
        /** Converts visible text to lower case. */
        ALL_LOWER,
        /** Converts visible text to upper case. */
        ALL_UPPER

    }

    /** Controls horizontal placement of eligible render lines. */
    public enum MarkdownLineAlignment {

        /** Aligns content to the left edge. */
        LEFT,
        /** Centers content in the available width. */
        CENTERED,
        /** Aligns content to the right edge. */
        RIGHT

    }

}
