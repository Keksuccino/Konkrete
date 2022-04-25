package de.keksuccino.konkrete.mixin.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.konkrete.mixin.MixinCache;
import net.minecraft.client.KeyboardHandler;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {

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
	
}
