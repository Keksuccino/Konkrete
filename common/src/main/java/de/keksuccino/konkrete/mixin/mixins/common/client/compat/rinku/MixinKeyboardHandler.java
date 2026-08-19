package de.keksuccino.konkrete.mixin.mixins.common.client.compat.rinku;

import de.keksuccino.konkrete.mixin.support.client.rinku.RinkuKeyboardInputBridge;
import de.keksuccino.konkrete.util.window.WindowHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    /** @reason Browser controls must receive a key before the containing Screen can consume it. */
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"), cancellable = true)
    private void before_keyPressed_Konkrete(long window, int action, KeyEvent event, CallbackInfo info) {
        routeKey_Konkrete(window, action, event, info);
    }

    /** @reason Release routing uses a separate vanilla branch and must preserve the same browser-first ownership. */
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"), cancellable = true)
    private void before_keyReleased_Konkrete(long window, int action, KeyEvent event, CallbackInfo info) {
        routeKey_Konkrete(window, action, event, info);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void before_charTyped_Konkrete(long window, CharacterEvent event, CallbackInfo info) {
        if (window != WindowHandler.getWindowHandle()) return;
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen != null && RinkuKeyboardInputBridge.routeCharacter(screen, event)) info.cancel();
    }

    @Unique
    private static void routeKey_Konkrete(long window, int action, KeyEvent event, CallbackInfo info) {
        if (window != WindowHandler.getWindowHandle()) return;
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen != null && RinkuKeyboardInputBridge.routeKey(screen, action, event)) info.cancel();
    }

}
