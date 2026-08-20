package de.keksuccino.konkrete.util.rendering.ui.contextmenu;

import de.keksuccino.konkrete.util.ScreenUtils;

import de.keksuccino.konkrete.util.cycle.ILocalizedValueCycle;
import de.keksuccino.konkrete.util.input.InputConstants;
import de.keksuccino.konkrete.util.input.InputUtils;
import de.keksuccino.konkrete.util.properties.RuntimePropertyContainer;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.GuiBlurRenderer;
import de.keksuccino.konkrete.util.rendering.IconAnimation;
import de.keksuccino.konkrete.util.rendering.IconAnimations;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.konkrete.util.rendering.ui.RoutableUIComponent;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.UIConfiguration;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.util.window.WindowHandler;
import de.keksuccino.konkrete.util.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
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
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Owns a hierarchical context menu, its entries, focus, animation, and input routing. */
@SuppressWarnings("all")
public class ContextMenu implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget, RoutableUIComponent {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MaterialIcon SUB_CONTEXT_MENU_ARROW_ICON = MaterialIcons.CHEVRON_RIGHT;
    private static final MaterialIcon SCROLL_UP_ICON = MaterialIcons.ARROW_DROP_UP;
    private static final MaterialIcon SCROLL_DOWN_ICON = MaterialIcons.ARROW_DROP_DOWN;
    private static final MaterialIcon CONTEXT_MENU_TOOLTIP_ICON = MaterialIcons.INFO;
    private static final DrawableColor SHADOW_COLOR = DrawableColor.of(new Color(43, 43, 43, 100));
    private static final int SCROLL_INDICATOR_HEIGHT = 12; // Space reserved for arrows
    private static final int SCROLL_INDICATOR_ICON_SIZE = 10;
    private static final float SCROLL_INDICATOR_BACKGROUND_ICON_MIX = 0.12F;
    private static final String SEARCH_ENTRY_IDENTIFIER = "context_menu_search";
    private static final String SEARCH_SEPARATOR_IDENTIFIER = "context_menu_search_separator";

    /** Menu entries in display order. */
    protected final List<ContextMenuEntry<?>> entries = new ArrayList<>();
    /** Menu-local GUI scale used for layout and pointer conversion. */
    protected float scale = UIBase.getUIScale();
    /** Whether menu coordinates use the configured UI scale. */
    protected boolean forceUIScale = true;
    /** Whether the menu is open. */
    protected boolean open = false;
    /** Horizontal GUI coordinate for raw. */
    protected float rawX; // without border
    /** Vertical GUI coordinate for raw. */
    protected float rawY; // without border
    /** Width in GUI units for raw. */
    protected float rawWidth; // without border
    /** Height in GUI units for raw. */
    protected float rawHeight; // without border
    /** Parent entry that receives this object's state changes. */
    protected SubMenuContextMenuEntry parentEntry = null;
    /** Side currently selected for opening child menus. */
    protected SubMenuOpeningSide subMenuOpeningSide = SubMenuOpeningSide.RIGHT;
    /** Whether text or geometry is drawn with a shadow. */
    protected boolean shadow = false;
    /** Whether to clamp the menu away from screen edges. */
    protected boolean keepDistanceToEdges = true;
    /** Whether supplied menu coordinates bypass GUI-scale conversion. */
    protected boolean forceRawXY = false;
    /** Whether to force the submenu-opening side. */
    protected boolean forceSide = false;
    /** Whether child menus inherit the forced opening side. */
    protected boolean forceSideSubMenus = true;
    /** Whether to round the top-left corner. */
    protected boolean roundTopLeftCorner = true;
    /** Whether to round the top-right corner. */
    protected boolean roundTopRightCorner = true;
    /** Whether to round the bottom-left corner. */
    protected boolean roundBottomLeftCorner = true;
    /** Whether to round the bottom-right corner. */
    protected boolean roundBottomRightCorner = true;
    /** Whether open animation is enabled. */
    protected boolean openAnimationEnabled = true;
    /** Vertical scroll offset in menu-local GUI units. */
    protected float scrollPosition = 0.0f; // Current scroll position
    private boolean needsScrolling = false; // Flag to track if menu is scrollable
    private float displayHeight = 0; // Adjusted height when scrollable
    private static final float OPEN_ANIMATION_GROW_TIME_MS = 120.0F;
    private static final float OPEN_ANIMATION_MIN_SCALE = 0.78F;
    private long openAnimationStartMs = 0L;
    private boolean openAnimationActive = false;
    private boolean supressOpenAnimationNextOpen = false;
    /** Horizontal GUI coordinate for render mouse. */
    protected int renderMouseX = 0;
    /** Vertical GUI coordinate for render mouse. */
    protected int renderMouseY = 0;
    private final SearchContextMenuEntry searchEntry;
    private final SeparatorContextMenuEntry searchSeparator;
    private boolean searchEntryRequested = false;
    private boolean searchEntryVisibleLast = false;
    private boolean alwaysShowSearchBar = false;
    private ContextMenu cachedSearchMenu = null;
    private boolean arrowNavigationActive = false;
    @Nullable
    private ContextMenu arrowNavigationMenu = null;
    @Nullable
    private ContextMenuEntry<?> arrowNavigationEntry = null;
    private int arrowNavigationMouseX = Integer.MIN_VALUE;
    private int arrowNavigationMouseY = Integer.MIN_VALUE;

    /** Creates an empty context menu with default state. */
    public ContextMenu() {
        this.searchEntry = new SearchContextMenuEntry(SEARCH_ENTRY_IDENTIFIER, this);
        this.searchSeparator = new SeparatorContextMenuEntry(SEARCH_SEPARATOR_IDENTIFIER, this);
        this.entries.add(this.searchEntry);
        this.entries.add(this.searchSeparator);
        this.searchEntry.addIsVisibleSupplier((menu, entry) -> this.isSearchEntryVisible());
        this.searchSeparator.addIsVisibleSupplier((menu, entry) -> this.isSearchEntryVisible());
        this.searchEntryVisibleLast = this.isSearchEntryVisible();
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.renderMouseX = mouseX;
        this.renderMouseY = mouseY;

        if (!this.isOpen()) return;

        UIBase.startUIScaleRendering();
        try {

        this.updateSearchVisibilityState(false);

        ContextMenu root = this.getRootMenu();
        if (root == this) {
            this.updateArrowNavigationMouseState(mouseX, mouseY);
            this.clearArrowNavigationIfMenuClosed();
            this.validateArrowNavigationSelection();
        }

        if (this.forceUIScale) this.scale = UIBase.getUIScale();

        boolean animationsEnabled = UIBase.shouldPlayAnimations() && this.openAnimationEnabled;
        boolean openingAnimation = animationsEnabled && this.isTopLevelOpenAnimationRunning();
        float uiScale = UIBase.calculateFixedRenderScale(this.getScale());
        float animationScale = animationsEnabled ? this.getOpenAnimationScale(partial) : 1.0F;
        float renderScale = uiScale * animationScale;

        RenderingUtils.setDepthTestLocked(true);

        graphics.pose().pushMatrix();
        graphics.pose().scale(renderScale, renderScale);

        List<ContextMenuEntry<?>> renderEntries = new ArrayList<>();
        renderEntries.add(new SpacerContextMenuEntry("unregistered_spacer_top", this));

        //Check if icon space should get added to entries
        boolean addIconSpace = this.shouldAddIconSpaceForEntries();

        this.rawWidth = 20;
        this.rawHeight = 0;

        String searchText = this.getActiveSearchText();
        String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
        boolean filterActive = (searchLower != null) && !searchLower.isBlank();
        List<ContextMenuEntry<?>> visibleEntries = new ArrayList<>();
        for (ContextMenuEntry<?> e : this.entries) {
            e.addSpaceForIcon = addIconSpace;
            if (e.isVisible() && (!filterActive || this.matchesSearchFilter(e, searchLower))) {
                visibleEntries.add(e);
            }
        }
        for (ContextMenuEntry<?> e : this.entries) {
            if (!visibleEntries.contains(e)) {
                e.setHovered(false);
            }
        }

        int startIndex = 0;
        int endIndex = visibleEntries.size() - 1;
        while (startIndex <= endIndex && visibleEntries.get(startIndex) instanceof SeparatorContextMenuEntry) {
            startIndex++;
        }
        while (endIndex >= startIndex && visibleEntries.get(endIndex) instanceof SeparatorContextMenuEntry && visibleEntries.get(endIndex) != this.searchSeparator) {
            endIndex--;
        }

        ContextMenuEntry<?> prev = null;
        for (int i = startIndex; i <= endIndex; i++) {
            ContextMenuEntry<?> e = visibleEntries.get(i);
            //Merge separator entries when they would render in a row
            if (e instanceof SeparatorContextMenuEntry && prev instanceof SeparatorContextMenuEntry) {
                continue;
            }
            //Pre-tick
            if (e.tickAction != null) {
                e.tickAction.run(this, e, false);
            }
            //Update width + height
            float w = e.getMinWidth();
            if (w > this.rawWidth) {
                this.rawWidth = w;
            }
            this.rawHeight += e.getHeight();
            renderEntries.add(e);
            prev = e;
        }
        this.rawHeight += 8; //add top and bottom spacer to total height

        renderEntries.add(new SpacerContextMenuEntry("unregistered_spacer_bottom", this));

        // Calculate max height considering both menu scale and GUI scale
        double guiScale = WindowHandler.getGuiScale();
        float menuScale = UIBase.calculateFixedRenderScale(this.scale);
        float maxMenuHeight = (getScreenHeight() / menuScale) * 0.7f;

        this.needsScrolling = this.rawHeight > maxMenuHeight;

        // If scrollable, adjust displayed height
        this.displayHeight = needsScrolling ? maxMenuHeight : this.rawHeight;
        boolean renderContent = !openingAnimation;

        float x = this.getActualX();
        float y = this.getActualY();
        float scaledX = (float)((float)x/ renderScale) + this.getBorderThickness();
        float scaledY = (float)((float)y/ renderScale) + this.getBorderThickness();
        float scaledMouseX = (float) ((float)mouseX / renderScale);
        float scaledMouseY = (float) ((float)mouseY / renderScale);
        boolean navigatingInSub = this.isUserNavigatingInSubMenu();
        boolean arrowNavigationActive = root.arrowNavigationActive;
        ContextMenu arrowNavigationMenu = root.arrowNavigationMenu;
        ContextMenuEntry<?> arrowNavigationEntry = root.arrowNavigationEntry;
        float normalRoundingRadius = UIBase.getInterfaceCornerRoundingRadius();
        float normalCornerTopLeft = this.roundTopLeftCorner ? normalRoundingRadius : 0.0F;
        float normalCornerTopRight = this.roundTopRightCorner ? normalRoundingRadius : 0.0F;
        float normalCornerBottomLeft = this.roundBottomLeftCorner ? normalRoundingRadius : 0.0F;
        float normalCornerBottomRight = this.roundBottomRightCorner ? normalRoundingRadius : 0.0F;
        float smoothScale = renderScale;
        float smoothX = scaledX * smoothScale;
        float smoothY = scaledY * smoothScale;
        float smoothWidth = this.getWidth() * smoothScale;
        float smoothHeight = displayHeight * smoothScale;
        float smoothCornerTopLeft = normalCornerTopLeft * smoothScale;
        float smoothCornerTopRight = normalCornerTopRight * smoothScale;
        float smoothCornerBottomLeft = normalCornerBottomLeft * smoothScale;
        float smoothCornerBottomRight = normalCornerBottomRight * smoothScale;

        //Render shadow
        if (this.hasShadow()) {
            SmoothRectangleRenderer.renderSmoothRectRoundAllCorners(
                    graphics,
                    (scaledX + 4.0F) * smoothScale,
                    (scaledY + 4.0F) * smoothScale,
                    smoothWidth,
                    smoothHeight,
                    smoothCornerTopLeft,
                    smoothCornerTopRight,
                    smoothCornerBottomRight,
                    smoothCornerBottomLeft,
                    SHADOW_COLOR.getColorInt(),
                    partial
            );
        }

        if (UIBase.shouldBlur()) {
            // Render blur background
            float blurX = smoothX;
            float blurY = smoothY;
            float blurWidth = smoothWidth;
            float blurHeight = smoothHeight;
            if (blurWidth > 0.0F && blurHeight > 0.0F) {
                GuiBlurRenderer.renderBlurAreaWithIntensityRoundAllCorners(
                        graphics,
                        blurX,
                        blurY,
                        blurWidth,
                        blurHeight,
                        UIBase.getBlurRadius(),
                        smoothCornerTopLeft,
                        smoothCornerTopRight,
                        smoothCornerBottomRight,
                        smoothCornerBottomLeft,
                        UIBase.getUITheme().ui_blur_overlay_background_tint,
                        partial
                );
            }
        } else {
            //Render normal background
            SmoothRectangleRenderer.renderSmoothRectRoundAllCorners(
                    graphics,
                    smoothX,
                    smoothY,
                    smoothWidth,
                    smoothHeight,
                    smoothCornerTopLeft,
                    smoothCornerTopRight,
                    smoothCornerBottomRight,
                    smoothCornerBottomLeft,
                    UIBase.getUITheme().ui_overlay_background_color.getColorInt(),
                    partial
            );
        }

        // Enable scissoring if scrollable
        if (needsScrolling && renderContent) {
            // GuiGraphicsExtractor transforms scissors through the active Matrix3x2fStack in 1.21.11.
            float scissorTopInScaledContext = scaledY + SCROLL_INDICATOR_HEIGHT;
            float scissorBottomInScaledContext = scaledY + displayHeight - SCROLL_INDICATOR_HEIGHT;
            float scissorLeftInScaledContext = scaledX;
            float scissorRightInScaledContext = scaledX + this.getWidth();

            graphics.enableScissor(
                    (int)Math.floor(scissorLeftInScaledContext),
                    (int)Math.floor(scissorTopInScaledContext),
                    (int)Math.ceil(scissorRightInScaledContext),
                    (int)Math.ceil(scissorBottomInScaledContext)
            );
        }

        //Update + render entries
        if (renderContent) {
            float entryY = scaledY;
            if (needsScrolling) {
                // Add space for scroll indicator and apply scroll position
                entryY += SCROLL_INDICATOR_HEIGHT - scrollPosition;
            }

            for (ContextMenuEntry<?> e : renderEntries) {
                e.x = scaledX;
                e.y = entryY; //already scaled
                e.width = this.getWidth(); //don't scale, because already scaled via graphics.pose().scale()

                boolean isVisible = true;
                if (needsScrolling) {
                    // Check if entry is visible in the scrollable area
                    float entryBottom = entryY + e.getHeight();
                    float visibleTop = scaledY + SCROLL_INDICATOR_HEIGHT;
                    float visibleBottom = scaledY + displayHeight - SCROLL_INDICATOR_HEIGHT;

                    // Entry is visible if it's at least partially within the visible area
                    isVisible = (entryY < visibleBottom && entryBottom > visibleTop);
                }

                boolean hover = e.isHovered();
                boolean shouldHover;
                if (arrowNavigationActive) {
                    shouldHover = (arrowNavigationMenu == this) && (arrowNavigationEntry == e);
                } else {
                    shouldHover = !navigatingInSub && UIBase.isXYInArea(scaledMouseX, scaledMouseY, e.x, e.y, e.width, e.getHeight());
                }
                // Only set hover if the entry is visible in the scroll area
                e.setHovered(isVisible && shouldHover);

                //Run hover action of element if its hover state changed to hovered
                if (!hover && e.isHovered() && (e.hoverAction != null)) {
                    e.hoverAction.run(this, e, false);
                }

                // Only render if visible
                if (isVisible) {
                    e.extractRenderState(graphics, (int) scaledMouseX, (int) scaledMouseY, partial);
                }

                entryY += e.getHeight(); //don't scale this, because already scaled via graphics.pose().scale()
            }
        } else {
            this.unhoverAllEntries();
        }

        // Disable scissoring and render arrow indicators if needed
        if (needsScrolling && renderContent) {
            graphics.disableScissor();

            // Calculate max scroll position
            float maxScrollPosition = this.rawHeight - (displayHeight - SCROLL_INDICATOR_HEIGHT * 2);

            int scrollIndicatorBackgroundColor = this.getScrollIndicatorBackgroundColor();

            // Render up arrow background and arrow if scrolled down
            if (scrollPosition > 0) {
                // Fill background with rounded top corners
                SmoothRectangleRenderer.renderSmoothRectRoundAllCorners(
                        graphics,
                        smoothX,
                        smoothY,
                        smoothWidth,
                        SCROLL_INDICATOR_HEIGHT * smoothScale,
                        smoothCornerTopLeft,
                        smoothCornerTopRight,
                        0.0F,
                        0.0F,
                        scrollIndicatorBackgroundColor,
                        partial
                );

                // Render arrow centered
                UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
                IconRenderData iconData = resolveMaterialIconData(SCROLL_UP_ICON, SCROLL_INDICATOR_ICON_SIZE, SCROLL_INDICATOR_ICON_SIZE);
                if (iconData != null) {
                    blitScaledIcon(
                            graphics,
                            iconData,
                            scaledX + this.getWidth() / 2.0F - (SCROLL_INDICATOR_ICON_SIZE / 2.0F),
                            scaledY + (SCROLL_INDICATOR_HEIGHT - SCROLL_INDICATOR_ICON_SIZE) / 2.0F, // Center vertically
                            SCROLL_INDICATOR_ICON_SIZE,
                            SCROLL_INDICATOR_ICON_SIZE
                    );
                }
                RenderingUtils.resetShaderColor(graphics);
            }

            // Render down arrow background and arrow if can scroll further
            if (scrollPosition < maxScrollPosition) {
                // Fill background with rounded bottom corners
                SmoothRectangleRenderer.renderSmoothRectRoundAllCorners(
                        graphics,
                        smoothX,
                        (scaledY + displayHeight - SCROLL_INDICATOR_HEIGHT) * smoothScale,
                        smoothWidth,
                        SCROLL_INDICATOR_HEIGHT * smoothScale,
                        0.0F,
                        0.0F,
                        smoothCornerBottomRight,
                        smoothCornerBottomLeft,
                        scrollIndicatorBackgroundColor,
                        partial
                );

                // Render arrow centered (with fixed position)
                UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
                IconRenderData iconData = resolveMaterialIconData(SCROLL_DOWN_ICON, SCROLL_INDICATOR_ICON_SIZE, SCROLL_INDICATOR_ICON_SIZE);
                if (iconData != null) {
                    blitScaledIcon(
                            graphics,
                            iconData,
                            scaledX + this.getWidth() / 2.0F - (SCROLL_INDICATOR_ICON_SIZE / 2.0F),
                            scaledY + displayHeight - SCROLL_INDICATOR_HEIGHT + (SCROLL_INDICATOR_HEIGHT - SCROLL_INDICATOR_ICON_SIZE) / 2.0F, // Centered in area
                            SCROLL_INDICATOR_ICON_SIZE,
                            SCROLL_INDICATOR_ICON_SIZE
                    );
                }
                RenderingUtils.resetShaderColor(graphics);
            }
        }

        //Render border
        float smoothBorderThickness = this.getBorderThickness() * smoothScale;
        float smoothBorderCornerTopLeft = normalCornerTopLeft > 0.0F ? (normalCornerTopLeft + this.getBorderThickness()) * smoothScale : 0.0F;
        float smoothBorderCornerTopRight = normalCornerTopRight > 0.0F ? (normalCornerTopRight + this.getBorderThickness()) * smoothScale : 0.0F;
        float smoothBorderCornerBottomRight = normalCornerBottomRight > 0.0F ? (normalCornerBottomRight + this.getBorderThickness()) * smoothScale : 0.0F;
        float smoothBorderCornerBottomLeft = normalCornerBottomLeft > 0.0F ? (normalCornerBottomLeft + this.getBorderThickness()) * smoothScale : 0.0F;
        SmoothRectangleRenderer.renderSmoothBorderRoundAllCorners(
                graphics,
                (scaledX - this.getBorderThickness()) * smoothScale,
                (scaledY - this.getBorderThickness()) * smoothScale,
                (this.getWidth() + (this.getBorderThickness() * 2.0F)) * smoothScale,
                (displayHeight + (this.getBorderThickness() * 2.0F)) * smoothScale,
                smoothBorderThickness,
                smoothBorderCornerTopLeft,
                smoothBorderCornerTopRight,
                smoothBorderCornerBottomRight,
                smoothBorderCornerBottomLeft,
                UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_border_color.getColorInt() : UIBase.getUITheme().ui_overlay_border_color.getColorInt(),
                partial
        );

        //Post-tick
        for (ContextMenuEntry<?> e : renderEntries) {
            if (e.tickAction != null) {
                e.tickAction.run(this, e, true);
            }
        }

        graphics.pose().popMatrix();

        RenderingUtils.setDepthTestLocked(false);

        //Render sub context menus
        for (ContextMenuEntry<?> e : renderEntries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (this.forceSideSubMenus) s.subContextMenu.forceSide = this.forceSide;
                s.subContextMenu.forceRawXY = this.forceRawXY;
                s.subContextMenu.shadow = this.shadow;
                s.subContextMenu.scale = this.scale;
                s.subContextMenu.forceUIScale = this.forceUIScale;
                s.subContextMenu.extractRenderState(graphics, mouseX, mouseY, partial);
            }
        }

        } finally {
            UIBase.stopUIScaleRendering();
        }

    }

    private int getScrollIndicatorBackgroundColor() {
        boolean blur = UIBase.shouldBlur();
        UITheme theme = UIBase.getUITheme();
        Color backgroundColor = blur ? theme.ui_blur_overlay_background_tint.getColor() : theme.ui_overlay_background_color.getColor();
        Color iconColor = blur ? theme.ui_blur_icon_texture_color.getColor() : theme.ui_icon_texture_color.getColor();
        return mixRgb(backgroundColor, iconColor, SCROLL_INDICATOR_BACKGROUND_ICON_MIX).getRGB();
    }

    private static @NotNull Color mixRgb(@NotNull Color baseColor, @NotNull Color mixColor, float mixFactor) {
        float clampedMixFactor = Math.max(0.0F, Math.min(1.0F, mixFactor));
        float baseFactor = 1.0F - clampedMixFactor;
        return new Color(
                Math.round((baseColor.getRed() * baseFactor) + (mixColor.getRed() * clampedMixFactor)),
                Math.round((baseColor.getGreen() * baseFactor) + (mixColor.getGreen() * clampedMixFactor)),
                Math.round((baseColor.getBlue() * baseFactor) + (mixColor.getBlue() * clampedMixFactor)),
                baseColor.getAlpha()
        );
    }

    /** Inserts a submenu entry at the requested index. */
    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryAt(int index, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return this.addEntryAt(index, e);
    }

    /** Adds sub menu entry before to this context menu. */
    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return this.addEntryBefore(addBeforeIdentifier, e);
    }

    /** Adds sub menu entry after to this context menu. */
    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return this.addEntryAfter(addAfterIdentifier, e);
    }

    /** Adds sub menu entry to this context menu. */
    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntry(@NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return this.addEntry(e);
    }

    /** Inserts a separator entry at the requested index. */
    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryAt(int index, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return this.addEntryAt(index, e);
    }

    /** Adds separator entry before to this context menu. */
    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return this.addEntryBefore(addBeforeIdentifier, e);
    }

    /** Adds separator entry after to this context menu. */
    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return this.addEntryAfter(addAfterIdentifier, e);
    }

    /** Adds separator entry to this context menu. */
    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntry(@NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return this.addEntry(e).setStackable(true);
    }

    /** Inserts a value-cycle entry at the requested index. */
    @NotNull
    public <T> ValueCycleContextMenuEntry<T> addValueCycleEntryAt(int index, @NotNull String identifier, @NotNull ILocalizedValueCycle<T> valueCycle) {
        ValueCycleContextMenuEntry<T> e = new ValueCycleContextMenuEntry<>(identifier, this, valueCycle);
        return this.addEntryAt(index, e);
    }

    /** Adds value cycle entry after to this context menu. */
    @NotNull
    public <T> ValueCycleContextMenuEntry<T> addValueCycleEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull ILocalizedValueCycle<T> valueCycle) {
        ValueCycleContextMenuEntry<T> e = new ValueCycleContextMenuEntry<>(identifier, this, valueCycle);
        return this.addEntryAfter(addAfterIdentifier, e);
    }

    /** Adds value cycle entry before to this context menu. */
    @NotNull
    public <T> ValueCycleContextMenuEntry<T> addValueCycleEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull ILocalizedValueCycle<T> valueCycle) {
        ValueCycleContextMenuEntry<T> e = new ValueCycleContextMenuEntry<>(identifier, this, valueCycle);
        return this.addEntryBefore(addBeforeIdentifier, e);
    }

    /** Adds value cycle entry to this context menu. */
    @NotNull
    public <T> ValueCycleContextMenuEntry<T> addValueCycleEntry(@NotNull String identifier, @NotNull ILocalizedValueCycle<T> valueCycle) {
        ValueCycleContextMenuEntry<T> e = new ValueCycleContextMenuEntry<>(identifier, this, valueCycle);
        return this.addEntry(e);
    }

    /** Inserts a clickable entry at the requested index. */
    @NotNull
    public ClickableContextMenuEntry<?> addClickableEntryAt(int index, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry<?> e = new ClickableContextMenuEntry<>(identifier, this, label, clickAction);
        return this.addEntryAt(index, e);
    }

    /** Adds clickable entry after to this context menu. */
    @NotNull
    public ClickableContextMenuEntry<?> addClickableEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry<?> e = new ClickableContextMenuEntry<>(identifier, this, label, clickAction);
        return this.addEntryAfter(addAfterIdentifier, e);
    }

    /** Adds clickable entry before to this context menu. */
    @NotNull
    public ClickableContextMenuEntry<?> addClickableEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry<?> e = new ClickableContextMenuEntry<>(identifier, this, label, clickAction);
        return this.addEntryBefore(addBeforeIdentifier, e);
    }

    /** Adds clickable entry to this context menu. */
    @NotNull
    public ClickableContextMenuEntry<?> addClickableEntry(@NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry<?> e = new ClickableContextMenuEntry<>(identifier, this, label, clickAction);
        return this.addEntry(e);
    }

    /** Adds entry after to this context menu. */
    @NotNull
    public <T extends ContextMenuEntry<?>> T addEntryAfter(@NotNull String identifier, @NotNull T entry) {
        int index = this.getEntryIndex(identifier);
        if (index >= 0) {
            index++;
        } else {
            LOGGER.error("[KONKRETE] Failed to add ContextMenu entry (" + entry.identifier + ") after other entry (" + identifier + ")! Target entry not found! Will add the entry at the end instead!");
            index = this.entries.size();
        }
        return this.addEntryAt(index, entry);
    }

    /** Adds entry before to this context menu. */
    @NotNull
    public <T extends ContextMenuEntry<?>> T addEntryBefore(@NotNull String identifier, @NotNull T entry) {
        int index = this.getEntryIndex(identifier);
        if (index < 0) {
            LOGGER.error("[KONKRETE] Failed to add ContextMenu entry (" + entry.identifier + ") before other entry (" + identifier + ")! Target entry not found! Will add the entry at the end instead!");
            index = this.entries.size();
        }
        return this.addEntryAt(index, entry);
    }

    /** Adds entry to this context menu. */
    @NotNull
    public <T extends ContextMenuEntry<?>> T addEntry(@NotNull T entry) {
        return this.addEntryAt(this.entries.size(), entry);
    }

    /** Inserts an existing entry at the requested index. */
    @NotNull
    public <T extends ContextMenuEntry<?>> T addEntryAt(int index, @NotNull T entry) {
        if ((entry instanceof SearchContextMenuEntry) && (entry != this.searchEntry)) {
            LOGGER.error("[KONKRETE] Failed to add ContextMenu search entry! Only one search entry is allowed per menu.");
            return entry;
        }
        if (this.hasEntry(entry.identifier)) {
            LOGGER.error("[KONKRETE] Failed to add ContextMenu entry! Identifier already in use: " + entry.identifier);
        } else {
            if (!this.isProtectedEntry(entry)) {
                int minIndex = this.getProtectedEntriesCount();
                if (index < minIndex) index = minIndex;
            }
            this.entries.add(index, entry);
        }
        return entry;
    }

    /** Removes entry from this context menu. */
    public ContextMenu removeEntry(String identifier) {
        if (this.isProtectedEntryIdentifier(identifier)) {
            LOGGER.error("[KONKRETE] Failed to remove ContextMenu entry! Entry is protected: " + identifier);
            return this;
        }
        ContextMenuEntry<?> e = this.getEntry(identifier);
        if (e != null) {
            this.entries.remove(e);
            e.onRemoved();
        }
        return this;
    }

    /** Clears entries state. */
    public ContextMenu clearEntries() {
        this.closeMenu();
        List<ContextMenuEntry<?>> entriesToRemove = new ArrayList<>(this.entries);
        for (ContextMenuEntry<?> e : entriesToRemove) {
            if (this.isProtectedEntry(e)) continue;
            this.entries.remove(e);
            e.onRemoved();
        }
        this.resetSearchState();
        return this;
    }

    /**
     * Clears all non-protected entries without closing the menu.
     */
    public ContextMenu clearEntriesKeepOpen() {
        this.closeSubMenus();
        this.unhoverAllEntries();
        ContextMenu root = this.getRootMenu();
        if (root.arrowNavigationMenu == this || root == this) {
            root.resetArrowNavigationState();
        }
        List<ContextMenuEntry<?>> entriesToRemove = new ArrayList<>(this.entries);
        for (ContextMenuEntry<?> e : entriesToRemove) {
            if (this.isProtectedEntry(e)) continue;
            this.entries.remove(e);
            e.onRemoved();
        }
        this.resetSearchState();
        return this;
    }

    /** Returns the matching entry, or {@code null} when absent. */
    @Nullable
    public ContextMenuEntry<?> getEntry(String identifier) {
        for (ContextMenuEntry<?> e : this.entries) {
            if (e.identifier.equals(identifier)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Returns the entry index or -1 if the entry was not found.
     */
    public int getEntryIndex(String identifier) {
        ContextMenuEntry<?> e = this.getEntry(identifier);
        if (e != null) {
            return this.entries.indexOf(e);
        }
        return -1;
    }

    /**
     * Returns a COPY of the entry list. Changes made to the list will not get reflected in the menu.
     */
    @NotNull
    public List<ContextMenuEntry<?>> getEntries() {
        return new ArrayList<>(this.entries);
    }

    /** Returns whether entry. */
    public boolean hasEntry(String identifier) {
        return this.getEntry(identifier) != null;
    }

    /** Returns the current render-scale multiplier. */
    public float getScale() {
        return this.scale;
    }

    /** Returns whether open animation enabled. */
    public boolean isOpenAnimationEnabled() {
        return this.openAnimationEnabled;
    }

    /** Sets open animation enabled for this context menu. */
    public ContextMenu setOpenAnimationEnabled(boolean openAnimationEnabled) {
        this.openAnimationEnabled = openAnimationEnabled;
        return this;
    }

    /** Skips the context menu's opening animation once. */
    public ContextMenu supressOpenAnimationForNextOpen() {
        this.supressOpenAnimationNextOpen = true;
        return this;
    }

    /** Sets scale for this context menu. */
    public ContextMenu setScale(float scale) {
        if (this.forceUIScale) LOGGER.error("[KONKRETE] Unable to set scale of ContextMenu while ContextMenu#isForceUIScale()!");
        this.scale = scale;
        return this;
    }

    /** Returns whether force UI scale. */
    public boolean isForceUIScale() {
        return this.forceUIScale;
    }

    /** Sets force UI scale for this context menu. */
    public ContextMenu setForceUIScale(boolean forceUIScale) {
        this.forceUIScale = forceUIScale;
        return this;
    }

    /** Returns whether always show search bar. */
    public boolean isAlwaysShowSearchBar() {
        return this.alwaysShowSearchBar;
    }

    /** Sets always show search bar for this context menu. */
    public ContextMenu setAlwaysShowSearchBar(boolean alwaysShowSearchBar) {
        this.alwaysShowSearchBar = alwaysShowSearchBar;
        this.updateSearchVisibilityState(false);
        return this;
    }

    private boolean isTopLevelOpenAnimationRunning() {
        if (!UIBase.shouldPlayAnimations() || !this.openAnimationEnabled) return false;
        if (this.isSubMenu() || !this.openAnimationActive) return false;
        float elapsedMs = (float) (net.minecraft.util.Util.getMillis() - this.openAnimationStartMs);
        if (elapsedMs >= OPEN_ANIMATION_GROW_TIME_MS) {
            this.openAnimationActive = false;
            return false;
        }
        return true;
    }

    private float getOpenAnimationScale(float partial) {
        if (!UIBase.shouldPlayAnimations() || !this.openAnimationEnabled) return 1.0F;
        if (this.isSubMenu() || !this.isOpen()) return 1.0F;
        if (!this.isTopLevelOpenAnimationRunning()) return 1.0F;

        float elapsedMs = (float) (net.minecraft.util.Util.getMillis() - this.openAnimationStartMs);
        float growT = Math.min(elapsedMs / OPEN_ANIMATION_GROW_TIME_MS, 1.0F);
        // Ease-out cubic for a quick pop
        float easedGrow = 1.0F - (float) Math.pow(1.0F - growT, 3);
        float baseScale = OPEN_ANIMATION_MIN_SCALE + (1.0F - OPEN_ANIMATION_MIN_SCALE) * easedGrow;
        return Math.max(baseScale, 0.01F);
    }

    /** Returns min distance to screen edge. */
    protected float getMinDistanceToScreenEdge() {
        if (!this.keepDistanceToEdges) return 0;
        return 5;
    }

    /** Clamps the context menu's horizontal position to the visible screen. */
    protected float clampActualXToScreen(float x, float width) {
        float minX = this.getMinDistanceToScreenEdge();
        float maxX = getScreenWidth() - width - this.getMinDistanceToScreenEdge() - 1;
        if (maxX < minX) {
            return minX;
        }
        return Math.max(minX, Math.min(x, maxX));
    }

    /** Returns projected sub menu actual x. */
    protected float getProjectedSubMenuActualX(@NotNull SubMenuOpeningSide side) {
        float scale = UIBase.calculateFixedRenderScale(this.scale);
        float scaledOffsetX = 5.0F * scale;
        if (side == SubMenuOpeningSide.LEFT) {
            return this.parentEntry.parent.getActualX() - this.getScaledWidth() + scaledOffsetX;
        }
        return this.parentEntry.parent.getActualX() + this.parentEntry.parent.getScaledWidth() - scaledOffsetX;
    }

    /** Returns whether projected sub menu within screen bounds. */
    protected boolean isProjectedSubMenuWithinScreenBounds(@NotNull SubMenuOpeningSide side) {
        float projectedX = this.getProjectedSubMenuActualX(side);
        float clampedX = this.clampActualXToScreen(projectedX, this.getScaledWidthWithBorder());
        return Math.abs(projectedX - clampedX) < 0.01F;
    }

    /** Returns projected sub menu chain overlap area. */
    protected float getProjectedSubMenuChainOverlapArea(@NotNull SubMenuOpeningSide side) {
        float projectedX = this.clampActualXToScreen(this.getProjectedSubMenuActualX(side), this.getScaledWidthWithBorder());
        float projectedY = this.getActualY();
        float projectedWidth = this.getScaledWidthWithBorder();
        float projectedHeight = this.getScaledHeightWithBorder();
        float overlapArea = 0.0F;
        for (ContextMenu openAncestor : this.getOpenAncestorMenusForOverlapChecks()) {
            overlapArea += this.getMenuOverlapArea(
                    projectedX,
                    projectedY,
                    projectedWidth,
                    projectedHeight,
                    openAncestor.getActualX(),
                    openAncestor.getActualY(),
                    openAncestor.getScaledWidthWithBorder(),
                    openAncestor.getScaledHeightWithBorder()
            );
        }
        return overlapArea;
    }

    /** Returns menu overlap area. */
    protected float getMenuOverlapArea(float firstX, float firstY, float firstWidth, float firstHeight, float secondX, float secondY, float secondWidth, float secondHeight) {
        float overlapWidth = Math.max(0.0F, Math.min(firstX + firstWidth, secondX + secondWidth) - Math.max(firstX, secondX));
        float overlapHeight = Math.max(0.0F, Math.min(firstY + firstHeight, secondY + secondHeight) - Math.max(firstY, secondY));
        return overlapWidth * overlapHeight;
    }

    /** Returns open ancestor menus for overlap checks. */
    @NotNull
    protected List<ContextMenu> getOpenAncestorMenusForOverlapChecks() {
        List<ContextMenu> openAncestors = new ArrayList<>();
        if (!this.isSubMenu()) {
            return openAncestors;
        }

        // Ignore the direct parent overlap because sub menus intentionally attach to it.
        ContextMenu current = this.parentEntry.parent;
        boolean skipDirectParent = true;
        while (current != null) {
            if (!skipDirectParent && current.isOpen()) {
                openAncestors.add(current);
            }
            skipDirectParent = false;
            SubMenuContextMenuEntry currentParentEntry = current.parentEntry;
            current = (currentParentEntry != null) ? currentParentEntry.parent : null;
        }
        return openAncestors;
    }

    /** Returns preferred non overlapping sub menu opening side. */
    @Nullable
    protected SubMenuOpeningSide getPreferredNonOverlappingSubMenuOpeningSide() {
        if (!this.isSubMenu()) {
            return null;
        }

        SubMenuOpeningSide chainSide = null;
        ContextMenu current = this.parentEntry.parent;
        while (current != null && current.isSubMenu() && current.isOpen()) {
            SubMenuOpeningSide currentSide = current.getPossibleSubMenuOpeningSide();
            if (chainSide == null) {
                chainSide = currentSide;
            } else if (chainSide != currentSide) {
                return null;
            }
            SubMenuContextMenuEntry currentParentEntry = current.parentEntry;
            current = (currentParentEntry != null) ? currentParentEntry.parent : null;
        }

        if (chainSide == null) {
            return null;
        }
        return this.getOppositeSubMenuOpeningSide(chainSide);
    }

    /** Returns opposite sub menu opening side. */
    @NotNull
    protected SubMenuOpeningSide getOppositeSubMenuOpeningSide(@NotNull SubMenuOpeningSide side) {
        return side == SubMenuOpeningSide.LEFT ? SubMenuOpeningSide.RIGHT : SubMenuOpeningSide.LEFT;
    }

    /** Returns actual x. */
    protected float getActualX() {
        if (this.isSubMenu()) {
            SubMenuOpeningSide side = this.getPossibleSubMenuOpeningSide();
            float actualX = this.getProjectedSubMenuActualX(side);
            return this.clampActualXToScreen(actualX, this.getScaledWidthWithBorder());
        }
        if (this.forceRawXY) {
            return this.getX();
        }
        return this.clampActualXToScreen(this.getX(), this.getScaledWidthWithBorder());
    }

    /** Returns actual y. */
    protected float getActualY() {
        float y = this.getY();
        if (this.isSubMenu()) {
            float scale = UIBase.calculateFixedRenderScale(this.scale);
            int scaledOffsetY = (int) (10.0F * scale);
            y = (float) ((float)this.parentEntry.y * scale);
            y += scaledOffsetY;
        }
        if (this.forceRawXY) {
            return y;
        }

        // Calculate the actual height to use for positioning considering scaling
        float menuScale = UIBase.calculateFixedRenderScale(this.scale);
        float heightToUse;

        if (this.needsScrolling) {
            // Use the actual display height that's determined during rendering
            // This is already capped at 70% of screen height in logical coordinates
            heightToUse = this.displayHeight * menuScale + this.getBorderThickness() * 2 * menuScale;
        } else {
            heightToUse = this.getScaledHeightWithBorder();
        }

        // Make sure the menu stays fully on screen
        if ((y + heightToUse) >= (getScreenHeight() - this.getMinDistanceToScreenEdge())) {
            return getScreenHeight() - heightToUse - this.getMinDistanceToScreenEdge() - 1;
        }

        return Math.max(y, this.getMinDistanceToScreenEdge());
    }

    /** Returns possible sub menu opening side. */
    @NotNull
    protected SubMenuOpeningSide getPossibleSubMenuOpeningSide() {
        if (this.forceSide) {
            return this.subMenuOpeningSide;
        }
        if (!this.isSubMenu()) {
            return this.subMenuOpeningSide;
        }

        boolean canOpenLeft = this.isProjectedSubMenuWithinScreenBounds(SubMenuOpeningSide.LEFT);
        boolean canOpenRight = this.isProjectedSubMenuWithinScreenBounds(SubMenuOpeningSide.RIGHT);
        if (canOpenLeft != canOpenRight) {
            return canOpenLeft ? SubMenuOpeningSide.LEFT : SubMenuOpeningSide.RIGHT;
        }

        float leftOverlapArea = this.getProjectedSubMenuChainOverlapArea(SubMenuOpeningSide.LEFT);
        float rightOverlapArea = this.getProjectedSubMenuChainOverlapArea(SubMenuOpeningSide.RIGHT);
        int overlapCompare = Float.compare(leftOverlapArea, rightOverlapArea);
        if (overlapCompare != 0) {
            return leftOverlapArea < rightOverlapArea ? SubMenuOpeningSide.LEFT : SubMenuOpeningSide.RIGHT;
        }

        SubMenuOpeningSide preferredSide = this.getPreferredNonOverlappingSubMenuOpeningSide();
        if (preferredSide != null) {
            return preferredSide;
        }

        return this.subMenuOpeningSide;
    }

    /** Returns border thickness. */
    public float getBorderThickness() {
        return 1;
    }

    /** Returns scaled border thickness. */
    public float getScaledBorderThickness() {
        float scale = UIBase.calculateFixedRenderScale(this.scale);
        return (float)((float)this.getBorderThickness() * scale);
    }

    /** Returns the horizontal position in GUI units. */
    public float getX() {
        return this.rawX;
    }

    /** Returns the vertical position in GUI units. */
    public float getY() {
        return this.rawY;
    }

    /** Returns the current width in GUI units. */
    public float getWidth() {
        return this.rawWidth;
    }

    /** Returns the content width plus both borders in GUI units. */
    public float getWidthWithBorder() {
        return this.getWidth() + (this.getBorderThickness() * 2);
    }

    /** Returns scaled width. */
    public float getScaledWidth() {
        float scale = UIBase.calculateFixedRenderScale(this.scale);
        return (float) ((float)this.getWidth() * scale);
    }

    /** Returns scaled width with border. */
    public float getScaledWidthWithBorder() {
        return this.getScaledWidth() + (this.getScaledBorderThickness() * 2);
    }

    /** Returns the current height in GUI units. */
    public float getHeight() {
        return this.rawHeight;
    }

    /** Returns the content height plus both borders in GUI units. */
    public float getHeightWithBorder() {
        return this.getHeight() + (this.getBorderThickness() * 2);
    }

    /** Returns scaled height. */
    public float getScaledHeight() {
        float scale = UIBase.calculateFixedRenderScale(this.scale);
        if (this.needsScrolling) {
            return (float)((float)this.displayHeight * scale);
        }
        return (float)((float)this.getHeight() * scale);
    }

    /** Returns scaled height with border. */
    public float getScaledHeightWithBorder() {
        return this.getScaledHeight() + (this.getScaledBorderThickness() * 2);
    }

    /** Reports whether the pointer currently hovers this element. */
    public boolean isHovered() {
        if (!this.isOpen()) return false;
        for (ContextMenuEntry<?> e : this.entries) {
            if (e.isHovered()) return true;
        }
        return false;
    }

    /** Clears hover state from every context-menu entry. */
    protected ContextMenu unhoverAllEntries() {
        for (ContextMenuEntry<?> e : this.entries) {
            e.setHovered(false);
        }
        return this;
    }

    /** Reports whether text or geometry is drawn with a shadow. */
    public boolean hasShadow() {
        return this.shadow;
    }

    /** Sets shadow for this context menu. */
    public ContextMenu setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /** Returns whether keep distance to edges. */
    public boolean isKeepDistanceToEdges() {
        return this.keepDistanceToEdges;
    }

    /** Sets keep distance to edges for this context menu. */
    public ContextMenu setKeepDistanceToEdges(boolean keepDistanceToEdges) {
        this.keepDistanceToEdges = keepDistanceToEdges;
        return this;
    }

    /** Returns whether force raw xy. */
    public boolean isForceRawXY() {
        return this.forceRawXY;
    }

    /** Sets force raw xy for this context menu. */
    public ContextMenu setForceRawXY(boolean forceRawXY) {
        this.forceRawXY = forceRawXY;
        return this;
    }

    /** Sets rounded corners for this context menu. */
    public ContextMenu setRoundedCorners(boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        this.roundTopLeftCorner = topLeft;
        this.roundTopRightCorner = topRight;
        this.roundBottomLeftCorner = bottomLeft;
        this.roundBottomRightCorner = bottomRight;
        return this;
    }

    /** Returns whether force side. */
    public boolean isForceSide() {
        return this.forceSide;
    }

    /** Sets force side for this context menu. */
    public ContextMenu setForceSide(boolean forceSide) {
        this.forceSide = forceSide;
        return this;
    }

    /** Returns whether force side sub menus. */
    public boolean isForceSideSubMenus() {
        return this.forceSideSubMenus;
    }

    /** Sets force side sub menus for this context menu. */
    public ContextMenu setForceSideSubMenus(boolean forceSideSubMenus) {
        this.forceSideSubMenus = forceSideSubMenus;
        return this;
    }

    /** Returns sub menu opening side. */
    @NotNull
    public SubMenuOpeningSide getSubMenuOpeningSide() {
        return this.subMenuOpeningSide;
    }

    /** Sets sub menu opening side for this context menu. */
    public ContextMenu setSubMenuOpeningSide(@NotNull SubMenuOpeningSide subMenuOpeningSide) {
        Objects.requireNonNull(subMenuOpeningSide);
        this.subMenuOpeningSide = subMenuOpeningSide;
        return this;
    }

    /** Returns whether sub menu. */
    public boolean isSubMenu() {
        return this.parentEntry != null;
    }

    /** Returns parent entry. */
    @Nullable
    public SubMenuContextMenuEntry getParentEntry() {
        return this.parentEntry;
    }

    /** Disconnects this submenu from its owning parent entry. */
    public ContextMenu detachFromParentEntry() {
        this.parentEntry = null;
        return this;
    }

    /** Returns whether sub menu hovered. */
    public boolean isSubMenuHovered() {
        if (!this.isOpen()) return false;
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isHovered()) return true;
            }
        }
        return false;
    }

    /** Returns whether sub menu open. */
    public boolean isSubMenuOpen() {
        if (!this.isOpen()) return false;
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isOpen()) return true;
            }
        }
        return false;
    }

    /**
     * Opens the {@link ContextMenu} at the given X and Y coordinates.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param entryPath The {@link SubMenuContextMenuEntry} path of menus to open.
     */
    public ContextMenu openMenuAt(float x, float y, @Nullable List<String> entryPath) {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.resetSearchState();
        if (!this.isSubMenu()) {
            this.resetArrowNavigationState();
        }
        this.rawX = x;
        this.rawY = y;
        this.open = true;
        boolean supressOpenAnimation = this.supressOpenAnimationNextOpen;
        this.supressOpenAnimationNextOpen = false;
        if (!this.isSubMenu() && UIBase.shouldPlayAnimations() && this.openAnimationEnabled && !supressOpenAnimation) {
            this.openAnimationStartMs = net.minecraft.util.Util.getMillis();
            this.openAnimationActive = true;
        } else {
            this.openAnimationActive = false;
        }
        this.scrollPosition = 0.0f; // Reset scroll position when opening menu
        if ((entryPath != null) && !entryPath.isEmpty()) {
            String firstId = entryPath.get(0);
            ContextMenuEntry<?> entry = this.getEntry(firstId);
            if (entry instanceof SubMenuContextMenuEntry sub) {
                sub.openSubMenu((entryPath.size() > 1) ? entryPath.subList(1, entryPath.size()) : null);
            }
        }
        return this;
    }

    /**
     * Opens the {@link ContextMenu} at the given X and Y coordinates.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public ContextMenu openMenuAt(float x, float y) {
        return this.openMenuAt(x, y, null);
    }

    /**
     * Opens the {@link ContextMenu} at the mouse position.
     *
     * @param entryPath The {@link SubMenuContextMenuEntry} path of menus to open.
     */
    public ContextMenu openMenuAtMouse(@Nullable List<String> entryPath) {
        return this.openMenuAt(MouseInput.getMouseX(), MouseInput.getMouseY(), entryPath);
    }

    /**
     * Opens the {@link ContextMenu} at the mouse position.
     */
    public ContextMenu openMenuAtMouse() {
        return this.openMenuAtMouse(null);
    }

    /**
     * Closes this menu, clears hover state, and closes any open sub-menus attached to it.
     */
    public ContextMenu closeMenu() {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.open = false;
        this.openAnimationActive = false;
        this.resetSearchState();
        ContextMenu root = this.getRootMenu();
        if (root.arrowNavigationMenu == this || root == this) {
            root.resetArrowNavigationState();
        }
        if (root.cachedSearchMenu == this) {
            root.cachedSearchMenu = null;
        }
        return this;
    }

    /**
     * Closes this menu and every parent menu in the chain up to the root.
     * This also closes any open sub-menus on each menu in the chain.
     */
    public ContextMenu closeMenuChain() {
        ContextMenu current = this;
        while (current != null) {
            current.closeMenu();
            SubMenuContextMenuEntry parent = current.parentEntry;
            current = (parent != null) ? parent.parent : null;
        }
        return this;
    }

    /**
     * Closes all sub-menus that belong to this menu without closing the menu itself.
     */
    public ContextMenu closeSubMenus() {
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                s.subContextMenu.closeMenu();
            }
        }
        return this;
    }

    /** Closes sub menus except. */
    protected ContextMenu closeSubMenusExcept(@Nullable SubMenuContextMenuEntry keepOpen) {
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (keepOpen == null || s != keepOpen) {
                    s.subContextMenu.closeMenu();
                }
            }
        }
        return this;
    }

    /** Returns whether open. */
    public boolean isOpen() {
        return this.open;
    }

    /** Returns whether user navigating in menu. */
    public boolean isUserNavigatingInMenu() {
        if (!this.isOpen()) return false;
        if (UIBase.shouldPlayAnimations() && this.openAnimationEnabled && this.isTopLevelOpenAnimationRunning()) {
            return true;
        }
        int mouseX = MouseInput.getMouseX();
        int mouseY = MouseInput.getMouseY();
        return this.getMenuUnderCursor(mouseX, mouseY) != null;
    }

    /** Returns whether user navigating in sub menu. */
    public boolean isUserNavigatingInSubMenu() {
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isUserNavigatingInMenu()) return true;
            }
        }
        return false;
    }

    /** Returns stackable entries. */
    @NotNull
    protected List<ContextMenuEntry<?>> getStackableEntries() {
        List<ContextMenuEntry<?>> l = new ArrayList<>();
        for (ContextMenuEntry<?> e : this.entries) {
            if (e.isStackable()) l.add(e);
        }
        return l;
    }

    // Helper to check if an entry is currently visible in the scrollable area
    private boolean isEntryVisible(ContextMenuEntry<?> entry) {
        if (!this.needsScrolling) return true;

        float scale = UIBase.calculateFixedRenderScale(this.getScale());
        float scaledY = (float)((float)this.getActualY()/scale) + this.getBorderThickness();

        float entryTop = entry.y;
        float entryBottom = entry.y + entry.getHeight();
        float visibleTop = scaledY + SCROLL_INDICATOR_HEIGHT;
        float visibleBottom = scaledY + this.displayHeight - SCROLL_INDICATOR_HEIGHT;

        return entryTop < visibleBottom && entryBottom > visibleTop;
    }

    private boolean isArrowKey(int keyCode) {
        return keyCode == InputConstants.KEY_UP
                || keyCode == InputConstants.KEY_DOWN
                || keyCode == InputConstants.KEY_LEFT
                || keyCode == InputConstants.KEY_RIGHT;
    }

    private boolean isTabKey(int keyCode) {
        return keyCode == InputConstants.KEY_TAB;
    }

    private boolean isEnterKey(int keyCode) {
        return keyCode == InputConstants.KEY_ENTER
                || keyCode == InputConstants.KEY_NUMPADENTER;
    }

    private boolean isEscapeKey(int keyCode) {
        return keyCode == InputConstants.KEY_ESCAPE;
    }

    private boolean isNavigationConsumeKey(int keyCode) {
        return this.isArrowKey(keyCode) || this.isTabKey(keyCode) || this.isEnterKey(keyCode) || this.isEscapeKey(keyCode);
    }

    private void updateArrowNavigationMouseState(int mouseX, int mouseY) {
        if (!this.arrowNavigationActive) {
            this.arrowNavigationMouseX = mouseX;
            this.arrowNavigationMouseY = mouseY;
            return;
        }
        if (mouseX != this.arrowNavigationMouseX || mouseY != this.arrowNavigationMouseY) {
            if (this.getMenuUnderCursor(mouseX, mouseY) != null) {
                this.resetArrowNavigationState();
            }
            this.arrowNavigationMouseX = mouseX;
            this.arrowNavigationMouseY = mouseY;
        }
    }

    private void clearArrowNavigationIfMenuClosed() {
        if (this.arrowNavigationMenu != null && !this.arrowNavigationMenu.isOpen()) {
            this.resetArrowNavigationState();
        }
    }

    private void validateArrowNavigationSelection() {
        if (!this.arrowNavigationActive || this.arrowNavigationMenu == null) {
            return;
        }
        List<ContextMenuEntry<?>> navigable = this.arrowNavigationMenu.getNavigableEntries();
        if (this.arrowNavigationEntry != null && !navigable.contains(this.arrowNavigationEntry)) {
            this.arrowNavigationMenu.unhoverAllEntries();
            this.arrowNavigationEntry = null;
        }
    }

    private void resetArrowNavigationState() {
        if (this.arrowNavigationMenu != null) {
            this.arrowNavigationMenu.unhoverAllEntries();
        }
        this.arrowNavigationActive = false;
        this.arrowNavigationMenu = null;
        this.arrowNavigationEntry = null;
    }

    private void onSearchFilterChanged() {
        ContextMenu root = this.getRootMenu();
        if (!this.isSearchEntryVisible()) {
            return;
        }
        String searchText = this.getActiveSearchText();
        if (searchText == null || searchText.isBlank()) {
            return;
        }
        if (!root.arrowNavigationActive || root.arrowNavigationMenu != this) {
            root.activateArrowNavigation(this);
        }
        List<ContextMenuEntry<?>> navigable = this.getNavigableEntries();
        ContextMenuEntry<?> first = navigable.isEmpty() ? null : navigable.get(0);
        root.setArrowNavigationSelection(this, first);
    }

    private void activateArrowNavigation(@NotNull ContextMenu menu) {
        if (!this.arrowNavigationActive) {
            this.cachedSearchMenu = null;
        }
        if (this.arrowNavigationMenu != menu) {
            if (this.arrowNavigationMenu != null) {
                this.arrowNavigationMenu.unhoverAllEntries();
            }
            this.arrowNavigationMenu = menu;
            this.arrowNavigationEntry = null;
        }
        menu.unhoverAllEntries();
        this.arrowNavigationActive = true;
        this.arrowNavigationMouseX = MouseInput.getMouseX();
        this.arrowNavigationMouseY = MouseInput.getMouseY();
    }

    private void setArrowNavigationSelection(@NotNull ContextMenu menu, @Nullable ContextMenuEntry<?> entry) {
        if (this.arrowNavigationMenu != menu) {
            if (this.arrowNavigationMenu != null) {
                this.arrowNavigationMenu.unhoverAllEntries();
            }
            this.arrowNavigationMenu = menu;
        }
        if (this.arrowNavigationEntry == entry && this.arrowNavigationMenu == menu) {
            this.arrowNavigationActive = true;
            this.arrowNavigationMouseX = MouseInput.getMouseX();
            this.arrowNavigationMouseY = MouseInput.getMouseY();
            return;
        }
        this.arrowNavigationEntry = entry;
        this.arrowNavigationActive = true;
        this.arrowNavigationMouseX = MouseInput.getMouseX();
        this.arrowNavigationMouseY = MouseInput.getMouseY();

        menu.unhoverAllEntries();
        if (entry != null) {
            entry.setHovered(true);
            if (entry.hoverAction != null) {
                entry.hoverAction.run(menu, entry, false);
            }
            menu.scrollEntryIntoView(entry);
            if (entry instanceof SubMenuContextMenuEntry sub) {
                menu.closeSubMenusExcept(sub);
                if (sub.isActive() && !sub.subContextMenu.isOpen()) {
                    sub.openSubMenu();
                }
            } else {
                menu.closeSubMenusExcept(null);
            }
        }
    }

    private void moveArrowSelection(@NotNull ContextMenu menu, int direction) {
        List<ContextMenuEntry<?>> navigable = menu.getNavigableEntries();
        if (navigable.isEmpty()) {
            this.setArrowNavigationSelection(menu, null);
            return;
        }
        int index = -1;
        if (this.arrowNavigationMenu == menu && this.arrowNavigationEntry != null) {
            index = navigable.indexOf(this.arrowNavigationEntry);
        }
        if (index < 0) {
            index = direction > 0 ? 0 : navigable.size() - 1;
        } else {
            int size = navigable.size();
            int nextIndex = index + direction;
            if (nextIndex < 0) {
                nextIndex = size - 1;
            } else if (nextIndex >= size) {
                nextIndex = 0;
            }
            index = nextIndex;
        }
        this.setArrowNavigationSelection(menu, navigable.get(index));
    }

    private void handleArrowHorizontalNavigation(@NotNull ContextMenu menu, int keyCode) {
        boolean goLeft = keyCode == InputConstants.KEY_LEFT;
        boolean goRight = keyCode == InputConstants.KEY_RIGHT;

        if (menu.isSubMenu()) {
            SubMenuContextMenuEntry parentEntry = menu.getParentEntry();
            if (parentEntry != null) {
                boolean opensRight = menu.getPossibleSubMenuOpeningSide() == SubMenuOpeningSide.RIGHT;
                if ((opensRight && goLeft) || (!opensRight && goRight)) {
                    menu.closeMenu();
                    this.setArrowNavigationSelection(parentEntry.parent, parentEntry);
                    return;
                }
            }
        }

        if (this.arrowNavigationEntry instanceof SubMenuContextMenuEntry sub) {
            boolean opensRight = sub.subContextMenu.getPossibleSubMenuOpeningSide() == SubMenuOpeningSide.RIGHT;
            if ((opensRight && goRight) || (!opensRight && goLeft)) {
                if (sub.isActive()) {
                    if (!sub.subContextMenu.isOpen()) {
                        menu.closeSubMenusExcept(sub);
                        sub.openSubMenu();
                    }
                    this.jumpToSubMenu(sub);
                }
            }
        }
    }

    private void jumpToSubMenu(@NotNull SubMenuContextMenuEntry entry) {
        ContextMenu subMenu = entry.getSubContextMenu();
        List<ContextMenuEntry<?>> navigable = subMenu.getNavigableEntries();
        if (navigable.isEmpty()) {
            this.setArrowNavigationSelection(subMenu, null);
            return;
        }
        this.setArrowNavigationSelection(subMenu, navigable.get(0));
    }

    private void performArrowNavigationClick() {
        if (this.arrowNavigationMenu == null || this.arrowNavigationEntry == null) {
            return;
        }
        ContextMenuEntry<?> entry = this.arrowNavigationEntry;
        if (!entry.isVisible()) {
            return;
        }
        entry.setHovered(true);
        entry.mouseClicked(entry.x + 1.0F, entry.y + 1.0F, 0);
    }

    private void scrollEntryIntoView(@NotNull ContextMenuEntry<?> entry) {
        if (!this.needsScrolling) {
            return;
        }
        float scale = UIBase.calculateFixedRenderScale(this.getScale());
        float scaledY = (float)((float)this.getActualY()/scale) + this.getBorderThickness();
        float visibleTop = scaledY + SCROLL_INDICATOR_HEIGHT;
        float visibleBottom = scaledY + this.displayHeight - SCROLL_INDICATOR_HEIGHT;
        float entryTop = entry.y;
        float entryBottom = entry.y + entry.getHeight();
        float maxScrollPosition = this.rawHeight - (this.displayHeight - SCROLL_INDICATOR_HEIGHT * 2);
        if (maxScrollPosition < 0.0F) {
            maxScrollPosition = 0.0F;
        }

        if (entryTop < visibleTop) {
            this.scrollPosition = Math.max(0.0F, this.scrollPosition - (visibleTop - entryTop));
        } else if (entryBottom > visibleBottom) {
            this.scrollPosition = Math.min(maxScrollPosition, this.scrollPosition + (entryBottom - visibleBottom));
        }
    }

    @NotNull
    private List<ContextMenuEntry<?>> getNavigableEntries() {
        String searchText = this.getActiveSearchText();
        String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
        boolean filterActive = (searchLower != null) && !searchLower.isBlank();
        List<ContextMenuEntry<?>> navigable = new ArrayList<>();
        for (ContextMenuEntry<?> entry : this.entries) {
            if (!entry.isVisible()) continue;
            if (filterActive && !this.matchesSearchFilter(entry, searchLower)) continue;
            if (entry instanceof SeparatorContextMenuEntry || entry instanceof SpacerContextMenuEntry || entry instanceof SearchContextMenuEntry) continue;
            if (!entry.isActive()) continue;
            navigable.add(entry);
        }
        return navigable;
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isUserNavigatingInMenu()) {
            float scale = UIBase.calculateFixedRenderScale(this.scale) * this.getOpenAnimationScale(0.0F);
            int scaledMouseX = (int) ((float)mouseX / scale);
            int scaledMouseY = (int) ((float)mouseY / scale);
            String searchText = this.getActiveSearchText();
            String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
            boolean filterActive = (searchLower != null) && !searchLower.isBlank();

            // Check if click is on scroll arrow areas first
            if (button == 0 && this.needsScrolling && this.isMouseOverMenu(mouseX, mouseY)) {
                float scaledX = (float)((float)this.getActualX()/scale) + this.getBorderThickness();
                float scaledY = (float)((float)this.getActualY()/scale) + this.getBorderThickness();

                // Calculate max scroll position
                float maxScrollPosition = this.rawHeight - (this.displayHeight - SCROLL_INDICATOR_HEIGHT * 2);

                // Check if clicking in up arrow area
                if (scaledMouseY >= scaledY && scaledMouseY <= scaledY + SCROLL_INDICATOR_HEIGHT) {
                    // Only scroll if the arrow is actually visible (can scroll up)
                    if (this.scrollPosition > 0) {
                        // Scroll up by a fixed amount (e.g., 3 entries worth)
                        this.scrollPosition = Math.max(0, this.scrollPosition - 60);
                        // Close sub-menus when scrolling
                        this.closeSubMenus();
                    }
                    // Always consume the click in the arrow area
                    return true;
                }

                // Check if clicking in down arrow area
                if (scaledMouseY >= scaledY + this.displayHeight - SCROLL_INDICATOR_HEIGHT &&
                    scaledMouseY <= scaledY + this.displayHeight) {
                    // Only scroll if the arrow is actually visible (can scroll down)
                    if (this.scrollPosition < maxScrollPosition) {
                        // Scroll down by a fixed amount (e.g., 3 entries worth)
                        this.scrollPosition = Math.min(maxScrollPosition, this.scrollPosition + 60);
                        // Close sub-menus when scrolling
                        this.closeSubMenus();
                    }
                    // Always consume the click in the arrow area
                    return true;
                }
            }

            // Process entries only if they're visible in the scroll area
            for (ContextMenuEntry<?> entry : this.entries) {
                if (!entry.isVisible()) continue;
                if (filterActive && !this.matchesSearchFilter(entry, searchLower)) continue;
                if (!this.needsScrolling || isEntryVisible(entry)) {
                    entry.mouseClicked(scaledMouseX, scaledMouseY, button);
                }
            }

            //Handle click for sub context menus
            for (ContextMenuEntry<?> e : this.entries) {
                if (e instanceof SubMenuContextMenuEntry s) {
                    s.subContextMenu.mouseClicked(mouseX, mouseY, button);
                }
            }
            return true;
        }
        return GuiEventListener.super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isUserNavigatingInMenu()) {
            float scale = UIBase.calculateFixedRenderScale(this.scale) * this.getOpenAnimationScale(0.0F);
            int scaledMouseX = (int) ((float)mouseX / scale);
            int scaledMouseY = (int) ((float)mouseY / scale);
            String searchText = this.getActiveSearchText();
            String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
            boolean filterActive = (searchLower != null) && !searchLower.isBlank();

            for (ContextMenuEntry<?> entry : this.entries) {
                if (!entry.isVisible()) continue;
                if (filterActive && !this.matchesSearchFilter(entry, searchLower)) continue;
                if (!this.needsScrolling || isEntryVisible(entry)) {
                    entry.mouseReleased(scaledMouseX, scaledMouseY, button);
                }
            }
            for (ContextMenuEntry<?> e : this.entries) {
                if (e instanceof SubMenuContextMenuEntry s) {
                    s.subContextMenu.mouseReleased(mouseX, mouseY, button);
                }
            }
            return true;
        }
        return GuiEventListener.super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isUserNavigatingInMenu()) {
            float scale = UIBase.calculateFixedRenderScale(this.scale) * this.getOpenAnimationScale(0.0F);
            int scaledMouseX = (int) ((float)mouseX / scale);
            int scaledMouseY = (int) ((float)mouseY / scale);
            double scaledDragX = dragX / scale;
            double scaledDragY = dragY / scale;
            String searchText = this.getActiveSearchText();
            String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
            boolean filterActive = (searchLower != null) && !searchLower.isBlank();

            for (ContextMenuEntry<?> entry : this.entries) {
                if (!entry.isVisible()) continue;
                if (filterActive && !this.matchesSearchFilter(entry, searchLower)) continue;
                if (!this.needsScrolling || isEntryVisible(entry)) {
                    entry.mouseDragged(scaledMouseX, scaledMouseY, button, scaledDragX, scaledDragY);
                }
            }
            for (ContextMenuEntry<?> e : this.entries) {
                if (e instanceof SubMenuContextMenuEntry s) {
                    s.subContextMenu.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                }
            }
            return true;
        }
        return GuiEventListener.super.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), dragX, dragY);
    }

    /** Routes wheel scrolling and reports whether it was consumed. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {

        if (this.isOpen()) {

            if (this.needsScrolling && this.isMouseOverMenu(mouseX, mouseY)) {
                // Close all sub-menus when scrolling in the parent menu
                this.closeSubMenus();

                // Update scroll position (scrollDeltaY is negative when scrolling down)
                this.scrollPosition -= scrollDeltaY * 30.0; // Adjust scroll speed

                // Clamp scroll position
                float maxScrollPosition = this.rawHeight - (this.displayHeight - SCROLL_INDICATOR_HEIGHT * 2);
                this.scrollPosition = Math.max(0, Math.min(this.scrollPosition, maxScrollPosition));

                return true; // We handled the scroll
            }

            // Check if any submenu can handle the scroll
            for (ContextMenuEntry<?> e : this.entries) {
                if (e instanceof SubMenuContextMenuEntry s) {
                    if (s.subContextMenu.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
                        return true;
                    }
                }
            }

        }

        return GuiEventListener.super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);

    }

    // Use dedicated menu hit testing so container-level routing does not treat this overlay as a normal child.
    /** Returns whether mouse over menu. */
    public boolean isMouseOverMenu(double mouseX, double mouseY) {
        float scale = UIBase.calculateFixedRenderScale(this.getScale()) * this.getOpenAnimationScale(0.0F);
        float actualX = this.getActualX() / scale + this.getBorderThickness();
        float actualY = this.getActualY() / scale + this.getBorderThickness();
        float width = this.getWidth();
        float height = this.needsScrolling ? this.displayHeight : this.getHeight();

        return mouseX >= actualX * scale &&
                mouseX <= (actualX + width) * scale &&
                mouseY >= actualY * scale &&
                mouseY <= (actualY + height) * scale;
    }

    // Context menus are routed through ContextMenuHandler instead of vanilla child hit testing.
    /** Reports whether the current pointer position lies inside this element's hitbox. */
    @Override
    public boolean isMouseOver(double $$0, double $$1) {
        return false;
    }

    /** Sets focused for this context menu. */
    @Override
    public void setFocused(boolean var1) {
    }

    /** Reports whether keyboard focus currently targets this control. */
    @Override
    public boolean isFocused() {
        return false;
    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isOpen()) return false;
        ContextMenu root = this.getRootMenu();
        root.clearCachedSearchMenuIfClosed();
        root.clearArrowNavigationIfMenuClosed();
        root.validateArrowNavigationSelection();
        int mouseX = MouseInput.getMouseX();
        int mouseY = MouseInput.getMouseY();
        ContextMenu hoverMenu = root.getMenuUnderCursor(mouseX, mouseY);
        ContextMenu targetMenu = (hoverMenu != null) ? hoverMenu : ((root.cachedSearchMenu != null) ? root.cachedSearchMenu : root);
        if (root.arrowNavigationActive && root.arrowNavigationMenu != null) {
            targetMenu = root.arrowNavigationMenu;
        }
        if (root.isNavigationConsumeKey(keyCode)) {
            if (root.isEscapeKey(keyCode)) {
                root.closeMenuChain();
                return true;
            }
            if (root.isEnterKey(keyCode)) {
                root.performArrowNavigationClick();
                return true;
            }
            if (root.isArrowKey(keyCode)) {
                ContextMenu activeMenu = (root.arrowNavigationActive && root.arrowNavigationMenu != null) ? root.arrowNavigationMenu : targetMenu;
                root.activateArrowNavigation(activeMenu);
                if (keyCode == InputConstants.KEY_UP) {
                    root.moveArrowSelection(activeMenu, -1);
                } else if (keyCode == InputConstants.KEY_DOWN) {
                    root.moveArrowSelection(activeMenu, 1);
                } else {
                    root.handleArrowHorizontalNavigation(activeMenu, keyCode);
                }
                return true;
            }
            if (root.isTabKey(keyCode)) {
                ContextMenu activeMenu = (root.arrowNavigationActive && root.arrowNavigationMenu != null) ? root.arrowNavigationMenu : targetMenu;
                root.activateArrowNavigation(activeMenu);
                int direction = net.minecraft.client.Minecraft.getInstance().hasShiftDown() ? -1 : 1;
                root.moveArrowSelection(activeMenu, direction);
                return true;
            }
        }
        if (targetMenu.isSearchShortcut(keyCode, modifiers)) {
            if (!root.arrowNavigationActive && hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            if (targetMenu.isAlwaysShowSearchBar()) {
                targetMenu.showSearchEntry(true);
            } else if (targetMenu.isSearchEntryVisible()) {
                targetMenu.hideSearchEntry();
            } else {
                targetMenu.showSearchEntry(true);
            }
            return true;
        }
        if (targetMenu.searchEntry.isVisible() && targetMenu.searchEntry.keyPressed(keyCode, scanCode, modifiers)) {
            if (!root.arrowNavigationActive && hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            return true;
        }
        if (targetMenu != root && root.searchEntry.isVisible() && root.searchEntry.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return GuiEventListener.super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    /** Routes a key release and reports whether it was consumed. */
    @Override
    public boolean keyReleased(KeyEvent event) {
        return this.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key release and reports whether it was consumed. */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!this.isOpen()) return false;
        ContextMenu root = this.getRootMenu();
        root.clearCachedSearchMenuIfClosed();
        if (root.isNavigationConsumeKey(keyCode)) {
            return true;
        }
        int mouseX = MouseInput.getMouseX();
        int mouseY = MouseInput.getMouseY();
        ContextMenu hoverMenu = root.getMenuUnderCursor(mouseX, mouseY);
        ContextMenu targetMenu = (hoverMenu != null) ? hoverMenu : ((root.cachedSearchMenu != null) ? root.cachedSearchMenu : root);
        if (root.arrowNavigationActive && root.arrowNavigationMenu != null) {
            targetMenu = root.arrowNavigationMenu;
        }
        if (targetMenu.searchEntry.isVisible() && targetMenu.searchEntry.keyReleased(keyCode, scanCode, modifiers)) {
            if (!root.arrowNavigationActive && hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            return true;
        }
        if (targetMenu != root && root.searchEntry.isVisible() && root.searchEntry.keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        }
        return GuiEventListener.super.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
    }

    /** Routes typed character input and reports whether it was consumed. */
    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), 0);
    }

    /** Routes typed character input and reports whether it was consumed. */
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.isOpen()) return false;
        ContextMenu root = this.getRootMenu();
        root.clearCachedSearchMenuIfClosed();
        int mouseX = MouseInput.getMouseX();
        int mouseY = MouseInput.getMouseY();
        ContextMenu hoverMenu = root.getMenuUnderCursor(mouseX, mouseY);
        ContextMenu targetMenu = (hoverMenu != null) ? hoverMenu : ((root.cachedSearchMenu != null) ? root.cachedSearchMenu : root);
        if (root.arrowNavigationActive && root.arrowNavigationMenu != null) {
            targetMenu = root.arrowNavigationMenu;
        }
        if (!targetMenu.searchEntry.isVisible() && !Character.isISOControl(codePoint)) {
            if (!root.arrowNavigationActive && hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            targetMenu.showSearchEntry(true);
        }
        if (targetMenu.searchEntry.isVisible() && targetMenu.searchEntry.charTyped(codePoint, modifiers)) {
            if (!root.arrowNavigationActive && hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            return true;
        }
        if (targetMenu != root && root.searchEntry.isVisible() && root.searchEntry.charTyped(codePoint, modifiers)) {
            return true;
        }
        return GuiEventListener.super.charTyped(new CharacterEvent(codePoint));
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

    /** Reports whether this control may receive keyboard focus. */
    @Override
    public boolean isFocusable() {
        return false;
    }

    /** Sets focusable for this context menu. */
    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("ContextMenus are not focusable!");
    }

    /** Reports whether keyboard navigation may target this control. */
    @Override
    public boolean isNavigatable() {
        return false;
    }

    /** Sets navigatable for this context menu. */
    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("ContextMenus are not navigatable!");
    }

    /** Returns the active screen width in GUI units. */
    protected static int getScreenWidth() {
        Screen s = ScreenUtils.getScreen();
        if (s != null) {
            return s.width;
        }
        return 1;
    }

    /** Returns the active screen height in GUI units. */
    protected static int getScreenHeight() {
        Screen s = ScreenUtils.getScreen();
        if (s != null) {
            return s.height;
        }
        return 1;
    }

    /** Returns whether search entry visible. */
    protected boolean isSearchEntryVisible() {
        return this.alwaysShowSearchBar || this.searchEntryRequested;
    }

    /** Opens search entry. */
    protected void showSearchEntry(boolean focus) {
        this.searchEntryRequested = true;
        this.scrollPosition = 0.0F;
        this.searchEntry.prepareForImmediateInput(this.shouldAddIconSpaceForEntries());
        this.updateSearchVisibilityState(focus);
    }

    /** Closes search entry. */
    protected void hideSearchEntry() {
        this.searchEntryRequested = false;
        this.updateSearchVisibilityState(false);
    }

    /** Clears the active query and restores all searchable entries. */
    protected void resetSearchState() {
        this.searchEntryRequested = false;
        this.searchEntry.resetSearchValue();
        this.searchEntryVisibleLast = this.isSearchEntryVisible();
    }

    /** Refreshes search visibility state from current state. */
    protected void updateSearchVisibilityState(boolean focusOnShow) {
        boolean visible = this.isSearchEntryVisible();
        if (visible != this.searchEntryVisibleLast) {
            if (!visible) {
                this.searchEntry.resetSearchValue();
            } else if (focusOnShow) {
                this.searchEntry.focusAndSelectAll();
            }
        } else if (visible && focusOnShow) {
            this.searchEntry.focusAndSelectAll();
        }
        this.searchEntryVisibleLast = visible;
    }

    /** Returns active search text. */
    @Nullable
    protected String getActiveSearchText() {
        if (!this.isSearchEntryVisible()) return null;
        String value = this.searchEntry.getSearchValue();
        return value.isBlank() ? null : value;
    }

    /** Returns whether add icon space for entries. */
    protected boolean shouldAddIconSpaceForEntries() {
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof ClickableContextMenuEntry<?> c) {
                if (c.hasIconAssigned()) {
                    return true;
                }
            }
            if (e instanceof SearchContextMenuEntry s) {
                if (s.hasIconAssigned() && s.isVisible()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns whether search filter. */
    protected boolean matchesSearchFilter(@NotNull ContextMenuEntry<?> entry, @NotNull String searchLower) {
        if ((entry == this.searchEntry) || (entry == this.searchSeparator)) {
            return true;
        }
        if (entry instanceof SeparatorContextMenuEntry || entry instanceof SpacerContextMenuEntry) {
            return true;
        }
        if (entry instanceof ClickableContextMenuEntry<?> clickable) {
            Component label = clickable.getLabel();
            String labelLower = label.getString().toLowerCase(Locale.ROOT);
            if (labelLower.contains(searchLower)) {
                return true;
            }
            String trimmedSearch = searchLower.trim();
            if (trimmedSearch.isEmpty()) {
                return true;
            }
            String[] parts = trimmedSearch.split("\\s+");
            if (parts.length <= 1) {
                return false;
            }
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                if (!labelLower.contains(part)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** Returns whether search shortcut. */
    protected boolean isSearchShortcut(int keyCode, int modifiers) {
        return keyCode == InputConstants.KEY_F && InputUtils.isGuiShortcutModifierDown(modifiers);
    }

    /** Clears cached search menu if closed state. */
    protected void clearCachedSearchMenuIfClosed() {
        if (this.cachedSearchMenu != null && !this.cachedSearchMenu.isOpen()) {
            this.cachedSearchMenu = null;
        }
    }

    /** Returns menu under cursor. */
    @Nullable
    protected ContextMenu getMenuUnderCursor(int mouseX, int mouseY) {
        if (!this.isOpen()) return null;
        ContextMenu hovered = this.isMouseOverMenu(mouseX, mouseY) ? this : null;
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s && s.subContextMenu.isOpen()) {
                ContextMenu subHovered = s.subContextMenu.getMenuUnderCursor(mouseX, mouseY);
                if (subHovered != null) {
                    hovered = subHovered;
                }
            }
        }
        return hovered;
    }

    /** Returns root menu. */
    @NotNull
    protected ContextMenu getRootMenu() {
        ContextMenu current = this;
        while (current.parentEntry != null) {
            current = current.parentEntry.parent;
        }
        return current;
    }

    /** Returns whether protected entry identifier. */
    protected boolean isProtectedEntryIdentifier(@NotNull String identifier) {
        return SEARCH_ENTRY_IDENTIFIER.equals(identifier)
                || SEARCH_SEPARATOR_IDENTIFIER.equals(identifier);
    }

    /** Returns whether protected entry. */
    protected boolean isProtectedEntry(@NotNull ContextMenuEntry<?> entry) {
        return entry == this.searchEntry
                || entry == this.searchSeparator
                || this.isProtectedEntryIdentifier(entry.identifier);
    }

    /** Returns protected entries count. */
    protected int getProtectedEntriesCount() {
        int count = 0;
        if (this.searchEntry != null) count++;
        if (this.searchSeparator != null) count++;
        return count;
    }

    /**
     * Stacks the given context menus into a single menu.
     * <p>
     * Only entries that are {@link ContextMenuEntry#isStackable()} and {@link ContextMenuEntry#isActive()}
     * in every menu are included. Stacked entries are linked via
     * {@link ContextMenuStackMeta#getNextInStack()}, with the first entry being the visible one.
     *
     * <p><b>Example (multi-select)</b>
     * <pre>{@code
     * ContextMenu stacked = ContextMenu.stackContextMenus(menu1, menu2, menu3);
     * stacked.openMenuAt(mouseX, mouseY);
     * }</pre>
     */
    @NotNull
    public static ContextMenu stackContextMenus(@NotNull List<ContextMenu> menusToStack) {
        return stackContextMenus(menusToStack.toArray(new ContextMenu[0]));
    }

    /**
     * Stacks the given context menus into a single menu.
     * <p>
     * This copies stackable entries from each menu, links them through {@link ContextMenuStackMeta},
     * and shares a single {@link RuntimePropertyContainer} across the stack.
     *
     * <p>Sub-menus are stacked recursively.
     */
    @NotNull
    public static ContextMenu stackContextMenus(@NotNull ContextMenu... menusToStack) {

        ContextMenu stacked = new ContextMenu();

        if (menusToStack.length > 0) {

            stacked.scale = menusToStack[0].scale;
            stacked.subMenuOpeningSide = menusToStack[0].subMenuOpeningSide;
            stacked.shadow = menusToStack[0].shadow;
            stacked.forceUIScale = menusToStack[0].forceUIScale;
            stacked.keepDistanceToEdges = menusToStack[0].keepDistanceToEdges;
            stacked.forceRawXY = menusToStack[0].forceRawXY;
            stacked.forceSide = menusToStack[0].forceSide;

            for (ContextMenuEntry<?> ignoredEntry : menusToStack[0].getStackableEntries()) {

                RuntimePropertyContainer stackProperties = new RuntimePropertyContainer();
                List<ContextMenuEntry<?>> entryStack = collectInstancesOfStackableEntryInMenus(ignoredEntry.identifier, menusToStack);
                if (!entryStack.isEmpty() && (entryStack.size() == menusToStack.length)) { // only stack entries if all menus have a stackable instance of it

                    ContextMenuEntry<?> firstOriginal = entryStack.get(0);
                    List<ContextMenuEntry<?>> entryStackCopyWithoutFirst = new ArrayList<>();
                    entryStack.forEach((entry) ->  {
                        if (entry != firstOriginal) entryStackCopyWithoutFirst.add(entry.copy());
                    });

                    ContextMenuEntry<?> first = firstOriginal.copy();
                    first.stackMeta.firstInStack = true;
                    first.stackMeta.lastInStack = false;
                    first.stackMeta.partOfStack = true;
                    first.stackMeta.properties = stackProperties;
                    first.parent = stacked;
                    stacked.addEntry(first);
                    if (first instanceof SubMenuContextMenuEntry s) {
                        s.setSubContextMenu(stackContextMenus(getSubContextMenusOfSubMenuEntries(entryStack)));
                        s.stackMeta.lastInStack = true;
                    } else {
                        ContextMenuEntry<?> prev = first;
                        for (ContextMenuEntry<?> e2 : entryStackCopyWithoutFirst) {
                            prev.stackMeta.nextInStack = e2;
                            prev = e2;
                            e2.stackMeta.properties = stackProperties;
                            e2.stackMeta.partOfStack = true;
                            e2.stackMeta.firstInStack = false;
                            e2.stackMeta.lastInStack = false;
                            e2.parent = stacked;
                        }
                        prev.stackMeta.lastInStack = true;
                    }

                }

            }

        }

        return stacked;

    }

    /** Collects instances of stackable entry in menus matching the supplied criteria. */
    protected static List<ContextMenuEntry<?>> collectInstancesOfStackableEntryInMenus(String entryIdentifier, ContextMenu[] menus) {
        List<ContextMenuEntry<?>> entries = new ArrayList<>();
        for (ContextMenu m : menus) {
            ContextMenuEntry<?> e = m.getEntry(entryIdentifier);
            if ((e != null) && e.isStackable() && e.isActive()) {
                entries.add(e);
            }
        }
        return entries;
    }

    /** Returns sub context menus of sub menu entries. */
    protected static List<ContextMenu> getSubContextMenusOfSubMenuEntries(List<ContextMenuEntry<?>> entries) {
        List<ContextMenu> l = new ArrayList<>();
        for (ContextMenuEntry<?> e : entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                l.add(s.subContextMenu);
            }
        }
        return l;
    }

    /** Base implementation for context menu entry. */
    public static abstract class ContextMenuEntry<T extends ContextMenuEntry<T>> implements Renderable, GuiEventListener {

        /** Stable identifier used for entry lookup and replacement. */
        protected String identifier;
        /** Context menu that owns and positions this entry. */
        protected ContextMenu parent;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected float x;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected float y;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected float width;
        /** Height in GUI units for height. */
        protected float height = 20;
        /** Optional action run during each menu tick. */
        @Nullable
        protected EntryTask tickAction;
        /** Action run while the pointer hovers the entry. */
        protected EntryTask hoverAction;
        /** Whether the pointer currently hovers this element. */
        protected boolean hovered = false;
        /**
         * Stack metadata for this entry.
         * <p>
         * This metadata is populated when menus are stacked via {@link ContextMenu#stackContextMenus(ContextMenu...)}.
         */
        protected ContextMenuStackMeta stackMeta = new ContextMenuStackMeta();
        /** Predicates combined to determine whether the entry is active. */
        protected List<BooleanSupplier> activeStateSuppliers = new ArrayList<>();
        /** Predicates combined to determine whether the entry is visible. */
        protected List<BooleanSupplier> visibleStateSuppliers = new ArrayList<>();
        /** Optionally supplies the tooltip for the current entry state. */
        @Nullable
        protected Supplier<UITooltip> tooltipSupplier;
    /** Font used to measure and draw entry labels. */
        protected Font font = Minecraft.getInstance().font;
        /** Whether to reserve icon spacing when no icon is present. */
        protected boolean addSpaceForIcon = false;
        /** Whether hover changes the entry background color. */
        protected boolean changeBackgroundColorOnHover = true;
        /**
         * Optional applier used by stack-aware builders to apply values across the stack.
         * See {@link ContextMenuBuilder#applyStackAppliers(ContextMenuEntry, Object)}.
         */
        @Nullable
        protected StackApplier stackApplier;
        /**
         * Optional value supplier used to read the current value for mixed-state detection.
         * See {@link ContextMenuBuilder#resolveStackValue(ContextMenuEntry)}.
         */
        @Nullable
        protected StackValueSupplier stackValueSupplier;
        /**
         * Optional group key used by {@link ContextMenuBuilder#runStackedClickActions(ContextMenu.ClickableContextMenuEntry)}
         * to avoid duplicate actions when multiple entries represent the same logical action.
         */
        @Nullable
        protected Object stackGroupKey;

        /** Attaches a stable entry identifier to its owning context menu. */
        public ContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            this.identifier = identifier;
            this.parent = parent;
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public abstract void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial);

        /** Returns the stable identifier used for registry or entry lookup. */
        @NotNull
        public String getIdentifier() {
            return identifier;
        }

        /** Returns the containing menu, widget, or entry. */
        @NotNull
        public ContextMenu getParent() {
            return parent;
        }

        /** Returns the current height in GUI units. */
        public float getHeight() {
            return this.height;
        }

        /** Sets height for this context menu entry. */
        public T setHeight(float height) {
            this.height = height;
            return (T) this;
        }

        /** Returns the minimum layout width in GUI units. */
        public abstract float getMinWidth();

        /** Sets hovered for this context menu entry. */
        protected void setHovered(boolean hovered) {
            this.hovered = hovered;
        }

        /** Reports whether the pointer currently hovers this element. */
        public boolean isHovered() {
            if (!this.parent.isOpen()) return false;
            return this.hovered;
        }

        /** Returns whether change background color on hover. */
        public boolean isChangeBackgroundColorOnHover() {
            return this.changeBackgroundColorOnHover;
        }

        /** Sets change background color on hover for this context menu entry. */
        public T setChangeBackgroundColorOnHover(boolean changeColor) {
            this.changeBackgroundColorOnHover = changeColor;
            return (T) this;
        }

        /** Reports whether this control accepts interaction. */
        public boolean isActive() {
            for (BooleanSupplier b : this.activeStateSuppliers) {
                if (!b.getBoolean(this.parent, this)) return false;
            }
            return true;
        }

        /**
         * @deprecated Use {@link ContextMenuEntry#addIsActiveSupplier(BooleanSupplier)} instead.
         */
        @Deprecated
        public T setIsActiveSupplier(@Nullable BooleanSupplier activeStateSupplier) {
            if (activeStateSupplier != null) this.addIsActiveSupplier(activeStateSupplier);
            return (T) this;
        }

        /**
         * Add a {@link BooleanSupplier} that controls if this entry should be active (clickable).<br>
         * These controllers stack, so multiple controllers can handle the active state of the entry at the same time. If at least one controller returns false, the entry gets disabled.
         */
        public T addIsActiveSupplier(@NotNull BooleanSupplier activeStateSupplier) {
            this.activeStateSuppliers.add(Objects.requireNonNull(activeStateSupplier));
            return (T) this;
        }

        /** Reports whether this element participates in rendering and input. */
        public boolean isVisible() {
            for (BooleanSupplier b : this.visibleStateSuppliers) {
                if (!b.getBoolean(this.parent, this)) return false;
            }
            return true;
        }

        /**
         * @deprecated Use {@link ContextMenuEntry#addIsVisibleSupplier(BooleanSupplier)} instead.
         */
        @Deprecated
        public T setIsVisibleSupplier(@Nullable BooleanSupplier visibleStateSupplier) {
            if (visibleStateSupplier != null) this.addIsVisibleSupplier(visibleStateSupplier);
            return (T) this;
        }

        /**
         * Add a {@link BooleanSupplier} that controls if this entry should be visible.<br>
         * These controllers stack, so multiple controllers can handle the visible state of the entry at the same time. If at least one controller returns false, the entry gets hidden.
         */
        public T addIsVisibleSupplier(@NotNull BooleanSupplier visibleStateSupplier) {
            this.visibleStateSuppliers.add(Objects.requireNonNull(visibleStateSupplier));
            return (T) this;
        }

        /** Sets tick action for this context menu entry. */
        public T setTickAction(@Nullable EntryTask tickAction) {
            this.tickAction = tickAction;
            return (T) this;
        }

        /** Sets hover action for this context menu entry. */
        public T setHoverAction(@Nullable EntryTask hoverAction) {
            this.hoverAction = hoverAction;
            return (T) this;
        }

        /** Sets tooltip supplier for this context menu entry. */
        public T setTooltipSupplier(@Nullable Supplier<UITooltip> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return (T) this;
        }

        /** Returns tooltip. */
        @Nullable
        public UITooltip getTooltip() {
            return (this.tooltipSupplier != null) ? this.tooltipSupplier.get(this.parent, this) : null;
        }

        /**
         * Marks this entry as stackable, allowing it to be included in stacked menus.
         */
        public T setStackable(boolean stackable) {
            this.getStackMeta().setStackable(stackable);
            return (T) this;
        }

        /**
         * @return true if this entry may be stacked with the same entry across menus.
         */
        public boolean isStackable() {
            return this.getStackMeta().isStackable();
        }

        /**
         * Returns the stack metadata for this entry.
         * <p>
         * Use {@link ContextMenuStackMeta#getNextInStack()} to walk the stack.
         */
        @NotNull
        public ContextMenuStackMeta getStackMeta() {
            return this.stackMeta;
        }

        /**
         * @return the stack applier assigned to this entry, or null if none.
         */
        @Nullable
        public StackApplier getStackApplier() {
            return this.stackApplier;
        }

        /**
         * Sets the stack applier for this entry.
         * <p>
         * The applier should only mutate the entry's {@link ContextMenuBuilder#self()} instance,
         * because it will be invoked once per stack entry.
         *
         * <p><b>Example</b>
         * <pre>{@code
         * entry.setStackApplier((stackEntry, value) -> {
         *     if (value instanceof Boolean b) {
         *         builder.self().setEnabled(b);
         *     }
         * });
         * }</pre>
         */
        @NotNull
        public T setStackApplier(@Nullable StackApplier stackApplier) {
            this.stackApplier = stackApplier;
            return (T) this;
        }

        /**
         * @return the stack value supplier for this entry, or null if none.
         */
        @Nullable
        public StackValueSupplier getStackValueSupplier() {
            return this.stackValueSupplier;
        }

        /**
         * Sets the stack value supplier for this entry.
         * <p>
         * This supplier is used by {@link ContextMenuBuilder#resolveStackValue(ContextMenuEntry)}
         * to detect mixed values.
         */
        @NotNull
        public T setStackValueSupplier(@Nullable StackValueSupplier stackValueSupplier) {
            this.stackValueSupplier = stackValueSupplier;
            return (T) this;
        }

        /**
         * @return the optional stack group key for this entry.
         */
        @Nullable
        public Object getStackGroupKey() {
            return this.stackGroupKey;
        }

        /**
         * Sets the optional stack group key for this entry.
         * <p>
         * Entries with the same group key are treated as a single logical action in
         * {@link ContextMenuBuilder#runStackedClickActions(ContextMenu.ClickableContextMenuEntry)}.
         */
        @NotNull
        public T setStackGroupKey(@Nullable Object stackGroupKey) {
            this.stackGroupKey = stackGroupKey;
            return (T) this;
        }

        /** Handles removed for this context menu entry. */
        protected void onRemoved() {
        }

        /** Creates an independent copy with the same configuration. */
        public abstract ContextMenuEntry<?> copy();

        /** Routes a mouse-button press and reports whether it was consumed. */
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return GuiEventListener.super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
        }

        /** Routes a mouse-button release and reports whether it was consumed. */
        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            return this.mouseReleased(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button release and reports whether it was consumed. */
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return GuiEventListener.super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
        }

        /** Routes pointer dragging and reports whether it was consumed. */
        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
        }

        /** Routes pointer dragging and reports whether it was consumed. */
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return GuiEventListener.super.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), dragX, dragY);
        }

        /** Routes a key press and reports whether it was consumed. */
        @Override
        public boolean keyPressed(KeyEvent event) {
            return this.keyPressed(event.key(), event.scancode(), event.modifiers());
        }

        /** Routes a key press and reports whether it was consumed. */
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return GuiEventListener.super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
        }

        /** Routes a key release and reports whether it was consumed. */
        @Override
        public boolean keyReleased(KeyEvent event) {
            return this.keyReleased(event.key(), event.scancode(), event.modifiers());
        }

        /** Routes a key release and reports whether it was consumed. */
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            return GuiEventListener.super.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
        }

        /** Routes typed character input and reports whether it was consumed. */
        @Override
        public boolean charTyped(CharacterEvent event) {
            return this.charTyped((char)event.codepoint(), 0);
        }

        /** Routes typed character input and reports whether it was consumed. */
        public boolean charTyped(char codePoint, int modifiers) {
            return GuiEventListener.super.charTyped(new CharacterEvent(codePoint));
        }

        /** Receives entry lifecycle callbacks during ticking and input handling. */
        @FunctionalInterface
        public interface EntryTask {

            /**
             * @param menu The {@link ContextMenu} this {@link EntryTask}'s {@link ContextMenuEntry} is part of.
             * @param entry The {@link ContextMenuEntry} this {@link EntryTask} is part of.
             * @param isPost Only used for the {@link ContextMenuEntry#tickAction}.
             */
            void run(ContextMenu menu, ContextMenuEntry<?> entry, boolean isPost);

        }

    }

    /** Represents one renderable, focusable clickable context menu entry. */
    public static class ClickableContextMenuEntry<T extends ClickableContextMenuEntry<T>> extends ContextMenuEntry<T> {

        /** Height in GUI units for icon width. */
        protected static final int ICON_WIDTH_HEIGHT = 10;
        /** Horizontal GUI coordinate for icon padding. */
        protected static final int ICON_PADDING_LEFT = 10;
        /** Icon label spacing in GUI pixels. */
        protected static final int ICON_LABEL_SPACING = 20;

        /** Action run when the entry is activated. */
        @NotNull
        protected ClickAction clickAction;
        /** Supplies the current entry label. */
        @NotNull
        protected Supplier<Component> labelSupplier;
        /** Optionally supplies the shortcut text shown beside the label. */
        @Nullable
        protected Supplier<Component> shortcutTextSupplier;
        /** Optional texture identifier rendered before the label. */
        @Nullable
        protected Identifier icon;
        /** Optional Material icon rendered before the label. */
        @Nullable
        protected MaterialIcon materialIcon;
        /** Whether the pointer currently hovers the tooltip icon. */
        protected boolean tooltipIconHovered = false;
        /** Whether the entry tooltip is currently visible. */
        protected boolean tooltipActive = false;
        /** Millisecond timestamp when tooltip-icon hover began, or {@code -1}. */
        protected long tooltipIconHoverStart = -1;
        /** Whether activation plays the click sound. */
        protected boolean enableClickSound = true;
        /** Built-in animation preset for icon wiggle animation. */
        @NotNull
        protected IconAnimation.Instance iconWiggleAnimation = IconAnimations.SHORT_WIGGLE_LEFT_RIGHT.createInstance();

        /** Adds a label and click action to an identified context-menu entry. */
        public ClickableContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(identifier, parent);
            this.clickAction = clickAction;
            this.labelSupplier = (menu, entry) -> label;
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

            this.extractBackground(graphics);

            int labelX = this.getLabelX();
            int labelY = (int) (this.y + (this.height / 2) - (UIBase.getUITextHeightNormal() / 2));
            UIBase.renderText(graphics, this.getLabel(), labelX, labelY, this.getLabelColor());

            int shortcutTextWidth = 0;
            Component shortcutText = this.getShortcutText();
            if (shortcutText != null) {
                shortcutTextWidth = (int) UIBase.getUITextWidthSmall(shortcutText);
                int shortcutX = (int) (this.x + this.width - 10 - shortcutTextWidth);
                int shortcutY = (int) (this.y + (this.height / 2) - (UIBase.getUITextHeightSmall() / 2));
                UIBase.renderText(graphics, shortcutText, shortcutX, shortcutY, this.getLabelColor(), UIBase.getUITextSizeSmall());
            }

            this.renderIcon(graphics);

            this.renderTooltipIconAndRegisterTooltip(graphics, mouseX, mouseY);

        }

        /** Renders icon into the active GUI extraction pass. */
        protected void renderIcon(GuiGraphicsExtractor graphics) {
            IconRenderData iconData = this.resolveIconData();
            if (iconData == null) {
                return;
            }
            float areaX = this.x + ICON_PADDING_LEFT + this.getIconWiggleOffsetX();
            float areaY = this.y + (this.getHeight() / 2.0F) - (ICON_WIDTH_HEIGHT / 2.0F);
            UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
            this.blitScaledIcon(graphics, iconData, areaX, areaY, ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
            RenderingUtils.resetShaderColor(graphics);
        }

        /** Computes icon data from the supplied inputs. */
        @Nullable
        protected IconRenderData resolveIconData() {
            if (this.materialIcon != null) {
                float renderSize = ICON_WIDTH_HEIGHT;
                Identifier location = this.materialIcon.getTextureLocationForUI(renderSize, renderSize);
                if (location == null) {
                    return null;
                }
                int iconSize = this.materialIcon.calculateBestTextureSizeForUI(renderSize, renderSize);
                int width = this.materialIcon.getWidth(iconSize);
                int height = this.materialIcon.getHeight(iconSize);
                if (width <= 0 || height <= 0) {
                    return null;
                }
                return new IconRenderData(location, width, height);
            }
            if (this.icon != null) {
                return new IconRenderData(this.icon, ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
            }
            return null;
        }

        /** Adds scaled icon geometry to the active GUI extraction pass. */
        protected void blitScaledIcon(@NotNull GuiGraphicsExtractor graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
            this.blitScaledIcon(graphics, iconData, areaX, areaY, areaWidth, areaHeight, 0.0F);
        }

        /** Adds scaled icon geometry to the active GUI extraction pass. */
        protected void blitScaledIcon(@NotNull GuiGraphicsExtractor graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight, float rotationDegrees) {
            if (areaWidth <= 0.0F || areaHeight <= 0.0F) {
                return;
            }
            float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
            if (!Float.isFinite(scale) || scale <= 0.0F) {
                return;
            }
            float scaledWidth = iconData.width * scale;
            float scaledHeight = iconData.height * scale;
            float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
            float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(drawX, drawY);
            graphics.pose().scale(scale, scale);
            if (rotationDegrees != 0.0F) {
                graphics.pose().translate(iconData.width * 0.5F, iconData.height * 0.5F);
                graphics.pose().rotate((float)Math.toRadians(rotationDegrees));
                graphics.pose().translate(-iconData.width * 0.5F, -iconData.height * 0.5F);
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
            graphics.pose().popMatrix();
        }

        /** Reports whether an explicit or Material icon is configured. */
        protected boolean hasIconAssigned() {
            return this.icon != null || this.materialIcon != null;
        }

        /** Sets hovered for this clickable context menu entry. */
        @Override
        protected void setHovered(boolean hovered) {
            boolean wasHovered = this.hovered;
            super.setHovered(hovered);
            if (!wasHovered && hovered && this.isActive() && UIBase.shouldPlayAnimations()) {
                this.iconWiggleAnimation.start();
            }
        }

        /** Returns the hover-animation horizontal offset in GUI units, or zero when animation is inactive. */
        protected float getIconWiggleOffsetX() {
            if (!UIBase.shouldPlayAnimations()) {
                this.iconWiggleAnimation.reset();
                return 0.0F;
            }
            if (!this.isActive()) {
                this.iconWiggleAnimation.reset();
                return 0.0F;
            }
            return this.iconWiggleAnimation.getOffsetX();
        }

        /** Renders tooltip icon and register tooltip into the active GUI extraction pass. */
        protected void renderTooltipIconAndRegisterTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {

            UITooltip tooltip = this.getTooltip();

            if (tooltip != null) {

                this.tooltipIconHovered = this.isTooltipIconHovered(mouseX, mouseY);
                if (this.tooltipIconHovered) {
                    if (this.tooltipIconHoverStart == -1) {
                        this.tooltipIconHoverStart = System.currentTimeMillis();
                    }
                } else {
                    this.tooltipIconHoverStart = -1;
                }
                this.tooltipActive = (this.tooltipIconHoverStart != -1) && ((this.tooltipIconHoverStart + 200) < System.currentTimeMillis());

                UIBase.getUITheme().ui_icon_texture_color.setAsShaderColor(graphics, this.tooltipIconHovered ? 1.0F : 0.2F);
                IconRenderData iconData = this.resolveTooltipIconData();
                if (iconData != null) {
                    this.blitScaledIcon(graphics, iconData, this.getTooltipIconX(), this.getTooltipIconY(), ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
                }
                RenderingUtils.resetShaderColor(graphics);

                if (this.tooltipActive) {
                    TooltipHandler.INSTANCE.addRenderTickTooltip(tooltip, () -> true);
                }

            } else {
                this.tooltipIconHovered = false;
                this.tooltipActive = false;
            }

        }

        /** Returns whether tooltip icon hovered. */
        protected boolean isTooltipIconHovered(int mouseX, int mouseY) {
            return UIBase.isXYInArea(mouseX, mouseY, this.getTooltipIconX(), this.getTooltipIconY(), ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
        }

        /** Returns label x. */
        protected int getLabelX() {
            int labelX = (int) (this.x + ICON_PADDING_LEFT);
            if (this.hasIconAssigned() || this.addSpaceForIcon) {
                labelX += ICON_LABEL_SPACING;
            }
            return labelX;
        }

        /** Returns tooltip icon x. */
        protected int getTooltipIconX() {
            int labelX = this.getLabelX();
            int labelWidth = (int) UIBase.getUITextWidthNormal(this.getLabel());
            int gap = Math.round(ICON_WIDTH_HEIGHT * (2.0F / 3.0F));
            return labelX + labelWidth + gap;
        }

        /** Returns tooltip icon y. */
        protected int getTooltipIconY() {
            return (int) (this.y + 5);
        }

        /** Computes tooltip icon data from the supplied inputs. */
        @Nullable
        protected IconRenderData resolveTooltipIconData() {
            float renderSize = ICON_WIDTH_HEIGHT;
            Identifier location = CONTEXT_MENU_TOOLTIP_ICON.getTextureLocationForUI(renderSize, renderSize);
            if (location == null) {
                return null;
            }
            int iconSize = CONTEXT_MENU_TOOLTIP_ICON.calculateBestTextureSizeForUI(renderSize, renderSize);
            int width = CONTEXT_MENU_TOOLTIP_ICON.getWidth(iconSize);
            int height = CONTEXT_MENU_TOOLTIP_ICON.getHeight(iconSize);
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new IconRenderData(location, width, height);
        }

        /** Adds this component's background draw state to the active GUI extraction pass. */
        protected void extractBackground(@NotNull GuiGraphicsExtractor graphics) {
            if (this.isChangeBackgroundColorOnHover() && this.isHovered() && this.isActive()) {
                int backColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1.getColorInt() : UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1.getColorInt();
                RenderingUtils.fillF(graphics, (float) this.x, (float) this.y, (float) (this.x + this.width), (float) (this.y + this.height), backColor);
            }
        }

        /** Returns label color. */
        protected int getLabelColor() {
            if (UIBase.shouldBlur()) {
                return this.isActive() ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive.getColorInt();
            }
            return this.isActive() ? UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt();
        }

        /** Resolves the explicit or rasterized Material icon texture; returns {@code null} when unset. */
        @Nullable
        public Identifier getIcon() {
            if (this.icon != null) {
                return this.icon;
            }
            if (this.materialIcon != null) {
                float renderSize = ICON_WIDTH_HEIGHT;
                return this.materialIcon.getTextureLocationForUI(renderSize, renderSize);
            }
            return null;
        }

        /**
         * Icons should be completely white. No other colors should be used.
         */
        public T setIcon(@Nullable Identifier icon) {
            this.icon = icon;
            this.materialIcon = null;
            return (T) this;
        }

        /**
         * Icons should be completely white. No other colors should be used.
         */
        public T setIcon(@Nullable MaterialIcon icon) {
            this.materialIcon = icon;
            this.icon = null;
            return (T) this;
        }

        /** Sets label supplier for this clickable context menu entry. */
        @NotNull
        public T setLabelSupplier(@NotNull Supplier<Component> labelSupplier) {
            Objects.requireNonNull(labelSupplier);
            this.labelSupplier = labelSupplier;
            return (T) this;
        }

        /** Returns the label resolved for the current state. */
        @NotNull
        public Component getLabel() {
            Component c = this.labelSupplier.get(this.parent, this);
            Objects.requireNonNull(c);
            return c;
        }

        /** Sets click action for this clickable context menu entry. */
        @NotNull
        public T setClickAction(@NotNull ClickAction clickAction) {
            Objects.requireNonNull(clickAction);
            this.clickAction = clickAction;
            return (T) this;
        }

        /** Invokes this entry's configured click callback. */
        public void runClickAction() {
            this.clickAction.onClick(this.parent, this);
        }

        /** Returns shortcut text. */
        @Nullable
        public Component getShortcutText() {
            return (this.shortcutTextSupplier != null) ? this.shortcutTextSupplier.get(this.parent, this) : null;
        }

        /** Sets shortcut text supplier for this clickable context menu entry. */
        @NotNull
        public T setShortcutTextSupplier(@Nullable Supplier<Component> shortcutTextSupplier) {
            this.shortcutTextSupplier = shortcutTextSupplier;
            return (T) this;
        }

        /** Returns whether click sound enabled. */
        public boolean isClickSoundEnabled() {
            return this.enableClickSound;
        }

        /** Sets click sound enabled for this clickable context menu entry. */
        public T setClickSoundEnabled(boolean enabled) {
            this.enableClickSound = enabled;
            return (T) this;
        }

        /** Creates an independent copy with the same configuration. */
        @Override
        public ClickableContextMenuEntry<T> copy() {
            ClickableContextMenuEntry<T> copy = new ClickableContextMenuEntry<>(this.identifier, this.parent, Component.literal(""), this.clickAction);
            copy.shortcutTextSupplier = this.shortcutTextSupplier;
            copy.labelSupplier = this.labelSupplier;
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.icon = this.icon;
            copy.materialIcon = this.materialIcon;
            copy.iconWiggleAnimation = this.iconWiggleAnimation.getAnimation().createInstance();
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
            return copy;
        }

        /** Returns the minimum layout width in GUI units. */
        @Override
        public float getMinWidth() {
            int i = (int) (UIBase.getUITextWidthNormal(this.getLabel()) + 20);
            if (this.tooltipSupplier != null) {
                i += 30;
            }
            Component shortcutText = this.getShortcutText();
            if (shortcutText != null) {
                i += UIBase.getUITextWidthSmall(shortcutText) + 30;
            }
            if (this.hasIconAssigned() || this.addSpaceForIcon) {
                i += ICON_LABEL_SPACING;
            }
            return i;
        }

        /** Sets focused for this clickable context menu entry. */
        @Override
        public void setFocused(boolean var1) {
        }

        /** Reports whether keyboard focus currently targets this control. */
        @Override
        public boolean isFocused() {
            return false;
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 0) && this.isHovered() && this.isActive() && !this.parent.isSubMenuHovered() && !this.tooltipIconHovered) {
                if (UIConfiguration.get().clickSoundsEnabled() && this.enableClickSound) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.clickAction.onClick(this.parent, this);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        /** Handles activation of a clickable context-menu entry. */
        @FunctionalInterface
        public interface ClickAction {

            /** Handles activation of this context-menu entry. */
            void onClick(ContextMenu menu, ClickableContextMenuEntry<?> entry);

        }

    }

    /** Stores a resolved icon texture and its intrinsic dimensions. */
    protected static final class IconRenderData {

        final Identifier texture;
        final int width;
        final int height;

        private IconRenderData(@NotNull Identifier texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }

    }

    @Nullable
    private static IconRenderData resolveMaterialIconData(@Nullable MaterialIcon icon, float renderWidth, float renderHeight) {
        if (icon == null) {
            return null;
        }
        Identifier location = icon.getTextureLocationForUI(renderWidth, renderHeight);
        if (location == null) {
            return null;
        }
        int iconSize = icon.calculateBestTextureSizeForUI(renderWidth, renderHeight);
        int width = icon.getWidth(iconSize);
        int height = icon.getHeight(iconSize);
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new IconRenderData(location, width, height);
    }

    private static void blitScaledIcon(@NotNull GuiGraphicsExtractor graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
        if (areaWidth <= 0.0F || areaHeight <= 0.0F) {
            return;
        }
        float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            return;
        }
        float scaledWidth = iconData.width * scale;
        float scaledHeight = iconData.height * scale;
        float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
        float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(drawX, drawY);
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
        graphics.pose().popMatrix();
    }

    /** Represents one renderable, focusable value cycle context menu entry. */
    public static class ValueCycleContextMenuEntry<V> extends ClickableContextMenuEntry<ValueCycleContextMenuEntry<V>> {

        /** Cycle advanced whenever this entry is activated. */
        protected final ILocalizedValueCycle<V> valueCycle;

        /** Binds an identified context-menu entry to a localized value cycle. */
        public ValueCycleContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull ILocalizedValueCycle<V> valueCycle) {
            super(identifier, parent, Component.empty(), (menu, entry) -> valueCycle.next());
            this.valueCycle = valueCycle;
            this.labelSupplier = (menu, entry) -> this.valueCycle.getCycleComponent();
        }

        /** Returns the cycle advanced whenever this entry is activated. */
        @NotNull
        public ILocalizedValueCycle<V> getValueCycle() {
            return this.valueCycle;
        }

        /** Sets label supplier for this value cycle context menu entry. */
        @Override
        public @NotNull ValueCycleContextMenuEntry<V> setLabelSupplier(@NotNull Supplier<Component> labelSupplier) {
            LOGGER.error("[KONKRETE] You can't set the label of ValueCycleContextMenuEntries!");
            return this;
        }

        /** Sets click action for this value cycle context menu entry. */
        @Override
        public @NotNull ValueCycleContextMenuEntry<V> setClickAction(@NotNull ClickAction clickAction) {
            LOGGER.error("[KONKRETE] You can't set the click action of ValueCycleContextMenuEntries!");
            return this;
        }

        /** Creates an independent copy with the same configuration. */
        @Override
        public ValueCycleContextMenuEntry<V> copy() {
            ValueCycleContextMenuEntry<V> copy = new ValueCycleContextMenuEntry<>(this.identifier, this.parent, this.valueCycle);
            copy.shortcutTextSupplier = this.shortcutTextSupplier;
            copy.labelSupplier = this.labelSupplier;
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.icon = this.icon;
            copy.materialIcon = this.materialIcon;
            copy.iconWiggleAnimation = this.iconWiggleAnimation.getAnimation().createInstance();
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
            return copy;
        }

    }

    /** Represents one renderable, focusable sub menu context menu entry. */
    public static class SubMenuContextMenuEntry extends ClickableContextMenuEntry<SubMenuContextMenuEntry> {

        /** Submenu opened by this entry. */
        @NotNull
        protected ContextMenu subContextMenu;
        /** Whether submenu hover timing has started. */
        protected boolean subMenuHoverTicked = false;
        /** Whether the pointer entered the submenu after it opened. */
        protected boolean subMenuHoveredAfterOpen = false;
        /** Millisecond timestamp when the pointer entered the parent menu. */
        protected long parentMenuHoverStartTime = -1;
        /** Millisecond timestamp when the pointer entered this entry. */
        protected long entryHoverStartTime = -1;
        /** Millisecond timestamp when the pointer left this entry. */
        protected long entryNotHoveredStartTime = -1;
        /** Built-in animation preset for sub menu arrow spin. */
        @NotNull
        protected IconAnimation.Instance subMenuArrowSpin = IconAnimations.SHORT_SPIN_UP_SUBTLE.createInstance();

        /** Associates a labeled context-menu entry with the submenu it opens. */
        public SubMenuContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
            super(identifier, parent, label, ((menu, entry) -> {}));
            this.subContextMenu = subContextMenu;
            this.subContextMenu.parentEntry = this;
            this.clickAction = (menu, entry) -> ((SubMenuContextMenuEntry) entry).openSubMenu();
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

            this.tickEntry();

            super.extractRenderState(graphics, mouseX, mouseY, partial);

            this.renderSubMenuArrow(graphics);

        }

        /** Renders sub menu arrow into the active GUI extraction pass. */
        protected void renderSubMenuArrow(GuiGraphicsExtractor graphics) {
            UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
            IconRenderData iconData = this.resolveSubMenuArrowIconData();
            if (iconData != null) {
                this.blitScaledIcon(graphics, iconData, (int) (this.x + this.width - 20), (int) (this.y + 5), ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT, this.getSubMenuArrowRotation());
            }
            RenderingUtils.resetShaderColor(graphics);
        }

        /** Returns sub menu arrow rotation. */
        protected float getSubMenuArrowRotation() {
            if (!UIBase.shouldPlayAnimations()) {
                this.subMenuArrowSpin.reset();
                return 0.0F;
            }
            if (!this.isActive()) {
                this.subMenuArrowSpin.reset();
                return 0.0F;
            }
            return this.subMenuArrowSpin.getRotationDegrees();
        }

        /** Computes sub menu arrow icon data from the supplied inputs. */
        @Nullable
        protected IconRenderData resolveSubMenuArrowIconData() {
            float renderSize = ICON_WIDTH_HEIGHT;
            Identifier location = SUB_CONTEXT_MENU_ARROW_ICON.getTextureLocationForUI(renderSize, renderSize);
            if (location == null) {
                return null;
            }
            int iconSize = SUB_CONTEXT_MENU_ARROW_ICON.calculateBestTextureSizeForUI(renderSize, renderSize);
            int width = SUB_CONTEXT_MENU_ARROW_ICON.getWidth(iconSize);
            int height = SUB_CONTEXT_MENU_ARROW_ICON.getHeight(iconSize);
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new IconRenderData(location, width, height);
        }

        /** Adds this component's background draw state to the active GUI extraction pass. */
        @Override
        protected void extractBackground(@NotNull GuiGraphicsExtractor graphics) {
            boolean hover = this.hovered;
            this.hovered = this.hovered || this.subContextMenu.isOpen();
            super.extractBackground(graphics);
            this.hovered = hover;
        }

        /** Advances entry by one client tick. */
        protected void tickEntry() {
            //Close sub menu when hovering over parent menu AFTER sub menu was hovered
            if (!this.subContextMenu.isOpen()) {
                this.subMenuHoveredAfterOpen = false;
                this.subMenuHoverTicked = false;
            }
            if (this.subContextMenu.isHovered()) {
                if (!this.subMenuHoverTicked) {
                    this.subMenuHoverTicked = true;
                } else {
                    this.subMenuHoveredAfterOpen = true;
                }
            }
            if (this.parent.isHovered() && !this.isHovered() && this.subContextMenu.isOpen() && !this.subContextMenu.isUserNavigatingInMenu() && this.subMenuHoveredAfterOpen) {
                if (this.parentMenuHoverStartTime == -1) {
                    this.parentMenuHoverStartTime = System.currentTimeMillis();
                }
                if ((this.parentMenuHoverStartTime + 400) < System.currentTimeMillis()) {
                    this.subContextMenu.closeMenu();
                }
            } else {
                this.parentMenuHoverStartTime = -1;
            }
            //Open sub menu on entry hover
            if (this.isActive() && this.isHovered() && !this.parent.isSubMenuHovered() && !this.tooltipIconHovered) {
                long now = System.currentTimeMillis();
                if (this.entryHoverStartTime == -1) {
                    this.entryHoverStartTime = now;
                }
                int openSpeed = 400 / UIConfiguration.get().contextMenuHoverOpenSpeed();
                if (((this.entryHoverStartTime + openSpeed) < now) && !this.subContextMenu.isOpen()) {
                    this.parent.closeSubMenus();
                    this.openSubMenu();
                }
            } else {
                this.entryHoverStartTime = -1;
            }
            //Close sub menu if not hovered
            if (!this.isHovered() && this.parent.isHovered() && !this.parent.isSubMenuHovered()) {
                long now = System.currentTimeMillis();
                if (this.entryNotHoveredStartTime == -1) {
                    this.entryNotHoveredStartTime = now;
                }
                if (((this.entryNotHoveredStartTime + 400) < now) && this.subContextMenu.isOpen()) {
                    this.subContextMenu.closeMenu();
                }
            } else {
                this.entryNotHoveredStartTime = -1;
            }
            //Close sub menu if tooltip is active
            if (this.tooltipActive && this.subContextMenu.isOpen()) {
                this.subContextMenu.closeMenu();
            }
        }

        /**
         * Opens the {@link ContextMenu} of this {@link SubMenuContextMenuEntry}.
         *
         * @param entryPath The {@link SubMenuContextMenuEntry} path of menus to open.
         */
        public void openSubMenu(@NotNull List<String> entryPath) {
            if (this.isActive() && !this.subContextMenu.isOpen()) {
                this.subMenuArrowSpin.start();
            }
            this.subContextMenu.openMenuAt(0, 0, entryPath);
        }

        /**
         * Opens the {@link ContextMenu} of this {@link SubMenuContextMenuEntry}.
         */
        public void openSubMenu() {
            if (this.isActive() && !this.subContextMenu.isOpen()) {
                this.subMenuArrowSpin.start();
            }
            this.subContextMenu.openMenuAt(0, 0);
        }

        /** Returns sub context menu. */
        @NotNull
        public ContextMenu getSubContextMenu() {
            return this.subContextMenu;
        }

        /** Sets sub context menu for this sub menu context menu entry. */
        public void setSubContextMenu(@NotNull ContextMenu subContextMenu) {
            this.subContextMenu.closeMenu();
            this.subContextMenu.parentEntry = null;
            this.subContextMenu = subContextMenu;
            this.subContextMenu.parentEntry = this;
        }

        /** Returns sub menu opening side. */
        @NotNull
        public SubMenuOpeningSide getSubMenuOpeningSide() {
            return this.subContextMenu.subMenuOpeningSide;
        }

        /** Sets sub menu opening side for this sub menu context menu entry. */
        public SubMenuContextMenuEntry setSubMenuOpeningSide(@NotNull SubMenuOpeningSide subMenuOpeningSide) {
            this.subContextMenu.subMenuOpeningSide = subMenuOpeningSide;
            return this;
        }

        /** Creates an independent copy with the same configuration. */
        @Override
        public SubMenuContextMenuEntry copy() {
            SubMenuContextMenuEntry copy = new SubMenuContextMenuEntry(this.identifier, this.parent, Component.literal(""), new ContextMenu());
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.labelSupplier = this.labelSupplier;
            copy.clickAction = this.clickAction;
            copy.icon = this.icon;
            copy.materialIcon = this.materialIcon;
            copy.iconWiggleAnimation = this.iconWiggleAnimation.getAnimation().createInstance();
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
            return copy;
        }

        /** Handles removed for this sub menu context menu entry. */
        @Override
        protected void onRemoved() {
            this.subContextMenu.closeMenu();
            this.subContextMenu.parentEntry = null;
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            //Close sub menu when left-clicking outside the menu
            if ((button == 0) && this.subContextMenu.isOpen() && !this.subContextMenu.isUserNavigatingInMenu()) {
                this.subContextMenu.closeMenu();
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        /** Returns the minimum layout width in GUI units. */
        @Override
        public float getMinWidth() {
            float i = super.getMinWidth();
            if (this.tooltipSupplier == null) {
                i += 30;
            } else {
                i += 15;
            }
            return i;
        }

        /** Sets click action for this sub menu context menu entry. */
        @Override
        public @NotNull SubMenuContextMenuEntry setClickAction(@NotNull ClickAction clickAction) {
            LOGGER.error("[KONKRETE] You can't set the click action of SubMenuContextMenuEntries.");
            return this;
        }

        /** Sets shortcut text supplier for this sub menu context menu entry. */
        @Override
        public @NotNull SubMenuContextMenuEntry setShortcutTextSupplier(@Nullable Supplier<Component> shortcutTextSupplier) {
            LOGGER.error("[KONKRETE] You can't set a shortcut text for SubMenuContextMenuEntries.");
            return this;
        }

    }

    /** Represents one renderable, focusable separator context menu entry. */
    public static class SeparatorContextMenuEntry extends ContextMenuEntry<SeparatorContextMenuEntry> {

        /** Adds a visual separator to its owning context menu. */
        public SeparatorContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            super(identifier, parent);
            this.height = 9;
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            float contextMenuScale = this.parent.getScale();
            if (!Float.isFinite(contextMenuScale) || (contextMenuScale <= 0.0F)) {
                contextMenuScale = 1.0F;
            }
            float lineThickness = 2.0F / contextMenuScale;
            float thinnessT = (contextMenuScale - 1.0F) / 1.5F;
            thinnessT = Math.min(1.0F, Math.max(0.0F, thinnessT));
            float alphaFactor = 0.55F + (0.45F * thinnessT);
            float minX = this.x + 10.0F;
            float maxX = this.x + this.width - 10.0F;
            float minY = this.y + 4.0F;
            float snappedPixelY = (float) Math.round(minY * contextMenuScale);
            minY = snappedPixelY / contextMenuScale;
            float maxY = minY + lineThickness;
            int lineColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_border_color.getColorInt() : UIBase.getUITheme().ui_overlay_border_color.getColorInt();
            if (alphaFactor < 0.999F) {
                int baseAlpha = (lineColor >>> 24) & 0xFF;
                int adjustedAlpha = Math.round(baseAlpha * alphaFactor);
                lineColor = RenderingUtils.replaceAlphaInColor(lineColor, adjustedAlpha);
            }
            RenderingUtils.fillF(graphics, minX, minY, maxX, maxY, lineColor);
        }

        /** Creates an independent copy with the same configuration. */
        @Override
        public SeparatorContextMenuEntry copy() {
            SeparatorContextMenuEntry copy = new SeparatorContextMenuEntry(this.identifier, this.parent);
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
            return copy;
        }

        /** Returns the minimum layout width in GUI units. */
        @Override
        public float getMinWidth() {
            int i = 20;
            if (this.addSpaceForIcon) i += 20;
            return i;
        }

        /** Sets focused for this separator context menu entry. */
        @Override
        public void setFocused(boolean var1) {
        }

        /** Reports whether keyboard focus currently targets this control. */
        @Override
        public boolean isFocused() {
            return false;
        }

    }

    /** Represents one renderable, focusable spacer context menu entry. */
    public static class SpacerContextMenuEntry extends ContextMenuEntry<SpacerContextMenuEntry> {

        /** Adds fixed empty space to its owning context menu. */
        public SpacerContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            super(identifier, parent);
            this.height = 4;
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        }

        /** Returns the minimum layout width in GUI units. */
        @Override
        public float getMinWidth() {
            int i = 20;
            if (this.addSpaceForIcon) i += 20;
            return i;
        }

        /** Creates an independent copy with the same configuration. */
        @Override
        public SpacerContextMenuEntry copy() {
            SpacerContextMenuEntry e = new SpacerContextMenuEntry(this.identifier, this.parent);
            e.height = this.height;
            e.stackApplier = this.stackApplier;
            e.stackValueSupplier = this.stackValueSupplier;
            e.stackGroupKey = this.stackGroupKey;
            return e;
        }

        /** Sets focused for this spacer context menu entry. */
        @Override
        public void setFocused(boolean var1) {
        }

        /** Reports whether keyboard focus currently targets this control. */
        @Override
        public boolean isFocused() {
            return false;
        }

    }

    /** Represents one renderable, focusable search context menu entry. */
    public static class SearchContextMenuEntry extends ContextMenuEntry<SearchContextMenuEntry> {

        private static final int FIELD_VERTICAL_PADDING = 2;
        private static final int FIELD_HORIZONTAL_PADDING = 10;
        private static final int MIN_FIELD_WIDTH = 40;
        private static final int MIN_FIELD_HEIGHT = 12;
        @Nullable
        private MaterialIcon icon = MaterialIcons.SEARCH;
        private final ExtendedEditBox searchBox;
        private boolean lastBlurState = UIBase.shouldBlur();

        /** Adds a search field that filters entries in its owning context menu. */
        public SearchContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            super(identifier, parent);
            this.height = 20;
            this.searchBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.empty());
            this.searchBox.setResponder(value -> {
                parent.closeSubMenus();
                parent.onSearchFilterChanged();
            });
            this.applyDefaultSkin();
            this.setChangeBackgroundColorOnHover(false);
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            this.applySkinIfNeeded();
            this.updateSearchBoxBounds();
            this.renderIcon(graphics);
            this.searchBox.extractRenderState(graphics, mouseX, mouseY, partial);
        }

        /** Returns the minimum layout width in GUI units. */
        @Override
        public float getMinWidth() {
            Component hint = Component.translatable("konkrete.ui.generic.search");
            float width = UIBase.getUITextWidthNormal(hint) + (FIELD_HORIZONTAL_PADDING * 2.0F);
            if (this.addSpaceForIcon) {
                width += ClickableContextMenuEntry.ICON_LABEL_SPACING;
            }
            return Math.max(width, 120.0F);
        }

        /** Creates an independent copy with the same configuration. */
        @Override
        public SearchContextMenuEntry copy() {
            SearchContextMenuEntry copy = new SearchContextMenuEntry(this.identifier, this.parent);
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.visibleStateSuppliers = new ArrayList<>(this.visibleStateSuppliers);
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
            copy.icon = this.icon;
            copy.searchBox.setValue(this.searchBox.getValue());
            return copy;
        }

        /** Sets focused for this search context menu entry. */
        @Override
        public void setFocused(boolean var1) {
        }

        /** Reports whether keyboard focus currently targets this control. */
        @Override
        public boolean isFocused() {
            return false;
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.isVisible()) return false;
            this.updateSearchBoxBounds();
            return this.searchBox.mouseClicked(mouseX, mouseY, button);
        }

        /** Routes a mouse-button release and reports whether it was consumed. */
        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            return this.mouseReleased(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button release and reports whether it was consumed. */
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (!this.isVisible()) return false;
            this.updateSearchBoxBounds();
            return this.searchBox.mouseReleased(mouseX, mouseY, button);
        }

        /** Routes pointer dragging and reports whether it was consumed. */
        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
        }

        /** Routes pointer dragging and reports whether it was consumed. */
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (!this.isVisible()) return false;
            this.updateSearchBoxBounds();
            return this.searchBox.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        /** Routes a key press and reports whether it was consumed. */
        @Override
        public boolean keyPressed(KeyEvent event) {
            return this.keyPressed(event.key(), event.scancode(), event.modifiers());
        }

        /** Routes a key press and reports whether it was consumed. */
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.isVisible()) return false;
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }

        /** Routes a key release and reports whether it was consumed. */
        @Override
        public boolean keyReleased(KeyEvent event) {
            return this.keyReleased(event.key(), event.scancode(), event.modifiers());
        }

        /** Routes a key release and reports whether it was consumed. */
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            if (!this.isVisible()) return false;
            return this.searchBox.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
        }

        /** Routes typed character input and reports whether it was consumed. */
        @Override
        public boolean charTyped(CharacterEvent event) {
            return this.charTyped((char)event.codepoint(), 0);
        }

        /** Routes typed character input and reports whether it was consumed. */
        public boolean charTyped(char codePoint, int modifiers) {
            if (!this.isVisible()) return false;
            if (this.parent.needsScrolling) {
                this.parent.scrollPosition = 0.0F;
            }
            return this.searchBox.charTyped(codePoint, modifiers);
        }

        /** Clears search value state. */
        public void resetSearchValue() {
            this.searchBox.setValue("");
            this.searchBox.setFocused(false);
        }

        /** Updates focus or selection for and select all. */
        public void focusAndSelectAll() {
            this.searchBox.setFocused(true);
            this.searchBox.moveCursorToEnd(false);
            this.searchBox.setHighlightPos(0);
        }

        /** Returns search value. */
        @NotNull
        public String getSearchValue() {
            return this.searchBox.getValue();
        }

        /** Reports whether an explicit or Material icon is configured. */
        public boolean hasIconAssigned() {
            return this.icon != null;
        }

        /** Sets icon for this search context menu entry. */
        public SearchContextMenuEntry setIcon(@Nullable MaterialIcon icon) {
            this.icon = icon;
            return this;
        }

        /** Focuses the search entry and selects its existing text. */
        public void prepareForImmediateInput(boolean addIconSpace) {
            this.addSpaceForIcon = addIconSpace;
            if (this.width <= 0.0F) {
                float fallbackWidth = Math.max(this.parent.getWidth(), this.getMinWidth());
                if (fallbackWidth <= 0.0F) {
                    fallbackWidth = this.getMinWidth();
                }
                this.width = fallbackWidth;
            }
            if (this.height <= 0.0F) {
                this.height = 20;
            }
            this.updateSearchBoxBounds();
            this.searchBox.setHighlightPos(this.searchBox.getCursorPosition());
        }

        private void updateSearchBoxBounds() {
            int paddingLeft = FIELD_HORIZONTAL_PADDING + (this.addSpaceForIcon ? ClickableContextMenuEntry.ICON_LABEL_SPACING : 0);
            int paddingRight = FIELD_HORIZONTAL_PADDING;
            int x = Math.round(this.x + paddingLeft);
            int y = Math.round(this.y + FIELD_VERTICAL_PADDING);
            int width = Math.max(MIN_FIELD_WIDTH, Math.round(this.width - paddingLeft - paddingRight));
            int height = Math.max(MIN_FIELD_HEIGHT, Math.round(this.height - FIELD_VERTICAL_PADDING * 2));
            this.searchBox.setX(x);
            this.searchBox.setY(y);
            this.searchBox.setWidth(width);
            this.searchBox.setHeight(height);
        }

        private void renderIcon(@NotNull GuiGraphicsExtractor graphics) {
            IconRenderData iconData = this.resolveIconData();
            if (iconData == null) {
                return;
            }
            float areaX = this.x + ClickableContextMenuEntry.ICON_PADDING_LEFT;
            float areaY = this.y + (this.getHeight() / 2.0F) - (ClickableContextMenuEntry.ICON_WIDTH_HEIGHT / 2.0F);
            UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
            this.blitScaledIcon(graphics, iconData, areaX, areaY, ClickableContextMenuEntry.ICON_WIDTH_HEIGHT, ClickableContextMenuEntry.ICON_WIDTH_HEIGHT);
            RenderingUtils.resetShaderColor(graphics);
        }

        @Nullable
        private IconRenderData resolveIconData() {
            if (this.icon == null) {
                return null;
            }
            float renderSize = ClickableContextMenuEntry.ICON_WIDTH_HEIGHT;
            Identifier location = this.icon.getTextureLocationForUI(renderSize, renderSize);
            if (location == null) {
                return null;
            }
            int iconSize = this.icon.calculateBestTextureSizeForUI(renderSize, renderSize);
            int width = this.icon.getWidth(iconSize);
            int height = this.icon.getHeight(iconSize);
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new IconRenderData(location, width, height);
        }

        private void blitScaledIcon(@NotNull GuiGraphicsExtractor graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
            if (areaWidth <= 0.0F || areaHeight <= 0.0F) {
                return;
            }
            float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
            if (!Float.isFinite(scale) || scale <= 0.0F) {
                return;
            }
            float scaledWidth = iconData.width * scale;
            float scaledHeight = iconData.height * scale;
            float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
            float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(drawX, drawY);
            graphics.pose().scale(scale, scale);
            graphics.blit(RenderPipelines.GUI_TEXTURED, iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
            graphics.pose().popMatrix();
        }

        private void applySkinIfNeeded() {
            boolean blur = UIBase.shouldBlur();
            if (blur != this.lastBlurState) {
                this.lastBlurState = blur;
                this.applyDefaultSkin();
            }
        }

        private void applyDefaultSkin() {
            UIBase.applyDefaultWidgetSkinTo(this.searchBox, this.lastBlurState);
            this.searchBox.setBackgroundColor(DrawableColor.FULLY_TRANSPARENT);
            this.searchBox.setBorderNormalColor(DrawableColor.FULLY_TRANSPARENT);
            this.searchBox.setBorderFocusedColor(DrawableColor.FULLY_TRANSPARENT);
        }

    }

    /**
     * Metadata for a stacked entry chain.
     * <p>
     * Each entry in a stacked menu has a {@link ContextMenuStackMeta} instance. All entries in
     * the same stack share the same {@link #properties} object for coordination.
     */
    public static class ContextMenuStackMeta {

        /** Runtime properties applied to this stack entry. */
        protected RuntimePropertyContainer properties = new RuntimePropertyContainer();
        /** Whether adjacent separators may join this separator stack. */
        protected boolean stackable = false;
        /** Whether this separator belongs to a joined stack. */
        protected boolean partOfStack = false;
        /** Whether this separator begins its joined stack. */
        protected boolean firstInStack = true;
        /** Whether this separator ends its joined stack. */
        protected boolean lastInStack = true;
        /** Next linked property in the stack. */
        protected ContextMenuEntry<?> nextInStack;

        /**
         * This is a shared instance. Every entry in the stack has access to the same
         * {@link RuntimePropertyContainer} instance.
         */
        @NotNull
        public RuntimePropertyContainer getProperties() {
            return this.properties;
        }

        /**
         * @return true if this entry is part of a stack.
         */
        public boolean isPartOfStack() {
            return this.partOfStack;
        }

        /**
         * @return true if this is the first entry in the stack (the one rendered in the menu).
         */
        public boolean isFirstInStack() {
            return this.firstInStack;
        }

        /**
         * @return true if this is the last entry in the stack.
         */
        public boolean isLastInStack() {
            return this.lastInStack;
        }

        /**
         * @return true if this entry is marked as stackable.
         */
        public boolean isStackable() {
            return this.stackable;
        }

        /**
         * Sets whether this entry can be stacked.
         */
        public void setStackable(boolean stackable) {
            this.stackable = stackable;
        }

        /**
         * @return the next entry in the stack chain, or null if this is the last.
         */
        @Nullable
        public ContextMenuEntry<?> getNextInStack() {
            return this.nextInStack;
        }

    }

    /** Identifies one supported sub menu opening option. */
    public enum SubMenuOpeningSide {

        /** Positions the element at left. */
        LEFT,
        /** Positions the element at right. */
        RIGHT

    }

    /** Supplies supplier values on demand. */
    @FunctionalInterface
    public interface Supplier<T> {

        /** Supplies a value using the active menu and entry state. */
        T get(ContextMenu menu, ContextMenuEntry<?> entry);

    }

    /**
     * Stack applier used to apply a new value on a stack entry.
     * <p>
     * The entry argument is the specific stack entry being applied.
     *
     * <p><b>Example</b>
     * <pre>{@code
     * entry.setStackApplier((stackEntry, value) -> {
     *     if (value instanceof Integer i) {
     *         builder.self().setPadding(i);
     *     }
     * });
     * }</pre>
     */
    @FunctionalInterface
    public interface StackApplier {

        /** Applies a value to one entry in the current stacked selection. */
        void apply(ContextMenuEntry<?> entry, @Nullable Object value);

    }

    /**
     * Supplies the current value for a stack entry, used to detect mixed state.
     *
     * <p><b>Example</b>
     * <pre>{@code
     * entry.setStackValueSupplier(stackEntry -> builder.self().getPadding());
     * }</pre>
     */
    @FunctionalInterface
    public interface StackValueSupplier {

        /** Returns the value represented by one stacked entry. */
        @Nullable
        Object get(ContextMenuEntry<?> entry);

    }

    /** Supplies boolean values on demand. */
    @FunctionalInterface
    public interface BooleanSupplier extends Supplier<Boolean> {

        /** Returns boolean for this widget. */
        default boolean getBoolean(ContextMenu menu, ContextMenuEntry<?> entry) {
            Boolean b = this.get(menu, entry);
            if (b != null) {
                return b;
            }
            return false;
        }

    }

    /** Resolves names from Konkrete's bundled white context-menu icon set. */
    public static class IconFactory {

        /** Builds the texture identifier for a bundled icon name without its {@code .png} suffix. */
        @NotNull
        public static Identifier getIcon(@NotNull String iconName) {
            return Identifier.fromNamespaceAndPath("konkrete", "textures/contextmenu/icons/" + iconName + ".png");
        }

    }

}
