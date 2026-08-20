package de.keksuccino.konkrete.util.window;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Dynamic, mod-owned window preferences consumed by the reusable window handler.
 */
public interface WindowConfiguration {

    /** Returns whether windowed clients should be switched to fullscreen. */
    default boolean forceFullscreen() {
        return false;
    }

    /** Returns whether configured custom icons should be applied. */
    default boolean customWindowIconEnabled() {
        return false;
    }

    /** Returns a 16x16 PNG icon path for Windows/Linux, or {@code null}. */
    @Nullable
    default Path customWindowIcon16() {
        return null;
    }

    /** Returns a 32x32 PNG icon path for Windows/Linux, or {@code null}. */
    @Nullable
    default Path customWindowIcon32() {
        return null;
    }

    /** Returns an ICNS icon path for macOS, or {@code null}. */
    @Nullable
    default Path customWindowIconMacOS() {
        return null;
    }

    /** Returns a custom title, or {@code null} to use vanilla's generated title. */
    @Nullable
    default String customWindowTitle() {
        return null;
    }

    /** Returns a configuration with every optional behavior disabled. */
    @NotNull
    static WindowConfiguration disabled() {
        return DisabledWindowConfiguration.INSTANCE;
    }

}

enum DisabledWindowConfiguration implements WindowConfiguration {

    INSTANCE

}
