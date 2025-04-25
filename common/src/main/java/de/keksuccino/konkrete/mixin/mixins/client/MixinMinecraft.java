package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterConstruct_Konkrete(GameConfig gameConfig, CallbackInfo info) {
        Konkrete.onGameInitCompleted();
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void headSetScreen_Konkrete(Screen $$0, CallbackInfo info) {
        MouseInput.mouseHandler_screenLeftMouseDown = false;
        MouseInput.mouseHandler_screenRightMouseDown = false;
    }

    @Inject(method = "resizeDisplay", at = @At("HEAD"))
    private void headResizeDisplay_Konkrete(CallbackInfo info) {
        MouseInput.mouseHandler_screenLeftMouseDown = false;
        MouseInput.mouseHandler_screenRightMouseDown = false;
    }

}
