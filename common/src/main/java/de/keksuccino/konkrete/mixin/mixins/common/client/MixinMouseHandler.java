package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Inject(method = "onButton", at = @At("HEAD"))
    private void before_onButton_Konkrete(long windowHandle, MouseButtonInfo mouseButtonInfo, int action, CallbackInfo info) {
        MouseInput.resetScreenMouseButtons();
    }

    /** @reason Record only events dispatched to a screen, before its click/release callbacks run. */
    @WrapOperation(method = "onButton", at = @At(value = "NEW", target = "Lnet/minecraft/client/input/MouseButtonEvent;"))
    private MouseButtonEvent wrap_MouseButtonEvent_init_Konkrete(double posX, double posY, MouseButtonInfo mouseButtonInfo, Operation<MouseButtonEvent> original, @Local(argsOnly = true) int action) {
        MouseInput.updateScreenMouseButton(mouseButtonInfo.button(), action == 1);
        return original.call(posX, posY, mouseButtonInfo);
    }

}
