package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.keksuccino.konkrete.util.resource.resources.audio.AudioEngineReloadHandler;
import net.minecraft.client.sounds.SoundEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {

    @Unique private static final Logger LOGGER_KONKRETE = LogManager.getLogger();

    /** @reason Video audio must be suspended before vanilla destroys OpenAL and restored even when the replacement sound engine fails to initialize. */
    @WrapMethod(method = "reload")
    private void wrap_reload_Konkrete(Operation<Void> original) {
        try {
            AudioEngineReloadHandler.beforeSoundEngineReload();
        } catch (Throwable throwable) {
            LOGGER_KONKRETE.error("[KONKRETE] Failed to suspend owned audio before Minecraft reloaded its sound engine", throwable);
        }
        try {
            original.call();
        } finally {
            try {
                AudioEngineReloadHandler.afterSoundEngineReload();
            } catch (Throwable throwable) {
                LOGGER_KONKRETE.error("[KONKRETE] Failed to restore owned audio after Minecraft reloaded its sound engine", throwable);
            }
        }
    }

}
