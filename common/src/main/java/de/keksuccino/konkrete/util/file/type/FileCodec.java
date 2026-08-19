package de.keksuccino.konkrete.util.file.type;

import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.resource.ResourceSource;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * Used to read files via {@link FileType}.
 *
 * @param <T> The object type returned by the read() methods of the codec.
 */
@SuppressWarnings("unused")
public abstract class FileCodec<T> {

    /**
     * Should only be used for placeholder-like cases.
     */
    @NotNull public static <T> FileCodec<T> empty(@NotNull Class<T> type) {
        return new FileCodec<T>() {
            /** {@inheritDoc} */
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                return null;
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocation(@NotNull Identifier location) {
                return null;
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                return null;
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                return null;
            }
        };
    }

    /** Creates a codec with readers for all source kinds. */
    @NotNull
    public static <T> FileCodec<T> generic(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        ConsumingSupplier<Identifier, T> locationReader = consumes -> {
          try {
              InputStream in = Minecraft.getInstance().getResourceManager().open(consumes);
              return streamReader.get(in);
          } catch (Exception ex) {
              ex.printStackTrace();
          }
          return null;
        };
        return basic(type, streamReader, locationReader);
    }

    /** Creates a codec for a generic resource source. */
    @NotNull
    public static <T> FileCodec<T> basic(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<Identifier, T> locationReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        return new FileCodec<T>() {
            /** {@inheritDoc} */
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocation(@NotNull Identifier location) {
                Objects.requireNonNull(location);
                return locationReader.get(location);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                Objects.requireNonNull(file);
                try {
                    return streamReader.get(new FileInputStream(file));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                Objects.requireNonNull(fileUrl);
                InputStream in = WebUtils.openResourceStream(fileUrl);
                if (in != null) return streamReader.get(in);
                return null;
            }
        };
    }

    /** Creates a codec with a local-file reader. */
    @NotNull
    public static <T> FileCodec<T> basicWithLocal(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<Identifier, T> locationReader, @NotNull ConsumingSupplier<File, T> fileReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        Objects.requireNonNull(fileReader);
        return new FileCodec<T>() {
            /** {@inheritDoc} */
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocation(@NotNull Identifier location) {
                Objects.requireNonNull(location);
                return locationReader.get(location);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                Objects.requireNonNull(file);
                return fileReader.get(file);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                Objects.requireNonNull(fileUrl);
                InputStream in = WebUtils.openResourceStream(fileUrl);
                if (in != null) return streamReader.get(in);
                return null;
            }
        };
    }

    /** Creates a codec with a web reader. */
    @NotNull
    public static <T> FileCodec<T> basicWithWeb(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<Identifier, T> locationReader, @NotNull ConsumingSupplier<String, T> urlReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        Objects.requireNonNull(urlReader);
        return new FileCodec<T>() {
            /** {@inheritDoc} */
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocation(@NotNull Identifier location) {
                Objects.requireNonNull(location);
                return locationReader.get(location);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                Objects.requireNonNull(file);
                try {
                    return streamReader.get(new FileInputStream(file));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                Objects.requireNonNull(fileUrl);
                return urlReader.get(fileUrl);
            }
        };
    }

    /** Creates a codec with independent readers for stream, location, local-file, and web sources. */
    @NotNull
    public static <T> FileCodec<T> advanced(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<Identifier, T> locationReader, @NotNull ConsumingSupplier<File, T> fileReader, @NotNull ConsumingSupplier<String, T> urlReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        Objects.requireNonNull(fileReader);
        Objects.requireNonNull(urlReader);
        return new FileCodec<T>() {
            /** {@inheritDoc} */
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocation(@NotNull Identifier location) {
                Objects.requireNonNull(location);
                return locationReader.get(location);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                Objects.requireNonNull(file);
                return fileReader.get(file);
            }
            /** {@inheritDoc} */
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                Objects.requireNonNull(fileUrl);
                return urlReader.get(fileUrl);
            }
        };
    }

    /** Decodes a caller-owned input stream, returning {@code null} when the format cannot be read. */
    @Nullable
    public abstract T read(@NotNull InputStream in);

    /** Decodes a Minecraft resource location, returning {@code null} when unavailable or invalid. */
    @Nullable
    public abstract T readLocation(@NotNull Identifier location);

    /** Decodes a local file, returning {@code null} when unavailable or invalid. */
    @Nullable
    public abstract T readLocal(@NotNull File file);

    /** Decodes a web URL, returning {@code null} when unavailable or invalid. */
    @Nullable
    public abstract T readWeb(@NotNull String fileUrl);

    /** Routes a resource source to its location, local, or web reader; failures produce {@code null}. */
    @Nullable
    public T read(@NotNull ResourceSource resourceSource) {
        Objects.requireNonNull(resourceSource);
        try {
            if (resourceSource.getSourceType() == ResourceSourceType.LOCATION) {
                Identifier loc = Identifier.tryParse(resourceSource.getSourceWithoutPrefix());
                return (loc != null) ? this.readLocation(loc) : null;
            }
            if (resourceSource.getSourceType() == ResourceSourceType.LOCAL) {
                File localFile = resourceSource.getValidatedLocalFile();
                return (localFile != null) ? this.readLocal(localFile) : null;
            }
            if (resourceSource.getSourceType() == ResourceSourceType.WEB) {
                return this.readWeb(resourceSource.getSourceWithoutPrefix());
            }
        } catch (Exception ignore) {}
        return null;
    }

}
