package de.keksuccino.konkrete.util.file.type;

import com.google.common.io.Files;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.input.TextValidators;
import de.keksuccino.konkrete.util.resource.ResourceSource;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import de.keksuccino.konkrete.util.file.type.types.*;

/**
 * Defines a specific file type.<br>
 * Has methods to identify local and web sources as the defined file type.<br>
 * Is used to decode files of the defined file type.<br><br>
 *
 * It is not recommended to create subclasses of this class.<br>
 * Instead, use the already defined subclasses for all {@link FileMediaType}s:<br>
 * {@link ImageFileType}, {@link AudioFileType}, {@link VideoFileType}, {@link TextFileType}
 *
 * @param <T> The object type returned by the read() methods of the {@link FileCodec} of this {@link FileType}.
 */
@SuppressWarnings("unused")
public class FileType<T> {

    /** Stores values in mutable insertion order. */
    protected final List<String> extensions = new ArrayList<>();
    /** Broad media category used for grouping and chooser filtering. */
    @NotNull
    protected FileMediaType mediaType;
    /** Optional MIME type used by advanced web detection. */
    @Nullable
    protected String mimeType;
    /** Decoder used to load matching resources. */
    @NotNull
    protected FileCodec<T> codec;
    /** Whether Minecraft resource locations are accepted. */
    protected boolean allowLocation = true;
    /** Whether local files are accepted. */
    protected boolean allowLocal = true;
    /** Whether web URLs are accepted. */
    protected boolean allowWeb = true;
    /** Optional label overriding the first-extension fallback. */
    @Nullable
    protected Component customDisplayName;

    /** Creates an extensionless {@link FileMediaType#OTHER} descriptor with the supplied codec. */
    protected FileType(@NotNull FileCodec<T> codec) {
        this.mediaType = FileMediaType.OTHER;
        this.codec = codec;
    }

    /** Creates a descriptor and normalizes each supplied extension. */
    protected FileType(@NotNull FileCodec<T> codec, @Nullable String mimeType, @NotNull FileMediaType mediaType, @NotNull String... extensions) {
        Arrays.asList(extensions).forEach(this::addExtension);
        this.mediaType = mediaType;
        this.mimeType = mimeType;
        this.codec = codec;
    }

    /** Returns whether the resource path extension matches this type. */
    public boolean isFileTypeLocation(@NotNull Identifier location) {
        return this.extensions.contains(Files.getFileExtension(location.getPath()).toLowerCase());
    }

    /** Returns whether the local filename extension matches this type. */
    public boolean isFileTypeLocal(@NotNull File file) {
        return this.extensions.contains(Files.getFileExtension(file.getPath()).toLowerCase());
    }

    /**
     * Checks if the URL starts with "http://" or "https://" and checks if it ends with a file extension of this {@link FileType}.<br>
     * Will NOT WORK for non-direct URLs that don't end with a file name + extension. In that case, use {@link FileType#isFileTypeWebAdvanced(String)}.
     */
    public boolean isFileTypeWeb(@NotNull String fileUrl) {
        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(fileUrl)) return false;
        if (fileUrl.endsWith("/")) fileUrl = fileUrl.substring(0, fileUrl.length()-1);
        fileUrl = fileUrl.toLowerCase();
        for (String extension : this.extensions) {
            if (fileUrl.endsWith("." + extension)) return true;
        }
        return false;
    }

    /**
     * If {@link FileType#isFileTypeWeb(String)} isn't enough, this method will open a connection to the web source and tries to get its mime type.<br>
     * If the returned mime type is the same as the one of this file type, this method returns TRUE.
     */
    public boolean isFileTypeWebAdvanced(@NotNull String fileUrl) {
        if (this.mimeType == null) return true;
        return Objects.equals(WebUtils.getMimeType(fileUrl), this.mimeType);
    }

    /** Dispatches type detection by source kind; optional advanced web checks may perform network I/O. */
    public boolean isFileType(@NotNull ResourceSource resourceSource, boolean doAdvancedWebChecks) {
        Objects.requireNonNull(resourceSource);
        try {
            if (resourceSource.getSourceType() == ResourceSourceType.LOCATION) {
                Identifier loc = Identifier.tryParse(resourceSource.getSourceWithoutPrefix());
                if (loc != null) return this.isFileTypeLocation(loc);
            }
            if (resourceSource.getSourceType() == ResourceSourceType.LOCAL) {
                File localFile = resourceSource.getValidatedLocalFile();
                return (localFile != null) && this.isFileTypeLocal(localFile);
            }
            if (resourceSource.getSourceType() == ResourceSourceType.WEB) {
                if (this.isFileTypeWeb(resourceSource.getSourceWithoutPrefix())) return true;
                if (doAdvancedWebChecks) {
                    if (this.isFileTypeWebAdvanced(resourceSource.getSourceWithoutPrefix())) return true;
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    /** Returns a mutable copy of the normalized extension list. */
    @NotNull
    public List<String> getExtensions() {
        return new ArrayList<>(this.extensions);
    }

    /** Normalizes and adds an extension once, then returns this descriptor. */
    public FileType<T> addExtension(@NotNull String extension) {
        extension = Objects.requireNonNull(extension).toLowerCase().replace(".", "").replace(" ", "");
        if (this.extensions.contains(extension)) return this;
        this.extensions.add(extension);
        return this;
    }

    /** Normalizes and removes every matching extension, then returns this descriptor. */
    public FileType<T> removeExtension(@NotNull String extension) {
        extension = Objects.requireNonNull(extension).toLowerCase().replace(".", "").replace(" ", "");
        while (this.extensions.contains(extension)) {
            this.extensions.remove(extension);
        }
        return this;
    }

    /** Returns this descriptor's broad media category. */
    @NotNull
    public FileMediaType getMediaType() {
        return this.mediaType;
    }

    /** Returns the optional MIME type used for advanced web detection. */
    @Nullable
    public String getMimeType() {
        return this.mimeType;
    }

    /** Returns the decoder associated with this type. */
    @NotNull
    public FileCodec<T> getCodec() {
        return this.codec;
    }

    /** Replaces the non-null decoder and returns this descriptor. */
    public FileType<T> setCodec(@NotNull FileCodec<T> codec) {
        this.codec = Objects.requireNonNull(codec);
        return this;
    }

    /** Returns whether Minecraft resource locations are allowed. */
    public boolean isLocationAllowed() {
        return this.allowLocation;
    }

    /** Configures resource-location support and returns this descriptor. */
    public FileType<T> setLocationAllowed(boolean allowLocation) {
        this.allowLocation = allowLocation;
        return this;
    }

    /** Returns whether local-file sources are allowed. */
    public boolean isLocalAllowed() {
        return this.allowLocal;
    }

    /** Configures local-file support and returns this descriptor. */
    public FileType<T> setLocalAllowed(boolean allowLocal) {
        this.allowLocal = allowLocal;
        return this;
    }

    /** Returns whether web sources are allowed. */
    public boolean isWebAllowed() {
        return this.allowWeb;
    }

    /** Configures web-source support and returns this descriptor. */
    public FileType<T> setWebAllowed(boolean allowWeb) {
        this.allowWeb = allowWeb;
        return this;
    }

    /** Returns the custom label, uppercase first extension, or an empty component. */
    @NotNull
    public Component getDisplayName() {
        if (this.customDisplayName != null) return this.customDisplayName;
        if (!this.extensions.isEmpty()) return Component.literal(this.extensions.get(0).toUpperCase());
        return Component.empty();
    }

    /** Sets or clears the custom label and returns this descriptor. */
    public FileType<T> setCustomDisplayName(@Nullable Component name) {
        this.customDisplayName = name;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "FileType{" +
                "extensions=" + extensions +
                ", mediaType=" + mediaType +
                ", mimeType='" + mimeType + '\'' +
                ", allowLocation=" + allowLocation +
                ", allowLocal=" + allowLocal +
                ", allowWeb=" + allowWeb +
                '}';
    }

}
