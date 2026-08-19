package de.keksuccino.konkrete.util.resource;

import org.jetbrains.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

/** Defines the closeable asynchronous lifecycle shared by all loaded resources. */
public interface Resource extends Closeable {

    /**
     * Opens a new stream over this resource when its implementation supports byte access.
     * The caller owns a non-null returned stream and must close it; implementations return {@code null} when byte access is unsupported.
     */
    @Nullable
    InputStream open() throws IOException;

    /**
     * The resource is considered ready once all important variables are set to real non-placeholder values.
     */
    boolean isReady();

    /**
     * Should only return TRUE if all asynchronous loading tasks completed successfully.<br>
     * {@link Resource#isReady()} should always return TRUE as well if this method returns TRUE.
     */
    boolean isLoadingCompleted();

    /**
     * Should only return TRUE if loading failed in some way, making it impossible for the {@link Resource} to correctly finish loading.<br>
     * It is possible that {@link Resource#isReady()} and this method both return TRUE.
     */
    boolean isLoadingFailed();

    /**
     * Cooperatively waits until {@link #isReady()} becomes {@code true}.
     * A non-positive timeout, expiry, or thread interruption returns silently without signalling success; callers must inspect {@link #isReady()} afterwards.
     * Interruption is observed without clearing the thread's interrupted status.
     */
    default void waitForReady(long timeoutMs) {
        awaitState(this::isReady, timeoutMs);
    }

    /**
     * Cooperatively waits until loading completes successfully or fails.
     * A non-positive timeout, expiry, or thread interruption returns silently without signalling the outcome; callers must inspect the loading-state methods afterwards.
     * Interruption is observed without clearing the thread's interrupted status.
     */
    default void waitForLoadingCompletedOrFailed(long timeoutMs) {
        awaitState(() -> this.isLoadingCompleted() || this.isLoadingFailed(), timeoutMs);
    }

    /** Returns whether {@link #close()} has completed and no live handles remain. */
    boolean isClosed();

    private static void awaitState(BooleanSupplier completed, long timeoutMs) {
        if (timeoutMs <= 0L || completed.getAsBoolean()) return;
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        long started = System.nanoTime();
        while (!completed.getAsBoolean()) {
            if (Thread.currentThread().isInterrupted()) return;
            long elapsed = System.nanoTime() - started;
            if (elapsed >= timeoutNanos) return;
            LockSupport.parkNanos(Math.min(TimeUnit.MILLISECONDS.toNanos(1L), timeoutNanos - elapsed));
        }
    }

}
