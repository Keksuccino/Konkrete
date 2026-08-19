package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.util.window.InitialLoadingOverlayIconRefreshController;
import de.keksuccino.konkrete.util.window.WindowHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {

    @Shadow @Nullable private Overlay overlay;
    @Unique private final InitialLoadingOverlayIconRefreshController iconRefreshController_Konkrete = new InitialLoadingOverlayIconRefreshController();

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void before_setScreen_Konkrete(Screen screen, CallbackInfo info) {
        MouseInput.mouseHandler_screenLeftMouseDown = false;
        MouseInput.mouseHandler_screenRightMouseDown = false;
    }

    @Inject(method = "setOverlay", at = @At("TAIL"))
    private void after_setOverlay_Konkrete(@Nullable Overlay overlay, CallbackInfo info) {
        if (this.iconRefreshController_Konkrete.afterOverlayAssignment(this.overlay instanceof LoadingOverlay)) WindowHandler.updateCustomWindowIcon();
    }

}
