package de.keksuccino.konkrete.util.ffmpeg.downloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * UI-independent result emitted when an FFmpeg downloader screen closes.
 *
 * @param outcome screen outcome
 * @param snapshot final observed snapshot
 * @param installation resulting installation when ready
 */
public record FFMPEGDownloaderScreenResult(@NotNull Outcome outcome, @NotNull FFMPEGDownloadSnapshot snapshot, @Nullable FFMPEGInstallation installation) {

    /** Validates the immutable screen result. */
    public FFMPEGDownloaderScreenResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    /** Maps a downloader snapshot to a screen result. */
    @NotNull
    public static FFMPEGDownloaderScreenResult fromSnapshot(@NotNull FFMPEGDownloadSnapshot snapshot) {
        FFMPEGDownloadSnapshot checkedSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        Outcome outcome = switch (checkedSnapshot.getStage()) {
            case COMPLETE -> checkedSnapshot.isSatisfiedByCache() ? Outcome.ALREADY_AVAILABLE : Outcome.INSTALLED;
            case FAILED -> Outcome.FAILED;
            case CANCELLED -> Outcome.CANCELLED;
            case CHECKING, DOWNLOADING, EXTRACTING, VALIDATING -> Outcome.IN_PROGRESS;
            case IDLE -> Outcome.NOT_STARTED;
        };
        return new FFMPEGDownloaderScreenResult(outcome, checkedSnapshot, checkedSnapshot.getInstallation());
    }

    /** Returns whether callers may immediately use FFmpeg. */
    public boolean isReady() {
        return this.outcome == Outcome.INSTALLED || this.outcome == Outcome.ALREADY_AVAILABLE;
    }

    /** Screen-close outcomes. */
    public enum Outcome {
        NOT_STARTED,
        IN_PROGRESS,
        INSTALLED,
        ALREADY_AVAILABLE,
        FAILED,
        CANCELLED
    }

}
