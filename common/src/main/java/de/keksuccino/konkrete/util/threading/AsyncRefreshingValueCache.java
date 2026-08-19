package de.keksuccino.konkrete.util.threading;

import de.keksuccino.konkrete.util.cache.BoundedConcurrentCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.ToLongBiFunction;

/**
 * A bounded stale-while-refresh cache for asynchronous sources. Cache reads stay lock-free, while admission rechecks
 * freshness under {@link AsyncLoadCoordinator}'s publication boundary so a caller cannot schedule redundant work from
 * an obsolete cache snapshot. The convenience constructor retains at most 512 entries or approximately 16 MiB.
 */
public final class AsyncRefreshingValueCache<K, V> {

    /** Refresh interval that keeps the first successfully loaded value until {@link #clear()}. */
    public static final long NO_REFRESH = -1L;
    private static final int DEFAULT_MAXIMUM_ENTRIES = 512;
    private static final long DEFAULT_MAXIMUM_WEIGHT = 16_777_216L;

    private final BoundedConcurrentCache<K, TimedValue<V>> cache;
    private final AsyncLoadCoordinator<K> loadCoordinator;
    private final TaskLauncher taskLauncher;
    private final ValueLoader<K, V> valueLoader;
    private final LoadFailureHandler<K, V> loadFailureHandler;
    private final BiConsumer<K, RuntimeException> launchFailureHandler;
    private final LongSupplier timeSource;

    /** Creates a 512-entry/approximately-16-MiB cache with injectable loading and timing policies. */
    public AsyncRefreshingValueCache(@NotNull TaskLauncher taskLauncher, @NotNull ValueLoader<K, V> valueLoader, @NotNull LoadFailureHandler<K, V> loadFailureHandler, @NotNull BiConsumer<K, RuntimeException> launchFailureHandler, @NotNull LongSupplier timeSource) {
        this(DEFAULT_MAXIMUM_ENTRIES, DEFAULT_MAXIMUM_WEIGHT, AsyncRefreshingValueCache::estimateWeight, taskLauncher, valueLoader, loadFailureHandler, launchFailureHandler, timeSource);
    }

    /**
     * Creates a cache with explicit entry/weight/load bounds. The non-negative weigher runs synchronously under the
     * short cache-publication lock and must not reenter this cache.
     */
    public AsyncRefreshingValueCache(int maximumEntries, long maximumWeight, @NotNull ToLongBiFunction<? super K, ? super V> weigher, @NotNull TaskLauncher taskLauncher, @NotNull ValueLoader<K, V> valueLoader, @NotNull LoadFailureHandler<K, V> loadFailureHandler, @NotNull BiConsumer<K, RuntimeException> launchFailureHandler, @NotNull LongSupplier timeSource) {
        Objects.requireNonNull(weigher, "weigher");
        this.cache = new BoundedConcurrentCache<>(maximumEntries, maximumWeight, (key, value) -> weigher.applyAsLong(key, value.value()));
        this.loadCoordinator = new AsyncLoadCoordinator<>(maximumEntries);
        this.taskLauncher = Objects.requireNonNull(taskLauncher, "taskLauncher");
        this.valueLoader = Objects.requireNonNull(valueLoader, "valueLoader");
        this.loadFailureHandler = Objects.requireNonNull(loadFailureHandler, "loadFailureHandler");
        this.launchFailureHandler = Objects.requireNonNull(launchFailureHandler, "launchFailureHandler");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }

    /** Returns the stale value immediately and admits at most one asynchronous load when absent or expired. */
    @Nullable
    public V getOrLoad(@NotNull K key, long refreshIntervalMillis) {
        Objects.requireNonNull(key, "key");
        if (refreshIntervalMillis < NO_REFRESH) throw new IllegalArgumentException("refreshIntervalMillis must be non-negative or NO_REFRESH");
        long currentTime = this.timeSource.getAsLong();
        TimedValue<V> cached = this.cache.get(key);
        if (this.needsRefresh(cached, currentTime, refreshIntervalMillis)) this.tryLaunch(key, currentTime, refreshIntervalMillis);
        return cached == null ? null : cached.value();
    }

    /** Returns the last published value without scheduling work. */
    @Nullable
    public V getCached(@NotNull K key) {
        TimedValue<V> cached = this.cache.get(Objects.requireNonNull(key, "key"));
        return cached == null ? null : cached.value();
    }

    /** Returns whether an asynchronous load for {@code key} is currently admitted. */
    public boolean isLoading(@NotNull K key) {
        return this.loadCoordinator.isLoading(Objects.requireNonNull(key, "key"));
    }

    /** Returns the number of published cache entries. */
    public int size() {
        return this.cache.size();
    }

    /** Cancels admitted loads and clears published values as one generation change. */
    public void clear() {
        this.loadCoordinator.reset(this.cache::clear);
    }

    private void tryLaunch(@NotNull K key, long currentTime, long refreshIntervalMillis) {
        AsyncLoadCoordinator.Claim<K> claim = this.loadCoordinator.tryClaim(key, currentTime, () -> this.needsRefresh(this.cache.get(key), currentTime, refreshIntervalMillis));
        if (claim == null) return;
        try {
            this.taskLauncher.launch(() -> this.loadCoordinator.runClaim(claim, () -> this.loadAndPublish(claim)));
        } catch (RuntimeException exception) {
            this.loadCoordinator.abandon(claim);
            this.launchFailureHandler.accept(key, exception);
        } catch (Error error) {
            this.loadCoordinator.abandon(claim);
            throw error;
        }
    }

    private void loadAndPublish(@NotNull AsyncLoadCoordinator.Claim<K> claim) {
        V value;
        try {
            value = Objects.requireNonNull(this.valueLoader.load(claim.key()), "The asynchronous value loader returned null");
        } catch (Exception exception) {
            value = Objects.requireNonNull(this.loadFailureHandler.recover(claim.key(), exception), "The asynchronous load failure handler returned null");
        }
        V publishedValue = value;
        long loadedAt = this.timeSource.getAsLong();
        this.loadCoordinator.publishIfCurrent(claim, () -> this.cache.put(claim.key(), new TimedValue<>(loadedAt, publishedValue)));
    }

    private boolean needsRefresh(@Nullable TimedValue<V> cached, long currentTime, long refreshIntervalMillis) {
        if (cached == null) return true;
        if (refreshIntervalMillis == NO_REFRESH) return false;
        return (currentTime - cached.loadedAt()) >= refreshIntervalMillis;
    }

    private static long estimateWeight(Object key, Object value) {
        return saturatedAdd(saturatedAdd(64L, estimateObjectWeight(key, 0)), estimateObjectWeight(value, 0));
    }

    private static long estimateObjectWeight(Object value, int depth) {
        if (value == null) return 0L;
        if (value instanceof CharSequence text) return 32L + text.length() * 2L;
        if (value instanceof byte[] bytes) return 16L + bytes.length;
        if (depth >= 2) return 64L;
        if (value instanceof Collection<?> collection) {
            long weight = 32L;
            int sampled = 0;
            for (Object element : collection) {
                if (sampled == 256) break;
                weight = saturatedAdd(weight, estimateObjectWeight(element, depth + 1));
                sampled++;
            }
            weight = saturatedAdd(weight, saturatedMultiply(Math.max(0L, collection.size() - sampled), 64L));
            return weight;
        }
        if (value instanceof Map<?, ?> map) {
            long weight = 64L;
            int sampled = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (sampled == 128) break;
                long entryWeight = saturatedAdd(estimateObjectWeight(entry.getKey(), depth + 1), estimateObjectWeight(entry.getValue(), depth + 1));
                weight = saturatedAdd(weight, entryWeight);
                sampled++;
            }
            weight = saturatedAdd(weight, saturatedMultiply(Math.max(0L, map.size() - sampled), 128L));
            return weight;
        }
        return 64L;
    }

    private static long saturatedAdd(long first, long second) {
        return second > Long.MAX_VALUE - first ? Long.MAX_VALUE : first + second;
    }

    private static long saturatedMultiply(long first, long second) {
        return first != 0L && second > Long.MAX_VALUE / first ? Long.MAX_VALUE : first * second;
    }

    private record TimedValue<V>(long loadedAt, @NotNull V value) {

        private TimedValue {
            Objects.requireNonNull(value, "value");
        }

    }

    /** Starts asynchronous cache work; launch failures are reported separately from load failures. */
    @FunctionalInterface
    public interface TaskLauncher {

        /** Accepts one guarded load task; throwing reports a launch failure and releases its claim. */
        void launch(@NotNull Runnable task);

    }

    /** Loads one non-null value for a cache key. */
    @FunctionalInterface
    public interface ValueLoader<K, V> {

        /** Loads a non-null value; any exception is passed to the configured failure handler. */
        @NotNull V load(@NotNull K key) throws Exception;

    }

    /** Converts a checked or runtime load failure into a non-null cached fallback. */
    @FunctionalInterface
    public interface LoadFailureHandler<K, V> {

        /** Produces the non-null fallback published after {@link ValueLoader#load(Object)} fails. */
        @NotNull V recover(@NotNull K key, @NotNull Exception exception);

    }

}
