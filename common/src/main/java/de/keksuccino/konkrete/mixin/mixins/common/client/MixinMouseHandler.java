package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Unique boolean cached_pressed_Konkrete = false;

    @Inject(method = "onButton", at = @At("HEAD"))
    private void head_onButton_Konkrete(long windowHandle, MouseButtonInfo mouseButtonInfo, int action, CallbackInfo info) {
        MouseInput.mouseHandler_screenLeftMouseDown = false;
        MouseInput.mouseHandler_screenRightMouseDown = false;
    }

    @Inject(method = "onButton", at = @At(value = "NEW", target = "Lnet/minecraft/client/input/MouseButtonEvent;"))
    private void before_MouseButtonEvent_init_Konkrete(long windowHandle, MouseButtonInfo mouseButtonInfo, int action, CallbackInfo info) {
        this.cached_pressed_Konkrete = action == 1;
    }

    @WrapOperation(method = "onButton", at = @At(value = "NEW", target = "Lnet/minecraft/client/input/MouseButtonEvent;"))
    private MouseButtonEvent wrap_MouseButtonEvent_init_Konkrete(double posX, double posY, MouseButtonInfo mouseButtonInfo, Operation<MouseButtonEvent> original) {
        if (this.cached_pressed_Konkrete) {
            if (mouseButtonInfo.button() == 0) {
                MouseInput.mouseHandler_screenLeftMouseDown = true;
            } else {
                MouseInput.mouseHandler_screenRightMouseDown = true;
            }
        } else {
            if (mouseButtonInfo.button() == 0) {
                MouseInput.mouseHandler_screenLeftMouseDown = false;
            } else {
                MouseInput.mouseHandler_screenRightMouseDown = false;
            }
        }
        return original.call(posX, posY, mouseButtonInfo);
    }

}
