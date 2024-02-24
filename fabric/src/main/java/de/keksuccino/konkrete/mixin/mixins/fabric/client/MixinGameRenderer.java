package de.keksuccino.konkrete.mixin.mixins.fabric.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.konkrete.gui.content.AdvancedButtonHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void wrapRenderScreen_Konkrete(Screen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
        original.call(instance, graphics, mouseX, mouseY, partial);
        AdvancedButtonHandler.onDrawScreen(graphics, mouseX, mouseY);
    }

}
