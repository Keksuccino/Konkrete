package de.keksuccino.konkrete.util.rendering.ui.widget.button;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinAbstractWidget;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinAbstractWidgetWithInactiveMessage;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinButton;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.IExtendedWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.slider.UIWidget;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

/** Extends Minecraft's button with icons, custom backgrounds, scaled labels, and hover feedback. */
@SuppressWarnings("unused")
public class ExtendedButton extends Button implements IExtendedWidget, UniqueWidget, NavigatableWidget, UIWidget {

    /** Vanilla button textures used when no custom background is configured. */
    public static final WidgetSprites SPRITES = new WidgetSprites(
            Identifier.parse("widget/button"),
            Identifier.parse("widget/button_disabled"),
            Identifier.parse("widget/button_highlighted")
    );

    /** Client instance providing font and sound services. */
    protected final Minecraft mc = Minecraft.getInstance();
    /** Whether the button label is rendered. */
    protected boolean enableLabel = true;
    /** Label color while the button is active. */
    protected DrawableColor labelBaseColorNormal = DrawableColor.of(new Color(0xFFFFFF));
    /** Label color while the button is inactive. */
    protected DrawableColor labelBaseColorInactive = DrawableColor.of(new Color(0xA0A0A0));
    /** Whether the control label is drawn with a shadow. */
    protected boolean labelShadow = true;
    /** Whether labels use UIBase text metrics and rendering. */
    protected boolean renderLabelWithUiBase = false;
    /** Computes the label from the button's current state. */
    @NotNull
    protected ConsumingSupplier<ExtendedButton, Component> labelSupplier = consumes -> Component.empty();
    /** Optionally computes a tooltip from the button's current state. */
    protected ConsumingSupplier<ExtendedButton, UITooltip> uiTooltipSupplier = null;
    /** Optional solid background while active and idle. */
    @Nullable
    protected DrawableColor backgroundColorNormal;
    /** Optional solid background while hovered or focused. */
    @Nullable
    protected DrawableColor backgroundColorHover;
    /** Optional solid background while inactive. */
    @Nullable
    protected DrawableColor backgroundColorInactive;
    /** Optional border color while active and idle. */
    @Nullable
    protected DrawableColor borderColorNormal;
    /** Optional border color while hovered or focused. */
    @Nullable
    protected DrawableColor borderColorHover;
    /** Optional border color while inactive. */
    @Nullable
    protected DrawableColor borderColorInactive;
    /** Icon displayed for icon normal. */
    @Nullable
    protected RenderableResource iconNormal;
    /** Icon displayed for icon hover. */
    @Nullable
    protected RenderableResource iconHover;
    /** Icon displayed for icon inactive. */
    @Nullable
    protected RenderableResource iconInactive;
    /** Computes whether the control is active. */
    @Nullable
    protected ConsumingSupplier<ExtendedButton, Boolean> activeSupplier;
    /** Computes whether the button is visible. */
    protected ConsumingSupplier<ExtendedButton, Boolean> visibilitySupplier;
    /** Whether this control may receive focus. */
    protected boolean focusable = true;
    /** Whether keyboard navigation may target this control. */
    protected boolean navigatable = true;
    /** Whether to draw the configurable background with rounded corners. */
    protected boolean roundedColorBackground = false;
    /** Optional stable identifier exposed through {@link UniqueWidget}. */
    @Nullable
    protected String identifier;

    /** Initializes a button with bounds, label, press action, and optional narration. */
    public ExtendedButton(int x, int y, int width, int height, @NotNull String label, @NotNull OnPress onPress) {
        super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
        this.setLabel(Component.literal(label));
    }

    /** Initializes a button with bounds, label, press action, and optional narration. */
    public ExtendedButton(int x, int y, int width, int height, @NotNull String label, @NotNull OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, Component.literal(""), onPress, narration);
        this.setLabel(Component.literal(label));
    }

    /** Initializes a button with bounds, label, press action, and optional narration. */
    public ExtendedButton(int x, int y, int width, int height, @NotNull Component label, @NotNull OnPress onPress) {
        super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
        this.setLabel(label);
    }

    /** Initializes a button with bounds, label, press action, and optional narration. */
    public ExtendedButton(int x, int y, int width, int height, @NotNull Component label, @NotNull OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, Component.literal(""), onPress, narration);
        this.setLabel(label);
    }

    /** Adds this component's content draw state to the active GUI extraction pass. */
    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.updateIsActive();
        this.updateVisibility();
        this.updateLabel();
        UITooltip tooltip = this.getUITooltip();
        if ((tooltip != null) && this.isHovered() && this.visible) {
            TooltipHandler.INSTANCE.addRenderTickTooltip(tooltip, () -> true);
        }

        RenderingUtils.setDepthTestLocked(true);

        this.extractBackground(graphics, partial);
        this.renderIcon(graphics);
        this.renderLabelText(graphics);

        RenderingUtils.setDepthTestLocked(false);

    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    protected void extractBackground(@NotNull GuiGraphicsExtractor graphics, float partial) {
        //Renders the custom widget background if one is present or the Vanilla background if no custom background is present
        if (this.getExtendedAsCustomizableWidget().renderCustomBackgroundKonkrete(this, graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight())) {
            if (this.renderColorBackground(graphics, partial)) {
                RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.alpha);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
                RenderingUtils.resetShaderColor(graphics);
            }
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    /**
     * Returns if the button should render its Vanilla background (true) or not (false).
     */
    protected boolean renderColorBackground(@NotNull GuiGraphicsExtractor graphics, float partial) {
        DrawableColor background = null;
        DrawableColor border = null;
        if (this.active) {
            if (this.isHoveredOrFocused()) {
                background = this.backgroundColorHover;
                border = this.borderColorHover;
            } else {
                background = this.backgroundColorNormal;
                border = this.borderColorNormal;
            }
        } else {
            background = this.backgroundColorInactive;
            border = this.borderColorInactive;
        }

        if (background != null) {
            renderColoredBackground(graphics, background.getColorInt(), border, partial);
            return false;
        }
        return true;
    }

    private void renderColoredBackground(@NotNull GuiGraphicsExtractor graphics, int backgroundColor, @Nullable DrawableColor borderColor, float partial) {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();
        float radius = this.roundedColorBackground ? UIBase.getWidgetCornerRoundingRadius() : 0.0F;
        int borderThickness = borderColor != null ? 1 : 0;
        int innerX = x + borderThickness;
        int innerY = y + borderThickness;
        int innerWidth = width - (borderThickness * 2);
        int innerHeight = height - (borderThickness * 2);
        if (radius > 0.0F) {
            if (innerWidth > 0 && innerHeight > 0) {
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        innerX,
                        innerY,
                        innerWidth,
                        innerHeight,
                        radius,
                        radius,
                        radius,
                        radius,
                        backgroundColor,
                        partial
                );
            }
            if (borderColor != null) {
                float borderRadius = radius > 0.0F ? radius + borderThickness : 0.0F;
                SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(
                        graphics,
                        x,
                        y,
                        width,
                        height,
                        borderThickness,
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderColor.getColorInt(),
                        partial
                );
            }
        } else {
            if (innerWidth > 0 && innerHeight > 0) {
                graphics.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, backgroundColor);
            }
            if (borderColor != null) {
                UIBase.renderBorder(graphics, x, y, x + width, y + height, 1, borderColor.getColorInt(), true, true, true, true);
            }
        }
    }

    /** Renders label text into the active GUI extraction pass. */
    protected void renderLabelText(@NotNull GuiGraphicsExtractor graphics) {
        if (this.enableLabel) {
            int k = this.active ? this.labelBaseColorNormal.getColorIntWithAlpha(this.alpha) : this.labelBaseColorInactive.getColorIntWithAlpha(this.alpha);
            boolean labelShadowFinal = this.labelShadow;
            if (this.renderLabelWithUiBase) {
                this.renderScrollingLabelUiBase(this, graphics, 2, k);
            } else {
                this.renderScrollingLabel(this, graphics, mc.font, 2, labelShadowFinal, k);
            }
        }
    }

    /** Renders icon into the active GUI extraction pass. */
    protected void renderIcon(@NotNull GuiGraphicsExtractor graphics) {
        RenderableResource icon = this.getCurrentIcon();
        if (icon == null) return;
        Identifier iconLocation = icon.getResourceLocation();
        if (iconLocation == null) return;
        int iconWidth = icon.getWidth();
        int iconHeight = icon.getHeight();
        if (iconWidth <= 0 || iconHeight <= 0) return;
        int renderHeight = this.getHeight();
        if (renderHeight <= 0) return;
        int renderWidth = Math.max(1, (int) Math.round((double) renderHeight * ((double) iconWidth / (double) iconHeight)));
        if (renderWidth <= 0) return;
        int renderX = this.getX() + ((this.getWidth() - renderWidth) / 2);
        RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.alpha);
        graphics.blit(RenderPipelines.GUI_TEXTURED, iconLocation, renderX, this.getY(), 0.0F, 0.0F, renderWidth, renderHeight, renderWidth, renderHeight);
        RenderingUtils.resetShaderColor(graphics);
    }

    /** Returns current icon. */
    @Nullable
    protected RenderableResource getCurrentIcon() {
        if (this.active) {
            if (this.isHoveredOrFocused()) {
                return this.iconHover;
            }
            return this.iconNormal;
        }
        return this.iconInactive;
    }

    /** Refreshes label from current state. */
    protected void updateLabel() {
        Component c = this.labelSupplier.get(this);
        if (c == null) c = Component.literal("");
        this.setVanillaLabelFields(c);
    }

    private void setVanillaLabelFields(@NotNull Component label) {
        ((AccessorMixinAbstractWidget)this).set_message_Konkrete(label);
        ((AccessorMixinAbstractWidgetWithInactiveMessage)this).set_inactiveMessage_Konkrete(label);
    }

    /** Refreshes is active from current state. */
    protected void updateIsActive() {
        if (this.activeSupplier != null) {
            Boolean b = this.activeSupplier.get(this);
            if (b != null) this.active = b;
        }
    }

    /** Refreshes visibility from current state. */
    protected void updateVisibility() {
        if (this.visibilitySupplier != null) {
            Boolean b = this.visibilitySupplier.get(this);
            if (b != null) this.visible = b;
        }
    }

    /** Sets height for this extended button. */
    public void setHeight(int height) {
        this.height = height;
    }

    /** Returns hover state. */
    protected int getHoverState() {
        if (this.isHovered) return 1;
        return 0;
    }

    /** Reports whether this control may receive keyboard focus. */
    public boolean isFocusable() {
        return this.focusable;
    }

    /** Sets focusable for this extended button. */
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    /** Reports whether keyboard navigation may target this control. */
    @Override
    public boolean isNavigatable() {
        return this.navigatable;
    }

    /** Sets navigatable for this extended button. */
    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

    /** Returns UI tooltip supplier. */
    @Nullable
    public ConsumingSupplier<ExtendedButton, UITooltip> getUITooltipSupplier() {
        return this.uiTooltipSupplier;
    }

    /** Sets UI tooltip supplier for this extended button. */
    public ExtendedButton setUITooltipSupplier(@Nullable ConsumingSupplier<ExtendedButton, UITooltip> tooltipSupplier) {
        this.uiTooltipSupplier = tooltipSupplier;
        return this;
    }

    /** Returns UI tooltip. */
    @Nullable
    public UITooltip getUITooltip() {
        if (this.uiTooltipSupplier != null) {
            return this.uiTooltipSupplier.get(this);
        }
        return null;
    }

    /** Sets UI tooltip for this extended button. */
    public ExtendedButton setUITooltip(@Nullable UITooltip tooltip) {
        if (tooltip == null) {
            this.uiTooltipSupplier = null;
        } else {
            this.uiTooltipSupplier = (button) -> tooltip;
        }
        return this;
    }

    /** Sets message for this extended button. */
    @Deprecated
    @Override
    public void setMessage(@NotNull Component msg) {
        this.setLabel(msg);
    }

    /** Returns the component currently exposed for rendering and narration. */
    @Deprecated
    @Override
    public @NotNull Component getMessage() {
        return super.getMessage();
    }

    /** Sets label for this extended button. */
    public ExtendedButton setLabel(@NotNull Component label) {
        this.labelSupplier = (btn) -> label;
        this.setVanillaLabelFields(label);
        return this;
    }

    /** Sets label for this extended button. */
    public ExtendedButton setLabel(@NotNull String label) {
        return this.setLabel(Component.literal(label));
    }

    /** Sets label supplier for this extended button. */
    public ExtendedButton setLabelSupplier(@NotNull ConsumingSupplier<ExtendedButton, Component> labelSupplier) {
        this.labelSupplier = labelSupplier;
        this.updateLabel();
        return this;
    }

    /** Returns label supplier. */
    @NotNull
    public ConsumingSupplier<ExtendedButton, Component> getLabelSupplier() {
        return this.labelSupplier;
    }

    /** Returns the label resolved for the current state. */
    @NotNull
    public Component getLabel() {
        Component c = this.getLabelSupplier().get(this);
        if (c == null) c = Component.empty();
        return c;
    }

    /** Returns whether label enabled. */
    public boolean isLabelEnabled() {
        return this.enableLabel;
    }

    /** Sets label enabled for this extended button. */
    public ExtendedButton setLabelEnabled(boolean enabled) {
        this.enableLabel = enabled;
        return this;
    }

    /** Returns label base color normal. */
    public DrawableColor getLabelBaseColorNormal() {
        return this.labelBaseColorNormal;
    }

    /** Sets label base color normal for this extended button. */
    public void setLabelBaseColorNormal(DrawableColor labelBaseColorNormal) {
        this.labelBaseColorNormal = labelBaseColorNormal;
    }

    /** Returns label base color inactive. */
    public DrawableColor getLabelBaseColorInactive() {
        return this.labelBaseColorInactive;
    }

    /** Sets label base color inactive for this extended button. */
    public void setLabelBaseColorInactive(DrawableColor labelBaseColorInactive) {
        this.labelBaseColorInactive = labelBaseColorInactive;
    }

    /** Returns whether label shadow enabled. */
    public boolean isLabelShadowEnabled() {
        return this.labelShadow;
    }

    /** Sets label shadow enabled for this extended button. */
    public ExtendedButton setLabelShadowEnabled(boolean enabled) {
        this.labelShadow = enabled;
        return this;
    }

    /** Returns whether label rendered with UI base. */
    public boolean isLabelRenderedWithUiBase() {
        return this.renderLabelWithUiBase;
    }

    /** Sets label rendered with UI base for this extended button. */
    public ExtendedButton setLabelRenderedWithUiBase(boolean renderLabelWithUiBase) {
        this.renderLabelWithUiBase = renderLabelWithUiBase;
        return this;
    }

    /** Sets is active supplier for this extended button. */
    public ExtendedButton setIsActiveSupplier(@Nullable ConsumingSupplier<ExtendedButton, Boolean> isActiveSupplier) {
        this.activeSupplier = isActiveSupplier;
        return this;
    }

    /** Returns is active supplier. */
    @Nullable
    public ConsumingSupplier<ExtendedButton, Boolean> getIsActiveSupplier() {
        return this.activeSupplier;
    }

    /** Sets visibility supplier for this extended button. */
    public ExtendedButton setVisibilitySupplier(@Nullable ConsumingSupplier<ExtendedButton, Boolean> visibilitySupplier) {
        this.visibilitySupplier = visibilitySupplier;
        return this;
    }

    /** Returns visibility supplier. */
    @Nullable
    public ConsumingSupplier<ExtendedButton, Boolean> getVisibilitySupplier() {
        return this.visibilitySupplier;
    }

    /** Returns the entry background while idle. */
    @Nullable
    public DrawableColor getBackgroundColorNormal() {
        return this.backgroundColorNormal;
    }

    /** Sets background color for this extended button. */
    public void setBackgroundColor(@Nullable DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorInactive, @Nullable DrawableColor borderColorNormal, @Nullable DrawableColor borderColorHover, @Nullable DrawableColor borderColorInactive) {
        this.backgroundColorNormal = backgroundColorNormal;
        this.backgroundColorHover = backgroundColorHover;
        this.backgroundColorInactive = backgroundColorInactive;
        this.borderColorNormal = borderColorNormal;
        this.borderColorHover = borderColorHover;
        this.borderColorInactive = borderColorInactive;
    }

    /** Sets background color normal for this extended button. */
    public void setBackgroundColorNormal(@Nullable DrawableColor backgroundColorNormal) {
        this.backgroundColorNormal = backgroundColorNormal;
    }

    /** Returns the entry background while hovered. */
    @Nullable
    public DrawableColor getBackgroundColorHover() {
        return this.backgroundColorHover;
    }

    /** Sets background color hover for this extended button. */
    public void setBackgroundColorHover(@Nullable DrawableColor backgroundColorHover) {
        this.backgroundColorHover = backgroundColorHover;
    }

    /** Returns background color inactive. */
    @Nullable
    public DrawableColor getBackgroundColorInactive() {
        return this.backgroundColorInactive;
    }

    /** Sets background color inactive for this extended button. */
    public void setBackgroundColorInactive(@Nullable DrawableColor backgroundColorInactive) {
        this.backgroundColorInactive = backgroundColorInactive;
    }

    /** Returns border color normal. */
    @Nullable
    public DrawableColor getBorderColorNormal() {
        return this.borderColorNormal;
    }

    /** Sets border color normal for this extended button. */
    public void setBorderColorNormal(@Nullable DrawableColor borderColorNormal) {
        this.borderColorNormal = borderColorNormal;
    }

    /** Returns border color hover. */
    @Nullable
    public DrawableColor getBorderColorHover() {
        return this.borderColorHover;
    }

    /** Sets border color hover for this extended button. */
    public void setBorderColorHover(@Nullable DrawableColor borderColorHover) {
        this.borderColorHover = borderColorHover;
    }

    /** Returns border color inactive. */
    @Nullable
    public DrawableColor getBorderColorInactive() {
        return this.borderColorInactive;
    }

    /** Sets border color inactive for this extended button. */
    public void setBorderColorInactive(@Nullable DrawableColor borderColorInactive) {
        this.borderColorInactive = borderColorInactive;
    }

    /** Returns whether rounded color background enabled. */
    public boolean isRoundedColorBackgroundEnabled() {
        return this.roundedColorBackground;
    }

    /** Sets rounded color background enabled for this extended button. */
    public ExtendedButton setRoundedColorBackgroundEnabled(boolean roundedColorBackground) {
        this.roundedColorBackground = roundedColorBackground;
        return this;
    }

    /** Returns background normal. */
    @Nullable
    public RenderableResource getBackgroundNormal() {
        return this.getExtendedAsCustomizableWidget().getCustomBackgroundNormalKonkrete();
    }

    /** Sets background normal for this extended button. */
    public ExtendedButton setBackgroundNormal(@Nullable RenderableResource background) {
        this.getExtendedAsCustomizableWidget().setCustomBackgroundNormalKonkrete(background);
        return this;
    }

    /** Returns background hover. */
    @Nullable
    public RenderableResource getBackgroundHover() {
        return this.getExtendedAsCustomizableWidget().getCustomBackgroundHoverKonkrete();
    }

    /** Sets background hover for this extended button. */
    public ExtendedButton setBackgroundHover(@Nullable RenderableResource background) {
        this.getExtendedAsCustomizableWidget().setCustomBackgroundHoverKonkrete(background);
        return this;
    }

    /** Returns background inactive. */
    @Nullable
    public RenderableResource getBackgroundInactive() {
        return this.getExtendedAsCustomizableWidget().getCustomBackgroundInactiveKonkrete();
    }

    /** Sets background inactive for this extended button. */
    public ExtendedButton setBackgroundInactive(@Nullable RenderableResource background) {
        this.getExtendedAsCustomizableWidget().setCustomBackgroundInactiveKonkrete(background);
        return this;
    }

    /** Returns the resource drawn while the button is active and not hovered, or {@code null}. */
    @Nullable
    public RenderableResource getIconNormal() {
        return this.iconNormal;
    }

    /** Sets icon normal for this extended button. */
    public ExtendedButton setIconNormal(@Nullable RenderableResource icon) {
        this.iconNormal = icon;
        return this;
    }

    /** Returns the resource drawn while the active button is hovered or focused, or {@code null}. */
    @Nullable
    public RenderableResource getIconHover() {
        return this.iconHover;
    }

    /** Sets icon hover for this extended button. */
    public ExtendedButton setIconHover(@Nullable RenderableResource icon) {
        this.iconHover = icon;
        return this;
    }

    /** Returns the resource drawn while the button is inactive, or {@code null}. */
    @Nullable
    public RenderableResource getIconInactive() {
        return this.iconInactive;
    }

    /** Sets icon inactive for this extended button. */
    public ExtendedButton setIconInactive(@Nullable RenderableResource icon) {
        this.iconInactive = icon;
        return this;
    }

    /** Returns press action. */
    public OnPress getPressAction() {
        return this.onPress;
    }

    /** Sets press action for this extended button. */
    public ExtendedButton setPressAction(@NotNull OnPress pressAction) {
        ((AccessorMixinButton)this).set_onPress_Konkrete(pressAction);
        return this;
    }

    /** Returns extended as customizable widget. */
    public CustomizableWidget getExtendedAsCustomizableWidget() {
        return (CustomizableWidget) this;
    }

    //This is to make the button work in FocuslessEventHandlers
    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Reports whether keyboard focus currently targets this control. */
    @Override
    public boolean isFocused() {
        if (!this.focusable) return false;
        return super.isFocused();
    }

    /** Sets focused for this extended button. */
    @Override
    public void setFocused(boolean $$0) {
        if (!this.focusable) {
            super.setFocused(false);
            return;
        }
        super.setFocused($$0);
    }

    /** Sets widget identifier for this extended button. */
    @Override
    public ExtendedButton setWidgetIdentifierKonkrete(@Nullable String identifier) {
        this.identifier = identifier;
        return this;
    }

    /** Returns the optional stable identifier used for widget lookup. */
    @Override
    public @Nullable String getWidgetIdentifierKonkrete() {
        return this.identifier;
    }

}
