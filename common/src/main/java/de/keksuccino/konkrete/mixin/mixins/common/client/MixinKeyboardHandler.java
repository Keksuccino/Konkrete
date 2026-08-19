package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.konkrete.util.input.InputUtils;
import de.keksuccino.konkrete.util.input.ScreenKeyEventDispatcher;
import de.keksuccino.konkrete.util.rendering.glsl.GlslRuntimeEventTracker;
import de.keksuccino.konkrete.util.window.WindowHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardHandler.class, priority = 900)
public abstract class MixinKeyboardHandler {

    /**
     * @reason Publish the key only after the exact screen call completes, including when the screen consumes it.
     *
     * This wrapper intentionally remains inside any higher-priority cancellation around {@code KeyboardHandler.keyPress}:
     * if the screen call is skipped, no screen-key notification may be emitted.
     */
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean wrap_keyPressed_in_keyPress_Konkrete(Screen screen, KeyEvent event, Operation<Boolean> operation, long windowPointer, int action) {
        return ScreenKeyEventDispatcher.dispatchAfterScreenCall(windowPointer, action, screen, event, () -> operation.call(screen, event));
    }

    /**
     * @reason Publish the release only after the exact screen call completes, preserving its handled result unchanged.
     *
     * See {@link #wrap_keyPressed_in_keyPress_Konkrete} for the intentional cancellation ordering.
     */
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean wrap_keyReleased_in_keyPress_Konkrete(Screen screen, KeyEvent event, Operation<Boolean> operation, long windowPointer, int action) {
        return ScreenKeyEventDispatcher.dispatchAfterScreenCall(windowPointer, action, screen, event, () -> operation.call(screen, event));
    }

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void before_keyPress_Konkrete(long window, int action, KeyEvent event, CallbackInfo info) {
        if (window == WindowHandler.getWindowHandle()) InputUtils.updateActiveModifiers(event.modifiers());
    }

    @Inject(method = "keyPress", at = @At("RETURN"))
    private void after_keyPress_Konkrete(long window, int action, KeyEvent event, CallbackInfo info) {
        if (window != WindowHandler.getWindowHandle()) return;
        if (action == GLFW.GLFW_RELEASE) GlslRuntimeEventTracker.onKeyReleased(event.key(), event.scancode(), event.modifiers());
        else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) GlslRuntimeEventTracker.onKeyPressed(event.key(), event.scancode(), event.modifiers(), action == GLFW.GLFW_REPEAT);
    }

    @Inject(method = "charTyped", at = @At("HEAD"))
    private void before_charTyped_Konkrete(long window, CharacterEvent event, CallbackInfo info) {
        if (window == WindowHandler.getWindowHandle()) GlslRuntimeEventTracker.onCharTyped(event.codepoint(), 0);
    }

}
