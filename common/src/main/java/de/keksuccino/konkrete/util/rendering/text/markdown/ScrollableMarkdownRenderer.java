package de.keksuccino.konkrete.util.rendering.text.markdown;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Hosts a Markdown renderer inside a two-axis scroll area. */
public class ScrollableMarkdownRenderer implements Renderable, ContainerEventHandler, NarratableEntry {

    /** Active viewport and scrollbar controller. */
    @NotNull
    protected ScrollArea scrollArea;
    /** Renderer currently hosted as the scroll area's sole entry. */
    @NotNull
    protected MarkdownRenderer markdownRenderer;
    /** Creates a fresh configured renderer whenever this component is rebuilt. */
    @NotNull
    protected final Supplier<? extends MarkdownRenderer> markdownRendererFactory;
    /** Event targets exposed to Minecraft's container dispatch. */
    @NotNull
    protected List<GuiEventListener> children = new ArrayList<>();
    /** Whether both scrollbars may activate and the vertical bar accepts wheel input. */
    protected boolean allowScrolling = true;
    /** Document source reapplied whenever the component is rebuilt. */
    @NotNull
    protected String text = "";
    /** Drag state required by the container event-handler contract. */
    protected boolean dragging = false;
    /** Retained compatibility state; focus queries are delegated to the scroll area. */
    protected boolean focused = false;

    /** Creates a scrollable renderer with default Markdown configuration. */
    public ScrollableMarkdownRenderer(float x, float y, float width, float height) {
        this(x, y, width, height, (Supplier<? extends MarkdownRenderer>)MarkdownRenderer::new);
    }

    /** Creates a scrollable renderer whose document text is passed through the supplied hook. */
    public ScrollableMarkdownRenderer(float x, float y, float width, float height, @NotNull UnaryOperator<String> textPreprocessor) {
        this(x, y, width, height, () -> new MarkdownRenderer().setTextPreprocessor(textPreprocessor));
    }

    /** Creates a scrollable renderer from a factory that returns a fresh instance with configured themes, hooks, and text rendering. */
    public ScrollableMarkdownRenderer(float x, float y, float width, float height, @NotNull Supplier<? extends MarkdownRenderer> markdownRendererFactory) {
        this.markdownRendererFactory = Objects.requireNonNull(markdownRendererFactory);
        this.rebuild(x, y, width, height);
    }

    /** Recreates the renderer and scroll area at the supplied bounds, then reapplies the retained document source. */
    public void rebuild(float x, float y, float width, float height) {

        this.children.clear();

        this.markdownRenderer = Objects.requireNonNull(this.markdownRendererFactory.get(), "The Markdown renderer factory returned null");

        this.scrollArea = new ScrollArea(x, y, width, height) {
            /** {@inheritDoc} */
            @Override
            public void updateScrollArea() {
                super.updateScrollArea();
                //Manually update scroll bar area size, so the grabbers are outside the area
                this.verticalScrollBar.scrollAreaEndX = x + width + 12;
                this.horizontalScrollBar.scrollAreaEndY = y + height + 12;
            }
        };
        this.scrollArea.minimumEntryWidthIsAreaWidth = false;
        this.scrollArea.makeEntriesWidthOfArea = false;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;
        this.scrollArea.verticalScrollBar.grabberWidth = 10;
        this.scrollArea.verticalScrollBar.grabberHeight = 20;
        this.scrollArea.horizontalScrollBar.grabberWidth = 20;
        this.scrollArea.horizontalScrollBar.grabberHeight = 10;
        this.scrollArea.backgroundColor = () -> DrawableColor.of(0,0,0,0);
        this.scrollArea.borderColor = () -> DrawableColor.of(0,0,0,0);

        this.scrollArea.addEntry(new MarkdownRendererEntry(this.scrollArea, this.markdownRenderer));

        this.scrollArea.setRenderOnlyEntriesInArea(false);

        //Don't render markdown lines outside visible area (for performance reasons)
        this.markdownRenderer.addLineRenderValidator(line -> {
            if ((line.parent.getY() + line.offsetY + line.getLineHeight()) < this.scrollArea.getInnerY()) {
                return false;
            }
            if ((line.parent.getY() + line.offsetY) > (this.scrollArea.getInnerY() + this.scrollArea.getInnerHeight())) {
                return false;
            }
            return true;
        });

        this.markdownRenderer.setText(this.text);

        this.children.add(this.markdownRenderer);
        this.children.add(this.scrollArea);

    }

    /** Updates scroll-wheel permissions and scrollbar activation. */
    protected void tick() {

        //Update scroll wheel ONLY FOR vertical bar (horizontal never scrolls via scroll wheel)
        this.scrollArea.verticalScrollBar.setScrollWheelAllowed(this.allowScrolling);

        //Update active state of scroll bars
        this.scrollArea.verticalScrollBar.active = (this.scrollArea.getTotalEntryHeight() > this.scrollArea.getInnerHeight()) && this.allowScrolling;
        this.scrollArea.horizontalScrollBar.active = (this.scrollArea.getTotalEntryWidth() > this.scrollArea.getInnerWidth()) && this.allowScrolling;

    }

    /** {@inheritDoc} */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.tick();

        this.scrollArea.extractRenderState(graphics, mouseX, mouseY, partial);
        RenderingUtils.resetShaderColor(graphics);

    }

    /** Replaces the retained document source and forwards it to the active renderer. */
    public ScrollableMarkdownRenderer setText(@NotNull String text) {
        this.text = Objects.requireNonNull(text);
        this.markdownRenderer.setText(text);
        return this;
    }

    /** Enables or disables scrollbar activation and vertical wheel input. */
    public ScrollableMarkdownRenderer setScrollingAllowed(boolean allowed) {
        this.allowScrolling = allowed;
        return this;
    }

    /** Returns whether scrollbar activation and vertical wheel input are enabled. */
    public boolean isScrollingAllowed() {
        return this.allowScrolling;
    }

    /** Returns the active renderer instance created by the configured factory. */
    @NotNull
    public MarkdownRenderer getMarkdownRenderer() {
        return this.markdownRenderer;
    }

    /** Returns the active viewport and scrollbar controller. */
    @NotNull
    public ScrollArea getScrollArea() {
        return this.scrollArea;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull List<GuiEventListener> children() {
        return this.children;
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
    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return this.scrollArea;
    }

    /** {@inheritDoc} */
    @Override
    public void setFocused(@Nullable GuiEventListener var1) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        return this.scrollArea.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Forwards a coordinate-based release to the renderer and scroll area without consuming it. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.markdownRenderer.mouseReleased(mouseX, mouseY, button);
        this.scrollArea.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    /** Adapts a Markdown renderer to the scroll area's entry sizing and positioning contract. */
    protected static class MarkdownRendererEntry extends ScrollAreaEntry {

        /** Renderer positioned and measured by this entry. */
        protected MarkdownRenderer markdownRenderer;

        /** Creates a non-selectable transparent entry hosting the supplied renderer. */
        public MarkdownRendererEntry(ScrollArea parent, MarkdownRenderer markdownRenderer) {
            super(parent, 20, 20);
            this.markdownRenderer = markdownRenderer;
            this.selectable = false;
            this.playClickSound = false;
            this.backgroundColorNormal = () -> DrawableColor.of(0,0,0,0);
            this.backgroundColorHover = () -> DrawableColor.of(0,0,0,0);
        }

        /** {@inheritDoc} */
        @Override
        public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            this.markdownRenderer.setOptimalWidth(this.parent.getInnerWidth());
            this.markdownRenderer.setX(this.x);
            this.markdownRenderer.setY(this.y);
            this.setWidth(this.markdownRenderer.getRealWidth());
            this.setHeight(this.markdownRenderer.getRealHeight());
            this.markdownRenderer.extractRenderState(graphics, mouseX, mouseY, partial);
        }

        /** {@inheritDoc} */
        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }

    }

}
