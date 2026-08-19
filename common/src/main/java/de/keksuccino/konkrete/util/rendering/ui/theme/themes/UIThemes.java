package de.keksuccino.konkrete.util.rendering.ui.theme.themes;

import de.keksuccino.konkrete.util.file.FileUtils;
import de.keksuccino.konkrete.util.rendering.ui.UIConfiguration;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;
import de.keksuccino.konkrete.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.konkrete.util.rendering.ui.theme.UIColorThemeSerializer;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Catalogs and registers the built-in UI themes. */
public class UIThemes {

    private static final Logger LOGGER = LogManager.getLogger();

    // HARDCODED DEFAULT THEMES
    /** Built-in light UI theme. */
    public static final LightUITheme LIGHT = new LightUITheme();
    /** Built-in light high contrast UI theme. */
    public static final LightHighContrastUITheme LIGHT_HIGH_CONTRAST = new LightHighContrastUITheme();
    /** Built-in dark UI theme. */
    public static final DarkUITheme DARK = new DarkUITheme();
    /** Built-in dark high contrast UI theme. */
    public static final DarkHighContrastUITheme DARK_HIGH_CONTRAST = new DarkHighContrastUITheme();
    /** Built-in cherry blossom UI theme. */
    public static final CherryBlossomUITheme CHERRY_BLOSSOM = new CherryBlossomUITheme();
    /** Built-in spooky season UI theme. */
    public static final SpookySeasonUITheme SPOOKY_SEASON = new SpookySeasonUITheme();
    /** Built-in pumpkin soup UI theme. */
    public static final PumpkinSoupUITheme PUMPKIN_SOUP = new PumpkinSoupUITheme();
    /** Built-in cozy campfire UI theme. */
    public static final CozyCampfireUITheme COZY_CAMPFIRE = new CozyCampfireUITheme();
    /** Built-in purple void UI theme. */
    public static final PurpleVoidUITheme PURPLE_VOID = new PurpleVoidUITheme();

    // ASSET THEMES
//    public static final Identifier OLED_PURPLE_THEME_LOCATION = Identifier.fromNamespaceAndPath("konkrete", "themes/oled_purple.json");
//    public static final Identifier NETHER_THEME_LOCATION = Identifier.fromNamespaceAndPath("konkrete", "themes/nether.json");
//    public static final Identifier BUTTER_DARK_THEME_LOCATION = Identifier.fromNamespaceAndPath("konkrete", "themes/butter_dark.json");
//    public static final Identifier BUTTER_OLED_THEME_LOCATION = Identifier.fromNamespaceAndPath("konkrete", "themes/butter_oled.json");

    /** Built-in themes written to disk and protected from custom-file replacement. */
    public static final UITheme[] DEFAULT_THEMES = new UITheme[]{ LIGHT, DARK, LIGHT_HIGH_CONTRAST, DARK_HIGH_CONTRAST, CHERRY_BLOSSOM, SPOOKY_SEASON, PUMPKIN_SOUP, COZY_CAMPFIRE, PURPLE_VOID };

    /** Registers built-in, asset-backed, and user themes, then selects the configured theme. */
    public static void registerAll() {

        registerDefaultThemes();

        registerAssetThemes();

        registerCustomThemes();

        setActiveThemeFromOptions();

    }

    /** Refreshes themes from current state. */
    public static void reloadThemes() {
        LOGGER.info("[KONKRETE] Reloading UI themes..");
        UIColorThemeRegistry.clearThemes();
        registerAll();
        setActiveThemeFromOptions();
    }

    private static void registerAssetThemes() {

//        registerAssetTheme(OLED_PURPLE_THEME_LOCATION);
//
//        registerAssetTheme(NETHER_THEME_LOCATION);
//
//        registerAssetTheme(BUTTER_DARK_THEME_LOCATION);
//
//        registerAssetTheme(BUTTER_OLED_THEME_LOCATION);

    }

    private static void registerAssetTheme(@NotNull Identifier themeLocation) {
        UITheme theme = UIColorThemeSerializer.deserializeThemeFromResource(themeLocation);
        if (theme != null) {
            UIColorThemeRegistry.register(theme);
        } else {
            LOGGER.error("[KONKRETE] Failed to register Konkrete theme from assets! Deserialization failed and returned NULL for: " + themeLocation, new NullPointerException("Theme was NULL"));
        }
    }

    private static void registerDefaultThemes() {

        registerDefaultTheme(LIGHT);

        registerDefaultTheme(DARK);

        registerDefaultTheme(LIGHT_HIGH_CONTRAST);

        registerDefaultTheme(DARK_HIGH_CONTRAST);

        registerDefaultTheme(CHERRY_BLOSSOM);

        registerDefaultTheme(SPOOKY_SEASON);

        registerDefaultTheme(PUMPKIN_SOUP);

        registerDefaultTheme(COZY_CAMPFIRE);

        registerDefaultTheme(PURPLE_VOID);

    }

    private static void registerDefaultTheme(UITheme theme) {
        UIColorThemeRegistry.register(theme);
        UIColorThemeSerializer.serializeThemeToFile(theme, new File(getThemeDirectory(), theme.getIdentifier() + ".json"));
    }

    private static void registerCustomThemes() {
        for (UITheme theme : readThemesFromFiles()) {
            if (!isIdentifierOfDefaultTheme(theme.getIdentifier())) {
                UIColorThemeRegistry.register(theme);
            }
        }
    }

    @NotNull
    private static List<UITheme> readThemesFromFiles() {
        List<UITheme> themes = new ArrayList<>();
        try {
            File themeDirectory = getThemeDirectory();
            File[] files = themeDirectory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().toLowerCase().endsWith(".json")) {
                        UITheme theme = UIColorThemeSerializer.deserializeThemeFromFile(f);
                        if (theme != null) {
                            themes.add(theme);
                        } else {
                            LOGGER.error("[KONKRETE] Failed to read UI Theme from file: " + f.getPath());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to enumerate custom UI themes.", ex);
        }
        return themes;
    }

    private static void setActiveThemeFromOptions() {
        UIColorThemeRegistry.setActiveTheme(UIConfiguration.get().activeThemeIdentifier());
    }

    @NotNull
    private static File getThemeDirectory() {
        return FileUtils.createDirectory(UIConfiguration.get().dataDirectory().resolve("ui_themes").toFile());
    }

    private static boolean isIdentifierOfDefaultTheme(@NotNull String identifier) {
        for (UITheme t : DEFAULT_THEMES) {
            if (t.getIdentifier().equals(identifier)) return true;
        }
        return false;
    }

}
