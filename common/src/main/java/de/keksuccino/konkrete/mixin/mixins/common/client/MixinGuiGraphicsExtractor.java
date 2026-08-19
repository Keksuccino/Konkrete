package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.util.rendering.RenderRotationUtil;
import de.keksuccino.konkrete.util.rendering.RenderScaleUtil;
import de.keksuccino.konkrete.util.rendering.RenderTranslationUtil;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public class MixinGuiGraphicsExtractor {

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;II)V", at = @At("TAIL"))
    private void after_init_Konkrete(Minecraft minecraft, GuiRenderState renderState, int mouseX, int mouseY, CallbackInfo info) {
        RenderScaleUtil.resetActiveRenderScale_Konkrete();
        RenderTranslationUtil.resetActiveRenderTranslation_Konkrete();
        RenderRotationUtil.resetActiveRenderRotation_Konkrete();
    }

    @Inject(method = "setTooltipForNextFrameInternal", at = @At("HEAD"), cancellable = true)
    private void before_setTooltipForNextFrameInternal_Konkrete(Font font, List<ClientTooltipComponent> lines, int x, int y, ClientTooltipPositioner positioner, @Nullable Identifier style, boolean replaceExisting, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    @Inject(method = "tooltip", at = @At("HEAD"), cancellable = true)
    private void before_tooltip_Konkrete(Font font, List<ClientTooltipComponent> lines, int x, int y, ClientTooltipPositioner positioner, @Nullable Identifier style, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    @ModifyArg(method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;)V"), index = 11)
    private int wrap_innerBlitColor_Konkrete(int color) {
        return RenderingUtils.applyShaderColor(color);
    }

    @ModifyArg(method = "innerTiledBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIIIFFFFI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/TiledBlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;)V"), index = 13)
    private int wrap_innerTiledBlitColor_Konkrete(int color) {
        return RenderingUtils.applyShaderColor(color);
    }

}
