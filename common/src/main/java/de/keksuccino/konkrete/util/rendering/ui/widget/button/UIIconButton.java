package de.keksuccino.konkrete.util.rendering.ui.widget.button;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import de.keksuccino.konkrete.util.rendering.IconAnimation;
import de.keksuccino.konkrete.util.rendering.IconAnimations;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.Identifier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

/** Renders a Material Icons glyph as a focusable, clickable control. */
@SuppressWarnings("unused")
public class UIIconButton implements Renderable, GuiEventListener, NarratableEntry {

    private static final float DEFAULT_ICON_PADDING = 4.0F;
    private float x;
    private float y;
    private float width;
    private float height;
    private float iconPadding = DEFAULT_ICON_PADDING;
    private float iconAlpha = 1.0F;
    @Nonnull
    private MaterialIcon icon;
    @Nullable
    private Consumer<UIIconButton> clickAction;
    @Nullable
    private IconAnimation iconHoverAnimation = IconAnimations.SHORT_DIAGONAL_BOUNCE;
    @Nullable
    private IconAnimation.Instance iconHoverAnimationInstance = this.iconHoverAnimation.createInstance();
    private boolean hovered = false;
    private boolean focused = false;

    /** Positions a Material icon button and installs its optional click callback. */
    public UIIconButton(float x, float y, float width, float height, @Nonnull MaterialIcon icon, @Nullable Consumer<UIIconButton> clickAction) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.icon = Objects.requireNonNull(icon, "icon");
        this.clickAction = clickAction;
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@Nonnull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        boolean wasHovered = this.hovered;
        this.hovered = this.isMouseOver(mouseX, mouseY);
        this.updateHoverAnimation(wasHovered);

        if (this.hovered) {
            UIBase.renderIconButtonHoverBackground(graphics, this.x, this.y, this.width, this.height);
        }

        IconAnimation.Offset offset = this.getHoverOffset();
        this.renderIcon(graphics, offset);
    }

    private void updateHoverAnimation(boolean wasHovered) {
        if (this.iconHoverAnimationInstance == null) {
            return;
        }
        if (!UIBase.shouldPlayAnimations()) {
            this.iconHoverAnimationInstance.reset();
            return;
        }
        if (this.hovered) {
            if (!wasHovered) {
                this.iconHoverAnimationInstance.start();
            }
        } else {
            this.iconHoverAnimationInstance.reset();
        }
    }

    @Nonnull
    private IconAnimation.Offset getHoverOffset() {
        if (!UIBase.shouldPlayAnimations()) {
            return IconAnimation.Offset.ZERO;
        }
        if (this.iconHoverAnimationInstance == null) {
            return IconAnimation.Offset.ZERO;
        }
        return this.iconHoverAnimationInstance.getOffset();
    }

    private void renderIcon(@Nonnull GuiGraphicsExtractor graphics, @Nonnull IconAnimation.Offset offset) {
        float maxSize = Math.min(this.width, this.height);
        float padding = Math.max(0.0F, this.iconPadding);
        float baseSize = Math.max(1.0F, maxSize - (padding * 2.0F));
        float areaWidth = Math.max(1.0F, baseSize + offset.widthOffset());
        float areaHeight = Math.max(1.0F, baseSize + offset.heightOffset());
        IconRenderData iconData = resolveMaterialIconData(this.icon, areaWidth, areaHeight);
        if (iconData == null) {
            return;
        }
        float baseX = this.x + (this.width - areaWidth) * 0.5F;
        float baseY = this.y + (this.height - areaHeight) * 0.5F;
        float drawX = baseX + offset.x();
        float drawY = baseY + offset.y();

        de.keksuccino.konkrete.util.rendering.RenderingUtils.defaultBlendFunc();
        UIBase.getUITheme().setUITextureShaderColor(graphics, this.iconAlpha);
        blitScaledIcon(graphics, iconData, drawX, drawY, areaWidth, areaHeight, offset.rotationDegrees());
        UIBase.resetShaderColor(graphics);
    }

    @Nullable
    private static IconRenderData resolveMaterialIconData(@Nullable MaterialIcon icon, float renderWidth, float renderHeight) {
        if (icon == null) {
            return null;
        }
        float safeRenderWidth = Math.max(1.0F, renderWidth);
        float safeRenderHeight = Math.max(1.0F, renderHeight);
        Identifier location = icon.getTextureLocationForUI(safeRenderWidth, safeRenderHeight);
        if (location == null) {
            return null;
        }
        int size = icon.calculateBestTextureSizeForUI(safeRenderWidth, safeRenderHeight);
        int width = icon.getWidth(size);
        int height = icon.getHeight(size);
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new IconRenderData(location, width, height);
    }

    private static void blitScaledIcon(@Nonnull GuiGraphicsExtractor graphics, @Nonnull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight, float rotationDegrees) {
        if (areaWidth <= 0.0F || areaHeight <= 0.0F || iconData.width <= 0 || iconData.height <= 0) {
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
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
        graphics.pose().popMatrix();
    }

    /** Returns the left edge in GUI units. */
    public float getX() {
        return this.x;
    }

    /** Returns the top edge in GUI units. */
    public float getY() {
        return this.y;
    }

    /** Returns the current width in GUI units. */
    public float getWidth() {
        return this.width;
    }

    /** Returns the current height in GUI units. */
    public float getHeight() {
        return this.height;
    }

    /** Returns the inset between the button bounds and icon in GUI units. */
    public float getIconPadding() {
        return this.iconPadding;
    }

    /** Returns the opacity multiplier applied to the icon. */
    public float getIconAlpha() {
        return this.iconAlpha;
    }

    /** Reports whether the pointer currently hovers this element. */
    public boolean isHovered() {
        return this.hovered;
    }

    /** Returns the Material glyph rendered by this button. */
    @Nonnull
    public MaterialIcon getIcon() {
        return this.icon;
    }

    /** Returns the optional animation sampled while the icon is hovered. */
    @Nullable
    public IconAnimation getIconHoverAnimation() {
        return this.iconHoverAnimation;
    }

    /** Returns the optional action receiving this button on activation. */
    @Nullable
    public Consumer<UIIconButton> getClickAction() {
        return this.clickAction;
    }

    /** Sets x for this UI icon button. */
    public UIIconButton setX(float x) {
        this.x = x;
        return this;
    }

    /** Sets y for this UI icon button. */
    public UIIconButton setY(float y) {
        this.y = y;
        return this;
    }

    /** Sets width for this UI icon button. */
    public UIIconButton setWidth(float width) {
        this.width = width;
        return this;
    }

    /** Sets height for this UI icon button. */
    public UIIconButton setHeight(float height) {
        this.height = height;
        return this;
    }

    /** Sets icon padding for this UI icon button. */
    public UIIconButton setIconPadding(float iconPadding) {
        this.iconPadding = Math.max(0.0F, iconPadding);
        return this;
    }

    /** Sets icon alpha for this UI icon button. */
    public UIIconButton setIconAlpha(float iconAlpha) {
        if (Float.isFinite(iconAlpha)) {
            this.iconAlpha = Math.max(0.0F, Math.min(1.0F, iconAlpha));
        }
        return this;
    }

    /** Sets icon for this UI icon button. */
    public UIIconButton setIcon(@Nonnull MaterialIcon icon) {
        this.icon = Objects.requireNonNull(icon, "icon");
        return this;
    }

    /** Sets icon hover animation for this UI icon button. */
    public UIIconButton setIconHoverAnimation(@Nullable IconAnimation iconHoverAnimation) {
        this.iconHoverAnimation = iconHoverAnimation;
        this.iconHoverAnimationInstance = (iconHoverAnimation != null) ? iconHoverAnimation.createInstance() : null;
        if (this.hovered && this.iconHoverAnimationInstance != null && UIBase.shouldPlayAnimations()) {
            this.iconHoverAnimationInstance.start();
        }
        return this;
    }

    /** Sets click action for this UI icon button. */
    public UIIconButton setClickAction(@Nullable Consumer<UIIconButton> clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (this.clickAction != null) {
            this.clickAction.accept(this);
        }
        return true;
    }

    /** Reports whether the current pointer position lies inside this element's hitbox. */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.width <= 0.0F || this.height <= 0.0F) {
            return false;
        }
        return UIBase.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    /** Sets focused for this UI icon button. */
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    /** Reports whether keyboard focus currently targets this control. */
    @Override
    public boolean isFocused() {
        return this.focused;
    }

    /** Returns this component's priority in the narration order. */
    @Override
    @Nonnull
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    /** Refreshes narration from current state. */
    @Override
    public void updateNarration(@Nonnull NarrationElementOutput var1) {
    }

    private static final class IconRenderData {
        private final Identifier texture;
        private final int width;
        private final int height;

        private IconRenderData(@Nonnull Identifier texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }
}
