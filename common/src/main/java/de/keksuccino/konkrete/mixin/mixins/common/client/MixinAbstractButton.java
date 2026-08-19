package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractButton.class)
public class MixinAbstractButton {

    /** @reason Route ordinary vanilla button backgrounds through the reusable per-widget background state. */
    @WrapOperation(method = "extractDefaultSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    private void wrap_blitSprite_in_extractDefaultSprite_Konkrete(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color, Operation<Void> original) {
        AbstractButton button = (AbstractButton) (Object) this;
        int previousColor = RenderingUtils.getShaderColor();
        try {
            if (((CustomizableWidget) (Object) this).renderCustomBackgroundKonkrete(button, graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight())) original.call(graphics, pipeline, sprite, x, y, width, height, color);
        } finally {
            RenderingUtils.setShaderColor(graphics, previousColor);
        }
    }
}
