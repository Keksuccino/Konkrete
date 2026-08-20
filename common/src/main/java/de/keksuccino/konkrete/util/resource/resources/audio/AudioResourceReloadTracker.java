package de.keksuccino.konkrete.util.resource.resources.audio;

import de.keksuccino.konkrete.util.resource.ResourceHandlers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Tracks whether the sound engine is reloading so audio resources avoid stale OpenAL state. */
public final class AudioResourceReloadTracker {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object AUDIO_INSTANCE_LOCK_KONKRETE = new Object();
    private static final Set<IAudio> AUDIO_INSTANCES_KONKRETE = Collections.newSetFromMap(new WeakHashMap<>());

    private AudioResourceReloadTracker() {
    }

    /** Registers the audio instance with this audio resource component. */
    public static void registerAudioInstance(@NotNull IAudio audio) {
        synchronized (AUDIO_INSTANCE_LOCK_KONKRETE) {
            AUDIO_INSTANCES_KONKRETE.add(audio);
        }
    }

    /** Returns the force reload all after sound engine reload produced by the audio resource. */
    public static int forceReloadAllAfterSoundEngineReload() {
        List<IAudio> audios;
        synchronized (AUDIO_INSTANCE_LOCK_KONKRETE) {
            audios = new ArrayList<>(AUDIO_INSTANCES_KONKRETE);
        }

        if (audios.isEmpty()) return 0;

        int releasedCount = 0;
        for (IAudio audio : audios) {
            if (audio == null) continue;
            try {
                ResourceHandlers.getAudioHandler().release(audio);
                releasedCount++;
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to release cached audio resource after sound engine reload!", ex);
            }
        }

        LOGGER.info("[KONKRETE] Forced audio resource reload after sound engine reload. audioResourcesReleased: {}", releasedCount);
        return releasedCount;
    }

}
