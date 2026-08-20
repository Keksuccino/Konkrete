package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.keksuccino.konkrete.mixin.support.client.widget.SliderCustomizationState;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(AbstractSliderButton.class)
public abstract class MixinAbstractSliderButton implements CustomizableSlider {

    @Shadow protected boolean canChangeValue;
    @Shadow protected double value;
    @Unique private final SliderCustomizationState sliderCustomizationState_Konkrete = new SliderCustomizationState();

    /** @reason Extended sliders override rendering, so lifecycle listeners must be installed during base construction rather than the vanilla render method. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void after_init_Konkrete(int x, int y, int width, int height, Component message, double initialValue, CallbackInfo info) {
        CustomizableWidget widget = this.asCustomizableWidget_Konkrete();
        widget.addResetCustomizationsListenerKonkrete(this.sliderCustomizationState_Konkrete::reset);
        widget.addHoverOrFocusStateListenerKonkrete(hovered -> {
            CustomizableWidget.CustomBackgroundResetBehavior behavior = widget.getCustomBackgroundResetBehaviorKonkrete();
            if (hovered && (behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER || behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER)) this.sliderCustomizationState_Konkrete.stopBackgrounds();
            if (!hovered && (behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_UNHOVER || behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER)) this.sliderCustomizationState_Konkrete.stopBackgrounds();
        });
    }

    /** @reason Replace only the vanilla slider track draw when a reusable custom track supplies a valid texture. */
    @WrapOperation(method = "extractWidgetRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", ordinal = 0))
    private void wrap_backgroundBlit_in_extractWidgetRenderState_Konkrete(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color, Operation<Void> original) {
        int previousColor = RenderingUtils.getShaderColor();
        try {
            if (this.renderSliderBackgroundKonkrete(graphics, (AbstractSliderButton) (Object) this, this.canChangeValue)) original.call(graphics, pipeline, sprite, x, y, width, height, color);
        } finally {
            RenderingUtils.setShaderColor(graphics, previousColor);
        }
    }

    /** @reason Replace only the vanilla handle draw, keeping the track and label extraction order unchanged. */
    @WrapOperation(method = "extractWidgetRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", ordinal = 1))
    private void wrap_handleBlit_in_extractWidgetRenderState_Konkrete(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color, Operation<Void> original) {
        AbstractSliderButton slider = (AbstractSliderButton) (Object) this;
        int handleX = slider.getX() + (int) (this.value * (slider.getWidth() - 8));
        int previousColor = RenderingUtils.getShaderColor();
        try {
            if (this.asCustomizableWidget_Konkrete().renderCustomBackgroundKonkrete(slider, graphics, handleX, slider.getY(), 8, slider.getHeight())) original.call(graphics, pipeline, sprite, x, y, width, height, color);
        } finally {
            RenderingUtils.setShaderColor(graphics, previousColor);
        }
    }

    @Unique
    private CustomizableWidget asCustomizableWidget_Konkrete() {
        return (CustomizableWidget) (Object) this;
    }

    @Unique
    @Override
    public void setNineSliceCustomSliderBackground_Konkrete(boolean nineSlice) { this.sliderCustomizationState_Konkrete.setNineSliceBackground(nineSlice); }
    @Unique
    @Override
    public boolean isNineSliceCustomSliderBackground_Konkrete() { return this.sliderCustomizationState_Konkrete.isNineSliceBackground(); }
    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderX_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setBackgroundBorderX(border); }
    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderX_Konkrete() { return this.sliderCustomizationState_Konkrete.getBackgroundBorderX(); }
    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderY_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setBackgroundBorderY(border); }
    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderY_Konkrete() { return this.sliderCustomizationState_Konkrete.getBackgroundBorderY(); }
    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderTop_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setBackgroundBorderTop(border); }
    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderTop_Konkrete() { return this.sliderCustomizationState_Konkrete.getBackgroundBorderTop(); }
    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderRight_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setBackgroundBorderRight(border); }
    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderRight_Konkrete() { return this.sliderCustomizationState_Konkrete.getBackgroundBorderRight(); }
    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderBottom_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setBackgroundBorderBottom(border); }
    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderBottom_Konkrete() { return this.sliderCustomizationState_Konkrete.getBackgroundBorderBottom(); }
    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderLeft_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setBackgroundBorderLeft(border); }
    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderLeft_Konkrete() { return this.sliderCustomizationState_Konkrete.getBackgroundBorderLeft(); }
    @Unique
    @Override
    public void setNineSliceCustomSliderHandle_Konkrete(boolean nineSlice) { this.sliderCustomizationState_Konkrete.setNineSliceHandle(nineSlice); }
    @Unique
    @Override
    public boolean isNineSliceCustomSliderHandle_Konkrete() { return this.sliderCustomizationState_Konkrete.isNineSliceHandle(); }
    @Unique
    @Override
    public void setNineSliceSliderHandleBorderX_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setHandleBorderX(border); }
    @Unique
    @Override
    public int getNineSliceSliderHandleBorderX_Konkrete() { return this.sliderCustomizationState_Konkrete.getHandleBorderX(); }
    @Unique
    @Override
    public void setNineSliceSliderHandleBorderY_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setHandleBorderY(border); }
    @Unique
    @Override
    public int getNineSliceSliderHandleBorderY_Konkrete() { return this.sliderCustomizationState_Konkrete.getHandleBorderY(); }
    @Unique
    @Override
    public void setNineSliceSliderHandleBorderTop_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setHandleBorderTop(border); }
    @Unique
    @Override
    public int getNineSliceSliderHandleBorderTop_Konkrete() { return this.sliderCustomizationState_Konkrete.getHandleBorderTop(); }
    @Unique
    @Override
    public void setNineSliceSliderHandleBorderRight_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setHandleBorderRight(border); }
    @Unique
    @Override
    public int getNineSliceSliderHandleBorderRight_Konkrete() { return this.sliderCustomizationState_Konkrete.getHandleBorderRight(); }
    @Unique
    @Override
    public void setNineSliceSliderHandleBorderBottom_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setHandleBorderBottom(border); }
    @Unique
    @Override
    public int getNineSliceSliderHandleBorderBottom_Konkrete() { return this.sliderCustomizationState_Konkrete.getHandleBorderBottom(); }
    @Unique
    @Override
    public void setNineSliceSliderHandleBorderLeft_Konkrete(int border) { this.sliderCustomizationState_Konkrete.setHandleBorderLeft(border); }
    @Unique
    @Override
    public int getNineSliceSliderHandleBorderLeft_Konkrete() { return this.sliderCustomizationState_Konkrete.getHandleBorderLeft(); }
    @Unique
    @Override
    public void setCustomSliderBackgroundNormalKonkrete(@Nullable RenderableResource background) { this.sliderCustomizationState_Konkrete.setBackgroundNormal(background); }
    @Unique
    @Override
    public @Nullable RenderableResource getCustomSliderBackgroundNormalKonkrete() { return this.sliderCustomizationState_Konkrete.getBackgroundNormal(); }
    @Unique
    @Override
    public void setCustomSliderBackgroundHighlightedKonkrete(@Nullable RenderableResource background) { this.sliderCustomizationState_Konkrete.setBackgroundHighlighted(background); }
    @Unique
    @Override
    public @Nullable RenderableResource getCustomSliderBackgroundHighlightedKonkrete() { return this.sliderCustomizationState_Konkrete.getBackgroundHighlighted(); }

}
