package de.keksuccino.konkrete.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;

@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
	private void wrapRenderScreenFancyMenu(Screen instance, PoseStack pose, int mouseX, int mouseY, float partial, Operation<Void> original) {
		Konkrete.getEventHandler().callEventsFor(new DrawScreenEvent.Pre(instance, pose, mouseX, mouseY, partial));
		original.call(instance, pose, mouseX, mouseY, partial);
		Konkrete.getEventHandler().callEventsFor(new DrawScreenEvent.Post(instance, pose, mouseX, mouseY, partial));
	}

}