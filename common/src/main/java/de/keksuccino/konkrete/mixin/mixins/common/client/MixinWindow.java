package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.konkrete.util.input.InputUtils;
import de.keksuccino.konkrete.util.window.PreciseGuiScaleWindow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class MixinWindow implements PreciseGuiScaleWindow {

    @Shadow @Final private long handle;
    @Shadow private int guiScaledWidth;
    @Shadow private int guiScaledHeight;
    @Unique private double preciseScale_Konkrete = -1.0D;

    @Shadow public abstract int getGuiScale();

    @Inject(method = "setGuiScale", at = @At("HEAD"))
    private void before_setGuiScale_Konkrete(int scale, CallbackInfo info) {
        // A vanilla scale update invalidates Konkrete's fractional-scale metadata.
        this.preciseScale_Konkrete = -1.0D;
    }

    /** @reason GLFW may omit matching modifier releases while the window is unfocused, so stale semantic modifiers must be cleared at the focus boundary. */
    @Inject(method = "onFocus", at = @At("HEAD"))
    private void before_onFocus_Konkrete(long windowHandle, boolean focused, CallbackInfo info) {
        if ((windowHandle == this.handle) && !focused) InputUtils.resetActiveModifiers();
    }

    @Unique
    @Override
    public double getPreciseGuiScale_Konkrete() {
        return this.preciseScale_Konkrete > 0.0D ? this.preciseScale_Konkrete : this.getGuiScale();
    }

    @Unique
    @Override
    public void setPreciseGuiScale_Konkrete(double scale) {
        this.preciseScale_Konkrete = scale;
    }

    @Unique
    @Override
    public void setGuiScaledSize_Konkrete(int width, int height) {
        this.guiScaledWidth = width;
        this.guiScaledHeight = height;
    }

}
