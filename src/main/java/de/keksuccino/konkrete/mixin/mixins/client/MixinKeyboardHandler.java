package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.ScreenCharTypedEvent;
import de.keksuccino.konkrete.events.client.ScreenKeyPressedEvent;
import de.keksuccino.konkrete.events.client.ScreenKeyReleasedEvent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.konkrete.mixin.MixinCache;
import net.minecraft.client.KeyboardHandler;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

	@Shadow private boolean sendRepeatsToGui;

	/**
	 * Caching args of onKey() method to use it in {@link MixinScreen}.
	 */
	@Inject(at = @At(value = "HEAD"), method = "keyPress")
	private void onCachingKeyArgs(long window, int key, int scancode, int i, int j, CallbackInfo ci) {
		MixinCache.currentKeyboardKey = key;
		MixinCache.currentKeyboardScancode = scancode;
		MixinCache.currentKeyboardAction = i;
		MixinCache.currentKeyboardModifiers = j;
	}
	
	/**
	 * Caching args of onChar() method to use it in {@link MixinScreen}.
	 */
	@Inject(at = @At(value = "HEAD"), method = "charTyped")
	private void onCachingCharArgs(long window, int i, int j, CallbackInfo ci) {
		MixinCache.currentKeyboardChar = i;
		MixinCache.currentKeyboardCharModifiers = j;
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V", shift = At.Shift.AFTER), method = "keyPress")
	private void onKeyPressHandlePress(long window, int keycode, int scancode, int i1, int modifiers, CallbackInfo info) {
		Screen.wrapScreenError(() -> {
			if (i1 == 1 || (i1 == 2 && this.sendRepeatsToGui)) {
				Konkrete.getEventHandler().callEventsFor(new ScreenKeyPressedEvent(keycode, scancode, modifiers));
			}
		}, "Konkrete keyPressed (handlePress) event handler", this.getClass().getCanonicalName());
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "keyPress")
	private void onKeyPressHandleRelease(long window, int keycode, int scancode, int i1, int modifiers, CallbackInfo info) {
		Screen.wrapScreenError(() -> {
			if (i1 != 1 && (i1 != 2 || !this.sendRepeatsToGui)) {
				if (i1 == 0) {
					Konkrete.getEventHandler().callEventsFor(new ScreenKeyReleasedEvent(keycode, scancode, modifiers));
				}
			}
		}, "Konkrete keyPressed (handleRelease) event handler", this.getClass().getCanonicalName());
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "charTyped")
	private void onCharTyped(long window, int character, int modifiers, CallbackInfo info) {
		Screen.wrapScreenError(() -> {
			Konkrete.getEventHandler().callEventsFor(new ScreenCharTypedEvent((char)character, modifiers));
		}, "Konkrete charTyped event handler", this.getClass().getCanonicalName());
	}
	
}
