package de.keksuccino.konkrete.util.ffmpeg.downloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable observable state of one FFmpeg installation operation.
 */
public final class FFMPEGDownloadSnapshot {

    private final Stage stage;
    private final String task;
    @Nullable private final String detail;
    private final double progress;
    private final long downloadedBytes;
    private final long totalBytes;
    @Nullable private final String failureMessage;
    @Nullable private final FFMPEGInstallation installation;
    private final boolean satisfiedByCache;

    /** Creates an immutable, range-normalized operation snapshot. */
    public FFMPEGDownloadSnapshot(@NotNull Stage stage, @NotNull String task, @Nullable String detail, double progress, long downloadedBytes, long totalBytes, @Nullable String failureMessage, @Nullable FFMPEGInstallation installation, boolean satisfiedByCache) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.task = Objects.requireNonNull(task, "task");
        this.detail = detail;
        this.progress = Double.isFinite(progress) ? Math.max(0.0D, Math.min(1.0D, progress)) : 0.0D;
        this.downloadedBytes = Math.max(0L, downloadedBytes);
        this.totalBytes = Math.max(0L, totalBytes);
        this.failureMessage = failureMessage;
        this.installation = installation;
        this.satisfiedByCache = satisfiedByCache;
    }

    /** Returns an idle initial snapshot. */
    @NotNull
    public static FFMPEGDownloadSnapshot idle() {
        return new FFMPEGDownloadSnapshot(Stage.IDLE, "Idle", null, 0.0D, 0L, 0L, null, null, false);
    }

    /** Returns the operation stage. */
    @NotNull
    public Stage getStage() {
        return this.stage;
    }

    /** Returns the primary task message. */
    @NotNull
    public String getTask() {
        return this.task;
    }

    /** Returns optional detail text. */
    @Nullable
    public String getDetail() {
        return this.detail;
    }

    /** Returns normalized progress from zero through one. */
    public double getProgress() {
        return this.progress;
    }

    /** Returns transferred or extracted bytes for the active stage. */
    public long getDownloadedBytes() {
        return this.downloadedBytes;
    }

    /** Returns expected bytes for the active stage, or zero when unknown. */
    public long getTotalBytes() {
        return this.totalBytes;
    }

    /** Returns a terminal failure description. */
    @Nullable
    public String getFailureMessage() {
        return this.failureMessage;
    }

    /** Returns the resulting installation when complete. */
    @Nullable
    public FFMPEGInstallation getInstallation() {
        return this.installation;
    }

    /** Returns whether completion reused an existing cached installation. */
    public boolean isSatisfiedByCache() {
        return this.satisfiedByCache;
    }

    /** Returns whether work is currently active. */
    public boolean isActive() {
        return switch (this.stage) {
            case CHECKING, DOWNLOADING, EXTRACTING, VALIDATING -> true;
            default -> false;
        };
    }

    /** Returns whether the operation reached a final state. */
    public boolean isTerminal() {
        return switch (this.stage) {
            case COMPLETE, FAILED, CANCELLED -> true;
            default -> false;
        };
    }

    /** Installation-operation stages. */
    public enum Stage {
        IDLE,
        CHECKING,
        DOWNLOADING,
        EXTRACTING,
        VALIDATING,
        COMPLETE,
        FAILED,
        CANCELLED
    }

}
