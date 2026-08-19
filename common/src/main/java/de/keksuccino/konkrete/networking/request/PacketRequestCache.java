package de.keksuccino.konkrete.networking.request;

import de.keksuccino.konkrete.networking.PacketSendResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;

/**
 * Bounded non-blocking cache for correlated request/response packets within one exact connection session.
 *
 * @param <S> connection/session identity type
 * @param <K> immutable request key type
 * @param <V> response value type
 */
public final class PacketRequestCache<S, K, V> {

    private final int maximumEntries;
    private final int maximumPendingRequests;
    private final long cacheDurationNanos;
    private final long requestTimeoutNanos;
    private final LongSupplier nanoTime;
    private final LinkedHashMap<K, CacheEntry<V>> entries = new LinkedHashMap<>(16, 0.75F, true);
    private final Map<K, PendingRequest> pendingByKey = new HashMap<>();
    private final Map<Long, K> keysByRequestId = new HashMap<>();

    @Nullable private S activeSession;
    private long nextRequestId;

    /**
     * Creates a system-clock cache with explicit memory and timing bounds.
     *
     * @param maximumEntries maximum retained response values
     * @param maximumPendingRequests maximum simultaneous requests
     * @param cacheDuration duration before a cached value is refreshed
     * @param requestTimeout duration before an unanswered request may be retried
     */
    public PacketRequestCache(int maximumEntries, int maximumPendingRequests, @NotNull Duration cacheDuration, @NotNull Duration requestTimeout) {
        this(maximumEntries, maximumPendingRequests, cacheDuration, requestTimeout, System::nanoTime);
    }

    PacketRequestCache(int maximumEntries, int maximumPendingRequests, @NotNull Duration cacheDuration, @NotNull Duration requestTimeout, @NotNull LongSupplier nanoTime) {
        if (maximumEntries < 1) throw new IllegalArgumentException("maximumEntries must be positive");
        if (maximumPendingRequests < 1) throw new IllegalArgumentException("maximumPendingRequests must be positive");
        this.maximumEntries = maximumEntries;
        this.maximumPendingRequests = maximumPendingRequests;
        this.cacheDurationNanos = requirePositiveNanos(cacheDuration, "cacheDuration");
        this.requestTimeoutNanos = requirePositiveNanos(requestTimeout, "requestTimeout");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    /**
     * Returns the latest value and schedules one refresh when it is absent or stale. Failed sends retain a short-lived
     * pending marker, preventing render-loop retries from flooding a peer that does not support the packet.
     *
     * @param session exact active connection identity
     * @param key immutable request key
     * @param sender sends a packet carrying the supplied positive correlation ID
     * @return the latest cached value, or {@code null} before the first response
     */
    public synchronized @Nullable V resolve(@NotNull S session, @NotNull K key, @NotNull LongFunction<PacketSendResult> sender) {
        S exactSession = Objects.requireNonNull(session, "session");
        K exactKey = Objects.requireNonNull(key, "key");
        LongFunction<PacketSendResult> exactSender = Objects.requireNonNull(sender, "sender");
        this.beginSession(exactSession);

        long now = this.nanoTime.getAsLong();
        this.purgeExpiredRequests(now);
        CacheEntry<V> entry = this.entries.get(exactKey);
        PendingRequest pending = this.pendingByKey.get(exactKey);

        boolean stale = entry == null || hasElapsed(now, entry.updatedAtNanos(), this.cacheDurationNanos);
        if (stale && pending == null && this.pendingByKey.size() < this.maximumPendingRequests) {
            long requestId = this.allocateRequestId();
            PendingRequest newPending = new PendingRequest(requestId, now);
            this.pendingByKey.put(exactKey, newPending);
            this.keysByRequestId.put(requestId, exactKey);
            try {
                exactSender.apply(requestId);
            } catch (RuntimeException | Error exception) {
                this.pendingByKey.remove(exactKey, newPending);
                this.keysByRequestId.remove(requestId, exactKey);
                throw exception;
            }
        }
        return entry == null ? null : entry.value();
    }

    /**
     * Completes only the matching correlation ID in the matching exact session. Unsolicited, duplicate, timed-out, and
     * previous-session responses are ignored.
     *
     * @param session exact receiving connection identity
     * @param requestId correlation ID returned to the peer
     * @param value resolved response value
     * @return whether the response completed a live request
     */
    public synchronized boolean complete(@NotNull S session, long requestId, @NotNull V value) {
        if (requestId <= 0 || this.activeSession != Objects.requireNonNull(session, "session")) return false;
        K key = this.keysByRequestId.remove(requestId);
        if (key == null) return false;
        PendingRequest pending = this.pendingByKey.get(key);
        if (pending == null || pending.requestId() != requestId) return false;
        if (hasElapsed(this.nanoTime.getAsLong(), pending.startedAtNanos(), this.requestTimeoutNanos)) {
            this.pendingByKey.remove(key);
            return false;
        }
        this.pendingByKey.remove(key);
        this.entries.put(key, new CacheEntry<>(Objects.requireNonNull(value, "value"), this.nanoTime.getAsLong()));
        this.trimEntries();
        return true;
    }

    /**
     * Starts a session, clearing values when the exact identity changes.
     *
     * @param session exact active connection identity
     */
    public synchronized void beginSession(@NotNull S session) {
        S exactSession = Objects.requireNonNull(session, "session");
        if (this.activeSession == exactSession) return;
        this.clearState();
        this.activeSession = exactSession;
    }

    /**
     * Ends and clears only the matching exact session.
     *
     * @param session disconnected identity, or {@code null} when unknown
     */
    public synchronized void endSession(@Nullable S session) {
        if (session == null || this.activeSession != session) return;
        this.clearState();
        this.activeSession = null;
    }

    /** Clears all cached and in-flight state. */
    public synchronized void clear() {
        this.clearState();
        this.activeSession = null;
    }

    synchronized int entryCount() {
        return this.entries.size();
    }

    synchronized int pendingCount() {
        return this.pendingByKey.size();
    }

    private long allocateRequestId() {
        do {
            this.nextRequestId++;
            if (this.nextRequestId <= 0) this.nextRequestId = 1;
        } while (this.keysByRequestId.containsKey(this.nextRequestId));
        return this.nextRequestId;
    }

    private void trimEntries() {
        Iterator<Map.Entry<K, CacheEntry<V>>> iterator = this.entries.entrySet().iterator();
        while (this.entries.size() > this.maximumEntries && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private void purgeExpiredRequests(long now) {
        Iterator<Map.Entry<K, PendingRequest>> iterator = this.pendingByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, PendingRequest> entry = iterator.next();
            PendingRequest pending = entry.getValue();
            if (!hasElapsed(now, pending.startedAtNanos(), this.requestTimeoutNanos)) continue;
            iterator.remove();
            this.keysByRequestId.remove(pending.requestId());
        }
    }

    private void clearState() {
        this.entries.clear();
        this.pendingByKey.clear();
        this.keysByRequestId.clear();
    }

    private static long requirePositiveNanos(@NotNull Duration duration, @NotNull String name) {
        long nanos = Objects.requireNonNull(duration, name).toNanos();
        if (nanos <= 0) throw new IllegalArgumentException(name + " must be positive");
        return nanos;
    }

    private static boolean hasElapsed(long now, long started, long duration) {
        return now - started >= duration;
    }

    private record CacheEntry<V>(@NotNull V value, long updatedAtNanos) {
    }

    private record PendingRequest(long requestId, long startedAtNanos) {
    }
}
