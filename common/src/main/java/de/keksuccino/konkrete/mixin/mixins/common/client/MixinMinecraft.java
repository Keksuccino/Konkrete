package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
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

    @Inject(method = "resizeGui", at = @At("HEAD"))
    private void headResizeDisplay_Konkrete(CallbackInfo info) {
        MouseInput.mouseHandler_screenLeftMouseDown = false;
        MouseInput.mouseHandler_screenRightMouseDown = false;
    }

}
