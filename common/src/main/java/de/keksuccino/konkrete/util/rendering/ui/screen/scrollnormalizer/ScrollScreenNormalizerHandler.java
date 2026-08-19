package de.keksuccino.konkrete.util.rendering.ui.screen.scrollnormalizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.konkrete.util.rendering.ui.UIConfiguration;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Coordinates scroll screen normalizer lifecycle and event dispatch. */
public class ScrollScreenNormalizerHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Boolean> NORMALIZE_SCREEN_MAP = new HashMap<>();
    private static Function<Screen, String> screenIdentifierProvider = screen -> screen.getClass().getName();

    private static boolean loaded = false;

    private static void loadFromFile() {
        if (loaded) return;
        try {
            File dir = getStorageFile();
            if (dir.exists()) {
                try (FileReader reader = new FileReader(dir)) {
                    Type mapType = new TypeToken<Map<String, Boolean>>(){}.getType();
                    Map<String, Boolean> loadedMap = GSON.fromJson(reader, mapType);
                    if (loadedMap != null) {
                        NORMALIZE_SCREEN_MAP.clear();
                        NORMALIZE_SCREEN_MAP.putAll(loadedMap);
                    }
                }
            }
            loaded = true;
        } catch (IOException e) {
            LOGGER.error("[KONKRETE] Failed to load normalized scroll screen states from file!", e);
            loaded = true; // Set to true even on error to prevent repeated attempts
        }
    }

    private static void saveToFile() {
        try {
            // Ensure parent directory exists
            File storageFile = getStorageFile();
            File parentDir = storageFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(storageFile)) {
                GSON.toJson(NORMALIZE_SCREEN_MAP, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[KONKRETE] Failed to save normalized scroll screen states to file!", e);
        }
    }

    /** Sets for screen for this scroll screen normalizer handler. */
    public static void setForScreen(@NotNull Screen screen, boolean normalize) {
        loadFromFile();
        NORMALIZE_SCREEN_MAP.put(screenIdentifierProvider.apply(screen), normalize);
        saveToFile();
    }

    /** Returns whether normalize. */
    public static boolean shouldNormalize(Screen screen) {
        if (screen == null) return false;
        loadFromFile();
        String id = screenIdentifierProvider.apply(screen);
        if (!NORMALIZE_SCREEN_MAP.containsKey(id)) return false;
        return NORMALIZE_SCREEN_MAP.get(id);
    }

    /**
     * Configures the stable identifier used to persist per-screen decisions.
     *
     * @param provider identifier provider
     */
    public static void setScreenIdentifierProvider(@NotNull Function<Screen, String> provider) {
        screenIdentifierProvider = Objects.requireNonNull(provider, "provider");
    }

    private static File getStorageFile() {
        return UIConfiguration.get().dataDirectory().resolve("normalized_scroll_screens.json").toFile();
    }

}
