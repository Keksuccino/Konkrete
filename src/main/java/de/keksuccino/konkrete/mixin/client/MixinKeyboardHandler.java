//---
package de.keksuccino.konkrete.mixin.client;

import de.keksuccino.konkrete.events.client.ScreenCharTypedEvent;
import de.keksuccino.konkrete.events.client.ScreenKeyPressedEvent;
import de.keksuccino.konkrete.events.client.ScreenKeyReleasedEvent;
import net.minecraft.client.KeyboardListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardListener.class)
public class MixinKeyboardHandler {

    @Shadow private boolean sendRepeatsToGui;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V", shift = At.Shift.AFTER), method = "keyPress")
    private void onKeyPressHandlePress(long window, int keycode, int scancode, int i1, int modifiers, CallbackInfo info) {
        Screen.wrapScreenError(() -> {
            if (i1 == 1 || (i1 == 2 && this.sendRepeatsToGui)) {
                MinecraftForge.EVENT_BUS.post(new ScreenKeyPressedEvent(keycode, scancode, modifiers));
            }
        }, "Konkrete keyPressed (handlePress) event handler", this.getClass().getCanonicalName());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "keyPress")
    private void onKeyPressHandleRelease(long window, int keycode, int scancode, int i1, int modifiers, CallbackInfo info) {
        Screen.wrapScreenError(() -> {
            if (i1 != 1 && (i1 != 2 || !this.sendRepeatsToGui)) {
                if (i1 == 0) {
                    MinecraftForge.EVENT_BUS.post(new ScreenKeyReleasedEvent(keycode, scancode, modifiers));
                }
            }
        }, "Konkrete keyPressed (handleRelease) event handler", this.getClass().getCanonicalName());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "charTyped")
    private void onCharTyped(long window, int character, int modifiers, CallbackInfo info) {
        Screen.wrapScreenError(() -> {
            MinecraftForge.EVENT_BUS.post(new ScreenCharTypedEvent((char)character, modifiers));
        }, "Konkrete charTyped event handler", this.getClass().getCanonicalName());
    }

}
