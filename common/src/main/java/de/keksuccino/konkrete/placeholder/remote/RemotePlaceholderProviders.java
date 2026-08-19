package de.keksuccino.konkrete.placeholder.remote;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Configures multiplayer values without coupling Konkrete to a product packet protocol. Consumers must install a
 * provider before registering server-NBT or multiplayer-gamerule built-ins.
 */
public final class RemotePlaceholderProviders {
    private static final RemotePlaceholderProvider UNAVAILABLE = (providerKey, requestKey, arguments) -> null;
    private static volatile RemotePlaceholderProvider provider = UNAVAILABLE;

    private RemotePlaceholderProviders() {
    }

    /** Atomically installs the consumer-owned multiplayer transport/cache adapter. */
    public static void set(@NotNull RemotePlaceholderProvider newProvider) {
        provider = Objects.requireNonNull(newProvider, "newProvider");
    }

    /** Returns the latest process-wide provider with volatile visibility; the default always returns {@code null}. */
    @NotNull public static RemotePlaceholderProvider get() {
        return provider;
    }

    /** Returns whether a consumer has installed a multiplayer transport adapter. */
    public static boolean isConfigured() {
        return provider != UNAVAILABLE;
    }

    /** Restores the unavailable provider and removes authorization for optional remote built-in registration. */
    public static void reset() {
        provider = UNAVAILABLE;
    }
}
