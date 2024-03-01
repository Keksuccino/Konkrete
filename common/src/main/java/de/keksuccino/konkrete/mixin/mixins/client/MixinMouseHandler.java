package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.gui.content.scrollarea.LegacyScrollAreaCompat;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Shadow private int fakeRightMouse;

    @Inject(method = "onScroll", at = @At(value = "HEAD"))
    private void beforeOnScroll_Konkrete(long window, double horizontal, double vertical, CallbackInfo info) {
        if (window == Minecraft.getInstance().getWindow().getWindow()) {
            double delta = (Minecraft.getInstance().options.discreteMouseScroll().get() ? Math.signum(vertical) : vertical) * Minecraft.getInstance().options.mouseWheelSensitivity().get();
            if (Minecraft.getInstance().getOverlay() == null) {
                if (Minecraft.getInstance().screen != null) {
                    LegacyScrollAreaCompat.notifyCallbacks((float)delta);
                }
            }
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void headOnPress_Konkrete(long windowHandle, int buttonRaw, int p_91533_, int p_91534_, CallbackInfo info) {

        Minecraft minecraft = Minecraft.getInstance();
        if (windowHandle == minecraft.getWindow().getWindow()) {
            boolean pressed = (p_91533_ == 1);
            if (Minecraft.ON_OSX && (buttonRaw == 0)) {
                if (pressed) {
                    if ((p_91534_ & 2) == 2) {
                        buttonRaw = 1;
                    }
                } else if (this.fakeRightMouse > 0) {
                    buttonRaw = 1;
                }
            }
            int button = buttonRaw;
            if (minecraft.getOverlay() == null) {
                if (minecraft.screen != null) {
                    if (pressed) {
                        if (button == 0) {
                            MouseInput.mouseHandler_screenLeftMouseDown = true;
                        } else {
                            MouseInput.mouseHandler_screenRightMouseDown = true;
                        }
                    } else {
                        if (button == 0) {
                            MouseInput.mouseHandler_screenLeftMouseDown = false;
                        } else {
                            MouseInput.mouseHandler_screenRightMouseDown = false;
                        }
                    }
                } else {
                    MouseInput.mouseHandler_screenLeftMouseDown = false;
                    MouseInput.mouseHandler_screenRightMouseDown = false;
                }
            }
        }

    }

}
