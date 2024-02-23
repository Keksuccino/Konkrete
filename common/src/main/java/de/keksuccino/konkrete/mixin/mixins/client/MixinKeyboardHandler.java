package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.gui.content.handling.AdvancedWidgetsHandler;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

	@Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V", shift = At.Shift.AFTER))
	private void afterScreenKeyPress_Konkrete(long window, int keycode, int scancode, int i1, int modifiers, CallbackInfo info) {
		try {
			if (i1 == 1 || (i1 == 2)) {
				AdvancedWidgetsHandler.onScreenKeyPressed(keycode, scancode, modifiers);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Inject(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
	private void beforeCharTyped_Konkrete(long window, int character, int modifiers, CallbackInfo info) {
		try {
			AdvancedWidgetsHandler.onScreenCharTyped((char)character, modifiers);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
