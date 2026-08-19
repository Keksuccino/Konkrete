package de.keksuccino.konkrete.util.ffmpeg.downloader;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Caller-owned downloader storage, network policy and distribution resolver.
 *
 * @param storageDirectory isolated cache/storage root
 * @param userAgent HTTP user agent
 * @param connectTimeout HTTP connect timeout
 * @param readTimeout HTTP read timeout
 * @param validationTimeout binary validation timeout
 * @param distributionResolver resolver for a detected platform
 */
public record FFMPEGDownloadConfiguration(@NotNull Path storageDirectory, @NotNull String userAgent, @NotNull Duration connectTimeout, @NotNull Duration readTimeout, @NotNull Duration validationTimeout, @NotNull DistributionResolver distributionResolver) {

    /** Validates and normalizes a configuration. */
    public FFMPEGDownloadConfiguration {
        storageDirectory = Objects.requireNonNull(storageDirectory, "storageDirectory").toAbsolutePath().normalize();
        userAgent = requireText(userAgent, "userAgent");
        connectTimeout = requirePositive(connectTimeout, "connectTimeout");
        readTimeout = requirePositive(readTimeout, "readTimeout");
        validationTimeout = requirePositive(validationTimeout, "validationTimeout");
        distributionResolver = Objects.requireNonNull(distributionResolver, "distributionResolver");
    }

    /** Creates a configuration using Konkrete's default public distribution resolver and conservative timeouts. */
    @NotNull
    public static FFMPEGDownloadConfiguration defaults(@NotNull Path storageDirectory, @NotNull String userAgent) {
        return new FFMPEGDownloadConfiguration(storageDirectory, userAgent, Duration.ofSeconds(15L), Duration.ofSeconds(30L), Duration.ofSeconds(10L), FFMPEGDownloader::resolveDefaultDistribution);
    }

    private static Duration requirePositive(Duration duration, String name) {
        Duration checked = Objects.requireNonNull(duration, name);
        if (checked.isNegative() || checked.toMillis() < 1L) throw new IllegalArgumentException(name + " must be at least one millisecond");
        return checked;
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name);
        if (checked.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return checked;
    }

    private static String requireId(String value, String name) {
        String checked = requireText(value, name);
        if (!checked.matches("[a-zA-Z0-9._-]+")) throw new IllegalArgumentException(name + " contains unsafe characters: " + checked);
        return checked;
    }

    /** Resolves downloadable artifacts for a normalized platform. */
    @FunctionalInterface
    public interface DistributionResolver {

        /** Returns a distribution or throws when the platform is intentionally unsupported. */
        @NotNull
        Distribution resolve(@NotNull Platform platform);

    }

    /**
     * Download provider and its independently checksummed ZIP artifacts.
     *
     * @param providerId stable provider ID
     * @param providerDisplayName display name
     * @param artifacts non-empty artifact list
     */
    public record Distribution(@NotNull String providerId, @NotNull String providerDisplayName, @NotNull List<Artifact> artifacts) {

        /** Validates and freezes distribution metadata. */
        public Distribution {
            providerId = requireId(providerId, "providerId");
            providerDisplayName = requireText(providerDisplayName, "providerDisplayName");
            artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts"));
            if (artifacts.isEmpty()) throw new IllegalArgumentException("artifacts must not be empty");
            Set<String> artifactIds = new HashSet<>();
            for (Artifact artifact : artifacts) {
                if (!artifactIds.add(artifact.id())) throw new IllegalArgumentException("Duplicate artifact ID: " + artifact.id());
            }
        }

    }

    /**
     * One ZIP archive and its adjacent {@code .sha256} checksum resource.
     *
     * @param id stable artifact ID
     * @param displayName display name
     * @param archiveUrl archive URL
     */
    public record Artifact(@NotNull String id, @NotNull String displayName, @NotNull String archiveUrl) {

        /** Validates artifact metadata. */
        public Artifact {
            id = requireId(id, "id");
            displayName = requireText(displayName, "displayName");
            archiveUrl = requireText(archiveUrl, "archiveUrl");
        }

    }

    /** Normalized platform family. */
    public enum PlatformFamily {
        WINDOWS,
        LINUX,
        MACOS
    }

    /**
     * Normalized platform attributes used by distribution resolvers.
     *
     * @param family OS family
     * @param osSegment provider OS segment
     * @param architectureSegment provider architecture segment
     * @param binarySuffix executable suffix
     * @param id stable platform ID
     * @param displayName display name
     */
    public record Platform(@NotNull PlatformFamily family, @NotNull String osSegment, @NotNull String architectureSegment, @NotNull String binarySuffix, @NotNull String id, @NotNull String displayName) {

        /** Validates normalized platform metadata. */
        public Platform {
            family = Objects.requireNonNull(family, "family");
            osSegment = Objects.requireNonNull(osSegment, "osSegment");
            architectureSegment = Objects.requireNonNull(architectureSegment, "architectureSegment");
            binarySuffix = Objects.requireNonNull(binarySuffix, "binarySuffix");
            id = Objects.requireNonNull(id, "id");
            displayName = Objects.requireNonNull(displayName, "displayName");
        }

    }

}
