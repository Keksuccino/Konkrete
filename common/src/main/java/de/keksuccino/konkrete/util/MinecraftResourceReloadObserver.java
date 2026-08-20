package de.keksuccino.konkrete.util;

import net.minecraft.client.ResourceLoadStateTracker;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Tracks minecraft resource reload observer changes across client ticks. */
public class MinecraftResourceReloadObserver {

    private static final Map<Long, Consumer<ReloadAction>> RELOAD_LISTENERS = new HashMap<>();
    private static long reloadListenerCount = 0L;

    /**
     * Registers a reload listener that gets notified when Minecraft starts and finishes a resource reload via {@link ResourceLoadStateTracker}.<br>
     * Returns the ID of the listener, to be able to unregister it via {@link MinecraftResourceReloadObserver#removeReloadListener(long)}.
     */
    public static long addReloadListener(@NotNull Consumer<ReloadAction> listener) {
        reloadListenerCount++;
        RELOAD_LISTENERS.put(reloadListenerCount, listener);
        return reloadListenerCount;
    }

    /** Removes reload listener. */
    public static void removeReloadListener(long id) {
        RELOAD_LISTENERS.remove(id);
    }

    /** Returns the reload listeners. */
    public static List<Consumer<ReloadAction>> getReloadListeners() {
        return new ArrayList<>(RELOAD_LISTENERS.values());
    }

    /** Receives a callback after Minecraft completes a resource reload. */
    public enum ReloadAction {

        /** A resource reload is starting. */
        STARTING,
        /** A resource reload completed successfully. */
        FINISHED

    }

}
