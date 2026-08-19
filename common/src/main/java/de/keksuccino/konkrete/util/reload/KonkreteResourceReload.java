package de.keksuccino.konkrete.util.reload;

import de.keksuccino.konkrete.util.MinecraftResourceReloadObserver;
import de.keksuccino.konkrete.util.resource.ClientResourceIndex;
import de.keksuccino.konkrete.util.resource.ResourceHandlers;
import de.keksuccino.konkrete.util.resource.preload.ResourcePreLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Coordinates Konkrete's prepared client-resource index and ordered, caller-extensible apply hooks on Minecraft's client reload-apply thread.
 */
public final class KonkreteResourceReload {

    /** Stable resource-listener identifier used by loader integrations. */
    public static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath("konkrete", "client_resource_reload");

    private static final int RESOURCE_HANDLER_ORDER = 0;
    private static final int PRELOAD_ORDER = 100;
    private static final long DEFAULT_PRELOAD_TIMEOUT_MILLIS = 120_000L;
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final AtomicLong preloadTimeoutMillis = new AtomicLong(DEFAULT_PRELOAD_TIMEOUT_MILLIS);
    private static final ConcurrentSkipListMap<ListenerKey, Runnable> LISTENERS = new ConcurrentSkipListMap<>();
    private static final ClientReloadListenerRegistration<SimplePreparableReloadListener<ClientResourceIndex.PreparedIndex>> CLIENT_LISTENER_REGISTRATION = new ClientReloadListenerRegistration<>(KonkreteResourceReload::createMinecraftPreparableReloadListener);

    static {
        MinecraftResourceReloadObserver.addReloadListener(ClientResourceIndex::onMinecraftResourceReload);
        registerReloadListener(RESOURCE_HANDLER_ORDER, ResourceHandlers::reloadAll);
        registerReloadListener(PRELOAD_ORDER, () -> {
            long timeout = preloadTimeoutMillis.get();
            if (timeout > 0L) ResourcePreLoader.preLoadAll(timeout);
        });
    }

    private KonkreteResourceReload() {}

    /**
     * Registers an apply hook after built-in resource release and before built-in preloading.
     *
     * @param listener apply hook
     * @return numeric listener ID for removal
     */
    public static long registerReloadListener(@NotNull Runnable listener) {
        return registerReloadListener(50, listener).id();
    }

    /**
     * Registers an ordered apply hook. Lower order values run first; equal orders retain registration order. An uncaught listener failure aborts the resource reload.
     *
     * @param order listener order
     * @param listener apply hook
     * @return closeable registration handle
     */
    @NotNull
    public static Registration registerReloadListener(int order, @NotNull Runnable listener) {
        long id = NEXT_ID.incrementAndGet();
        ListenerKey key = new ListenerKey(order, id);
        LISTENERS.put(key, Objects.requireNonNull(listener, "listener"));
        return new Registration(id, key);
    }

    /**
     * Removes a listener by numeric ID.
     *
     * @param listenerId listener ID
     * @return whether a listener was removed
     */
    public static boolean removeReloadListener(long listenerId) {
        return LISTENERS.keySet().removeIf(key -> key.id() == listenerId);
    }

    /**
     * Configures the built-in preload wait per resource. Zero disables automatic preloading.
     *
     * @param timeoutMillis non-negative timeout in milliseconds
     */
    public static void setPreloadTimeoutMillis(long timeoutMillis) {
        if (timeoutMillis < 0L) throw new IllegalArgumentException("timeoutMillis must not be negative");
        preloadTimeoutMillis.set(timeoutMillis);
    }

    /** Returns the current built-in preload timeout, where zero means disabled. */
    public static long getPreloadTimeoutMillis() {
        return preloadTimeoutMillis.get();
    }

    /**
     * Registers the singleton reload listener with exactly one client loader.
     *
     * @param loader owning loader
     * @param registrar loader-specific registrar
     * @return {@code true} for the first registration and {@code false} for an idempotent same-loader retry
     */
    @ApiStatus.Internal
    public static boolean registerClientReloadListener(@NotNull ClientLoader loader, @NotNull Consumer<? super SimplePreparableReloadListener<ClientResourceIndex.PreparedIndex>> registrar) {
        return CLIENT_LISTENER_REGISTRATION.register(loader, registrar);
    }

    /**
     * Creates the listener used by Fabric and NeoForge registration adapters.
     *
     * @return preparable reload listener
     */
    @ApiStatus.Internal
    @NotNull
    public static SimplePreparableReloadListener<ClientResourceIndex.PreparedIndex> createMinecraftPreparableReloadListener() {
        return new SimplePreparableReloadListener<>() {
            /** {@inheritDoc} */
            @Override
            protected @NotNull ClientResourceIndex.PreparedIndex prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                return ClientResourceIndex.prepare(resourceManager);
            }

            /** {@inheritDoc} */
            @Override
            protected void apply(@NotNull ClientResourceIndex.PreparedIndex preparedIndex, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                // A later listener may still fail, so publication is intentionally deferred until ResourceLoadStateTracker reports whole-reload success.
                ClientResourceIndex.stage(preparedIndex);
                for (Runnable listener : List.copyOf(LISTENERS.values())) listener.run();
            }
        };
    }

    /** Supported owners of the shared client listener. */
    public enum ClientLoader {
        FABRIC,
        NEOFORGE
    }

    /** Closeable listener registration. */
    public static final class Registration implements AutoCloseable {

        private final long id;
        private final ListenerKey key;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Registration(long id, ListenerKey key) {
            this.id = id;
            this.key = key;
        }

        /** Returns the numeric listener ID. */
        public long id() {
            return this.id;
        }

        /** Removes this listener once. */
        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) LISTENERS.remove(this.key);
        }

    }

    private record ListenerKey(int order, long id) implements Comparable<ListenerKey> {
        /** {@inheritDoc} */
        @Override
        public int compareTo(@NotNull ListenerKey other) {
            int orderComparison = Integer.compare(this.order, other.order);
            return orderComparison != 0 ? orderComparison : Long.compare(this.id, other.id);
        }
    }

}
