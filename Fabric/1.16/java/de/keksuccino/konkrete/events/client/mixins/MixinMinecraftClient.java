package de.keksuccino.konkrete.events.client.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import de.keksuccino.konkrete.events.client.GameInitializationCompletedEvent;
import de.keksuccino.konkrete.events.client.GuiOpenEvent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;

@Mixin(value = MinecraftClient.class)
public class MixinMinecraftClient {

	private static boolean gameInitDone = false;
	
	//ClientTickEvent.Pre
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V"), method = "tick", cancellable = false)
	public void onClientTickPre(CallbackInfo info) {
		ClientTickEvent.Pre e = new ClientTickEvent.Pre();
		Konkrete.getEventHandler().callEventsFor(e);
	}

	//ClientTickEvent.Post
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V"), method = "tick", cancellable = false)
	public void onClientTickPost(CallbackInfo info) {
		ClientTickEvent.Post e = new ClientTickEvent.Post();
		Konkrete.getEventHandler().callEventsFor(e);
	}
	
	//This is trash, but I don't think I have many alternatives atm
	@Inject(at = @At("HEAD"), method = "setOverlay")
	private void onReloadResources(Overlay overlay, CallbackInfo info) {
		if (!gameInitDone) {
			gameInitDone = true;
			GameInitializationCompletedEvent e = new GameInitializationCompletedEvent(MinecraftClient.getInstance(), FabricLoader.getInstance());
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
	@Inject(at = @At("HEAD"), method = "openScreen")
	private void onOpenScreen(Screen screen, CallbackInfo info) {
		GuiOpenEvent e = new GuiOpenEvent(screen);
		Konkrete.getEventHandler().callEventsFor(e);
		if (e.isCanceled()) {
			info.cancel();
		}
	}
	

}
