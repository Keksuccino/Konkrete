package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.util.MouseUtil;
import de.keksuccino.konkrete.util.VanillaEvents;
import de.keksuccino.konkrete.util.input.ClicksPerSecondTracker;
import de.keksuccino.konkrete.util.input.InputUtils;
import de.keksuccino.konkrete.util.rendering.glsl.GlslRuntimeEventTracker;
import de.keksuccino.konkrete.util.rendering.ui.UIInputRouter;
import de.keksuccino.konkrete.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 2147483647)
public class MixinMouseHandler {

    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Unique private final Minecraft minecraft_Konkrete = Minecraft.getInstance();
    @Unique @Nullable private MouseButtonInfo mappedButtonInfo_Konkrete;

    @Inject(method = "onButton", at = @At("HEAD"))
    private void before_onButton_Konkrete(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo info) {
        this.mappedButtonInfo_Konkrete = null;
        if (window != WindowHandler.getWindowHandle()) return;
        InputUtils.updateActiveModifiers(buttonInfo.modifiers());
        VanillaEvents.updateLatestVanillaMouseButtonInfo(buttonInfo);
    }

    /** @reason Cache vanilla's platform-aware button mapping instead of duplicating its macOS Control-click state machine. */
    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;simulateRightClick(Lnet/minecraft/client/input/MouseButtonInfo;Z)Lnet/minecraft/client/input/MouseButtonInfo;"))
    private MouseButtonInfo wrap_simulateRightClick_Konkrete(MouseHandler handler, MouseButtonInfo buttonInfo, boolean pressed, Operation<MouseButtonInfo> original) {
        MouseButtonInfo mappedButtonInfo = original.call(handler, buttonInfo, pressed);
        this.mappedButtonInfo_Konkrete = mappedButtonInfo;
        VanillaEvents.updateLatestVanillaMouseButtonInfo(mappedButtonInfo);
        return mappedButtonInfo;
    }

    /** @reason Record one reusable input/lifecycle event after vanilla has completed each accepted button callback. */
    @Inject(method = "onButton", at = @At("RETURN"))
    private void after_onButton_Konkrete(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo info) {
        if (window != WindowHandler.getWindowHandle()) return;
        MouseButtonInfo mappedButtonInfo = this.mappedButtonInfo_Konkrete != null ? this.mappedButtonInfo_Konkrete : buttonInfo;
        this.mappedButtonInfo_Konkrete = null;
        int button = mappedButtonInfo.button();
        double mouseX = getScaledX_Konkrete();
        double mouseY = getScaledY_Konkrete();
        if (action == GLFW.GLFW_PRESS) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) MouseInput.mouseHandler_screenLeftMouseDown = true;
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) MouseInput.mouseHandler_screenRightMouseDown = true;
            ClicksPerSecondTracker.recordClick(button);
            MouseUtil.onMouseButtonPressed(button, mouseX, mouseY);
            GlslRuntimeEventTracker.onMouseButtonPressed(button, mouseX, mouseY);
        } else if (action == GLFW.GLFW_RELEASE) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) MouseInput.mouseHandler_screenLeftMouseDown = false;
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) MouseInput.mouseHandler_screenRightMouseDown = false;
            MouseUtil.onMouseButtonReleased(button, mouseX, mouseY);
            GlslRuntimeEventTracker.onMouseButtonReleased(button, mouseX, mouseY);
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void before_onScroll_Konkrete(long window, double scrollX, double scrollY, CallbackInfo info) {
        if (window != WindowHandler.getWindowHandle()) return;
        boolean discrete = this.minecraft_Konkrete.options.discreteMouseScroll().get();
        double sensitivity = this.minecraft_Konkrete.options.mouseWheelSensitivity().get();
        double deltaX = (discrete ? Math.signum(scrollX) : scrollX) * sensitivity;
        double deltaY = (discrete ? Math.signum(scrollY) : scrollY) * sensitivity;
        GlslRuntimeEventTracker.onMouseScrolled(getScaledX_Konkrete(), getScaledY_Konkrete(), deltaX, deltaY);
    }

    /** @reason Give reusable Konkrete UI children first refusal on scroll while preserving the complete vanilla screen call when none consume it. */
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean wrap_mouseScrolled_Konkrete(Screen screen, double mouseX, double mouseY, double deltaX, double deltaY, Operation<Boolean> original) {
        return UIInputRouter.routeMouseScrolled(screen.children(), mouseX, mouseY, deltaX, deltaY) || original.call(screen, mouseX, mouseY, deltaX, deltaY);
    }

    @Inject(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void before_mouseMoved_Konkrete(CallbackInfo info) {
        double scaleX = this.minecraft_Konkrete.getWindow().getGuiScaledWidth() / (double) this.minecraft_Konkrete.getWindow().getScreenWidth();
        double scaleY = this.minecraft_Konkrete.getWindow().getGuiScaledHeight() / (double) this.minecraft_Konkrete.getWindow().getScreenHeight();
        GlslRuntimeEventTracker.onMouseMoved(this.xpos * scaleX, this.ypos * scaleY, this.accumulatedDX * scaleX, this.accumulatedDY * scaleY);
    }

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"))
    private boolean wrap_mouseDragged_Konkrete(Screen screen, MouseButtonEvent event, double dragX, double dragY, Operation<Boolean> original) {
        VanillaEvents.updateLatestVanillaMouseButtonInfo(event.buttonInfo());
        return original.call(screen, event, dragX, dragY);
    }

    @Unique
    private double getScaledX_Konkrete() {
        return this.xpos * this.minecraft_Konkrete.getWindow().getGuiScaledWidth() / this.minecraft_Konkrete.getWindow().getScreenWidth();
    }

    @Unique
    private double getScaledY_Konkrete() {
        return this.ypos * this.minecraft_Konkrete.getWindow().getGuiScaledHeight() / this.minecraft_Konkrete.getWindow().getScreenHeight();
    }

}
