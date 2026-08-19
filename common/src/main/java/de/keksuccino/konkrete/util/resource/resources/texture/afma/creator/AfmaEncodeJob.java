package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Runs one cancellable AFMA encode operation and publishes its status and progress. */
public class AfmaEncodeJob {

    private final @NotNull AtomicBoolean cancelled = new AtomicBoolean(false);
    private final @NotNull AtomicReference<AfmaEncodeProgress> progress = new AtomicReference<>(AfmaEncodeProgress.idle());
    private volatile @NotNull Status status = Status.RUNNING;
    private volatile @Nullable Throwable failure;
    private volatile @Nullable File outputFile;

    /** Initializes a new {@code AfmaEncodeJob} for AFMA creator use. */
    public AfmaEncodeJob() {
    }

    /** Requests cancellation and publishes the cancelling progress state. */
    public void cancel() {
        this.cancelled.set(true);
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.CANCELLED, "Cancelling AFMA job...", null, this.progress.get().progress()));
    }

    /** Returns whether cancellation requested. */
    public boolean isCancellationRequested() {
        return this.cancelled.get();
    }

    /** Returns the progress used by this AFMA creator instance. */
    public @NotNull AfmaEncodeProgress getProgress() {
        return this.progress.get();
    }

    /** Sets the progress used by subsequent AFMA creator operations. */
    public void setProgress(@NotNull AfmaEncodeProgress progress) {
        this.progress.set(Objects.requireNonNull(progress));
    }

    /** Returns the status used by this AFMA creator instance. */
    public @NotNull Status getStatus() {
        return this.status;
    }

    /** Returns whether running. */
    public boolean isRunning() {
        return this.status == Status.RUNNING;
    }

    /** Returns the failure, or {@code null} when it is not available. */
    public @Nullable Throwable getFailure() {
        return this.failure;
    }

    /** Returns the output file, or {@code null} when it is not available. */
    public @Nullable File getOutputFile() {
        return this.outputFile;
    }

    /** Updates the complete success state in the AFMA creator. */
    public void completeSuccess(@Nullable File outputFile) {
        this.outputFile = outputFile;
        this.status = Status.SUCCEEDED;
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.COMPLETE, "AFMA job finished.", null, 1.0D));
    }

    /** Updates the complete cancelled state in the AFMA creator. */
    public void completeCancelled() {
        this.status = Status.CANCELLED;
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.CANCELLED, "AFMA job cancelled.", null, this.progress.get().progress()));
    }

    /** Updates the complete failure state in the AFMA creator. */
    public void completeFailure(@NotNull Throwable throwable) {
        this.failure = Objects.requireNonNull(throwable);
        this.status = Status.FAILED;
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.FAILED, "AFMA job failed.", throwable.getMessage(), this.progress.get().progress()));
    }

    /** Reports mutable or snapshot {@code Status} state for the AFMA creator. */
    public enum Status {
        /** Indicates that the encode job is still running. */
        RUNNING,
        /** Indicates that the encode job completed successfully. */
        SUCCEEDED,
        /** Indicates that the operation terminated with a failure. */
        FAILED,
        /** Indicates that cancellation terminated the operation. */
        CANCELLED
    }

}
