package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.events.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.konkrete.Konkrete;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	private static boolean gameInitDone = false;
	
	private boolean cancelGuiOpen = false;
	
	//ClientTickEvent.Pre
	@Inject(at = @At(value = "HEAD"), method = "tick")
	public void onClientTickPre(CallbackInfo info) {
		ClientTickEvent.Pre e = new ClientTickEvent.Pre();
		Konkrete.getEventHandler().callEventsFor(e);
	}

	//ClientTickEvent.Post
	@Inject(at = @At(value = "TAIL"), method = "tick")
	public void onClientTickPost(CallbackInfo info) {
		ClientTickEvent.Post e = new ClientTickEvent.Post();
		Konkrete.getEventHandler().callEventsFor(e);
	}
	
	//This is trash, but I don't think I have many alternatives atm
	@Inject(at = @At("HEAD"), method = "setOverlay")
	private void onReloadResources(Overlay overlay, CallbackInfo info) {
		if (!gameInitDone) {
			gameInitDone = true;
			GameInitializationCompletedEvent e = new GameInitializationCompletedEvent(Minecraft.getInstance(), FabricLoader.getInstance());
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
	@ModifyVariable(at = @At("HEAD"), method = "setScreen", argsOnly = true, index = 1)
	private Screen onOpenScreen(Screen s) {
		GuiOpenEvent e = new GuiOpenEvent(s);
		Konkrete.getEventHandler().callEventsFor(e);
		this.cancelGuiOpen = e.isCanceled();
		return e.getGui();
	}
	
	@Inject(at = @At(value = "HEAD"), method = "setScreen", cancellable = true)
	private void onOpenScreenCancel(Screen screen, CallbackInfo info) {
		if (this.cancelGuiOpen) {
			info.cancel();
		}
		this.cancelGuiOpen = false;
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V", shift = At.Shift.AFTER))
	private void onOpenScreenPost(Screen screen, CallbackInfo info) {
		GuiOpenedEvent e = new GuiOpenedEvent(screen);
		Konkrete.getEventHandler().callEventsFor(e);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "tick")
	private void onScreenTickInTick(CallbackInfo info) {
		Screen.wrapScreenError(() -> {
			Konkrete.getEventHandler().callEventsFor(new ScreenTickEvent());
		}, "Konkrete Ticking screen", this.getClass().getCanonicalName());
	}
	
}