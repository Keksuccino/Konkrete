package de.keksuccino.konkrete.placeholder.placeholders.server;

import de.keksuccino.konkrete.util.cache.BoundedConcurrentCache;
import de.keksuccino.konkrete.placeholder.LogCooldownTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/**
 * Bounded stale-while-refresh server-status provider using Minecraft's status pinger. The first lookup schedules work
 * and returns {@code null}; later lookups return the last immutable result while a 30-second refresh runs. At most 128
 * addresses and 16 active pings are retained, and every ping releases its permit after a 10-second timeout.
 */
public final class VanillaServerStatusProvider implements ServerStatusProviders.OwnedProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long REFRESH_INTERVAL_MILLIS = 30_000L;
    private static final long LOAD_TIMEOUT_MILLIS = 10_000L;
    private static final int MAXIMUM_CONCURRENT_PINGS = 16;
    private static final int MAXIMUM_ADDRESS_LENGTH = 255;
    private static final LogCooldownTracker TICK_FAILURE_COOLDOWNS = new LogCooldownTracker(10_000L, 4, 1_024L);

    private final BoundedConcurrentCache<String, Entry> entries = new BoundedConcurrentCache<>(128, 262_144L, (address, ignored) -> 128L + address.length());
    private final ServerStatusPinger pinger;
    private final ScheduledExecutorService executor;
    private final LongSupplier clock;
    private final Semaphore pingPermits = new Semaphore(MAXIMUM_CONCURRENT_PINGS);
    private final AtomicBoolean closed = new AtomicBoolean();

    /** Creates a daemon-backed provider and starts the lightweight vanilla connection tick. */
    public VanillaServerStatusProvider() {
        this(new ServerStatusPinger(), Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("Konkrete-ServerStatus").factory()), System::currentTimeMillis);
    }

    VanillaServerStatusProvider(@NotNull ServerStatusPinger pinger, @NotNull ScheduledExecutorService executor, @NotNull LongSupplier clock) {
        this.pinger = Objects.requireNonNull(pinger, "pinger");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.executor.scheduleWithFixedDelay(this::tickSafely, 0L, 50L, TimeUnit.MILLISECONDS);
    }

    /** Returns the last completed immutable status and schedules an initial or expired vanilla ping. */
    @Override
    @Nullable
    public ServerStatus getStatus(@NotNull String address) {
        if (this.closed.get()) return null;
        String normalizedAddress = Objects.requireNonNull(address, "address").trim();
        if (normalizedAddress.isEmpty()) return null;
        if (normalizedAddress.length() > MAXIMUM_ADDRESS_LENGTH) return ServerStatus.offline();
        Entry entry = this.entries.compute(normalizedAddress, (ignored, current) -> current != null ? current : new Entry());
        long now = this.clock.getAsLong();
        long attempt = entry.shouldRefresh(now) ? entry.begin() : 0L;
        if (attempt != 0L) this.launch(normalizedAddress, entry, attempt);
        return entry.status;
    }

    /** Cancels vanilla connections, stops executor work, and clears bounded status state. */
    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) return;
        this.executor.shutdownNow();
        this.pinger.removeAll();
        this.entries.clear();
    }

    /** Returns whether this Konkrete-owned provider has released its executor and vanilla connections. */
    public boolean isClosed() {
        return this.closed.get();
    }

    private void launch(@NotNull String address, @NotNull Entry entry, long attempt) {
        if (!this.pingPermits.tryAcquire()) {
            entry.complete(attempt, ServerStatus.offline(), this.clock.getAsLong());
            return;
        }
        AtomicBoolean completed = new AtomicBoolean();
        try {
            this.executor.execute(() -> this.ping(address, entry, attempt, completed));
            this.executor.schedule(() -> this.complete(entry, attempt, completed, ServerStatus.offline()), LOAD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (RuntimeException exception) {
            this.complete(entry, attempt, completed, ServerStatus.offline());
            LOGGER.warn("[KONKRETE] Failed to schedule server status ping for '{}'", address, exception);
        }
    }

    private void ping(@NotNull String address, @NotNull Entry entry, long attempt, @NotNull AtomicBoolean completed) {
        ServerData data = new ServerData(address, address, ServerData.Type.OTHER);
        data.ping = -1L;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            EventLoopGroupHolder eventLoops = EventLoopGroupHolder.remote(minecraft.options.useNativeTransport());
            this.pinger.pingServer(data, () -> { }, () -> this.complete(entry, attempt, completed, fromServerData(data)), eventLoops);
        } catch (Exception exception) {
            this.complete(entry, attempt, completed, ServerStatus.offline());
            LOGGER.debug("[KONKRETE] Server status ping failed for '{}'", address, exception);
        }
    }

    private void complete(@NotNull Entry entry, long attempt, @NotNull AtomicBoolean completed, @NotNull ServerStatus status) {
        if (!completed.compareAndSet(false, true)) return;
        entry.complete(attempt, status, this.clock.getAsLong());
        this.pingPermits.release();
    }

    @NotNull
    private static ServerStatus fromServerData(@NotNull ServerData data) {
        String motd = data.motd != null ? data.motd.getString() : "";
        String playerCount = data.status != null ? data.status.getString() : "0/0";
        String version = data.version != null ? data.version.getString() : "";
        return new ServerStatus(data.ping >= 0L, data.ping, motd, playerCount, version);
    }

    private void tickSafely() {
        try {
            this.pinger.tick();
        } catch (RuntimeException exception) {
            if (TICK_FAILURE_COOLDOWNS.tryAcquire("pinger-tick", this.clock.getAsLong())) LOGGER.warn("[KONKRETE] Failed to tick vanilla server status connections", exception);
        }
    }

    private static final class Entry {

        private boolean loading;
        private long attempt;
        private volatile ServerStatus status;
        private long loadedAt = Long.MIN_VALUE;

        private synchronized long begin() {
            if (this.loading) return 0L;
            this.loading = true;
            this.attempt++;
            if (this.attempt == 0L) this.attempt++;
            return this.attempt;
        }

        private synchronized boolean shouldRefresh(long now) {
            if (this.loading) return false;
            if (this.status == null || now < this.loadedAt) return true;
            return now - this.loadedAt >= REFRESH_INTERVAL_MILLIS;
        }

        private synchronized void complete(long completedAttempt, @NotNull ServerStatus result, long now) {
            // A late pong from a timed-out attempt must not overwrite a newer request or clear its loading state.
            if (!this.loading || completedAttempt != this.attempt) return;
            this.status = result;
            this.loadedAt = now;
            this.loading = false;
        }

    }

}
