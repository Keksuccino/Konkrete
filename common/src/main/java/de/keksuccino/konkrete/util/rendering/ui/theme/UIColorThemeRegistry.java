package de.keksuccino.konkrete.util.rendering.ui.theme;

import de.keksuccino.konkrete.util.rendering.ui.theme.themes.UIThemes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Registers and resolves UI color theme values by stable identifiers. */
public class UIColorThemeRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, UITheme> THEMES = new LinkedHashMap<>();
    private static final List<Consumer<UITheme>> THEME_CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

    private static UITheme activeTheme;

    /** Registers a theme by identifier, replacing an earlier theme with the same identifier. */
    public static void register(@NotNull UITheme theme) {
        Objects.requireNonNull(theme);
        Objects.requireNonNull(theme.getIdentifier());
        if (THEMES.containsKey(theme.identifier)) {
            LOGGER.warn("[KONKRETE] UIColorTheme with identifier '" + theme.getIdentifier() + "' already exists! Overriding theme!");
        }
        THEMES.put(theme.getIdentifier(), theme);
    }

    /** Returns the selected theme, falling back to the built-in dark theme before initialization. */
    @NotNull
    public static UITheme getActiveTheme() {
        if (activeTheme != null) {
            return activeTheme;
        }
        return UIThemes.DARK;
    }

    /** Selects a theme by identifier, falls back to dark, then notifies change listeners synchronously. */
    public static void setActiveTheme(@NotNull String identifier) {
        activeTheme = getTheme(identifier);
        if (activeTheme == null) {
            LOGGER.error("[KONKRETE] Unable to switch theme! Theme not found: " + identifier);
            LOGGER.error("[KONKRETE] Falling back to DARK theme!");
            activeTheme = UIThemes.DARK;
        }
        UITheme selectedTheme = getActiveTheme();
        for (Consumer<UITheme> listener : THEME_CHANGE_LISTENERS) listener.accept(selectedTheme);
    }

    /** Looks up a registered theme without applying a fallback. */
    @Nullable
    public static UITheme getTheme(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        return THEMES.get(identifier);
    }

    /** Snapshots registered themes in registration order. */
    @NotNull
    public static List<UITheme> getThemes() {
        return new ArrayList<>(THEMES.values());
    }

    /** Removes all registrations and clears the active selection without notifying listeners. */
    public static void clearThemes() {
        THEMES.clear();
        activeTheme = null;
    }

    /**
     * Registers a callback invoked after the active theme changes.
     *
     * @param listener theme-change listener
     */
    public static void addThemeChangeListener(@NotNull Consumer<UITheme> listener) {
        THEME_CHANGE_LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Removes a previously registered theme-change callback.
     *
     * @param listener theme-change listener
     * @return whether the listener was registered
     */
    public static boolean removeThemeChangeListener(@NotNull Consumer<UITheme> listener) {
        return THEME_CHANGE_LISTENERS.remove(Objects.requireNonNull(listener, "listener"));
    }

}
