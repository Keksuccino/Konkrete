package de.keksuccino.konkrete.util.rendering.ui.menubar.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.GuiBlurRenderer;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.PressState;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.UIConfiguration;
import de.keksuccino.konkrete.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIconTexture;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.konkrete.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.ListUtils;
import de.keksuccino.konkrete.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Renders a horizontal entry strip and routes focus, clicks, and deferred context-menu opens. */
@SuppressWarnings("unused")
public class MenuBar implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Pixel size in GUI pixels. */
    public static final int PIXEL_SIZE = 28;
    /** Horizontal GUI coordinate for entry label space left. */
    public static final int ENTRY_LABEL_SPACE_LEFT_RIGHT = 6;

    /** Entries laid out from the left edge. */
    protected final List<MenuBarEntry> leftEntries = new ArrayList<>();
    /** Entries laid out from the right edge. */
    protected final List<MenuBarEntry> rightEntries = new ArrayList<>();
    /** Listeners notified after an entry is activated. */
    protected final List<MenuBarClickListener> clickListeners = new ArrayList<>();
    /** Context-menu opens deferred until the current input dispatch finishes. */
    protected final List<PendingContextMenuOpen> pendingContextMenuOpens = new ArrayList<>();
    /** Resource identifier for collapse icon. */
    protected static final MaterialIconTexture COLLAPSE_ICON_TEXTURE = new MaterialIconTexture(MaterialIcons.UNFOLD_LESS_DOUBLE);
    /** Resource identifier for expand icon. */
    protected static final MaterialIconTexture EXPAND_ICON_TEXTURE = new MaterialIconTexture(MaterialIcons.UNFOLD_MORE_DOUBLE);
    /** Whether the pointer currently hovers this element. */
    protected boolean hovered = false;
    /** Whether the menu bar displays its entries. */
    protected boolean expanded = true;
    /** Whether entry bounds are ready for rendering and input. */
    protected boolean layoutReady = false;
    /** Entry toggling the bar between collapsed and expanded layouts. */
    protected ClickableMenuBarEntry collapseOrExpandEntry;
    /** Whether the current pointer press may activate an entry. */
    protected boolean clickActive = false;
    /** Mouse button captured by the current press, or {@code -1}. */
    protected int clickActiveButton = -1;

    /** Creates an empty menu bar with default state. */
    public MenuBar() {
        this.collapseOrExpandEntry = this.addClickableEntry(Side.RIGHT, "collapse_or_expand", Component.empty(), (bar, entry) -> {
            this.setExpanded(!this.expanded);
        }).setIconTextureSupplier((bar, entry) -> this.expanded ? COLLAPSE_ICON_TEXTURE : EXPAND_ICON_TEXTURE)
                .setIconPaddingSupplier(entry -> 4)
                .setBaseWidth(16);
        this.addSpacerEntry(Side.RIGHT, "spacer_after_collapse_or_expand_entry").setWidth(10);
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        UIBase.startUIScaleRendering();
        try {
            float scale = getRenderScale();
            int scaledMouseX = (int) ((float)mouseX / scale);
            int scaledMouseY = (int) ((float)mouseY / scale);
            int y = 0;
            int width = ScreenUtils.getScreenWidth();
            int scaledWidth = (width != 0) ? (int)((float)width / scale) : 0;

            this.collapseOrExpandEntry.x = scaledWidth - this.collapseOrExpandEntry.getWidth();
            this.collapseOrExpandEntry.y = y;
            this.collapseOrExpandEntry.height = PIXEL_SIZE;
            this.collapseOrExpandEntry.hovered = this.collapseOrExpandEntry.isMouseOver(scaledMouseX, scaledMouseY);

            this.hovered = this.isMouseOver(mouseX, mouseY);

            RenderingUtils.setDepthTestLocked(true);

            UIBase.resetShaderColor(graphics);

            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);

            if (this.expanded) {
                this.extractBackground(graphics, 0, y, scaledWidth, PIXEL_SIZE, partial, scale);
            } else {
                this.extractBackground(graphics, this.collapseOrExpandEntry.x, y, this.collapseOrExpandEntry.x + this.collapseOrExpandEntry.getWidth(), PIXEL_SIZE, partial, scale);
            }

            this.layoutReady = false;
            if (this.expanded) {
                //Render all visible entries
                int leftX = 0;
                for (MenuBarEntry e : this.leftEntries) {
                    e.x = leftX;
                    e.y = y;
                    e.height = PIXEL_SIZE;
                    e.hovered = e.isMouseOver(scaledMouseX, scaledMouseY);
                    if (e.isVisible()) {
                        UIBase.resetShaderColor(graphics);
                        e.extractRenderState(graphics, scaledMouseX, scaledMouseY, partial);
                    }
                    leftX += e.getWidth();
                }
                int rightX = scaledWidth;
                for (MenuBarEntry e : this.rightEntries) {
                    e.x = rightX - e.getWidth();
                    e.y = y;
                    e.height = PIXEL_SIZE;
                    e.hovered = e.isMouseOver(scaledMouseX, scaledMouseY);
                    if (e.isVisible()) {
                        UIBase.resetShaderColor(graphics);
                        e.extractRenderState(graphics, scaledMouseX, scaledMouseY, partial);
                    }
                    rightX -= e.getWidth();
                }
                this.layoutReady = true;
            } else {
                this.collapseOrExpandEntry.extractRenderState(graphics, scaledMouseX, scaledMouseY, partial);
            }

            this.flushPendingContextMenuOpens();

            if (this.expanded) {
                this.renderBottomLine(graphics, scaledWidth);
            } else {
                this.renderExpandEntryBorder(graphics, scaledWidth);
            }

            graphics.pose().popMatrix();

            RenderingUtils.setDepthTestLocked(false);
            UIBase.resetShaderColor(graphics);
        } finally {
            UIBase.stopUIScaleRendering();
        }

    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    protected void extractBackground(GuiGraphicsExtractor graphics, int xMin, int yMin, int xMax, int yMax, float partial, float scale) {
        boolean blurEnabled = UIBase.shouldBlur();
        int widthScaled = xMax - xMin;
        int heightScaled = yMax - yMin;
        if (!blurEnabled) {
            // Classic solid background in the menu bar's scaled coordinate space.
            graphics.fill(xMin, yMin, xMax, yMax, UIBase.getUITheme().ui_overlay_background_color.getColorInt());
            UIBase.resetShaderColor(graphics);
            return;
        }

        // Menu bar is rendered at a custom scale; convert back to screen-space for blur so the blurred region matches what users see.
        float width = widthScaled * scale;
        float height = heightScaled * scale;
        float blurX = xMin * scale;
        float blurY = yMin * scale;
        if (width > 0 && height > 0) {
            // Blur the menu bar background without rounded corners; use the UI theme color as a light tint.
            GuiBlurRenderer.renderBlurAreaWithIntensity(graphics, blurX, blurY, width, height, UIBase.getBlurRadius(), 0.0F, UIBase.getUITheme().ui_blur_overlay_background_tint, partial);
        }
        UIBase.resetShaderColor(graphics);
    }

    /** Renders bottom line into the active GUI extraction pass. */
    protected void renderBottomLine(GuiGraphicsExtractor graphics, int width) {
        graphics.fill(0, MenuBar.PIXEL_SIZE - this.getBottomLineThickness(), width, MenuBar.PIXEL_SIZE, this.getBottomLineColor().getColorInt());
        UIBase.resetShaderColor(graphics);
    }

    /** Renders expand entry border into the active GUI extraction pass. */
    protected void renderExpandEntryBorder(GuiGraphicsExtractor graphics, int width) {
        //bottom line
        graphics.fill(this.collapseOrExpandEntry.x, MenuBar.PIXEL_SIZE - this.getBottomLineThickness(), width, MenuBar.PIXEL_SIZE, this.getBottomLineColor().getColorInt());
        //left side line
        graphics.fill(this.collapseOrExpandEntry.x - this.getBottomLineThickness(), 0, this.collapseOrExpandEntry.x, MenuBar.PIXEL_SIZE, this.getBottomLineColor().getColorInt());
        UIBase.resetShaderColor(graphics);
    }

    /** Returns bottom line color. */
    protected DrawableColor getBottomLineColor() {
        return UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_border_color : UIBase.getUITheme().ui_overlay_border_color;
    }

    /** Adds spacer entry after to this menu bar. */
    @NotNull
    public SpacerMenuBarEntry addSpacerEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier) {
        return this.addEntryAfter(addAfterIdentifier, new SpacerMenuBarEntry(identifier, this));
    }

    /** Adds spacer entry before to this menu bar. */
    @NotNull
    public SpacerMenuBarEntry addSpacerEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier) {
        return this.addEntryBefore(addBeforeIdentifier, new SpacerMenuBarEntry(identifier, this));
    }

    /** Adds spacer entry to this menu bar. */
    @NotNull
    public SpacerMenuBarEntry addSpacerEntry(@NotNull Side side, @NotNull String identifier) {
        return this.addEntry(side, new SpacerMenuBarEntry(identifier, this));
    }

    /** Inserts a flexible spacer at the requested index. */
    @NotNull
    public SpacerMenuBarEntry addSpacerEntryAt(int index, @NotNull Side side, @NotNull String identifier) {
        return this.addEntryAt(index, side, new SpacerMenuBarEntry(identifier, this));
    }

    /** Adds separator entry after to this menu bar. */
    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier) {
        return this.addEntryAfter(addAfterIdentifier, new SeparatorMenuBarEntry(identifier, this));
    }

    /** Adds separator entry before to this menu bar. */
    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier) {
        return this.addEntryBefore(addBeforeIdentifier, new SeparatorMenuBarEntry(identifier, this));
    }

    /** Adds separator entry to this menu bar. */
    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntry(@NotNull Side side, @NotNull String identifier) {
        return this.addEntry(side, new SeparatorMenuBarEntry(identifier, this));
    }

    /** Inserts a separator at the requested index. */
    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntryAt(int index, @NotNull Side side, @NotNull String identifier) {
        return this.addEntryAt(index, side, new SeparatorMenuBarEntry(identifier, this));
    }

    /**
     * {@link ContextMenuBarEntry}s should only get added to the LEFT {@link Side} of the {@link MenuBar}.
     */
    @NotNull
    public ContextMenuBarEntry addContextMenuEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return this.addEntryAfter(addAfterIdentifier, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    /**
     * {@link ContextMenuBarEntry}s should only get added to the LEFT {@link Side} of the {@link MenuBar}.
     */
    @NotNull
    public ContextMenuBarEntry addContextMenuEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return this.addEntryBefore(addBeforeIdentifier, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    /**
     * {@link ContextMenuBarEntry}s should only get added to the LEFT {@link Side} of the {@link MenuBar}.
     */
    @NotNull
    public ContextMenuBarEntry addContextMenuEntry(@NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return this.addEntry(Side.LEFT, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    /**
     * {@link ContextMenuBarEntry}s should only get added to the LEFT {@link Side} of the {@link MenuBar}.
     */
    @NotNull
    public ContextMenuBarEntry addContextMenuEntryAt(int index, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return this.addEntryAt(index, Side.LEFT, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    /** Adds clickable entry after to this menu bar. */
    @NotNull
    public ClickableMenuBarEntry addClickableEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return this.addEntryAfter(addAfterIdentifier, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    /** Adds clickable entry before to this menu bar. */
    @NotNull
    public ClickableMenuBarEntry addClickableEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return this.addEntryBefore(addBeforeIdentifier, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    /** Adds clickable entry to this menu bar. */
    @NotNull
    public ClickableMenuBarEntry addClickableEntry(@NotNull Side side, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return this.addEntry(side, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    /** Inserts a clickable entry at the requested index. */
    @NotNull
    public ClickableMenuBarEntry addClickableEntryAt(int index, @NotNull Side side, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return this.addEntryAt(index, side, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    /** Adds entry after to this menu bar. */
    @NotNull
    public <T extends MenuBarEntry> T addEntryAfter(@NotNull String addAfterIdentifier, @NotNull T entry) {
        Objects.requireNonNull(addAfterIdentifier);
        int index = this.getEntryIndex(addAfterIdentifier);
        Side side = this.getEntrySide(addAfterIdentifier);
        if ((index >= 0) && (side != null)) {
            index++;
        } else {
            LOGGER.error("[KONKRETE] Failed to add MenuBar entry (" + entry.identifier + ") after other entry (" + addAfterIdentifier + ")! Target entry not found! Will add the entry at the end of left side instead!");
            index = this.leftEntries.size();
            side = Side.LEFT;
        }
        return this.addEntryAt(index, side, entry);
    }

    /** Adds entry before to this menu bar. */
    @NotNull
    public <T extends MenuBarEntry> T addEntryBefore(@NotNull String addBeforeIdentifier, @NotNull T entry) {
        Objects.requireNonNull(addBeforeIdentifier);
        int index = this.getEntryIndex(addBeforeIdentifier);
        Side side = this.getEntrySide(addBeforeIdentifier);
        if ((index < 0) || (side == null)) {
            LOGGER.error("[KONKRETE] Failed to add MenuBar entry (" + entry.identifier + ") before other entry (" + addBeforeIdentifier + ")! Target entry not found! Will add the entry at the end of left side instead!");
            index = this.leftEntries.size();
            side = Side.LEFT;
        }
        return this.addEntryAt(index, side, entry);
    }

    /** Adds entry to this menu bar. */
    @NotNull
    public <T extends MenuBarEntry> T addEntry(@NotNull Side side, @NotNull T entry) {
        int index = (side == Side.LEFT) ? this.leftEntries.size() : this.rightEntries.size();
        return this.addEntryAt(index, side, entry);
    }

    /** Inserts an existing entry at the requested index. */
    @NotNull
    public <T extends MenuBarEntry> T addEntryAt(int index, @NotNull Side side, @NotNull T entry) {
        Objects.requireNonNull(side);
        Objects.requireNonNull(entry);
        Objects.requireNonNull(entry.identifier);
        if (this.hasEntry(entry.identifier)) {
            LOGGER.error("[KONKRETE] Failed to add MenuBar entry! Identifier already in use: " + entry.identifier);
        } else {
            if (side == Side.LEFT) {
                this.leftEntries.add(Math.max(0, Math.min(index, this.leftEntries.size())), entry);
            }
            if (side == Side.RIGHT) {
                this.rightEntries.add(Math.max(0, Math.min(index, this.rightEntries.size())), entry);
            }
            this.markLayoutDirty();
        }
        return entry;
    }

    /** Removes entry from this menu bar. */
    public MenuBar removeEntry(@NotNull String identifier) {
        MenuBarEntry e = this.getEntry(identifier);
        if (e != null) {
            this.leftEntries.remove(e);
            this.rightEntries.remove(e);
            this.markLayoutDirty();
        }
        return this;
    }

    /** Clears left entries state. */
    public MenuBar clearLeftEntries() {
        this.leftEntries.clear();
        this.markLayoutDirty();
        return this;
    }

    /** Clears right entries state. */
    public MenuBar clearRightEntries() {
        this.rightEntries.clear();
        this.markLayoutDirty();
        return this;
    }

    /** Clears entries state. */
    public MenuBar clearEntries() {
        this.leftEntries.clear();
        this.rightEntries.clear();
        this.markLayoutDirty();
        return this;
    }

    /** Returns entry index. */
    public int getEntryIndex(@NotNull String identifier) {
        MenuBarEntry e = this.getEntry(identifier);
        if (e != null) {
            int index = this.leftEntries.indexOf(e);
            if (index == -1) index = this.rightEntries.indexOf(e);
            return index;
        }
        return -1;
    }

    /** Returns entry side. */
    @Nullable
    public Side getEntrySide(@NotNull String identifier) {
        MenuBarEntry e = this.getEntry(identifier);
        if (e != null) {
            if (this.leftEntries.contains(e)) return Side.LEFT;
            return Side.RIGHT;
        }
        return null;
    }

    /** Returns the matching entry, or {@code null} when absent. */
    @Nullable
    public MenuBarEntry getEntry(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        for (MenuBarEntry e : this.getEntries()) {
            if (e.identifier.equals(identifier)) return e;
        }
        return null;
    }

    /** Returns whether entry. */
    public boolean hasEntry(@NotNull String identifier) {
        return this.getEntry(identifier) != null;
    }

    /** Returns left entries. */
    @NotNull
    public List<MenuBarEntry> getLeftEntries() {
        return new ArrayList<>(this.leftEntries);
    }

    /** Returns right entries. */
    @NotNull
    public List<MenuBarEntry> getRightEntries() {
        return new ArrayList<>(this.rightEntries);
    }

    /** Returns entries in their current display order. */
    @NotNull
    public List<MenuBarEntry> getEntries() {
        return ListUtils.mergeLists(this.leftEntries, this.rightEntries);
    }

    /** Returns bottom line thickness. */
    public int getBottomLineThickness() {
        return 1;
    }

    /** Reports whether the pointer currently hovers this element. */
    public boolean isHovered() {
        return this.hovered;
    }

    /** Returns whether user navigating in menu bar. */
    public boolean isUserNavigatingInMenuBar() {
        if (this.isHovered()) return true;
        for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
            if (e instanceof ContextMenuBarEntry c) {
                if (c.contextMenu.isUserNavigatingInMenu()) return true;
            }
        }
        return false;
    }

    /** Returns whether entry context menu open. */
    public boolean isEntryContextMenuOpen() {
        for (MenuBarEntry e : this.getEntries()) {
            if (e instanceof ContextMenuBarEntry c) {
                if (c.contextMenu.isOpen()) return true;
            }
        }
        return false;
    }

    /** Adds click listener to this menu bar. */
    public MenuBar addClickListener(@NotNull MenuBarClickListener listener) {
        this.clickListeners.add(listener);
        return this;
    }

    /** Removes click listener from this menu bar. */
    public MenuBar removeClickListener(@NotNull MenuBarClickListener listener) {
        this.clickListeners.remove(listener);
        return this;
    }

    /** Clears click listeners state. */
    public MenuBar clearClickListeners() {
        this.clickListeners.clear();
        return this;
    }

    /** Closes all context menus. */
    public MenuBar closeAllContextMenus() {
        for (MenuBarEntry e : this.getEntries()) {
            if (e instanceof ContextMenuBarEntry c) {
                c.contextMenu.closeMenu();
            }
        }
        return this;
    }

    /** Returns whether expanded. */
    public boolean isExpanded() {
        return this.expanded;
    }

    /** Sets expanded for this menu bar. */
    public MenuBar setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (!this.expanded) this.closeAllContextMenus();
        this.markLayoutDirty();
        return this;
    }

    /** Sets focused for this menu bar. */
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
    @NotNull
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    /** Refreshes narration from current state. */
    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = getRenderScale();
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        boolean entryClick = false;
        if (this.expanded) {
            for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
                if (e.isVisible()) {
                    if (e.mouseClicked(scaledMouseX, scaledMouseY, button)) entryClick = true;
                }
            }
        } else {
            if (this.collapseOrExpandEntry.mouseClicked(scaledMouseX, scaledMouseY, button)) entryClick = true;
        }
        if (this.isUserNavigatingInMenuBar() || entryClick) {
            fireClickListeners(button, PressState.PRESSED);
            this.clickActive = true;
            this.clickActiveButton = button;
            Screen current = ScreenUtils.getScreen();
            if (current != null) {
                current.clearFocus();
            }
            return true;
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
        if (this.clickActive && this.clickActiveButton == button) {
            fireClickListeners(button, PressState.RELEASED);
            this.clickActive = false;
            this.clickActiveButton = -1;
        }
        return GuiEventListener.super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
    }

    /** Routes wheel scrolling and reports whether it was consumed. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        float scale = getRenderScale();
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        boolean entryClick = false;
        if (this.expanded) {
            for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
                if (e.isVisible()) {
                    if (e.mouseScrolled(scaledMouseX, scaledMouseY, scrollDeltaX, scrollDeltaY)) return true;
                }
            }
        }
        return false;
    }

    /** Reports whether the current pointer position lies inside this element's hitbox. */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.expanded) return this.collapseOrExpandEntry.hovered;
        float scale = getRenderScale();
        int width = ScreenUtils.getScreenWidth();
        int scaledHeight = (MenuBar.PIXEL_SIZE != 0) ? (int)((float)MenuBar.PIXEL_SIZE * scale) : 0;
        return UIBase.isXYInArea((int)mouseX, (int)mouseY, 0, 0, width, scaledHeight);
    }

    /** Reports whether this control may receive keyboard focus. */
    @Override
    public boolean isFocusable() {
        return false;
    }

    /** Sets focusable for this menu bar. */
    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("MenuBars are not focusable!");
    }

    /** Reports whether keyboard navigation may target this control. */
    @Override
    public boolean isNavigatable() {
        return false;
    }

    /** Sets navigatable for this menu bar. */
    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("ContextMenus are not navigatable!");
    }

    private void fireClickListeners(int button, @NotNull PressState state) {
        if (this.clickListeners.isEmpty()) {
            return;
        }
        for (MenuBarClickListener listener : new ArrayList<>(this.clickListeners)) {
            listener.onClick(button, state);
        }
    }

    /** Receives menu bar click lifecycle notifications. */
    @FunctionalInterface
    public interface MenuBarClickListener {

        /** Receives the pressed button and resulting press state. */
        void onClick(int button, @NotNull PressState state);

    }

    /** Returns base scale. */
    public static float getBaseScale() {
        return UIBase.getUIScale();
    }

    /** Returns render scale. */
    public static float getRenderScale() {
        return UIBase.calculateFixedRenderScale(getBaseScale());
    }

    /** Marks layout dirty for the next lifecycle phase. */
    protected void markLayoutDirty() {
        this.layoutReady = false;
    }

    /** Returns whether defer context menu open. */
    protected boolean shouldDeferContextMenuOpen() {
        return !this.layoutReady;
    }

    /** Defers a context-menu open until current input dispatch completes. */
    protected void queueContextMenuOpen(@NotNull ContextMenuBarEntry entry, @Nullable List<String> entryPath) {
        for (int i = 0; i < this.pendingContextMenuOpens.size(); i++) {
            PendingContextMenuOpen pending = this.pendingContextMenuOpens.get(i);
            if (pending.entry == entry) {
                this.pendingContextMenuOpens.set(i, new PendingContextMenuOpen(entry, entryPath));
                return;
            }
        }
        this.pendingContextMenuOpens.add(new PendingContextMenuOpen(entry, entryPath));
    }

    /** Opens and clears every context menu deferred during input dispatch. */
    protected void flushPendingContextMenuOpens() {
        if (!this.layoutReady || this.pendingContextMenuOpens.isEmpty()) {
            return;
        }
        List<PendingContextMenuOpen> pending = new ArrayList<>(this.pendingContextMenuOpens);
        this.pendingContextMenuOpens.clear();
        for (PendingContextMenuOpen open : pending) {
            if (this.hasEntry(open.entry.identifier)) {
                open.entry.openContextMenuInternal(open.entryPath);
            }
        }
    }

    /** Base implementation for menu bar entry. */
    public static abstract class MenuBarEntry implements Renderable, GuiEventListener {

        /** Stable identifier used for entry lookup and replacement. */
        protected final String identifier;
        /** Menu bar that owns and positions this entry. */
        @NotNull
        protected MenuBar parent;
        /** Horizontal component of the current transform. */
        protected int x;
        /** Vertical component of the current transform. */
        protected int y;
        /** Height in GUI units for height. */
        protected int height;
        /** Width in GUI units for base. */
        protected int baseWidth = 2;
        /** Whether the pointer currently hovers this element. */
        protected boolean hovered = false;
        /** Computes whether the control is active. */
        protected MenuBarEntryBooleanSupplier activeSupplier;
        /** Computes whether the control is visible. */
        protected MenuBarEntryBooleanSupplier visibleSupplier;
        /** Optionally supplies the tooltip for the current entry state. */
        @Nullable
        protected ConsumingSupplier<MenuBarEntry, UITooltip> tooltipSupplier;

        /** Attaches a stable entry identifier to its owning menu bar. */
        public MenuBarEntry(@NotNull String identifier, @NotNull MenuBar parent) {
            this.identifier = identifier;
            this.parent = parent;
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            this.renderEntry(graphics, mouseX, mouseY, partial);
            if (this.hovered && (this.tooltipSupplier != null)) {
                UITooltip tooltip = this.tooltipSupplier.get(this);
                if (tooltip != null) {
                    TooltipHandler.INSTANCE.addRenderTickTooltip(tooltip, () -> true);
                }
            }
        }

        /** Renders entry into the active GUI extraction pass. */
        protected abstract void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial);

        /** Returns the containing menu, widget, or entry. */
        public @NotNull MenuBar getParent() {
            return parent;
        }

        /** Returns the current width in GUI units. */
        protected int getWidth() {
            return Math.max(1, this.baseWidth);
        }

        /** Reports whether the pointer currently hovers this element. */
        public boolean isHovered() {
            return this.hovered;
        }

        /** Reports whether this control accepts interaction. */
        public boolean isActive() {
            return (this.activeSupplier == null) || this.activeSupplier.get(this.parent, this);
        }

        /** Sets active for this menu bar entry. */
        public MenuBarEntry setActive(boolean active) {
            this.activeSupplier = (menuBar, entry) -> active;
            return this;
        }

        /** Sets active supplier for this menu bar entry. */
        public MenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            this.activeSupplier = activeSupplier;
            return this;
        }

        /** Reports whether this element participates in rendering and input. */
        public boolean isVisible() {
            return (this.visibleSupplier == null) || this.visibleSupplier.get(this.parent, this);
        }

        /** Sets visible for this menu bar entry. */
        public MenuBarEntry setVisible(boolean visible) {
            this.visibleSupplier = (menuBar, entry) -> visible;
            return this;
        }

        /** Sets visible supplier for this menu bar entry. */
        public MenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            this.visibleSupplier = visibleSupplier;
            return this;
        }

        /** Sets tooltip supplier for this menu bar entry. */
        public MenuBarEntry setTooltipSupplier(@Nullable ConsumingSupplier<MenuBarEntry, UITooltip> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        /** Sets base width for this menu bar entry. */
        public MenuBarEntry setBaseWidth(int baseWidth) {
            this.baseWidth = Math.max(1, baseWidth);
            return this;
        }

        /** Returns the stable identifier used for registry or entry lookup. */
        @NotNull
        public String getIdentifier() {
            return this.identifier;
        }

        /** Sets focused for this menu bar entry. */
        @Override
        public void setFocused(boolean var1) {
        }

        /** Reports whether keyboard focus currently targets this control. */
        @Override
        public boolean isFocused() {
            return false;
        }

        /** Reports whether the current pointer position lies inside this element's hitbox. */
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return UIBase.isXYInArea((int) mouseX, (int) mouseY, this.x, this.y, this.getWidth(), PIXEL_SIZE);
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return GuiEventListener.super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
        }

        /** Supplies menu bar entry boolean values on demand. */
        @FunctionalInterface
        public interface MenuBarEntryBooleanSupplier {

            /** Supplies a boolean using the current bar and entry state. */
            boolean get(MenuBar bar, MenuBarEntry entry);

        }

        /** Supplies menu bar entry values on demand. */
        @FunctionalInterface
        public interface MenuBarEntrySupplier<T> {

            /** Supplies a value using the current bar and entry state. */
            T get(MenuBar bar, MenuBarEntry entry);

        }

    }

    /** Represents one renderable, focusable clickable menu bar entry. */
    public static class ClickableMenuBarEntry extends MenuBarEntry {

        /** Supplies the label for the current entry state. */
        @NotNull
        protected MenuBarEntrySupplier<Component> labelSupplier;
        /** Optionally supplies the entry icon texture. */
        @Nullable
        protected MenuBarEntrySupplier<ITexture> iconTextureSupplier;
        /** Supplies the entry icon tint. */
        @Nullable
        protected Supplier<DrawableColor> iconTextureColor = () -> UIBase.getUITheme().ui_icon_texture_color;
        /** Optionally computes the spacing between the icon and label. */
        @Nullable
        protected ConsumingSupplier<ClickableMenuBarEntry, Integer> iconPaddingSupplier;
        /** Action run when the entry is activated. */
        @NotNull
        protected ClickAction clickAction;
        /** Font used to measure and draw the entry label. */
        protected Font font = Minecraft.getInstance().font;

        /** Adds a label and click action to an identified menu-bar entry. */
        public ClickableMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(identifier, menuBar);
            this.labelSupplier = (bar, entry) -> label;
            this.clickAction = clickAction;
        }

        /** Renders entry into the active GUI extraction pass. */
        @Override
        protected void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            this.extractBackground(graphics);
            this.renderLabelOrIcon(graphics);
        }

        /** Adds this component's background draw state to the active GUI extraction pass. */
        protected void extractBackground(GuiGraphicsExtractor graphics) {
            UIBase.resetShaderColor(graphics);
            graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + PIXEL_SIZE, this.getEntryBackgroundColor());
            UIBase.resetShaderColor(graphics);
        }

        /** Renders label or icon into the active GUI extraction pass. */
        protected void renderLabelOrIcon(GuiGraphicsExtractor graphics) {
            Component label = this.getLabel();
            ITexture iconTexture = this.getIconTexture();
            if (iconTexture != null) {
                int padding = 0;
                if (this.iconPaddingSupplier != null) {
                    Integer suppliedPadding = this.iconPaddingSupplier.get(this);
                    if (suppliedPadding != null) {
                        padding = Math.max(0, suppliedPadding);
                    }
                }
                int entryWidth = this.getWidth();
                int entryHeight = PIXEL_SIZE;
                int availableWidth = Math.max(1, entryWidth - (padding * 2));
                int availableHeight = Math.max(1, entryHeight - (padding * 2));
                int drawWidth;
                int drawHeight;
                int textureWidth;
                int textureHeight;
                int drawX = this.x;
                int drawY = this.y;
                int baseWidth = iconTexture.getWidth();
                int baseHeight = iconTexture.getHeight();
                boolean isMaterialIcon = iconTexture instanceof MaterialIconTexture;
                Identifier materialIconLocation = null;
                if (isMaterialIcon) {
                    MaterialIconTexture materialIconTexture = (MaterialIconTexture) iconTexture;
                    materialIconTexture.updateRenderContext((float) availableWidth, (float) availableHeight, UIBase.getUIScale());
                    baseWidth = materialIconTexture.getWidth();
                    baseHeight = materialIconTexture.getHeight();
                    materialIconLocation = materialIconTexture.getResourceLocation();
                }
                boolean hasBaseSize = baseWidth > 0 && baseHeight > 0;
                if (hasBaseSize) {
                    float scale = Math.min(availableWidth / (float) baseWidth, availableHeight / (float) baseHeight);
                    if (isMaterialIcon) {
                        scale = Math.min(1.0F, scale);
                    }
                    if (!Float.isFinite(scale) || scale <= 0.0F) {
                        scale = 1.0F;
                    }
                    drawWidth = Math.max(1, Math.round(baseWidth * scale));
                    drawHeight = Math.max(1, Math.round(baseHeight * scale));
                    textureWidth = baseWidth;
                    textureHeight = baseHeight;
                } else {
                    int[] size = iconTexture.getAspectRatio().getAspectRatioSizeByMaximumSize(availableWidth, availableHeight);
                    drawWidth = size[0];
                    drawHeight = size[1];
                    textureWidth = size[0];
                    textureHeight = size[1];
                }
                drawX = this.x + ((entryWidth - drawWidth) / 2);
                drawY = this.y + ((entryHeight - drawHeight) / 2);
                UIBase.resetShaderColor(graphics);
                DrawableColor iconColor = (this.iconTextureColor != null) ? this.iconTextureColor.get() : null;
                if (iconColor != null) UIBase.setShaderColor(graphics, iconColor);
                Identifier loc = materialIconLocation;
                if (loc == null) {
                    if (isMaterialIcon) {
                        loc = ITexture.MISSING_TEXTURE_LOCATION;
                    } else {
                        Identifier fallback = iconTexture.getResourceLocation();
                        loc = (fallback != null) ? fallback : ITexture.MISSING_TEXTURE_LOCATION;
                    }
                }
                graphics.blit(RenderPipelines.GUI_TEXTURED, loc, drawX, drawY, 0.0F, 0.0F, drawWidth, drawHeight, textureWidth, textureHeight, textureWidth, textureHeight);
            } else {
                UIBase.renderText(graphics, label, this.x + ENTRY_LABEL_SPACE_LEFT_RIGHT, this.y + ((float) PIXEL_SIZE / 2) - (UIBase.getUITextHeightNormal() / 2), this.getLabelColor());
            }
            UIBase.resetShaderColor(graphics);
        }

        /** Returns label color. */
        protected int getLabelColor() {
            if (UIBase.shouldBlur()) {
                return this.isActive() ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive.getColorInt();
            }
            return this.isActive() ? UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt();
        }

        /** Returns the current width in GUI units. */
        @Override
        protected int getWidth() {
            Component label = this.getLabel();
            ITexture iconTexture = this.getIconTexture();
            if (iconTexture != null) {
                return Math.max(1, this.baseWidth);
            }
            int labelWidth = (int) (UIBase.getUITextWidthNormal(label) + (ENTRY_LABEL_SPACE_LEFT_RIGHT * 2));
            return Math.max(Math.max(1, this.baseWidth), labelWidth);
        }

        /** Sets active for this clickable menu bar entry. */
        @Override
        public ClickableMenuBarEntry setActive(boolean active) {
            return (ClickableMenuBarEntry) super.setActive(active);
        }

        /** Sets active supplier for this clickable menu bar entry. */
        @Override
        public ClickableMenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            return (ClickableMenuBarEntry) super.setActiveSupplier(activeSupplier);
        }

        /** Sets visible for this clickable menu bar entry. */
        @Override
        public ClickableMenuBarEntry setVisible(boolean visible) {
            return (ClickableMenuBarEntry) super.setVisible(visible);
        }

        /** Sets visible supplier for this clickable menu bar entry. */
        @Override
        public ClickableMenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            return (ClickableMenuBarEntry) super.setVisibleSupplier(visibleSupplier);
        }

        /** Sets base width for this clickable menu bar entry. */
        @Override
        public ClickableMenuBarEntry setBaseWidth(int baseWidth) {
            return (ClickableMenuBarEntry) super.setBaseWidth(baseWidth);
        }

        /** Sets icon texture color for this clickable menu bar entry. */
        public ClickableMenuBarEntry setIconTextureColor(@Nullable Supplier<DrawableColor> iconTextureColor) {
            this.iconTextureColor = iconTextureColor;
            return this;
        }

        /** Returns entry background color. */
        protected int getEntryBackgroundColor() {
            if (this.isHovered() && this.isActive()) {
                if (UIBase.shouldBlur()) {
                    return UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1.getColorInt();
                }
                return UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1.getColorInt();
            }
            return DrawableColor.BLACK.getColorIntWithAlpha(0.0F); // always fully transparent
        }

        /** Returns the label resolved for the current state. */
        @NotNull
        protected Component getLabel() {
            Component c = this.labelSupplier.get(this.parent, this);
            return (c != null) ? c : Component.empty();
        }

        /** Sets label supplier for this clickable menu bar entry. */
        public ClickableMenuBarEntry setLabelSupplier(@NotNull MenuBarEntrySupplier<Component> labelSupplier) {
            this.labelSupplier = labelSupplier;
            return this;
        }

        /** Sets label for this clickable menu bar entry. */
        public ClickableMenuBarEntry setLabel(@NotNull Component label) {
            this.labelSupplier = ((bar, entry) -> label);
            return this;
        }

        /** Resolves the icon texture for the entry's current state, or {@code null}. */
        @Nullable
        protected ITexture getIconTexture() {
            if (this.iconTextureSupplier != null) return this.iconTextureSupplier.get(this.parent, this);
            return null;
        }

        /** Returns the optional state-aware icon supplier. */
        @Nullable
        public MenuBarEntrySupplier<ITexture> getIconTextureSupplier() {
            return this.iconTextureSupplier;
        }

        /** Sets icon texture supplier for this clickable menu bar entry. */
        public ClickableMenuBarEntry setIconTextureSupplier(@Nullable MenuBarEntrySupplier<ITexture> iconTextureSupplier) {
            this.iconTextureSupplier = iconTextureSupplier;
            return this;
        }

        /** Sets icon texture for this clickable menu bar entry. */
        public ClickableMenuBarEntry setIconTexture(@Nullable ITexture iconTexture) {
            this.iconTextureSupplier = (iconTexture != null) ? ((bar, entry) -> iconTexture) : null;
            return this;
        }

        /** Sets icon padding supplier for this clickable menu bar entry. */
        public ClickableMenuBarEntry setIconPaddingSupplier(@Nullable ConsumingSupplier<ClickableMenuBarEntry, Integer> iconPaddingSupplier) {
            this.iconPaddingSupplier = iconPaddingSupplier;
            return this;
        }

        /** Returns click action. */
        @NotNull
        public ClickAction getClickAction() {
            return this.clickAction;
        }

        /** Sets click action for this clickable menu bar entry. */
        public ClickableMenuBarEntry setClickAction(@NotNull ClickAction clickAction) {
            this.clickAction = clickAction;
            return this;
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 0) && (this.isActive() && this.isVisible() && this.isHovered())) {
                if (UIConfiguration.get().clickSoundsEnabled()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.clickAction.onClick(this.parent, this);
                return true;
            }
            return false;
        }

        /** Handles activation of a clickable menu-bar entry. */
        @FunctionalInterface
        public interface ClickAction {

            /** Handles activation of this menu-bar entry. */
            void onClick(MenuBar bar, MenuBarEntry entry);

        }

    }

    /** Represents one renderable, focusable context menu bar entry. */
    public static class ContextMenuBarEntry extends ClickableMenuBarEntry {

        /** Context menu opened by this menu-bar entry. */
        @NotNull
        protected final ContextMenu contextMenu;

        /** Associates a labeled menu-bar entry with the context menu it opens. */
        public ContextMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar, @NotNull Component label, @NotNull ContextMenu contextMenu) {
            super(identifier, menuBar, label, (bar, entry) -> {});
            this.contextMenu = contextMenu;
            this.clickAction = (bar, entry) -> this.openContextMenu();
        }

        /**
         * Opens the {@link ContextMenu}.
         */
        public void openContextMenu() {
            this.openContextMenu(null);
        }

        /**
         * Opens the {@link ContextMenu}.
         *
         * @param entryPath The {@link ContextMenu.SubMenuContextMenuEntry} path of menus to open.
         */
        public void openContextMenu(@Nullable List<String> entryPath) {
            if (this.parent.shouldDeferContextMenuOpen()) {
                this.parent.queueContextMenuOpen(this, entryPath);
                return;
            }
            this.openContextMenuInternal(entryPath);
        }

        private void openContextMenuInternal(@Nullable List<String> entryPath) {

            float scale = getRenderScale();
            float scaledX = (float)this.x * scale;
            float scaledY = (float)this.y * scale;
            float scaledHeight = (float) PIXEL_SIZE * scale;

            this.contextMenu.setRoundedCorners(false, false, true, true);

            ContextMenuHandler.INSTANCE.setAndOpen(this.contextMenu, scaledX, scaledY + scaledHeight - this.contextMenu.getScaledBorderThickness(), entryPath);

        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            this.handleOpenOnHover();
            super.extractRenderState(graphics, mouseX, mouseY, partial);
        }

        /** Handles open on hover for this context menu bar entry. */
        protected void handleOpenOnHover() {
            if (this.isHovered() && this.isActive() && this.isVisible() && !this.contextMenu.isOpen() && this.parent.isEntryContextMenuOpen()) {
                this.parent.closeAllContextMenus();
                this.openContextMenu();
            }
        }

        /** Returns context menu. */
        @NotNull
        public ContextMenu getContextMenu() {
            return this.contextMenu;
        }

        /** Sets active for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setActive(boolean active) {
            return (ContextMenuBarEntry) super.setActive(active);
        }

        /** Sets active supplier for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            return (ContextMenuBarEntry) super.setActiveSupplier(activeSupplier);
        }

        /** Sets visible for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setVisible(boolean visible) {
            return (ContextMenuBarEntry) super.setVisible(visible);
        }

        /** Sets visible supplier for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            return (ContextMenuBarEntry) super.setVisibleSupplier(visibleSupplier);
        }

        /** Sets base width for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setBaseWidth(int baseWidth) {
            return (ContextMenuBarEntry) super.setBaseWidth(baseWidth);
        }

        /** Sets label for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setLabel(@NotNull Component label) {
            return (ContextMenuBarEntry) super.setLabel(label);
        }

        /** Sets label supplier for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setLabelSupplier(@NotNull MenuBarEntrySupplier<Component> labelSupplier) {
            return (ContextMenuBarEntry) super.setLabelSupplier(labelSupplier);
        }

        /** Sets icon texture for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setIconTexture(@Nullable ITexture iconTexture) {
            return (ContextMenuBarEntry) super.setIconTexture(iconTexture);
        }

        /** Sets icon texture supplier for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setIconTextureSupplier(@Nullable MenuBarEntrySupplier<ITexture> iconTextureSupplier) {
            return (ContextMenuBarEntry) super.setIconTextureSupplier(iconTextureSupplier);
        }

        /** Sets click action for this context menu bar entry. */
        @Override
        public ContextMenuBarEntry setClickAction(@NotNull ClickAction clickAction) {
            LOGGER.error("[KONKRETE] You can't change the click action of ContextMenuBarEntries!");
            return this;
        }

        /** Returns entry background color. */
        @Override
        protected int getEntryBackgroundColor() {
            if (this.contextMenu.isOpen()) {
                if (UIBase.shouldBlur()) {
                    return UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1.getColorInt();
                }
                return UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1.getColorInt();
            }
            return super.getEntryBackgroundColor();
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((!this.isHovered() || !this.isActive() || !this.isVisible()) && !this.contextMenu.isUserNavigatingInMenu() && this.contextMenu.isOpen()) {
                this.contextMenu.closeMenu();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

    }

    /** Represents one renderable, focusable spacer menu bar entry. */
    public static class SpacerMenuBarEntry extends MenuBarEntry {

        /** Width in GUI units for width. */
        protected int width = 10;

        /** Adds flexible empty space to its owning menu bar. */
        public SpacerMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar) {
            super(identifier, menuBar);
        }

        /** Renders entry into the active GUI extraction pass. */
        @Override
        protected void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        }

        /** Returns the current width in GUI units. */
        @Override
        protected int getWidth() {
            return this.width;
        }

        /** Sets active for this spacer menu bar entry. */
        @Override
        public SpacerMenuBarEntry setActive(boolean active) {
            return (SpacerMenuBarEntry) super.setActive(active);
        }

        /** Sets active supplier for this spacer menu bar entry. */
        @Override
        public SpacerMenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            return (SpacerMenuBarEntry) super.setActiveSupplier(activeSupplier);
        }

        /** Sets visible for this spacer menu bar entry. */
        @Override
        public SpacerMenuBarEntry setVisible(boolean visible) {
            return (SpacerMenuBarEntry) super.setVisible(visible);
        }

        /** Sets visible supplier for this spacer menu bar entry. */
        @Override
        public SpacerMenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            return (SpacerMenuBarEntry) super.setVisibleSupplier(visibleSupplier);
        }

        /** Sets width for this spacer menu bar entry. */
        public SpacerMenuBarEntry setWidth(int width) {
            this.width = width;
            return this;
        }

    }

    /** Represents one renderable, focusable separator menu bar entry. */
    public static class SeparatorMenuBarEntry extends MenuBarEntry {

        /** Supplies the separator color for the current theme state. */
        @NotNull
        protected Supplier<DrawableColor> color = () -> UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_border_color : UIBase.getUITheme().ui_interface_widget_border_color;

        /** Adds a visual separator to its owning menu bar. */
        public SeparatorMenuBarEntry(@NotNull String identifier, @NotNull MenuBar parent) {
            super(identifier, parent);
        }

        /** Renders entry into the active GUI extraction pass. */
        @Override
        protected void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            UIBase.resetShaderColor(graphics);
            graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + PIXEL_SIZE, this.getColor().getColorInt());
            UIBase.resetShaderColor(graphics);
        }

        /** Returns the current width in GUI units. */
        @Override
        protected int getWidth() {
            return 1;
        }

        /** Sets active for this separator menu bar entry. */
        @Override
        public SeparatorMenuBarEntry setActive(boolean active) {
            return (SeparatorMenuBarEntry) super.setActive(active);
        }

        /** Sets active supplier for this separator menu bar entry. */
        @Override
        public SeparatorMenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            return (SeparatorMenuBarEntry) super.setActiveSupplier(activeSupplier);
        }

        /** Sets visible for this separator menu bar entry. */
        @Override
        public SeparatorMenuBarEntry setVisible(boolean visible) {
            return (SeparatorMenuBarEntry) super.setVisible(visible);
        }

        /** Sets visible supplier for this separator menu bar entry. */
        @Override
        public SeparatorMenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            return (SeparatorMenuBarEntry) super.setVisibleSupplier(visibleSupplier);
        }

        /** Returns the color resolved for the current state. */
        @NotNull
        public DrawableColor getColor() {
            return this.color.get();
        }

        /** Sets color for this separator menu bar entry. */
        public SeparatorMenuBarEntry setColor(@NotNull Supplier<DrawableColor> color) {
            this.color = color;
            return this;
        }

    }

    /** Identifies one supported side option. */
    public enum Side {

        /** Positions the element at left. */
        LEFT,
        /** Positions the element at right. */
        RIGHT

    }

    /** Stores a context-menu request until current input dispatch completes. */
    protected static final class PendingContextMenuOpen {

        private final ContextMenuBarEntry entry;
        @Nullable
        private final List<String> entryPath;

        private PendingContextMenuOpen(@NotNull ContextMenuBarEntry entry, @Nullable List<String> entryPath) {
            this.entry = entry;
            this.entryPath = entryPath;
        }

    }

}
