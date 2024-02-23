package de.keksuccino.konkrete.mixin.client;

import de.keksuccino.konkrete.events.EventHooks;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V", shift = At.Shift.AFTER), method = "keyPress")
    private void onKeyPressHandlePress(long window, int keycode, int scancode, int i1, int modifiers, CallbackInfo info) {
        Screen.wrapScreenError(() -> {
            if (i1 == 1 || (i1 == 2)) {
                EventHooks.onScreenKeyPressed(keycode, scancode, modifiers);
            }
        }, "Konkrete keyPressed (handlePress) event handler", this.getClass().getCanonicalName());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "keyPress")
    private void onKeyPressHandleRelease(long window, int keycode, int scancode, int i1, int modifiers, CallbackInfo info) {
        Screen.wrapScreenError(() -> {
            if (i1 != 1 && (i1 != 2)) {
                if (i1 == 0) {
                    EventHooks.onScreenKeyReleased(keycode, scancode, modifiers);
                }
            }
        }, "Konkrete keyPressed (handleRelease) event handler", this.getClass().getCanonicalName());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "charTyped")
    private void onCharTyped(long window, int character, int modifiers, CallbackInfo info) {
        Screen.wrapScreenError(() -> EventHooks.onScreenCharTyped((char)character, modifiers), "Konkrete charTyped event handler", this.getClass().getCanonicalName());
    }

}
