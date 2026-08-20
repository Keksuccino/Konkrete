package de.keksuccino.konkrete.util.lifecycle;

import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.konkrete.util.resource.ResourceHandlers;
import de.keksuccino.konkrete.util.resource.resources.texture.TextureManagerReleaseDispatcher;
import de.keksuccino.konkrete.util.threading.KonkreteExecutors;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ordered, failure-isolated client cleanup registry with default hooks for Konkrete-owned services.
 */
public final class ClientShutdownHandler {

    /** Order used for network and monitor quiescence. */
    public static final int ORDER_NETWORK = 100;
    /** Order used for owned executor shutdown. */
    public static final int ORDER_EXECUTORS = 200;
    /** Order used for optional media shutdown. */
    public static final int ORDER_MEDIA = 300;
    /** Order used for rendering and resource disposal while client infrastructure is alive. */
    public static final int ORDER_RENDERING_RESOURCES = 400;
    /** Order used for final main-thread queues and bridges. */
    public static final int ORDER_FINAL = 500;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final CleanupRegistry REGISTRY = createDefaultRegistry();

    private ClientShutdownHandler() {}

    /** Returns whether shutdown has started. */
    public static boolean isShuttingDown() {
        return REGISTRY.isShuttingDown();
    }

    /**
     * Registers a cleanup operation. Operations with lower order values execute first.
     * Late registrations after shutdown starts execute immediately to avoid leaking newly registered resources.
     *
     * @param name diagnostic cleanup name
     * @param order cleanup order
     * @param cleanup cleanup operation
     * @return closeable registration
     */
    @NotNull
    public static CleanupRegistration registerCleanup(@NotNull String name, int order, @NotNull Runnable cleanup) {
        return REGISTRY.register(name, order, cleanup);
    }

    /** Executes all registered cleanup operations exactly once. */
    public static void shutdown() {
        REGISTRY.shutdown();
    }

    private static CleanupRegistry createDefaultRegistry() {
        CleanupRegistry registry = new CleanupRegistry();
        registry.register("internet availability monitor", ORDER_NETWORK, WebUtils::shutdown);
        registry.register("Konkrete executors", ORDER_EXECUTORS, KonkreteExecutors::shutdownAll);
        registry.register("GLFW cursors", ORDER_RENDERING_RESOURCES, CursorHandler::shutdown);
        registry.register("resources", ORDER_RENDERING_RESOURCES + 10, ResourceHandlers::shutdownAll);
        registry.register("pending texture-manager releases", ORDER_RENDERING_RESOURCES + 20, TextureManagerReleaseDispatcher::flushPendingReleases);
        registry.register("main-thread task queue", ORDER_FINAL, MainThreadTaskExecutor::shutdown);
        return registry;
    }

    /**
     * Independent cleanup registry used by the global handler and available for focused subsystem lifecycles.
     */
    public static final class CleanupRegistry {

        private final AtomicLong nextId = new AtomicLong();
        private final ConcurrentSkipListMap<CleanupKey, CleanupEntry> cleanups = new ConcurrentSkipListMap<>();
        private final AtomicBoolean shutdownStarted = new AtomicBoolean();

        /** Creates an empty thread-safe registry with no default cleanups. */
        public CleanupRegistry() {}

        /**
         * Registers an ordered cleanup, or runs it immediately if shutdown already started.
         *
         * @param name diagnostic name
         * @param order execution order
         * @param cleanup operation
         * @return closeable registration
         */
        @NotNull
        public CleanupRegistration register(@NotNull String name, int order, @NotNull Runnable cleanup) {
            CleanupEntry entry = new CleanupEntry(Objects.requireNonNull(name, "name"), Objects.requireNonNull(cleanup, "cleanup"));
            CleanupKey key = new CleanupKey(order, this.nextId.incrementAndGet());
            if (this.shutdownStarted.get()) {
                runCleanup(entry);
                return CleanupRegistration.closed();
            }
            this.cleanups.put(key, entry);
            // Close the registration race: a shutdown between the first state check and map insertion owns and runs this entry here.
            if (this.shutdownStarted.get() && this.cleanups.remove(key, entry)) runCleanup(entry);
            return new CleanupRegistration(() -> this.cleanups.remove(key, entry));
        }

        /** Returns whether this registry has started shutting down. */
        public boolean isShuttingDown() {
            return this.shutdownStarted.get();
        }

        /** Runs a stable ordered snapshot once while isolating failures between entries. */
        public void shutdown() {
            if (!this.shutdownStarted.compareAndSet(false, true)) return;
            List<CleanupEntry> snapshot = new ArrayList<>(this.cleanups.values());
            this.cleanups.clear();
            for (CleanupEntry entry : snapshot) runCleanup(entry);
        }

        private static void runCleanup(CleanupEntry entry) {
            try {
                entry.cleanup().run();
            } catch (Throwable throwable) {
                LOGGER.error("[KONKRETE] Failed to clean up {} during client shutdown", entry.name(), throwable);
            }
        }

    }

    /** Closeable cleanup registration that can cancel an operation before shutdown starts. */
    public static final class CleanupRegistration implements AutoCloseable {

        private final AtomicBoolean closed = new AtomicBoolean();
        private final Runnable remover;

        private CleanupRegistration(Runnable remover) {
            this.remover = remover;
        }

        private static CleanupRegistration closed() {
            CleanupRegistration registration = new CleanupRegistration(() -> {});
            registration.closed.set(true);
            return registration;
        }

        /** Removes the cleanup operation once when it has not already been claimed by shutdown. */
        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) this.remover.run();
        }

    }

    private record CleanupEntry(String name, Runnable cleanup) {

    }

    private record CleanupKey(int order, long id) implements Comparable<CleanupKey> {

        /** {@inheritDoc} */
        @Override
        public int compareTo(@NotNull CleanupKey other) {
            int orderComparison = Integer.compare(this.order, other.order);
            return orderComparison != 0 ? orderComparison : Long.compare(this.id, other.id);
        }

    }

}
