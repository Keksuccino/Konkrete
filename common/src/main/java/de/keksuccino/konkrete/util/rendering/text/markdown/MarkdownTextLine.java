package de.keksuccino.konkrete.util.rendering.text.markdown;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Groups laid-out Markdown fragments into one render line. */
public class MarkdownTextLine implements Renderable {

    /** Renderer that owns and positions this line. */
    public MarkdownRenderer parent;
    /** Horizontal offset from the renderer origin. */
    public float offsetX;
    /** Vertical offset from the renderer origin. */
    public float offsetY;
    /** Whether any fragment belongs to a fenced code block. */
    public boolean containsMultilineCodeBlockFragments = false;
    /** Alignment inherited from the first fragment during preparation. */
    @NotNull
    public MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
    /** Whether the first fragment starts a bullet-list item. */
    public boolean bulletListItemStartLine = false;
    /** Visible start and end fragments for each inline-code span crossing this line. */
    public final Map<MarkdownTextFragment.CodeBlockContext, SingleLineCodeBlockPart> singleLineCodeBlockStartEndPairs = new HashMap<>();
    /** Ordered fragments laid out on this line. */
    public final List<MarkdownTextFragment> fragments = new ArrayList<>();

    /** Creates an empty line owned by the supplied renderer. */
    public MarkdownTextLine(@NotNull MarkdownRenderer parent) {
        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.onRender(graphics, mouseX, mouseY, partial, true);
    }

    /** Positions fragments and optionally extracts their render state. */
    protected void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, boolean shouldRender) {

        float textX = this.parent.x + this.offsetX;
        for (MarkdownTextFragment f : this.fragments) {
            f.x = textX;
            f.y = this.parent.y + this.offsetY;
            if (shouldRender) {
                f.extractRenderState(graphics, mouseX, mouseY, partial);
            }
            textX += f.getRenderWidth();
        }

    }

    /** Derives structural metadata and updates fragment positions without drawing. */
    public void prepareLine() {
        this.prepareFragments();
        this.onRender(null, 0, 0, 0, false);
    }

    /** Rebuilds line-level metadata and inline-code ranges from the current fragments. */
    public void prepareFragments() {
        this.containsMultilineCodeBlockFragments = false;
        this.singleLineCodeBlockStartEndPairs.clear();
        this.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
        this.bulletListItemStartLine = false;
        for (MarkdownTextFragment f : this.fragments) {
            f.parentLine = this;
            f.startOfRenderLine = false;
            f.autoLineBreakAfter = false;
            //Check if line is Multi-Line Code Block
            if ((f.codeBlockContext != null) && !f.codeBlockContext.singleLine) {
                this.containsMultilineCodeBlockFragments = true;
            }
            //Discover Single-Line Code Block parts of the line
            if ((f.codeBlockContext != null) && f.codeBlockContext.singleLine && !this.singleLineCodeBlockStartEndPairs.containsKey(f.codeBlockContext)) {
                SingleLineCodeBlockPart part = new SingleLineCodeBlockPart();
                part.start = f;
                int index = f.codeBlockContext.codeBlockFragments.indexOf(f);
                if (index >= 0) {
                    List<MarkdownTextFragment> subList = f.codeBlockContext.codeBlockFragments.subList(index, f.codeBlockContext.codeBlockFragments.size());
                    for (MarkdownTextFragment cf : subList) {
                        if (!this.fragments.contains(cf)) break;
                        if (cf != f) part.end = cf;
                    }
                    if (part.end == null) part.end = f;
                    this.singleLineCodeBlockStartEndPairs.put(f.codeBlockContext, part);
                }
            }
        }
        if (!this.fragments.isEmpty()) {
            MarkdownTextFragment first = this.fragments.get(0);
            MarkdownTextFragment last = this.fragments.get(this.fragments.size()-1);
            //Set line alignment by first fragment
            this.alignment = first.alignment;
            //Set bullet list start by first fragment
            this.bulletListItemStartLine = first.bulletListItemStart;
            first.startOfRenderLine = true;
            last.autoLineBreakAfter = !last.naturalLineBreakAfter;
            // Don't set autoLineBreakAfter for tables
            if (last.isTable()) {
                last.autoLineBreakAfter = false;
            }
        }
    }

    /** Returns the sum of fragment widths, excluding a trailing space from the visual bound. */
    public float getLineWidth() {
        float f = 0;
        MarkdownTextFragment last = null;
        for (MarkdownTextFragment frag : this.fragments) {
            f += frag.getRenderWidth();
            last = frag;
        }
        if (last != null) {
            if (last.text.endsWith(" ")) {
                f -= (this.parent.getUnscaledTextWidth(" ") * last.getScale());
            }
        }
        if (f < 0) {
            f = 0;
        }
        return f;
    }

    /** Returns the greatest layout height among this line's fragments. */
    public float getLineHeight() {
        float f = 0;
        for (MarkdownTextFragment frag : this.fragments) {
            if (frag.getRenderHeight() > f) {
                f = frag.getRenderHeight();
            }
        }
        return f;
    }

    /** Returns whether the requested alignment is compatible with fenced code content. */
    public boolean isAlignmentAllowed(@NotNull MarkdownRenderer.MarkdownLineAlignment alignment) {
        if (alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) return true;
        return !this.containsMultilineCodeBlockFragments;
    }

    /** Identifies the visible portion of an inline code block on a line. */
    public static class SingleLineCodeBlockPart {
        MarkdownTextFragment start;
        MarkdownTextFragment end;

        /** Creates an empty inline-code range. */
        public SingleLineCodeBlockPart() {
        }
    }

}
