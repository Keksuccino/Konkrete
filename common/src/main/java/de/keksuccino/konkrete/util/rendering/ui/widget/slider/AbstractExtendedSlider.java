package de.keksuccino.konkrete.util.rendering.ui.widget.slider;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinAbstractSliderButton;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.VanillaEvents;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.IExtendedWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.UIWidget;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

/** Base implementation for abstract extended slider. */
@SuppressWarnings("unused")
public abstract class AbstractExtendedSlider extends AbstractSliderButton implements IExtendedWidget, NavigatableWidget, UIWidget {

    /** Vanilla slider-track sprite. */
    public static final Identifier SLIDER_SPRITE = Identifier.parse("widget/slider");
    /** Vanilla focused slider-track sprite. */
    public static final Identifier HIGHLIGHTED_SPRITE = Identifier.parse("widget/slider_highlighted");
    /** Vanilla slider-handle sprite. */
    public static final Identifier SLIDER_HANDLE_SPRITE = Identifier.parse("widget/slider_handle");
    /** Vanilla focused slider-handle sprite. */
    public static final Identifier SLIDER_HANDLE_HIGHLIGHTED_SPRITE = Identifier.parse("widget/slider_handle_highlighted");

    /** Optional track fill while idle. */
    @Nullable
    protected DrawableColor sliderBackgroundColorNormal;
    /** Optional track fill while hovered or focused. */
    @Nullable
    protected DrawableColor sliderBackgroundColorHighlighted;
    /** Optional track border while idle. */
    @Nullable
    protected DrawableColor sliderBorderColorNormal;
    /** Optional track border while hovered or focused. */
    @Nullable
    protected DrawableColor sliderBorderColorHighlighted;
    /** Optional handle fill while active and idle. */
    @Nullable
    protected DrawableColor sliderHandleColorNormal;
    /** Optional handle fill while hovered or focused. */
    @Nullable
    protected DrawableColor sliderHandleColorHover;
    /** Optional handle fill while inactive. */
    @Nullable
    protected DrawableColor sliderHandleColorInactive;
    /** Label color while the slider is active. */
    @NotNull
    protected DrawableColor labelColorNormal = DrawableColor.of(new Color(16777215));
    /** Label color while the slider is inactive. */
    @NotNull
    protected DrawableColor labelColorInactive = DrawableColor.of(new Color(10526880));
    /** Whether the control label is drawn with a shadow. */
    protected boolean labelShadow = true;
    /** Whether labels use UIBase text metrics and rendering. */
    protected boolean renderLabelWithUiBase = false;
    /** Optional listener notified after the slider value changes. */
    @Nullable
    protected SliderValueUpdateListener sliderValueUpdateListener;
    /** Computes the label from the slider's current value. */
    @NotNull
    protected ConsumingSupplier<AbstractExtendedSlider, Component> labelSupplier = slider -> Component.literal(slider.getValueDisplayText());
    /** Whether this control may receive focus. */
    protected boolean focusable = true;
    /** Whether keyboard navigation may target this control. */
    protected boolean navigatable = true;
    /** Whether to draw the configurable background with rounded corners. */
    protected boolean roundedColorBackground = false;
    /** Computes whether the control is active. */
    @Nullable
    protected ConsumingSupplier<AbstractExtendedSlider, Boolean> isActiveSupplier = null;
    /** Whether the primary mouse button is currently held. */
    protected boolean leftMouseDown = false;

    /** Initializes a slider with GUI bounds, a label, and a normalized value. */
    public AbstractExtendedSlider(int x, int y, int width, int height, Component label, double value) {
        super(x, y, width, height, label, value);
    }

    /** Returns sprite. */
    public Identifier getSprite() {
        return this.isFocused() && !((AccessorMixinAbstractSliderButton)this).get_canChangeValue_Konkrete() ? HIGHLIGHTED_SPRITE : SLIDER_SPRITE;
    }

    /** Returns handle sprite. */
    public Identifier getHandleSprite() {
        return !this.isHovered && !((AccessorMixinAbstractSliderButton)this).get_canChangeValue_Konkrete() ? SLIDER_HANDLE_SPRITE : SLIDER_HANDLE_HIGHLIGHTED_SPRITE;
    }

    /** Renders slider widget into the active GUI extraction pass. */
    protected void renderSliderWidget(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.extractBackground(graphics, mouseX, mouseY, partial);
        RenderingUtils.resetShaderColor(graphics);

        this.renderHandle(graphics, mouseX, mouseY, partial);
        RenderingUtils.resetShaderColor(graphics);

        this.renderLabel(graphics, mouseX, mouseY, partial);
        RenderingUtils.resetShaderColor(graphics);

    }

    /** Adds this widget's draw state to the active GUI extraction pass. */
    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (this.isActiveSupplier != null) this.active = this.isActiveSupplier.get(this);
        this.renderSliderWidget(graphics, mouseX, mouseY, partial);
        this.handleCursor(graphics);
    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    protected void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        boolean renderVanilla = this.renderColorBackground(graphics, mouseX, mouseY, partial);
        if (renderVanilla) renderVanilla = this.getAsCustomizableSlider().renderSliderBackgroundKonkrete(graphics, this, this.getAccessor().get_canChangeValue_Konkrete());
        if (renderVanilla) this.renderVanillaBackground(graphics, mouseX, mouseY, partial);
    }

    /**
     * Returns if the slider should render its Vanilla background (true) or not (false).
     */
    protected boolean renderColorBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        RenderingUtils.resetShaderColor(graphics);
        if ((this.isFocused() && !this.getAccessor().get_canChangeValue_Konkrete()) && (this.sliderBackgroundColorHighlighted != null)) {
            if (this.roundedColorBackground) {
                float radius = UIBase.getWidgetCornerRoundingRadius();
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight(),
                        radius,
                        radius,
                        radius,
                        radius,
                        this.sliderBackgroundColorHighlighted.getColorInt(),
                        partial
                );
            } else {
                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.sliderBackgroundColorHighlighted.getColorInt());
            }
            if (this.sliderBorderColorHighlighted != null) {
                if (this.roundedColorBackground) {
                    float radius = UIBase.getWidgetCornerRoundingRadius();
                    float borderThickness = 1.0F;
                    float borderRadius = radius > 0.0F ? radius + borderThickness : 0.0F;
                    SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(
                            graphics,
                            this.getX(),
                            this.getY(),
                            this.getWidth(),
                            this.getHeight(),
                            borderThickness,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            this.sliderBorderColorHighlighted.getColorInt(),
                            partial
                    );
                } else {
                    UIBase.renderBorder(graphics, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 1, this.sliderBorderColorHighlighted.getColorInt(), true, true, true, true);
                }
            }
            return false;
        } else if (this.sliderBackgroundColorNormal != null) {
            if (this.roundedColorBackground) {
                float radius = UIBase.getWidgetCornerRoundingRadius();
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight(),
                        radius,
                        radius,
                        radius,
                        radius,
                        this.sliderBackgroundColorNormal.getColorInt(),
                        partial
                );
            } else {
                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.sliderBackgroundColorNormal.getColorInt());
            }
            if (this.sliderBorderColorNormal != null) {
                if (this.roundedColorBackground) {
                    float radius = UIBase.getWidgetCornerRoundingRadius();
                    float borderThickness = 1.0F;
                    float borderRadius = radius > 0.0F ? radius + borderThickness : 0.0F;
                    SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(
                            graphics,
                            this.getX(),
                            this.getY(),
                            this.getWidth(),
                            this.getHeight(),
                            borderThickness,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            this.sliderBorderColorNormal.getColorInt(),
                            partial
                    );
                } else {
                    UIBase.renderBorder(graphics, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 1, this.sliderBorderColorNormal.getColorInt(), true, true, true, true);
                }
            }
            return false;
        }
        return true;
    }

    /** Renders vanilla background into the active GUI extraction pass. */
    protected void renderVanillaBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        de.keksuccino.konkrete.util.rendering.RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.alpha);
        de.keksuccino.konkrete.util.rendering.RenderingUtils.defaultBlendFunc();
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.getSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        RenderingUtils.resetShaderColor(graphics);
    }

    /** Renders handle into the active GUI extraction pass. */
    protected void renderHandle(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        boolean renderVanilla = this.renderColorHandle(graphics, mouseX, mouseY, partial);
        if (renderVanilla) renderVanilla = this.getAsCustomizableWidget().renderCustomBackgroundKonkrete(this, graphics, this.getHandleX(), this.getY(), this.getHandleWidth(), this.getHeight());
        if (renderVanilla) this.renderVanillaHandle(graphics, mouseX, mouseY, partial);
    }

    /**
     * Returns if the slider should render its Vanilla handle (true) or not (false).
     */
    protected boolean renderColorHandle(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        int handleX = this.getHandleX();
        int handleWidth = this.getHandleWidth();
        if (this.active) {
            if (this.isHoveredOrFocused()) {
                if (this.sliderHandleColorHover != null) {
                    if (this.roundedColorBackground) {
                        float radius = UIBase.getWidgetCornerRoundingRadius();
                        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                                graphics,
                                handleX,
                                this.getY(),
                                handleWidth,
                                this.getHeight(),
                                radius,
                                radius,
                                radius,
                                radius,
                                this.sliderHandleColorHover.getColorInt(),
                                partial
                        );
                    } else {
                        graphics.fill(handleX, this.getY(), handleX + handleWidth, this.getY() + this.getHeight(), this.sliderHandleColorHover.getColorInt());
                    }
                    return false;
                }
            } else {
                if (this.sliderHandleColorNormal != null) {
                    if (this.roundedColorBackground) {
                        float radius = UIBase.getWidgetCornerRoundingRadius();
                        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                                graphics,
                                handleX,
                                this.getY(),
                                handleWidth,
                                this.getHeight(),
                                radius,
                                radius,
                                radius,
                                radius,
                                this.sliderHandleColorNormal.getColorInt(),
                                partial
                        );
                    } else {
                        graphics.fill(handleX, this.getY(), handleX + handleWidth, this.getY() + this.getHeight(), this.sliderHandleColorNormal.getColorInt());
                    }
                    return false;
                }
            }
        } else {
            if (this.sliderHandleColorInactive != null) {
                if (this.roundedColorBackground) {
                    float radius = UIBase.getWidgetCornerRoundingRadius();
                    SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                            graphics,
                            handleX,
                            this.getY(),
                            handleWidth,
                            this.getHeight(),
                            radius,
                            radius,
                            radius,
                            radius,
                            this.sliderHandleColorInactive.getColorInt(),
                            partial
                    );
                } else {
                    graphics.fill(handleX, this.getY(), handleX + handleWidth, this.getY() + this.getHeight(), this.sliderHandleColorInactive.getColorInt());
                }
                return false;
            }
        }
        return true;
    }

    /** Renders vanilla handle into the active GUI extraction pass. */
    protected void renderVanillaHandle(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        de.keksuccino.konkrete.util.rendering.RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.alpha);
        de.keksuccino.konkrete.util.rendering.RenderingUtils.defaultBlendFunc();
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.getHandleSprite(), this.getHandleX(), this.getY(), this.getHandleWidth(), this.getHeight());
        RenderingUtils.resetShaderColor(graphics);
    }

    /** Renders label into the active GUI extraction pass. */
    protected void renderLabel(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        int textColor = this.active ? this.labelColorNormal.getColorInt() : this.labelColorInactive.getColorInt();
        int finalTextColor = RenderingUtils.replaceAlphaInColor(textColor, this.alpha);
        boolean labelShadowFinal = this.labelShadow;
        if (this.renderLabelWithUiBase) {
            this.renderScrollingLabelUiBase(this, graphics, 2, finalTextColor);
        } else {
            this.renderScrollingLabel(this, graphics, Minecraft.getInstance().font, 2, labelShadowFinal, finalTextColor);
        }
    }

    /** Returns handle x. */
    public int getHandleX() {
        return this.getX() + (int)(this.value * (double)(this.getWidth() - this.getHandleWidth()));
    }

    /** Returns handle width. */
    public int getHandleWidth() {
        return 8;
    }

    /** Refreshes message from current state. */
    @Override
    public void updateMessage() {
        Component label = this.labelSupplier.get(this);
        if (label == null) label = Component.empty();
        this.setMessage(label);
    }

    /** Applies value to the supplied target. */
    @Override
    protected void applyValue() {
        if (this.sliderValueUpdateListener != null) {
            this.sliderValueUpdateListener.update(this, this.getValueDisplayText(), this.value);
        }
    }

    /** Formats the current model value for the slider label and update listener. */
    @NotNull
    public abstract String getValueDisplayText();

    /** Sets slider value update listener for this abstract extended slider. */
    public AbstractExtendedSlider setSliderValueUpdateListener(@Nullable SliderValueUpdateListener listener) {
        this.sliderValueUpdateListener = listener;
        this.updateMessage();
        return this;
    }

    /** Sets value for this abstract extended slider. */
    public void setValue(double value) {
        double d0 = this.value;
        this.value = Mth.clamp(value, 0.0D, 1.0D);
        if (d0 != this.value) {
            this.applyValue();
        }
        this.updateMessage();
    }

    /** Returns the normalized slider position in the inclusive range {@code [0, 1]}. */
    public double getValue() {
        return this.value;
    }

    /** Sets is active supplier for this abstract extended slider. */
    public AbstractExtendedSlider setIsActiveSupplier(@Nullable ConsumingSupplier<AbstractExtendedSlider, Boolean> supplier) {
        this.isActiveSupplier = supplier;
        return this;
    }

    /** Returns handle texture normal. */
    @Nullable
    public RenderableResource getHandleTextureNormal() {
        return this.getAsCustomizableWidget().getCustomBackgroundNormalKonkrete();
    }

    /** Sets handle texture normal for this abstract extended slider. */
    public AbstractExtendedSlider setHandleTextureNormal(@Nullable RenderableResource texture) {
        this.getAsCustomizableWidget().setCustomBackgroundNormalKonkrete(texture);
        return this;
    }

    /** Returns handle texture hover. */
    @Nullable
    public RenderableResource getHandleTextureHover() {
        return this.getAsCustomizableWidget().getCustomBackgroundHoverKonkrete();
    }

    /** Sets handle texture hover for this abstract extended slider. */
    public AbstractExtendedSlider setHandleTextureHover(@Nullable RenderableResource texture) {
        this.getAsCustomizableWidget().setCustomBackgroundHoverKonkrete(texture);
        return this;
    }

    /** Returns handle texture inactive. */
    @Nullable
    public RenderableResource getHandleTextureInactive() {
        return this.getAsCustomizableWidget().getCustomBackgroundInactiveKonkrete();
    }

    /** Sets handle texture inactive for this abstract extended slider. */
    public AbstractExtendedSlider setHandleTextureInactive(@Nullable RenderableResource texture) {
        this.getAsCustomizableWidget().setCustomBackgroundInactiveKonkrete(texture);
        return this;
    }

    /** Returns background texture normal. */
    @Nullable
    public RenderableResource getBackgroundTextureNormal() {
        return this.getAsCustomizableSlider().getCustomSliderBackgroundNormalKonkrete();
    }

    /** Sets background texture normal for this abstract extended slider. */
    public AbstractExtendedSlider setBackgroundTextureNormal(@Nullable RenderableResource texture) {
        this.getAsCustomizableSlider().setCustomSliderBackgroundNormalKonkrete(texture);
        return this;
    }

    /** Returns background texture highlighted. */
    @Nullable
    public RenderableResource getBackgroundTextureHighlighted() {
        return this.getAsCustomizableSlider().getCustomSliderBackgroundHighlightedKonkrete();
    }

    /** Sets background texture highlighted for this abstract extended slider. */
    public AbstractExtendedSlider setBackgroundTextureHighlighted(@Nullable RenderableResource texture) {
        this.getAsCustomizableSlider().setCustomSliderBackgroundHighlightedKonkrete(texture);
        return this;
    }

    /** Returns slider background color normal. */
    @Nullable
    public DrawableColor getSliderBackgroundColorNormal() {
        return sliderBackgroundColorNormal;
    }

    /** Sets slider background color normal for this abstract extended slider. */
    public AbstractExtendedSlider setSliderBackgroundColorNormal(@Nullable DrawableColor sliderBackgroundColorNormal) {
        this.sliderBackgroundColorNormal = sliderBackgroundColorNormal;
        return this;
    }

    /** Returns slider background color highlighted. */
    @Nullable
    public DrawableColor getSliderBackgroundColorHighlighted() {
        return sliderBackgroundColorHighlighted;
    }

    /** Sets slider background color highlighted for this abstract extended slider. */
    public AbstractExtendedSlider setSliderBackgroundColorHighlighted(@Nullable DrawableColor sliderBackgroundColorHighlighted) {
        this.sliderBackgroundColorHighlighted = sliderBackgroundColorHighlighted;
        return this;
    }

    /** Returns slider border color normal. */
    @Nullable
    public DrawableColor getSliderBorderColorNormal() {
        return sliderBorderColorNormal;
    }

    /** Sets slider border color normal for this abstract extended slider. */
    public AbstractExtendedSlider setSliderBorderColorNormal(@Nullable DrawableColor sliderBorderColorNormal) {
        this.sliderBorderColorNormal = sliderBorderColorNormal;
        return this;
    }

    /** Returns slider border color highlighted. */
    @Nullable
    public DrawableColor getSliderBorderColorHighlighted() {
        return sliderBorderColorHighlighted;
    }

    /** Sets slider border color highlighted for this abstract extended slider. */
    public AbstractExtendedSlider setSliderBorderColorHighlighted(@Nullable DrawableColor sliderBorderColorHighlighted) {
        this.sliderBorderColorHighlighted = sliderBorderColorHighlighted;
        return this;
    }

    /** Returns slider handle color normal. */
    @Nullable
    public DrawableColor getSliderHandleColorNormal() {
        return sliderHandleColorNormal;
    }

    /** Sets slider handle color normal for this abstract extended slider. */
    public AbstractExtendedSlider setSliderHandleColorNormal(@Nullable DrawableColor sliderHandleColorNormal) {
        this.sliderHandleColorNormal = sliderHandleColorNormal;
        return this;
    }

    /** Returns slider handle color hover. */
    @Nullable
    public DrawableColor getSliderHandleColorHover() {
        return sliderHandleColorHover;
    }

    /** Sets slider handle color hover for this abstract extended slider. */
    public AbstractExtendedSlider setSliderHandleColorHover(@Nullable DrawableColor sliderHandleColorHover) {
        this.sliderHandleColorHover = sliderHandleColorHover;
        return this;
    }

    /** Returns slider handle color inactive. */
    @Nullable
    public DrawableColor getSliderHandleColorInactive() {
        return sliderHandleColorInactive;
    }

    /** Sets slider handle color inactive for this abstract extended slider. */
    public AbstractExtendedSlider setSliderHandleColorInactive(@Nullable DrawableColor sliderHandleColorInactive) {
        this.sliderHandleColorInactive = sliderHandleColorInactive;
        return this;
    }

    /** Returns label color normal. */
    @NotNull
    public DrawableColor getLabelColorNormal() {
        return this.labelColorNormal;
    }

    /** Sets label color normal for this abstract extended slider. */
    public AbstractExtendedSlider setLabelColorNormal(@NotNull DrawableColor labelColorNormal) {
        this.labelColorNormal = labelColorNormal;
        return this;
    }

    /** Returns label color inactive. */
    @NotNull
    public DrawableColor getLabelColorInactive() {
        return this.labelColorInactive;
    }

    /** Sets label color inactive for this abstract extended slider. */
    public AbstractExtendedSlider setLabelColorInactive(@NotNull DrawableColor labelColorInactive) {
        this.labelColorInactive = labelColorInactive;
        return this;
    }

    /** Reports whether the control label is drawn with a shadow. */
    public boolean isLabelShadow() {
        return labelShadow;
    }

    /** Sets label shadow for this abstract extended slider. */
    public AbstractExtendedSlider setLabelShadow(boolean labelShadow) {
        this.labelShadow = labelShadow;
        return this;
    }

    /** Returns whether label rendered with UI base. */
    public boolean isLabelRenderedWithUiBase() {
        return this.renderLabelWithUiBase;
    }

    /** Sets label rendered with UI base for this abstract extended slider. */
    public AbstractExtendedSlider setLabelRenderedWithUiBase(boolean renderLabelWithUiBase) {
        this.renderLabelWithUiBase = renderLabelWithUiBase;
        return this;
    }

    /** Returns whether rounded color background enabled. */
    public boolean isRoundedColorBackgroundEnabled() {
        return this.roundedColorBackground;
    }

    /** Sets rounded color background enabled for this abstract extended slider. */
    public AbstractExtendedSlider setRoundedColorBackgroundEnabled(boolean roundedColorBackground) {
        this.roundedColorBackground = roundedColorBackground;
        return this;
    }

    /** Returns label supplier. */
    @NotNull
    public ConsumingSupplier<AbstractExtendedSlider, Component> getLabelSupplier() {
        return this.labelSupplier;
    }

    /** Sets label supplier for this abstract extended slider. */
    public AbstractExtendedSlider setLabelSupplier(@NotNull ConsumingSupplier<AbstractExtendedSlider, Component> labelSupplier) {
        this.labelSupplier = labelSupplier;
        return this;
    }

    /** Returns accessor. */
    public AccessorMixinAbstractSliderButton getAccessor() {
        return (AccessorMixinAbstractSliderButton) this;
    }

    /** Returns as customizable slider. */
    public CustomizableSlider getAsCustomizableSlider() {
        return (CustomizableSlider) this;
    }

    /** Returns as customizable widget. */
    public CustomizableWidget getAsCustomizableWidget() {
        return (CustomizableWidget) this;
    }

    /** Reports whether this control may receive keyboard focus. */
    @Override
    public boolean isFocusable() {
        return focusable;
    }

    /** Sets focusable for this abstract extended slider. */
    @Override
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    /** Reports whether keyboard navigation may target this control. */
    @Override
    public boolean isNavigatable() {
        return navigatable;
    }

    /** Sets navigatable for this abstract extended slider. */
    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (!this.canClick()) return false;
        boolean handled = super.mouseClicked(event, isDoubleClick);
        if (event.button() == 0) this.leftMouseDown = handled;
        return handled;
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.mouseClicked(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button, 0), false);
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean wasLeftMouseDown = this.leftMouseDown;
        this.leftMouseDown = false;
        if (!wasLeftMouseDown || (event.button() != 0)) return false;
        return super.mouseReleased(event);
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.mouseReleased(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button, 0));
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!this.leftMouseDown) return false;
        return super.mouseDragged(event, dragX, dragY);
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.mouseDragged(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button, 0), dragX, dragY);
    }

    /** Returns whether click. */
    protected boolean canClick() {
        return (this.isHovered() && this.isActive() && this.visible);
    }

    /** Receives slider value update lifecycle notifications. */
    @FunctionalInterface
    public interface SliderValueUpdateListener {

        /** Receives the slider's display text and numeric value after an update. */
        void update(@NotNull AbstractExtendedSlider slider, @NotNull String valueDisplayText, double value);

    }

}
