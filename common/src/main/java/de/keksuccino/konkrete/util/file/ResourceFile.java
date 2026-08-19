package de.keksuccino.konkrete.util.file;

import com.google.common.io.Files;
import de.keksuccino.konkrete.util.file.type.FileMediaType;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.file.type.types.FileTypes;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/** Represents a game-directory-relative file and its registered resource type. */
@SuppressWarnings("unused")
public class ResourceFile {

    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable
    private static volatile File configuredAssetsDirectory;

    /** Normalized game-directory-relative path with a leading slash. */
    protected String shortPath;
    /** Absolute file resolved from the active game directory. */
    protected File file;
    /** Registered type detected from the local filename. */
    protected FileType<?> type;
    /** Optional source-prefix kind retained from construction input. */
    @Nullable
    protected ResourceSourceType resourceSourceType;

    /**
     * Returns a new {@link ResourceFile} instance for the given asset.<br>
     * Asset files must be below the configured asset directory and exist.
     */
    @Nullable
    public static ResourceFile asset(@NotNull File gameDirectoryFile) {
        return asset(gameDirectoryFile.getAbsolutePath());
    }

    /**
     * Returns a new {@link ResourceFile} instance for the given asset.<br>
     * Asset files must be below the configured asset directory and exist.
     */
    @Nullable
    public static ResourceFile asset(@NotNull String gameDirectoryFilePath) {
        ResourceFile resourceFile = of(gameDirectoryFilePath);
        if (!resourceFile.isExistingAsset()) {
            LOGGER.error("[KONKRETE] Asset ResourceFile does not exist or is outside the configured asset directory '{}': {}", getAssetsDirectory(), gameDirectoryFilePath);
            return null;
        }
        return resourceFile;
    }

    /**
     * Returns a new {@link ResourceFile} instance for the given file.<br>
     * The file needs to be in the game instance directory.<br>
     * The file does not need to exist.
     */
    @NotNull
    public static ResourceFile of(@NotNull File gameDirectoryFile) {
        return of(gameDirectoryFile.getAbsolutePath());
    }

    /**
     * Returns a new {@link ResourceFile} instance for the given file.<br>
     * The file needs to be in the game instance directory.<br>
     * The file does not need to exist.
     */
    @NotNull
    public static ResourceFile of(@NotNull String gameDirectoryFilePath) {
        ResourceFile resourceFile = new ResourceFile();
        if (ResourceSourceType.hasSourcePrefix(gameDirectoryFilePath)) {
            resourceFile.resourceSourceType = ResourceSourceType.getSourceTypeOf(gameDirectoryFilePath);
        }
        gameDirectoryFilePath = ResourceSourceType.getWithoutSourcePrefix(gameDirectoryFilePath);
        gameDirectoryFilePath = gameDirectoryFilePath.replace("\\", "/");
        gameDirectoryFilePath = GameDirectoryUtils.getPathWithoutGameDirectory(gameDirectoryFilePath).replace("\\", "/");
        if (!gameDirectoryFilePath.startsWith("/")) {
            gameDirectoryFilePath = "/" + gameDirectoryFilePath;
        }
        if (gameDirectoryFilePath.replace(" ", "").replace("/", "").isEmpty()) {
            gameDirectoryFilePath = "";
        }
        if (gameDirectoryFilePath.startsWith("/./")) {
            gameDirectoryFilePath = gameDirectoryFilePath.substring(2);
        }
        resourceFile.file = new File(GameDirectoryUtils.getGameDirectory(), gameDirectoryFilePath);
        resourceFile.shortPath = gameDirectoryFilePath;
        resourceFile.type = FileTypes.getLocalType(resourceFile.file);
        if (resourceFile.type == null) {
            resourceFile.type = FileTypes.UNKNOWN;
        }
        if (resourceFile.resourceSourceType == null) {
            resourceFile.resourceSourceType = ResourceSourceType.getSourceTypeOf(gameDirectoryFilePath);
        }
        return resourceFile;
    }

    /** Creates an uninitialized instance for the static factories. */
    protected ResourceFile() {
    }

    /** Configures the directory that bounds calls to {@link #asset(File)} and {@link #asset(String)}. */
    public static void setAssetsDirectory(@NotNull File directory) {
        configuredAssetsDirectory = Objects.requireNonNull(directory, "directory").getAbsoluteFile();
    }

    /** Returns the currently configured asset directory. */
    @NotNull
    public static File getAssetsDirectory() {
        File configured = configuredAssetsDirectory;
        return configured != null ? configured : defaultAssetsDirectory();
    }

    /** Restores the default {@code config/konkrete/assets} asset directory. */
    public static void resetAssetsDirectory() {
        configuredAssetsDirectory = null;
    }

    /** Returns the retained source-prefix kind, if one was detected. */
    @Nullable
    public ResourceSourceType getResourceSourceType() {
        return this.resourceSourceType;
    }

    /** Returns the short path prefixed for its resource-source kind when present. */
    @NotNull
    public String getAsResourceSource() {
        String prefix = (this.resourceSourceType != null) ? this.resourceSourceType.getSourcePrefix() : "";
        return prefix + this.getShortPath();
    }

    /** Returns the normalized game-directory-relative path. */
    @NotNull
    public String getShortPath() {
        return this.shortPath;
    }

    /** Returns the backing file's absolute path string. */
    @NotNull
    public String getAbsolutePath() {
        return this.file.getAbsolutePath();
    }

    /** Returns the backing file below the game directory. */
    @NotNull
    public File getFile() {
        return this.file;
    }

    /**
     * Returns the file extension or an empty String if the given file has no extension or is a directory.
     */
    @NotNull
    public String getFileExtension() {
        return Files.getFileExtension(this.shortPath);
    }

    /** Returns the file name without extension. */
    @NotNull
    public String getFileNameWithoutExtension() {
        return Files.getNameWithoutExtension(this.shortPath);
    }

    /** Returns the file name. */
    @NotNull
    public String getFileName() {
        return this.file.getName();
    }

    /** Returns whether the backing path currently exists. */
    public boolean exists() {
        return this.file.exists();
    }

    /**
     * Returns TRUE if the file exists AND is a file, otherwise returns FALSE.
     */
    public boolean isFile() {
        return this.file.isFile();
    }

    /**
     * Returns TRUE if the file exists AND is a directory, otherwise returns FALSE.
     */
    public boolean isDirectory() {
        return this.file.isDirectory();
    }

    /** Returns whether the backing path exists and resolves below the configured asset root. */
    public boolean isExistingAsset() {
        return this.exists() && this.isAsset();
    }

    /**
     * Returns whether this existing file resolves below the configured asset directory.
     * Real paths are intentional so a symlink cannot make an apparent asset escape its configured boundary.
     */
    public boolean isAsset() {
        try {
            Path assetRoot = getAssetsDirectory().toPath().toRealPath();
            Path candidate = this.file.toPath().toRealPath();
            return candidate.startsWith(assetRoot) && !candidate.equals(assetRoot);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Returns the {@link FileType} of the file or {@link FileTypes#UNKNOWN} if the file does not have a known type.
     */
    @NotNull
    public FileType<?> getType() {
        return this.type;
    }

    /** Returns the broad media category of the detected file type. */
    @NotNull
    public FileMediaType getMediaType() {
        return this.type.getMediaType();
    }

    private static File defaultAssetsDirectory() {
        return new File(GameDirectoryUtils.getGameDirectory(), "config/konkrete/assets").getAbsoluteFile();
    }

}
