package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.util.rendering.ui.cursor.GlfwCursorTracker;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GLFW.class, remap = false)
public class MixinGLFW {

    @Inject(method = "glfwSetCursor", at = @At("RETURN"))
    private static void after_glfwSetCursor_Konkrete(long window, long cursor, CallbackInfo info) {
        GlfwCursorTracker.onGlfwSetCursor(window, cursor);
    }

    @Inject(method = "glfwCreateStandardCursor", at = @At("RETURN"))
    private static void after_glfwCreateStandardCursor_Konkrete(int shape, CallbackInfoReturnable<Long> info) {
        GlfwCursorTracker.onGlfwCreateStandardCursor(shape, info.getReturnValue());
    }

    @Inject(method = "glfwDestroyWindow", at = @At("RETURN"))
    private static void after_glfwDestroyWindow_Konkrete(long window, CallbackInfo info) {
        GlfwCursorTracker.onGlfwDestroyWindow(window);
    }

    @Inject(method = "glfwDestroyCursor", at = @At("RETURN"))
    private static void after_glfwDestroyCursor_Konkrete(long cursor, CallbackInfo info) {
        GlfwCursorTracker.onGlfwDestroyCursor(cursor);
    }

}
