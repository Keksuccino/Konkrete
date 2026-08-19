package de.keksuccino.konkrete.util;

import de.keksuccino.konkrete.util.file.ResourceFile;
import de.keksuccino.konkrete.util.resource.ResourceSupplier;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import de.keksuccino.konkrete.util.resource.resources.text.IText;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.resource.resources.video.IVideo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Serializes values through a configured JSON adapter. */
public interface SerializationHelper {

    /** Shared stateless serialization helper. */
    public static final SerializationHelper INSTANCE = new SerializationHelper() {};

    /** Deserializes an optional image resource source. */
    @Nullable
    default ResourceSupplier<ITexture> deserializeImageResourceSupplier(@Nullable String resourceSource) {
        if (resourceSource != null) return ResourceSupplier.image(resourceSource);
        return null;
    }

    /** Deserializes an optional audio resource source. */
    @Nullable
    default ResourceSupplier<IAudio> deserializeAudioResourceSupplier(@Nullable String resourceSource) {
        if (resourceSource != null) return ResourceSupplier.audio(resourceSource);
        return null;
    }

    /** Deserializes an optional video resource source. */
    @Nullable
    default ResourceSupplier<IVideo> deserializeVideoResourceSupplier(@Nullable String resourceSource) {
        if (resourceSource != null) return ResourceSupplier.video(resourceSource);
        return null;
    }

    /** Deserializes an optional text resource source. */
    @Nullable
    default ResourceSupplier<IText> deserializeTextResourceSupplier(@Nullable String resourceSource) {
        if (resourceSource != null) return ResourceSupplier.text(resourceSource);
        return null;
    }

    /** Deserializes an optional file constrained to the configured asset directory. */
    @Nullable
    default ResourceFile deserializeAssetResourceFile(@Nullable String gameDirectoryFilePath) {
        if (gameDirectoryFilePath == null) return null;
        else return ResourceFile.asset(gameDirectoryFilePath);
    }

    /** Deserializes an optional game-directory-relative file. */
    @Nullable
    default ResourceFile deserializeResourceFile(@Nullable String gameDirectoryFilePath) {
        if (gameDirectoryFilePath == null) return null;
        else return ResourceFile.of(gameDirectoryFilePath);
    }

    /** Deserializes a supported number type, returning the fallback on invalid input. */
    @NotNull
    default <T extends Number> T deserializeNumber(@NotNull Class<T> type, @NotNull T fallbackValue, @Nullable String serialized) {
        try {
            if (serialized != null) {
                serialized = serialized.replace(" ", "");
                if (type == Float.class) {
                    return (T) Float.valueOf(serialized);
                }
                if (type == Double.class) {
                    return (T) Double.valueOf(serialized);
                }
                if (type == Integer.class) {
                    return (T) Integer.valueOf(serialized);
                }
                if (type == Long.class) {
                    return (T) Long.valueOf(serialized);
                }
            }
        } catch (Exception ignore) {}
        return fallbackValue;
    }

    /** Deserializes a boolean, returning the fallback on invalid input. */
    default boolean deserializeBoolean(boolean fallbackValue, @Nullable String serialized) {
        if (serialized != null) {
            if (serialized.replace(" ", "").equalsIgnoreCase("true")) {
                return true;
            }
            if (serialized.replace(" ", "").equalsIgnoreCase("false")) {
                return false;
            }
        }
        return fallbackValue;
    }

}
