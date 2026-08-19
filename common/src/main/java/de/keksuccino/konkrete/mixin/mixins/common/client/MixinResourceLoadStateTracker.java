package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ResourceLoadStateTracker.class)
public class MixinResourceLoadStateTracker {

    @Unique private static final Logger LOGGER_KONKRETE = LogManager.getLogger();

    @Inject(method = "startReload", at = @At("HEAD"))
    private void before_startReload_Konkrete(CallbackInfo info) {
        if (isMinecraftReloadTracker_Konkrete()) dispatchReloadAction_Konkrete(MinecraftResourceReloadObserver.ReloadAction.STARTING);
    }

    @Inject(method = "finishReload", at = @At("RETURN"))
    private void after_finishReload_Konkrete(CallbackInfo info) {
        if (isMinecraftReloadTracker_Konkrete()) dispatchReloadAction_Konkrete(MinecraftResourceReloadObserver.ReloadAction.FINISHED);
    }

    @Unique
    private boolean isMinecraftReloadTracker_Konkrete() {
        return ((AccessorMixinMinecraft) Minecraft.getInstance()).get_reloadStateTracker_Konkrete() == (Object) this;
    }

    @Unique
    private static void dispatchReloadAction_Konkrete(MinecraftResourceReloadObserver.ReloadAction action) {
        for (Consumer<MinecraftResourceReloadObserver.ReloadAction> listener : MinecraftResourceReloadObserver.getReloadListeners()) {
            try {
                listener.accept(action);
            } catch (Throwable throwable) {
                LOGGER_KONKRETE.error("[KONKRETE] Resource-reload observer failed while handling {}", action, throwable);
            }
        }
    }

}
