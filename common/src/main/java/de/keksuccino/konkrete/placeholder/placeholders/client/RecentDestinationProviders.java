package de.keksuccino.konkrete.placeholder.placeholders.client;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Process-wide lifecycle for optional consumer-owned world/server history. */
public final class RecentDestinationProviders {

    private static final RecentDestinationProvider UNAVAILABLE = () -> null;
    private static volatile RecentDestinationProvider provider = UNAVAILABLE;

    private RecentDestinationProviders() {
    }

    /** Atomically installs a history source used by subsequent placeholder evaluations. */
    public static void set(@NotNull RecentDestinationProvider newProvider) {
        provider = Objects.requireNonNull(newProvider, "newProvider");
    }

    /** Returns the active history source; the default reports no destination. */
    @NotNull
    public static RecentDestinationProvider get() {
        return provider;
    }

    /** Returns whether a consumer has installed a history source. */
    public static boolean isConfigured() {
        return provider != UNAVAILABLE;
    }

    /** Restores the unavailable source and removes authorization for optional built-in registration. */
    public static void reset() {
        provider = UNAVAILABLE;
    }
}
