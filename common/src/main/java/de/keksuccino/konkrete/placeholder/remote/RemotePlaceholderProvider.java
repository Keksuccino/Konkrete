package de.keksuccino.konkrete.placeholder.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Consumer-owned multiplayer bridge for gamerule and server-NBT values; Konkrete defines no packets. */
@FunctionalInterface
public interface RemotePlaceholderProvider {
    /**
     * Returns a current value, or {@code null} while unavailable. Implementations own transport, caching, request
     * coalescing, concurrent calls, and thread handoff; placeholder evaluation must not be blocked waiting for network
     * I/O. Konkrete supplies an immutable argument snapshot, so implementations must copy it before retaining derived
     * mutable state rather than attempting to modify it.
     */
    @Nullable String resolve(@NotNull String providerKey, @NotNull String requestKey, @NotNull Map<String, String> arguments);
}
