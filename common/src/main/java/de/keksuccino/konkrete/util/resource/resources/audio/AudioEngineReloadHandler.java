package de.keksuccino.konkrete.util.resource.resources.audio;

import de.keksuccino.konkrete.util.resource.ResourceHandlers;
import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.resources.video.Mp4VideoSoundEngineReloadHandler;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import de.keksuccino.melody.resources.audio.MinecraftSoundSettingsObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Coordinates resource-owned audio cleanup before reload and caller callbacks after OpenAL is rebuilt. */
public final class AudioEngineReloadHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean registered = false;

    private AudioEngineReloadHandler() {
    }

    /** Registers the idempotent Melody sound-engine reload observer. */
    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftSoundSettingsObserver.registerSoundEngineReloadListener(() -> {
            MainThreadTaskExecutor.executeInMainThread(
                    () -> {
                        LOGGER.info("[KONKRETE] Sound engine reload detected. Releasing cached audio resources.");
                        AudioResourceReloadTracker.forceReloadAllAfterSoundEngineReload();
                        ResourceHandlers.getAudioHandler().releaseAll();
                        ResourceRuntime.fireAudioEngineReloaded();
                    },
                    MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK
            );
        });
    }

    /** Suspends video audio immediately before Minecraft replaces its sound engine. */
    public static void beforeSoundEngineReload() {
        Mp4VideoSoundEngineReloadHandler.beforeSoundEngineReload();
    }

    /** Restores video audio after Minecraft installs the replacement sound engine. */
    public static void afterSoundEngineReload() {
        Mp4VideoSoundEngineReloadHandler.afterSoundEngineReload();
    }

}
