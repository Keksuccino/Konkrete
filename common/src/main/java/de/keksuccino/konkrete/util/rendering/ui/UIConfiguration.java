package de.keksuccino.konkrete.util.rendering.ui;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable, application-supplied configuration for Konkrete's reusable UI toolkit.
 *
 * @param uiScale logical scale used by toolkit widgets
 * @param useMinecraftFont whether UI text should use Minecraft's font renderer
 * @param blurEnabled whether themes may render blurred surfaces
 * @param blurIntensity multiplier applied to requested blur radii
 * @param animationsEnabled whether toolkit animations should advance
 * @param clickSoundsEnabled whether interactive UI elements should play click sounds
 * @param contextMenuHoverOpenSpeed hover-open speed multiplier for nested context menus
 * @param pipWindowDockingEnabled whether picture-in-picture windows dock to nearby edges
 * @param pipWindowDebugEnabled whether picture-in-picture debug bounds are rendered
 * @param smoothFontMultilineEnabled whether smooth-font strings interpret line breaks
 * @param activeThemeIdentifier identifier of the theme selected during registry initialization
 * @param dataDirectory directory used by optional toolkit persistence
 */
public record UIConfiguration(@NotNull UIScale uiScale, boolean useMinecraftFont, boolean blurEnabled, float blurIntensity, boolean animationsEnabled, boolean clickSoundsEnabled, int contextMenuHoverOpenSpeed, boolean pipWindowDockingEnabled, boolean pipWindowDebugEnabled, boolean smoothFontMultilineEnabled, @NotNull String activeThemeIdentifier, @NotNull Path dataDirectory) {

    private static volatile UIConfiguration current = defaults();

    /** Validates and normalizes a configuration instance. */
    public UIConfiguration {
        Objects.requireNonNull(uiScale, "uiScale");
        Objects.requireNonNull(activeThemeIdentifier, "activeThemeIdentifier");
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        if (!Float.isFinite(blurIntensity) || blurIntensity < 0.0F) throw new IllegalArgumentException("blurIntensity must be finite and non-negative");
        if (contextMenuHoverOpenSpeed < 1) throw new IllegalArgumentException("contextMenuHoverOpenSpeed must be at least one");
        if (activeThemeIdentifier.isBlank()) throw new IllegalArgumentException("activeThemeIdentifier must not be blank");
        dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    /**
     * Returns the active toolkit configuration.
     *
     * @return active configuration
     */
    @NotNull
    public static UIConfiguration get() {
        return current;
    }

    /**
     * Replaces the active toolkit configuration.
     *
     * @param configuration new configuration
     */
    public static void set(@NotNull UIConfiguration configuration) {
        current = Objects.requireNonNull(configuration, "configuration");
    }

    /**
     * Atomically derives and installs a configuration from the current value.
     *
     * @param updater configuration transformation
     * @return installed configuration
     */
    @NotNull
    public static synchronized UIConfiguration update(@NotNull UnaryOperator<UIConfiguration> updater) {
        UIConfiguration updated = Objects.requireNonNull(updater, "updater").apply(current);
        current = Objects.requireNonNull(updated, "updater result");
        return current;
    }

    /**
     * Creates the toolkit's standalone defaults.
     *
     * @return default configuration
     */
    @NotNull
    public static UIConfiguration defaults() {
        return new UIConfiguration(UIScale.AUTO, false, false, 3.0F, true, true, 1, true, false, false, "dark", Path.of("config", "konkrete"));
    }

}
