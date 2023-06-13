package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.events.client.GuiInitCompletedEvent;
import de.keksuccino.konkrete.mixin.MixinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import de.keksuccino.konkrete.Konkrete;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"), method = "render")
	private void beforeRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo info) {

		MixinCache.cachedCurrentScreen = Minecraft.getInstance().screen;

		//InitCompleted ---------------
		if (MixinCache.triggerInitCompleted) {
			GuiInitCompletedEvent e2 = new GuiInitCompletedEvent(Minecraft.getInstance().screen);
			Konkrete.getEventHandler().callEventsFor(e2);
			MixinCache.triggerInitCompleted = false;
		}
		//-----------------------------

	}

}