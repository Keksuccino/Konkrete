package de.keksuccino.konkrete.util.ffmpeg.downloader;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

/**
 * Validated FFmpeg/ffprobe installation and its provider metadata.
 */
public final class FFMPEGInstallation {

    private final File installDirectory;
    private final File ffmpegBinary;
    private final File ffprobeBinary;
    private final String platformId;
    private final String providerId;
    private final String ffmpegVersionLine;
    private final String ffprobeVersionLine;

    /**
     * Creates installation metadata.
     *
     * @param installDirectory installation root
     * @param ffmpegBinary FFmpeg binary
     * @param ffprobeBinary ffprobe binary
     * @param platformId normalized platform identifier
     * @param providerId distribution provider identifier
     * @param ffmpegVersionLine validated FFmpeg version line
     * @param ffprobeVersionLine validated ffprobe version line
     */
    public FFMPEGInstallation(@NotNull File installDirectory, @NotNull File ffmpegBinary, @NotNull File ffprobeBinary, @NotNull String platformId, @NotNull String providerId, @NotNull String ffmpegVersionLine, @NotNull String ffprobeVersionLine) {
        this.installDirectory = Objects.requireNonNull(installDirectory, "installDirectory");
        this.ffmpegBinary = Objects.requireNonNull(ffmpegBinary, "ffmpegBinary");
        this.ffprobeBinary = Objects.requireNonNull(ffprobeBinary, "ffprobeBinary");
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.ffmpegVersionLine = Objects.requireNonNull(ffmpegVersionLine, "ffmpegVersionLine");
        this.ffprobeVersionLine = Objects.requireNonNull(ffprobeVersionLine, "ffprobeVersionLine");
    }

    /** Returns the installation root. */
    @NotNull
    public File getInstallDirectory() {
        return this.installDirectory;
    }

    /** Returns the FFmpeg binary. */
    @NotNull
    public File getFfmpegBinary() {
        return this.ffmpegBinary;
    }

    /** Returns the ffprobe binary. */
    @NotNull
    public File getFfprobeBinary() {
        return this.ffprobeBinary;
    }

    /** Returns the normalized platform identifier. */
    @NotNull
    public String getPlatformId() {
        return this.platformId;
    }

    /** Returns the distribution provider identifier. */
    @NotNull
    public String getProviderId() {
        return this.providerId;
    }

    /** Returns the validated FFmpeg version line. */
    @NotNull
    public String getFfmpegVersionLine() {
        return this.ffmpegVersionLine;
    }

    /** Returns the validated ffprobe version line. */
    @NotNull
    public String getFfprobeVersionLine() {
        return this.ffprobeVersionLine;
    }

    /** Returns whether both binaries and the installation root currently exist. */
    public boolean isValid() {
        return this.installDirectory.isDirectory() && this.ffmpegBinary.isFile() && this.ffprobeBinary.isFile();
    }

}
