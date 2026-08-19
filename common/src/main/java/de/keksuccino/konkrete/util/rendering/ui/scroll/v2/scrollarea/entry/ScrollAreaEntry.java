package de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.TooltipHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

/** Base implementation for scroll area entry. */
@SuppressWarnings("unused")
public abstract class ScrollAreaEntry extends UIBase implements Renderable {

    /** Scroll area displaying parent. */
    public ScrollArea parent;
    /** Horizontal component of the current transform. */
    protected float x;
    /** Vertical component of the current transform. */
    protected float y;
    /** Width in GUI units for width. */
    protected float width;
    /** Height in GUI units for height. */
    protected float height;
    /** Supplies the entry background while idle. */
    @Nullable
    protected Supplier<DrawableColor> backgroundColorNormal = () -> {
        if (this.parent.isSetupForBlurInterface() && UIBase.shouldBlur()) return DrawableColor.FULLY_TRANSPARENT;
        return getUITheme().ui_interface_area_background_color_type_1;
    };
    /** Supplies the entry background while hovered. */
    @Nullable
    protected Supplier<DrawableColor> backgroundColorHover = () -> {
        if (this.parent.isSetupForBlurInterface() && UIBase.shouldBlur()) return getUITheme().ui_blur_interface_area_entry_selected_color;
        return getUITheme().ui_interface_area_entry_selected_color;
    };
    /** Tooltip shown for tooltip. */
    @Nullable
    protected UITooltip tooltip;
    /** Whether this entry can become selected. */
    protected boolean selectable = true;
    /** Whether this entry is selected. */
    protected boolean selected = false;
    /** Whether this entry responds to pointer activation. */
    protected boolean clickable = true;
    /** Whether entry activation plays the click sound. */
    protected boolean playClickSound = true;
    /** Whether selecting one entry clears other selections. */
    public boolean deselectOtherEntriesOnSelect = true;
    /** Whether pointer activation selects the entry. */
    public boolean selectOnClick = true;
    /** Zero-based display index assigned by the parent scroll area. */
    public int index = 0;
    /** Whether the pointer currently hovers this element. */
    protected boolean hovered = false;
    /** GUI frame identifier of the most recent hover-state update. */
    protected int lastHoverUpdateFrameId = -1;

    /** Attaches an entry of the supplied size to its owning scroll area. */
    public ScrollAreaEntry(ScrollArea parent, float width, float height) {
        this.parent = parent;
        this.width = width;
        this.height = height;
    }

    /** Renders entry into the active GUI extraction pass. */
    public abstract void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial);

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.hovered = this.isMouseOver(mouseX, mouseY);
        if (this.parent != null) {
            this.lastHoverUpdateFrameId = this.parent.getRenderFrameId();
        }
        if (this.hovered && (this.tooltip != null)) TooltipHandler.INSTANCE.addRenderTickTooltip(this.tooltip, () -> true);
        this.extractBackground(graphics, mouseX, mouseY, partial);
        this.renderEntry(graphics, mouseX, mouseY, partial);
    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    protected void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (!this.isHovered() && !this.isSelected()) {
            if (this.backgroundColorNormal != null) {
                DrawableColor c = this.backgroundColorNormal.get();
                if (c != null) this.renderRoundedEntryBackground(graphics, partial, c.getColorInt());
            }
        } else if (this.backgroundColorHover != null) {
            DrawableColor c = this.backgroundColorHover.get();
            if (c != null) this.renderRoundedEntryBackground(graphics, partial, c.getColorInt());
        }
    }

    /** Renders rounded entry background into the active GUI extraction pass. */
    protected void renderRoundedEntryBackground(@NotNull GuiGraphicsExtractor graphics, float partial, int color) {
        if (this.parent == null || !this.parent.isRoundedStyle()) {
            fillF(graphics, this.x, this.y, this.x + this.width, this.y + this.height, color);
            return;
        }
        float areaX = this.parent.getInnerX();
        float areaY = this.parent.getInnerY();
        float areaWidth = this.parent.getInnerWidth();
        float areaHeight = this.parent.getInnerHeight();
        if (areaWidth <= 0.0F || areaHeight <= 0.0F) {
            return;
        }
        float entryTop = this.y;
        float entryBottom = this.y + this.height;
        float visibleTop = Math.max(entryTop, areaY);
        float visibleBottom = Math.min(entryBottom, areaY + areaHeight);
        float visibleHeight = visibleBottom - visibleTop;
        if (visibleHeight <= 0.0F) {
            return;
        }
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        float maxRadius = Math.min(areaWidth, areaHeight) * 0.5F;
        if (maxRadius <= 0.0F) {
            return;
        }
        radius = Math.min(radius, maxRadius);
        if (radius <= 0.0F) {
            fillF(graphics, this.x, visibleTop, this.x + this.width, visibleTop + visibleHeight, color);
            return;
        }
        float topRoundedLimit = areaY + radius;
        float bottomRoundedLimit = areaY + areaHeight - radius;
        boolean needsRounded = visibleTop < topRoundedLimit || visibleBottom > bottomRoundedLimit;
        float left = Math.max(this.x, areaX);
        float right = Math.min(this.x + this.width, areaX + areaWidth);
        if (right <= left) {
            return;
        }
        if (needsRounded) {
            float scissorPadding = this.parent.isScissorEnabled() ? 2.0F : 0.0F;
            float scissorX = areaX + scissorPadding;
            float scissorY = areaY + scissorPadding;
            float scissorWidth = areaWidth - (scissorPadding * 2.0F);
            float scissorHeight = areaHeight - (scissorPadding * 2.0F);
            float scissorLeft = Math.max(left, scissorX);
            float scissorRight = Math.min(right, scissorX + scissorWidth);
            float scissorTop = Math.max(visibleTop, scissorY);
            float scissorBottom = Math.min(visibleBottom, scissorY + scissorHeight);
            if (scissorRight <= scissorLeft || scissorBottom <= scissorTop) {
                return;
            }
            graphics.enableScissor((int) Math.floor(scissorLeft), (int) Math.floor(scissorTop), (int) Math.ceil(scissorRight), (int) Math.ceil(scissorBottom));
            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, areaX, areaY, areaWidth, areaHeight, radius, radius, radius, radius, color, partial);
            graphics.disableScissor();
        } else {
            fillF(graphics, left, visibleTop, right, visibleTop + visibleHeight, color);
        }
    }

    /** Handles click for this scroll area entry. */
    public abstract void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button);

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isClickable() && this.isHovered() && !this.parent.isMouseInteractingWithGrabbers() && this.parent.isInnerAreaHovered()) {
            if ((button == 0) && this.selectOnClick) {
                this.setSelected(true);
            }
            if ((button == 0) && this.playClickSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.onClick(this, mouseX, mouseY, button);
            return true;
        }
        return false;
    }

    /** Reports whether the current pointer position lies inside this element's hitbox. */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    /** Sets x for this scroll area entry. */
    public void setX(float x) {
        this.x = x;
    }

    /** Returns the horizontal position in GUI units. */
    public float getX() {
        return this.x;
    }

    /** Sets y for this scroll area entry. */
    public void setY(float y) {
        this.y = y;
    }

    /** Returns the vertical position in GUI units. */
    public float getY() {
        return this.y;
    }

    /** Sets width for this scroll area entry. */
    public void setWidth(float width) {
        this.width = width;
    }

    /** Returns the current width in GUI units. */
    public float getWidth() {
        return this.width;
    }

    /** Sets height for this scroll area entry. */
    public void setHeight(float height) {
        this.height = height;
    }

    /** Returns the current height in GUI units. */
    public float getHeight() {
        return this.height;
    }

    /** Reports whether the pointer currently hovers this element. */
    public boolean isHovered() {
        if (this.parent != null && this.lastHoverUpdateFrameId != this.parent.getRenderFrameId()) return false;
        if (!this.parent.isInnerAreaHovered()) return false;
        if (this.parent.isMouseInteractingWithGrabbers()) return false;
        return this.hovered;
    }

    /** Reports whether this entry is currently selected. */
    public boolean isSelected() {
        return this.selectable && this.selected;
    }

    /** Sets selected for this scroll area entry. */
    public void setSelected(boolean selected) {
        if (this.selectable) {
            this.selected = selected;
            if (selected && this.deselectOtherEntriesOnSelect) {
                for (ScrollAreaEntry e : this.parent.getEntries()) {
                    if (e != this) {
                        e.setSelected(false);
                    }
                }
            }
        }
    }

    /** Reports whether this entry can become selected. */
    public boolean isSelectable() {
        return this.selectable;
    }

    /** Sets selectable for this scroll area entry. */
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        if (!selectable) {
            this.selected = false;
        }
    }

    /** Sets clickable for this scroll area entry. */
    public void setClickable(boolean clickable) {
        this.clickable = true;
    }

    /** Returns whether clickable. */
    public boolean isClickable() {
        return this.clickable;
    }

    /** Sets play click sound for this scroll area entry. */
    public void setPlayClickSound(boolean playClickSound) {
        this.playClickSound = playClickSound;
    }

    /** Returns whether play click sound. */
    public boolean isPlayClickSound() {
        return this.playClickSound;
    }

    /** Returns the entry background while idle. */
    @Nullable
    public Supplier<DrawableColor> getBackgroundColorNormal() {
        return this.backgroundColorNormal;
    }

    /** Sets background color normal for this scroll area entry. */
    public void setBackgroundColorNormal(@Nullable Supplier<DrawableColor> backgroundColorNormal) {
        this.backgroundColorNormal = backgroundColorNormal;
    }

    /** Returns the entry background while hovered. */
    @Nullable
    public Supplier<DrawableColor> getBackgroundColorHover() {
        return this.backgroundColorHover;
    }

    /** Sets background color hover for this scroll area entry. */
    public void setBackgroundColorHover(@Nullable Supplier<DrawableColor> backgroundColorHover) {
        this.backgroundColorHover = backgroundColorHover;
    }

    /** Sets tooltip for this scroll area entry. */
    public void setTooltip(@Nullable UITooltip UITooltip) {
        this.tooltip = UITooltip;
    }

}
