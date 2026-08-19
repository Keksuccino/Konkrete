package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/** Provides {@code AfmaIoHelper} transformations and validation used by the AFMA codec. */
public final class AfmaIoHelper {

    /** System-property name that overrides the temp dir setting. */
    public static final @NotNull String TEMP_DIR_PROPERTY = "konkrete.afma.temp_dir";
    /** Environment-variable name that overrides the temp dir setting. */
    public static final @NotNull String TEMP_DIR_ENV = "KONKRETE_AFMA_TEMP_DIR";
    /** Default payload chunk cache size used by the AFMA codec. */
    public static final int DEFAULT_PAYLOAD_CHUNK_CACHE_SIZE = 2;
    /** Lowest-priority compatibility alias retained for existing FancyMenu AFMA command-line scripts. */
    private static final @NotNull String LEGACY_TEMP_DIR_PROPERTY = "fancymenu.afma.temp_dir";
    private static final @NotNull String FALLBACK_TEMP_DIR_NAME = "konkrete_afma";

    private AfmaIoHelper() {
    }

    /** Normalizes the entry path for the AFMA codec. */
    @NotNull
    public static String normalizeEntryPath(@NotNull String entryPath) {
        String normalized = entryPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    /** Builds the named temp directory for the AFMA codec. */
    @NotNull
    public static File createNamedTempDirectory(@NotNull String childName) {
        return ensureDirectory(new File(resolveBaseTempDirectory(), childName));
    }

    /** Configures the base temp directory for the AFMA codec. */
    public static void configureBaseTempDirectory(@NotNull File baseTempDirectory) {
        File configuredDirectory = ensureDirectory(Objects.requireNonNull(baseTempDirectory));
        System.setProperty(TEMP_DIR_PROPERTY, configuredDirectory.getAbsolutePath());
    }

    /** Resolves the base temp directory for the AFMA codec. */
    @NotNull
    protected static File resolveBaseTempDirectory() {
        // The legacy property is read last so existing standalone AFMA tooling keeps working while the new Konkrete property wins deterministically.
        String configuredPath = firstNonBlank(System.getProperty(TEMP_DIR_PROPERTY), firstNonBlank(System.getenv(TEMP_DIR_ENV), System.getProperty(LEGACY_TEMP_DIR_PROPERTY)));
        if (configuredPath != null) {
            return ensureDirectory(new File(configuredPath));
        }
        String javaTemp = firstNonBlank(System.getProperty("java.io.tmpdir"), ".");
        return ensureDirectory(new File(javaTemp, FALLBACK_TEMP_DIR_NAME));
    }

    /** Ensures the directory is valid for the AFMA codec. */
    @NotNull
    protected static File ensureDirectory(@NotNull File directory) {
        try {
            Files.createDirectories(directory.toPath());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create AFMA temp directory: " + directory.getAbsolutePath(), ex);
        }
        return directory;
    }

    /** Returns the first non blank produced by the AFMA codec. */
    @Nullable
    protected static String firstNonBlank(@Nullable String first, @Nullable String second) {
        if ((first != null) && !first.isBlank()) {
            return first;
        }
        if ((second != null) && !second.isBlank()) {
            return second;
        }
        return null;
    }

}
