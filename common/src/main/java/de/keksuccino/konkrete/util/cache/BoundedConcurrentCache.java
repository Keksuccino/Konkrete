package de.keksuccino.konkrete.util.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.ToLongBiFunction;

/**
 * Concurrent insertion-ordered cache with exact entry and weight bounds.
 *
 * <p>Reads are lock-free. Writes are serialized so replacing a key cannot leave a stale eviction node that later
 * removes the new value.</p>
 */
public final class BoundedConcurrentCache<K, V> {

    private final ConcurrentMap<K, WeightedValue<V>> entries = new ConcurrentHashMap<>();
    private final ArrayDeque<K> evictionOrder = new ArrayDeque<>();
    private final Object writeLock = new Object();
    private final int maximumEntries;
    private final long maximumWeight;
    private final ToLongBiFunction<? super K, ? super V> weigher;
    private long weightedSize;

    /** Creates a cache with strict positive bounds; the synchronous weigher must return a non-negative value and must not reenter this cache. */
    public BoundedConcurrentCache(int maximumEntries, long maximumWeight, @NotNull ToLongBiFunction<? super K, ? super V> weigher) {
        if (maximumEntries <= 0) throw new IllegalArgumentException("maximumEntries must be positive");
        if (maximumWeight <= 0L) throw new IllegalArgumentException("maximumWeight must be positive");
        this.maximumEntries = maximumEntries;
        this.maximumWeight = maximumWeight;
        this.weigher = Objects.requireNonNull(weigher, "weigher");
    }

    /** Returns the cached value or {@code null}. */
    @Nullable
    public V get(@NotNull K key) {
        WeightedValue<V> value = this.entries.get(Objects.requireNonNull(key, "key"));
        return value != null ? value.value() : null;
    }

    /** Stores a value, evicting oldest entries as required. Oversized values are not retained. */
    public void put(@NotNull K key, @NotNull V value) {
        synchronized (this.writeLock) {
            this.putLocked(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        }
    }

    /** Atomically computes a value; {@code null} removes the key, while an oversized returned value is not retained. */
    @Nullable
    public V compute(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(remappingFunction, "remappingFunction");
        synchronized (this.writeLock) {
            WeightedValue<V> previous = this.entries.get(key);
            V replacement = remappingFunction.apply(key, previous != null ? previous.value() : null);
            if (replacement == null) this.removeLocked(key);
            else this.putLocked(key, replacement);
            return replacement;
        }
    }

    /** Returns the current entry count. */
    public int size() {
        return this.entries.size();
    }

    /** Returns the current weighted size. */
    public long weightedSize() {
        synchronized (this.writeLock) {
            return this.weightedSize;
        }
    }

    /** Removes all entries. */
    public void clear() {
        synchronized (this.writeLock) {
            this.entries.clear();
            this.evictionOrder.clear();
            this.weightedSize = 0L;
        }
    }

    private void putLocked(@NotNull K key, @NotNull V value) {
        long measuredWeight = this.weigher.applyAsLong(key, value);
        if (measuredWeight < 0L) throw new IllegalArgumentException("Cache weigher returned a negative value");
        long weight = Math.max(1L, measuredWeight);
        this.removeLocked(key);
        if (weight > this.maximumWeight) return;
        while (this.entries.size() >= this.maximumEntries || wouldExceed(this.weightedSize, weight, this.maximumWeight)) {
            K evictedKey = this.evictionOrder.pollFirst();
            if (evictedKey == null) break;
            WeightedValue<V> evicted = this.entries.remove(evictedKey);
            if (evicted != null) this.weightedSize -= evicted.weight();
        }
        this.entries.put(key, new WeightedValue<>(value, weight));
        this.evictionOrder.addLast(key);
        this.weightedSize += weight;
    }

    private void removeLocked(@NotNull K key) {
        WeightedValue<V> removed = this.entries.remove(key);
        if (removed == null) return;
        this.weightedSize -= removed.weight();
        this.evictionOrder.removeFirstOccurrence(key);
    }

    private static boolean wouldExceed(long currentWeight, long addedWeight, long maximumWeight) {
        return addedWeight > maximumWeight - currentWeight;
    }

    private record WeightedValue<V>(@NotNull V value, long weight) {

    }

}
