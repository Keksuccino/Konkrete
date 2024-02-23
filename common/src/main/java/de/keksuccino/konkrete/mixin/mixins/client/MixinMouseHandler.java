package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.gui.content.scrollarea.LegacyScrollAreaCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

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

}
