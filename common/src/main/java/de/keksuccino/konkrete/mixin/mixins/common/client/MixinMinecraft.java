package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.util.MouseUtil;
import de.keksuccino.konkrete.util.lifecycle.ClientShutdownHandler;
import de.keksuccino.konkrete.util.player.CameraRotationObserver;
import de.keksuccino.konkrete.util.player.PlayerPositionObserver;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
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
    private void after_init_Konkrete(GameConfig gameConfig, CallbackInfo info) {
        Konkrete.onGameInitCompleted();
    }

    /** @reason Konkrete-owned native and asynchronous resources must be released while Minecraft's render and resource infrastructure is still available. */
    @Inject(method = "close", at = @At("HEAD"))
    private void before_close_Konkrete(CallbackInfo info) {
        ClientShutdownHandler.shutdown();
    }

    @Inject(method = "resizeGui", at = @At("HEAD"))
    private void before_resizeGui_Konkrete(CallbackInfo info) {
        MouseInput.mouseHandler_screenLeftMouseDown = false;
        MouseInput.mouseHandler_screenRightMouseDown = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void before_tick_Konkrete(CallbackInfo info) {
        MouseUtil.tick();
        CameraRotationObserver.tick();
        PlayerPositionObserver.tick();
        executeMainThreadTasks_Konkrete(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void after_tick_Konkrete(CallbackInfo info) {
        executeMainThreadTasks_Konkrete(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    }

    @Unique
    private static void executeMainThreadTasks_Konkrete(MainThreadTaskExecutor.ExecuteTiming timing) {
        for (Runnable task : MainThreadTaskExecutor.getAndClearQueue(timing)) {
            try {
                task.run();
            } catch (Throwable throwable) {
                LOGGER_KONKRETE.error("[KONKRETE] Error while executing {} main-thread task", timing, throwable);
            }
        }
    }

}
