package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.ShutdownHelper;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.threading.ClientThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Unique private static final Logger LOGGER_KONKRETE = LogManager.getLogger();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void return_construct_Konkrete(GameConfig gameConfig, CallbackInfo info) {
        Konkrete.onGameInitCompleted();
    }

    @Inject(method = "resizeGui", at = @At("HEAD"))
    private void head_resizeDisplay_Konkrete(CallbackInfo info) {
        MouseInput.mouseHandler_screenLeftMouseDown = false;
        MouseInput.mouseHandler_screenRightMouseDown = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void head_tick_Konkrete(CallbackInfo info) {
        for (Runnable r : ClientThreadTaskExecutor.getAndClearQueue(ClientThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK)) {
            try {
                r.run();
            } catch (Exception ex) {
                LOGGER_KONKRETE.error("[KONKRETE] Failed to execute PRE-TICK task on client thread.", ex);
            }
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void return_tick_Konkrete(CallbackInfo info) {
        for (Runnable r : ClientThreadTaskExecutor.getAndClearQueue(ClientThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK)) {
            try {
                r.run();
            } catch (Exception ex) {
                LOGGER_KONKRETE.error("[KONKRETE] Failed to execute POST-TICK task on client thread.", ex);
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void head_close_Konkrete(CallbackInfo info) {
        LOGGER_KONKRETE.info("[KONKRETE] Running shutdown tasks..");
        ShutdownHelper.getShutdownTasks().forEach(Runnable::run);
        LOGGER_KONKRETE.info("[KONKRETE] Finished running shutdown tasks.");
    }

}
