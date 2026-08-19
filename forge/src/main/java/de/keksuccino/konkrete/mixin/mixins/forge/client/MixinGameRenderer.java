package de.keksuccino.konkrete.mixin.mixins.forge.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.gui.content.AdvancedButtonHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", remap = false))
    private void wrapRenderScreen_Konkrete(Screen instance, PoseStack poseStack, int mouseX, int mouseY, float partial, Operation<Void> original) {
        original.call(instance, poseStack, mouseX, mouseY, partial);
        AdvancedButtonHandler.onDrawScreen(poseStack, mouseX, mouseY);
    }

}
