package de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.konkrete.util.rendering.ui.UIConfiguration;
import de.keksuccino.konkrete.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.List;
import java.util.Objects;

/** Persists the file browser's audio-preview volume in the configured data directory. */
public class BrowserAudioSettings {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean initialized = false;
    private static float volume = 1.0F;

    private BrowserAudioSettings() {
    }

    /** Resolves the audio-preview settings file from the current UI data directory. */
    @NotNull
    public static File getSettingsFile() {
        return UIConfiguration.get().dataDirectory().resolve("browser_audio_settings.json").toFile();
    }

    /** Returns the persisted preview volume in the range 0.0 through 1.0. */
    public static float getVolume() {
        if (!initialized) read();
        initialized = true;
        return volume;
    }

    /** Clamps and persists the preview volume. */
    public static void setVolume(float newVolume) {
        if (!initialized) read();
        initialized = true;
        volume = clampVolume(newVolume);
        write();
    }

    private static void read() {
        try {
            File settingsFile = getSettingsFile();
            if (!UIConfiguration.get().dataDirectory().toFile().exists()) {
                UIConfiguration.get().dataDirectory().toFile().mkdirs();
            }
            settingsFile.createNewFile();
            List<String> lines = FileUtils.readTextLinesFrom(settingsFile);
            StringBuilder builder = new StringBuilder();
            lines.forEach(builder::append);
            String raw = builder.toString();
            if (raw.isBlank()) return;
            SettingsData data = GSON.fromJson(raw, SettingsData.class);
            if (data != null) {
                volume = clampVolume(data.volume);
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to read browser audio settings!", ex);
        }
    }

    private static void write() {
        try {
            File settingsFile = getSettingsFile();
            if (!UIConfiguration.get().dataDirectory().toFile().exists()) {
                UIConfiguration.get().dataDirectory().toFile().mkdirs();
            }
            settingsFile.createNewFile();
            String json = GSON.toJson(new SettingsData(volume));
            FileUtils.writeTextToFile(settingsFile, false, Objects.requireNonNull(json));
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to write browser audio settings!", ex);
        }
    }

    private static float clampVolume(float value) {
        if (value < 0.0F) return 0.0F;
        if (value > 1.0F) return 1.0F;
        return value;
    }

    private static class SettingsData {

        /** Volume fraction in the range 0.0 through 1.0. */
        public float volume = 1.0F;

        /** Creates the serialized settings payload. */
        public SettingsData(float volume) {
            this.volume = volume;
        }

    }

}
