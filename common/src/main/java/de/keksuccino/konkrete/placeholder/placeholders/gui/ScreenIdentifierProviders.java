package de.keksuccino.konkrete.placeholder.placeholders.gui;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Process-wide configuration for the generic current-screen identifier placeholder. */
public final class ScreenIdentifierProviders {

    private static final ScreenIdentifierProvider CLASS_NAME_PROVIDER = screen -> screen.getClass().getName();
    private static volatile ScreenIdentifierProvider provider = CLASS_NAME_PROVIDER;

    private ScreenIdentifierProviders() {
    }

    /** Atomically installs a mapping used by subsequent {@code screenid} evaluations. */
    public static void set(@NotNull ScreenIdentifierProvider newProvider) {
        provider = Objects.requireNonNull(newProvider, "newProvider");
    }

    /** Returns the currently configured mapping with volatile visibility. */
    @NotNull
    public static ScreenIdentifierProvider get() {
        return provider;
    }

    /** Restores fully qualified screen class names as the dependency-free default identifier. */
    public static void reset() {
        provider = CLASS_NAME_PROVIDER;
    }
}
