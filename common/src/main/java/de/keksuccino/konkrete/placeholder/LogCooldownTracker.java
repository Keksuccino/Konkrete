package de.keksuccino.konkrete.placeholder;

import de.keksuccino.konkrete.util.cache.BoundedConcurrentCache;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Atomically claims bounded per-message cooldowns without retaining every distinct failure forever. */
public final class LogCooldownTracker {

    private static final long ENTRY_OVERHEAD_WEIGHT = 64L;

    private final BoundedConcurrentCache<String, CooldownEntry> cooldowns;
    private final long cooldownMillis;

    /** Creates a bounded cooldown tracker. */
    public LogCooldownTracker(long cooldownMillis, int maximumEntries, long maximumWeight) {
        if (cooldownMillis <= 0L) throw new IllegalArgumentException("cooldownMillis must be positive");
        this.cooldownMillis = cooldownMillis;
        this.cooldowns = new BoundedConcurrentCache<>(maximumEntries, maximumWeight, (error, ignored) -> ENTRY_OVERHEAD_WEIGHT + error.length());
    }

    /** Returns whether the caller acquired this error's cooldown at the supplied time. */
    public boolean tryAcquire(@NotNull String error, long nowMillis) {
        Objects.requireNonNull(error, "error");
        CooldownEntry candidate = new CooldownEntry(nowMillis);
        CooldownEntry result = this.cooldowns.compute(error, (ignored, current) -> current == null || hasElapsed(current.claimedAtMillis(), nowMillis, this.cooldownMillis) ? candidate : current);
        return result == candidate;
    }

    /** Returns the retained cooldown count. */
    public int size() {
        return this.cooldowns.size();
    }

    private static boolean hasElapsed(long previousMillis, long nowMillis, long cooldownMillis) {
        if (nowMillis < previousMillis) return true;
        long elapsed = nowMillis - previousMillis;
        return elapsed < 0L || elapsed >= cooldownMillis;
    }

    private record CooldownEntry(long claimedAtMillis) {
    }
}
