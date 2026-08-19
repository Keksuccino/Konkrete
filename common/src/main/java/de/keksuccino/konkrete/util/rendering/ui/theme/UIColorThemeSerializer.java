package de.keksuccino.konkrete.util.rendering.ui.theme;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.keksuccino.konkrete.util.file.FileUtils;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Encodes and decodes UI color theme serializer. */
public class UIColorThemeSerializer {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Gson adapter encoding {@link DrawableColor} values as hexadecimal strings. */
    public static final TypeAdapter<DrawableColor> DRAWABLE_COLOR_TYPE_ADAPTER = new TypeAdapter<>() {
        /** Writes a drawable color as a hexadecimal JSON object. */
        @Override
        public void write(JsonWriter out, DrawableColor value) throws IOException {
            out.beginObject();
            out.name("hex").value(value.getHex());
            out.endObject();
        }
        /** Reads a drawable color from its hexadecimal JSON object. */
        @Override
        public DrawableColor read(JsonReader in) throws IOException {
            String hex = null;
            in.beginObject();
            while(in.hasNext()) {
                String name = in.nextName();
                if (name.equals("hex")) {
                    hex = in.nextString();
                    break;
                }
            }
            in.endObject();
            return (hex != null) ? DrawableColor.of(hex) : DrawableColor.WHITE;
        }
    };

    /** Decodes theme from the supplied representation. */
    @Nullable
    public static UITheme deserializeTheme(@NotNull String json) {
        Objects.requireNonNull(json);
        try {
            Gson gson = buildGsonInstance();
            return gson.fromJson(json, UITheme.class);
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to deserialize Konkrete theme!", ex);
        }
        return null;
    }

    /** Decodes theme from resource from the supplied representation. */
    @Nullable
    public static UITheme deserializeThemeFromResource(@NotNull Identifier resource) {
        try (InputStream in = Objects.requireNonNull(Minecraft.getInstance().getResourceManager().open(resource))) {
            StringBuilder json = new StringBuilder();
            for (String s : FileUtils.readTextLinesFrom(in)) {
                json.append(s);
            }
            return deserializeTheme(json.toString());
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to deserialize Konkrete theme from Identifier: " + resource, ex);
        }
        return null;
    }

    /** Decodes theme from file from the supplied representation. */
    @Nullable
    public static UITheme deserializeThemeFromFile(@NotNull File file) {
        try {
            StringBuilder json = new StringBuilder();
            for (String s : FileUtils.readTextLinesFrom(file)) {
                json.append(s);
            }
            return deserializeTheme(json.toString());
        } catch (IOException ex) {
            LOGGER.error("[KONKRETE] Failed to deserialize Konkrete theme from file: " + file.getAbsolutePath(), ex);
            return null;
        }
    }

    /** Encodes theme for storage or transfer. */
    @Nullable
    public static String serializeTheme(@NotNull UITheme theme) {
        Objects.requireNonNull(theme);
        try {
            Gson gson = buildGsonInstance();
            return gson.toJson(theme);
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to serialize Konkrete theme!", ex);
        }
        return null;
    }

    /** Encodes theme to file for storage or transfer. */
    public static void serializeThemeToFile(@NotNull UITheme theme, @NotNull File file) {
        Objects.requireNonNull(theme);
        Objects.requireNonNull(file);
        try {
            Gson gson = buildGsonInstance();
            String json = gson.toJson(theme);
            if (json != null) {
                FileUtils.writeTextToFile(file, false, json);
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to serialize Konkrete theme to file!", ex);
        }
    }

    private static Gson buildGsonInstance() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(DrawableColor.class, DRAWABLE_COLOR_TYPE_ADAPTER);
        return gsonBuilder.create();
    }

}
