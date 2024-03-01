package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.command.ClientExecutor;
import de.keksuccino.konkrete.gui.content.handling.AdvancedWidgetsHandler;
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

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void beforeTick_Konkrete(CallbackInfo info) {
        try {
            AdvancedWidgetsHandler.onClientTick();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            ClientExecutor.onClientTick();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void beforeScreenAdded_Konkrete(Screen screen, CallbackInfo info) {
        try {
            AdvancedWidgetsHandler.onOpenScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
