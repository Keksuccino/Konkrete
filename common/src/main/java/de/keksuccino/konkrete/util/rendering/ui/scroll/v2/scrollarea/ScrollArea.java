package de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Displays clipped entries and routes focus, pointer input, narration, and scrolling. */
@SuppressWarnings("unused")
public class ScrollArea implements GuiEventListener, Renderable, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Scroll bar controlling vertical scroll bar. */
    public final ScrollBar verticalScrollBar;
    /** Scroll bar controlling horizontal scroll bar. */
    public final ScrollBar horizontalScrollBar;
    /** Horizontal component of the current transform. */
    protected float x;
    /** Vertical component of the current transform. */
    protected float y;
    /** Width in GUI units for width. */
    protected float width;
    /** Height in GUI units for height. */
    protected float height;
    /** Supplies the scroll-area background color for the current theme state. */
    @Nullable
    public Supplier<DrawableColor> backgroundColor = () -> this.setupForBlurInterface ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1 : UIBase.getUITheme().ui_interface_area_background_color_type_1;
    /** Supplies the scroll-area border color for the current theme state. */
    @Nullable
    public Supplier<DrawableColor> borderColor = () -> this.setupForBlurInterface ? UIBase.getUITheme().ui_blur_interface_area_border_color : UIBase.getUITheme().ui_interface_area_border_color;
    /** Border thickness in GUI pixels. */
    protected float borderThickness = 1;
    /** Whether each entry expands to the scroll-area width. */
    public boolean makeEntriesWidthOfArea = false;
    /** Whether an entry's minimum width is the scroll-area width. */
    public boolean minimumEntryWidthIsAreaWidth = true;
    /** Whether every entry adopts the widest entry width. */
    public boolean makeAllEntriesWidthOfWidestEntry = true;
    /** Scrollable entries in display order. */
    protected List<ScrollAreaEntry> entries = new ArrayList<>();
    /** Width in GUI units for overridden total scroll. */
    public float overriddenTotalScrollWidth = -1;
    /** Height in GUI units for overridden total scroll. */
    public float overriddenTotalScrollHeight = -1;
    /** Whether entry positions are recomputed after list changes. */
    public boolean correctYOnAddingRemovingEntries = true;
    /** Whether the pointer currently hovers this element. */
    protected boolean hovered = false;
    /** Whether the pointer is inside the scrollable content bounds. */
    protected boolean innerAreaHovered = false;
    /** Whether the scroll area uses rounded backgrounds and clipping. */
    protected boolean roundedStyle = true;
    /** Whether colors and bounds are configured for a blurred interface. */
    protected boolean setupForBlurInterface = false;
    /** Whether entries are clipped to the scroll-area bounds. */
    protected boolean scissorEnabled = true;
    /** Whether to skip extraction for entries outside the viewport. */
    protected boolean renderOnlyEntriesInArea = true;
    private int renderFrameId = 0;

    /** Defines a scrollable viewport in floating-point GUI coordinates. */
    public ScrollArea(float x, float y, float width, float height) {
        this.setX(x, true);
        this.setY(y, true);
        this.setWidth(width, true);
        this.setHeight(height, true);
        this.verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
        this.verticalScrollBar.setScrollWheelAllowed(true);
        this.verticalScrollBar.setRoundedGrabberEnabled(true);
        this.horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, UIBase.HORIZONTAL_SCROLL_BAR_WIDTH, UIBase.HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
        this.horizontalScrollBar.setRoundedGrabberEnabled(true);
        this.updateScrollArea();
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.hovered = this.isMouseOver(mouseX, mouseY);
        this.innerAreaHovered = this.isMouseOverInnerArea(mouseX, mouseY);

        this.updateScrollArea();
        this.updateWheelScrollSpeed();
        this.resetScrollOnFit();

        this.extractBackground(graphics, mouseX, mouseY, partial);

        this.renderEntries(graphics, mouseX, mouseY, partial);

        this.renderBorder(graphics, mouseX, mouseY, partial);

        if (this.isVerticalScrollBarVisible()) {
            this.verticalScrollBar.extractRenderState(graphics, mouseX, mouseY, partial);
        }
        if (this.isHorizontalScrollBarVisible()) {
            this.horizontalScrollBar.extractRenderState(graphics, mouseX, mouseY, partial);
        }

    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (this.backgroundColor == null) return;
        DrawableColor backColor = this.backgroundColor.get();
        if (backColor != null) {
            if (this.roundedStyle) {
                float radius = UIBase.getInterfaceCornerRoundingRadius();
                UIBase.renderRoundedRect(graphics, this.getInnerX(), this.getInnerY(), this.getInnerWidth(), this.getInnerHeight(), radius, radius, radius, radius, backColor.getColorInt());
            } else {
                UIBase.fillF(graphics, this.getInnerX(), this.getInnerY(), this.getInnerX() + this.getInnerWidth(), this.getInnerY() + this.getInnerHeight(), backColor.getColorInt());
            }
        }
    }

    /** Renders border into the active GUI extraction pass. */
    public void renderBorder(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (this.borderColor == null) return;
        DrawableColor borColor = this.borderColor.get();
        if (borColor != null) {
            if (this.roundedStyle) {
                float radius = UIBase.getInterfaceCornerRoundingRadius();
                UIBase.renderRoundedBorder(graphics, this.getXWithBorder(), this.getYWithBorder(), this.getXWithBorder() + this.getWidthWithBorder(), this.getYWithBorder() + this.getHeightWithBorder(), this.getBorderThickness(), radius, radius, radius, radius, borColor.getColorInt());
            } else {
                UIBase.renderBorder(graphics, this.getXWithBorder(), this.getYWithBorder(), this.getXWithBorder() + this.getWidthWithBorder(), this.getYWithBorder() + this.getHeightWithBorder(), this.getBorderThickness(), borColor.getColorInt(), true, true, true, true);
            }
        }
    }

    /** Renders entries into the active GUI extraction pass. */
    public void renderEntries(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.renderFrameId++;

        final float totalWidth = this.makeAllEntriesWidthOfWidestEntry ? this.getTotalEntryWidth() : 0;
        if (this.scissorEnabled) {
            int scissorMinX = (int) (this.getInnerX() + 2);
            int scissorMinY = (int) (this.getInnerY() + 2);
            int scissorMaxX = (int) (this.getInnerX() + this.getInnerWidth() - 2);
            int scissorMaxY = (int) (this.getInnerY() + this.getInnerHeight() - 2);
            graphics.enableScissor(scissorMinX, scissorMinY, scissorMaxX, scissorMaxY);
        }
        this.updateEntriesForRender((entry) -> {
            if (this.makeAllEntriesWidthOfWidestEntry) entry.setWidth(totalWidth);
            if (this.minimumEntryWidthIsAreaWidth && (entry.getWidth() < this.getInnerWidth())) {
                entry.setWidth(this.getInnerWidth());
            }
            if (!this.renderOnlyEntriesInArea || this.isEntryVisible(entry)) {
                entry.extractRenderState(graphics, mouseX, mouseY, partial);
            }
        });
        if (this.scissorEnabled) {
            graphics.disableScissor();
        }

    }

    /** Returns render frame ID. */
    public int getRenderFrameId() {
        return this.renderFrameId;
    }
    private boolean isEntryVisible(@NotNull ScrollAreaEntry entry) {
        float innerX = this.getInnerX();
        float innerY = this.getInnerY();
        float innerMaxX = innerX + this.getInnerWidth();
        float innerMaxY = innerY + this.getInnerHeight();
        float entryMinX = entry.getX();
        float entryMinY = entry.getY();
        float entryMaxX = entryMinX + entry.getWidth();
        float entryMaxY = entryMinY + entry.getHeight();
        return (entryMaxX > innerX) && (entryMinX < innerMaxX) && (entryMaxY > innerY) && (entryMinY < innerMaxY);
    }


    /** Returns entry render offset x. */
    public float getEntryRenderOffsetX() {
        return this.getEntryRenderOffsetX(this.getTotalScrollWidth());
    }

    /** Returns entry render offset y. */
    public float getEntryRenderOffsetY() {
        return this.getEntryRenderOffsetY(this.getTotalScrollHeight());
    }

    /** Returns entry render offset x. */
    public float getEntryRenderOffsetX(float totalScrollWidth) {
        return -((totalScrollWidth / 100.0F) * (this.horizontalScrollBar.getScroll() * 100.0F));
    }

    /** Returns entry render offset y. */
    public float getEntryRenderOffsetY(float totalScrollHeight) {
        return -((totalScrollHeight / 100.0F) * (this.verticalScrollBar.getScroll() * 100.0F));
    }

    /** Returns total scroll width. */
    public float getTotalScrollWidth() {
        if (this.overriddenTotalScrollWidth != -1) {
            return this.overriddenTotalScrollWidth;
        }
        return Math.max(0f, this.getTotalEntryWidth() - this.getInnerWidth());
    }

    /** Returns total scroll height. */
    public float getTotalScrollHeight() {
        if (this.overriddenTotalScrollHeight != -1) {
            return this.overriddenTotalScrollHeight;
        }
        return Math.max(0f, this.getTotalEntryHeight() - this.getInnerHeight());
    }

    /** Refreshes entries from current state. */
    public void updateEntries(@Nullable Consumer<ScrollAreaEntry> doAfterEachEntryUpdate) {
        this.updateEntriesInternal(doAfterEachEntryUpdate, false);
    }

    private void updateEntriesForRender(@NotNull Consumer<ScrollAreaEntry> doAfterEachEntryUpdate) {
        this.updateEntriesInternal(doAfterEachEntryUpdate, this.renderOnlyEntriesInArea);
    }

    private void updateEntriesInternal(@Nullable Consumer<ScrollAreaEntry> doAfterEachEntryUpdate, boolean stopAfterVisibleRange) {
        try {
            int index = 0;
            float y = this.getInnerY();
            float renderOffsetX = this.getEntryRenderOffsetX();
            float renderOffsetY = this.getEntryRenderOffsetY();
            float innerMaxY = this.getInnerY() + this.getInnerHeight();
            List<ScrollAreaEntry> l = new ArrayList<>(this.entries);
            for (ScrollAreaEntry e : l) {
                e.index = index;
                e.setX(this.getInnerX() + renderOffsetX);
                e.setY(y + renderOffsetY);
                if (this.makeEntriesWidthOfArea) {
                    e.setWidth(this.getInnerWidth());
                }
                if (doAfterEachEntryUpdate != null) {
                    doAfterEachEntryUpdate.accept(e);
                }
                index++;
                y += e.getHeight();
                if (stopAfterVisibleRange && ((y + renderOffsetY) > innerMaxY)) {
                    break;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to update entries!", ex);
        }
    }

    /** Refreshes scroll area from current state. */
    public void updateScrollArea() {

        this.verticalScrollBar.scrollAreaStartX = this.getInnerX() + 1;
        this.verticalScrollBar.scrollAreaStartY = this.getInnerY() + 1;
        this.verticalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - 1;
        this.verticalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - this.horizontalScrollBar.grabberHeight - 2;

        this.horizontalScrollBar.scrollAreaStartX = this.getInnerX() + 1;
        this.horizontalScrollBar.scrollAreaStartY = this.getInnerY() + 1;
        this.horizontalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - this.verticalScrollBar.grabberWidth - 2;
        this.horizontalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - 1;

    }

    /** Refreshes wheel scroll speed from current state. */
    public void updateWheelScrollSpeed() {
        //Adjust the scroll wheel speed depending on the amount of entries
        this.verticalScrollBar.setWheelScrollSpeed(1.0F / (this.getTotalScrollHeight() / 500.0F));
    }

    /** Clears scroll on fit state. */
    public void resetScrollOnFit() {
        //Reset scrolls if content fits area
        if (this.getTotalEntryWidth() <= this.getInnerWidth()) {
            this.horizontalScrollBar.setScroll(0.0F);
        }
        if (this.getTotalEntryHeight() <= this.getInnerHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }
    }

    /**
     * Corrects the Y scroll after removing or adding entries.
     * @param removed If TRUE, the entry list will be treated as list of REMOVED entries. If FALSE, the list is treated as ADDED entries.
     * @param addedOrRemovedEntries List of entries that were added or removed.
     */
    public void correctYScrollAfterAddingOrRemovingEntries(boolean removed, ScrollAreaEntry... addedOrRemovedEntries) {
        if ((addedOrRemovedEntries != null) && (addedOrRemovedEntries.length > 0)) {
            float oldTotalScrollHeight;
            int totalHeightRemovedAdded = 0;
            for (ScrollAreaEntry e : addedOrRemovedEntries) {
                totalHeightRemovedAdded += (int) e.getHeight();
            }
            if (!removed) {
                oldTotalScrollHeight = this.getTotalScrollHeight() - totalHeightRemovedAdded;
            } else {
                oldTotalScrollHeight = this.getTotalScrollHeight() + totalHeightRemovedAdded;
            }
            float yOld = this.getEntryRenderOffsetY(oldTotalScrollHeight);
            float yNew = this.getEntryRenderOffsetY();
            float yDiff = Math.max(yOld, yNew) - Math.min(yOld, yNew);
            if (this.getTotalScrollHeight() <= 0) {
                return;
            }
            float scrollDiff = Math.max(0f, Math.min(1f, yDiff / this.getTotalScrollHeight()));
            if (!removed) {
                scrollDiff = -scrollDiff;
            }
            this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() + scrollDiff);
        }
    }

    /** Returns whether mouse interacting with grabbers. */
    public boolean isMouseInteractingWithGrabbers() {
        return (this.isVerticalScrollBarVisible() && (this.verticalScrollBar.isGrabberGrabbed() || this.verticalScrollBar.isGrabberHovered()))
                || (this.isHorizontalScrollBarVisible() && (this.horizontalScrollBar.isGrabberGrabbed() || this.horizontalScrollBar.isGrabberHovered()));
    }

    /** Returns whether vertical scroll bar visible. */
    public boolean isVerticalScrollBarVisible() {
        return this.verticalScrollBar.active && (this.getTotalScrollHeight() > 0.0F);
    }

    /** Returns whether horizontal scroll bar visible. */
    public boolean isHorizontalScrollBarVisible() {
        return this.horizontalScrollBar.active && (this.getTotalScrollWidth() > 0.0F);
    }

    /** Sets x for this scroll area. */
    public void setX(float x, boolean respectBorder) {
        this.x = x;
        if (respectBorder) {
            this.x += this.borderThickness;
        }
    }

    /** Sets x for this scroll area. */
    public void setX(float x) {
        this.setX(x, true);
    }

    /** Returns inner x. */
    public float getInnerX() {
        return this.x;
    }

    /** Returns x with border. */
    public float getXWithBorder() {
        return this.x - this.borderThickness;
    }

    /** Sets y for this scroll area. */
    public void setY(float y, boolean respectBorder) {
        this.y = y;
        if (respectBorder) {
            this.y += this.borderThickness;
        }
    }

    /** Sets y for this scroll area. */
    public void setY(float y) {
        this.setY(y, true);
    }

    /** Returns inner y. */
    public float getInnerY() {
        return this.y;
    }

    /** Returns y with border. */
    public float getYWithBorder() {
        return this.y - this.borderThickness;
    }

    /** Sets width for this scroll area. */
    public void setWidth(float width, boolean respectBorder) {
        this.width = width;
        if (respectBorder) {
            this.width -= (this.borderThickness * 2);
        }
    }

    /** Sets width for this scroll area. */
    public void setWidth(float width) {
        this.setWidth(width, true);
    }

    /** Returns inner width. */
    public float getInnerWidth() {
        return this.width;
    }

    /** Returns the content width plus both borders in GUI units. */
    public float getWidthWithBorder() {
        return this.width + (this.borderThickness * 2);
    }

    /** Sets height for this scroll area. */
    public void setHeight(float height, boolean respectBorder) {
        this.height = height;
        if (respectBorder) {
            this.height -= (this.borderThickness * 2);
        }
    }

    /** Sets height for this scroll area. */
    public void setHeight(float height) {
        this.setHeight(height, true);
    }

    /** Returns inner height. */
    public float getInnerHeight() {
        return this.height;
    }

    /** Returns the content height plus both borders in GUI units. */
    public float getHeightWithBorder() {
        return this.height + (this.borderThickness * 2);
    }

    /** Sets border thickness for this scroll area. */
    public void setBorderThickness(float borderThickness) {
        this.borderThickness = borderThickness;
    }

    /** Returns border thickness. */
    public float getBorderThickness() {
        return this.borderThickness;
    }

    /** Returns whether inner area hovered. */
    public boolean isInnerAreaHovered() {
        return this.innerAreaHovered;
    }

    /** Reports whether the pointer currently hovers this element. */
    public boolean isHovered() {
        return this.hovered;
    }

    /** Returns whether rounded style. */
    public boolean isRoundedStyle() {
        return this.roundedStyle;
    }

    /** Sets rounded style enabled for this scroll area. */
    public ScrollArea setRoundedStyleEnabled(boolean rounded) {
        this.roundedStyle = rounded;
        this.verticalScrollBar.setRoundedGrabberEnabled(rounded);
        this.horizontalScrollBar.setRoundedGrabberEnabled(rounded);
        return this;
    }

    /** Returns whether setup for blur interface. */
    public boolean isSetupForBlurInterface() {
        return this.setupForBlurInterface;
    }

    /** Sets setup for blur interface for this value. */
    public ScrollArea setSetupForBlurInterface(boolean setupForBlurInterface) {
        this.setupForBlurInterface = setupForBlurInterface;
        return this;
    }

    /** Sets scissor enabled for this value. */
    public ScrollArea setScissorEnabled(boolean scissorEnabled) {
        this.scissorEnabled = scissorEnabled;
        return this;
    }

    /** Returns whether scissor enabled. */
    public boolean isScissorEnabled() {
        return this.scissorEnabled;
    }

    /**
     * Controls whether entries are culled based on their current size and the visible area.
     * Disabling this can help when entries determine their size during render (e.g., markdown)
     * so they still get a chance to measure themselves, at the cost of extra rendering work.
     */
    public ScrollArea setRenderOnlyEntriesInArea(boolean renderOnlyEntriesInArea) {
        this.renderOnlyEntriesInArea = renderOnlyEntriesInArea;
        return this;
    }

    /** Returns whether render only entries in area. */
    public boolean isRenderOnlyEntriesInArea() {
        return this.renderOnlyEntriesInArea;
    }

    /** Returns whether mouse over inner area. */
    public boolean isMouseOverInnerArea(double mouseX, double mouseY) {
        return UIBase.isXYInArea(mouseX, mouseY, this.getInnerX(), this.getInnerY(), this.getInnerWidth(), this.getInnerHeight());
    }

    /** Reports whether the current pointer position lies inside this element's hitbox. */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.isMouseInteractingWithGrabbers()) return true;
        return UIBase.isXYInArea(mouseX, mouseY, this.getXWithBorder(), this.getYWithBorder(), this.getWidthWithBorder(), this.getHeightWithBorder());
    }

    /** Returns entry count. */
    public int getEntryCount() {
        return this.entries.size();
    }

    /** Returns total entry width. */
    public float getTotalEntryWidth() {
        float i = this.width;
        for (ScrollAreaEntry e : this.entries) {
            if (e.getWidth() > i) {
                i = e.getWidth();
            }
        }
        return i;
    }

    /** Returns total entry height. */
    public float getTotalEntryHeight() {
        float i = 0;
        for (ScrollAreaEntry e : this.entries) {
            i += e.getHeight();
        }
        return i;
    }

    /** Returns focused entry. */
    @Nullable
    public ScrollAreaEntry getFocusedEntry() {
        for (ScrollAreaEntry e : this.entries) {
            if (e.isSelected()) {
                return e;
            }
        }
        return null;
    }

    /**
     * @return The index of the focused entry or -1 if no entry is focused.
     */
    public int getFocusedEntryIndex() {
        ScrollAreaEntry e = this.getFocusedEntry();
        if (e != null) {
            return this.getIndexOfEntry(e);
        }
        return -1;
    }

    /** Returns entries in their current display order. */
    public List<ScrollAreaEntry> getEntries() {
        return new ArrayList<>(this.entries);
    }

    /** Returns the matching entry, or {@code null} when absent. */
    @Nullable
    public ScrollAreaEntry getEntry(int index) {
        if (index <= this.entries.size()-1) {
            return this.entries.get(index);
        }
        return null;
    }

    /** Adds entry to this value. */
    public void addEntry(ScrollAreaEntry entry) {
        if (!this.entries.contains(entry)) {
            this.entries.add(entry);
            if (this.correctYOnAddingRemovingEntries) {
                this.correctYScrollAfterAddingOrRemovingEntries(false, entry);
            }
        }
        this.makeCurrentEntriesSameWidth();
    }

    /** Adds entry at index to this value. */
    public void addEntryAtIndex(ScrollAreaEntry entry, int index) {
        if (index > this.getEntryCount()) {
            index = this.getEntryCount();
        }
        this.entries.add(index, entry);
        if (this.correctYOnAddingRemovingEntries) {
            this.correctYScrollAfterAddingOrRemovingEntries(false, entry);
        }
        this.makeCurrentEntriesSameWidth();
    }

    /** Removes entry from this value. */
    public void removeEntry(ScrollAreaEntry entry) {
        this.entries.remove(entry);
        if (this.correctYOnAddingRemovingEntries) {
            this.correctYScrollAfterAddingOrRemovingEntries(true, entry);
        }
        this.makeCurrentEntriesSameWidth();
    }

    /** Removes entry at index from this value. */
    public void removeEntryAtIndex(int index) {
        if (index <= this.getEntryCount()-1) {
            ScrollAreaEntry entry = this.entries.remove(index);
            if ((entry != null) && this.correctYOnAddingRemovingEntries) {
                this.correctYScrollAfterAddingOrRemovingEntries(true, entry);
            }
        }
        this.makeCurrentEntriesSameWidth();
    }

    /** Clears entries state. */
    public void clearEntries() {
        this.entries.clear();
        this.verticalScrollBar.setScroll(0.0F);
        this.horizontalScrollBar.setScroll(0.0F);
    }

    /**
     * @return The index of the entry or -1 if the entry is not part of the ScrollArea.
     */
    public int getIndexOfEntry(ScrollAreaEntry entry) {
        return this.entries.indexOf(entry);
    }

    /** Expands current entries to a shared maximum width. */
    public void makeCurrentEntriesSameWidth() {
        float totalWidth = this.getTotalEntryWidth();
        for (ScrollAreaEntry e : this.getEntries()) {
            e.setWidth(totalWidth);
        }
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isVerticalScrollBarVisible() && this.verticalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.isHorizontalScrollBarVisible() && this.horizontalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
        for (ScrollAreaEntry entry : this.entries) {
            if (entry.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isVerticalScrollBarVisible() && this.verticalScrollBar.mouseReleased(mouseX, mouseY, button)) return true;
        if (this.isHorizontalScrollBarVisible() && this.horizontalScrollBar.mouseReleased(mouseX, mouseY, button)) return true;
        return false;
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double $$3, double $$4) {
        return this.mouseDragged(event.x(), event.y(), event.button(), $$3, $$4);
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isVerticalScrollBarVisible() && this.verticalScrollBar.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (this.isHorizontalScrollBarVisible() && this.horizontalScrollBar.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return false;
    }

    /** Routes wheel scrolling and reports whether it was consumed. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (this.isVerticalScrollBarVisible() && this.verticalScrollBar.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        if (this.isHorizontalScrollBarVisible() && this.horizontalScrollBar.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        return false;
    }

    /** Sets focused for this value. */
    @Override
    public void setFocused(boolean var1) {
    }

    /** Reports whether keyboard focus currently targets this control. */
    @Override
    public boolean isFocused() {
        return false;
    }

    /** Returns this component's priority in the narration order. */
    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    /** Refreshes narration from current state. */
    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

}
