package de.keksuccino.konkrete.util.watermedia;

import de.keksuccino.konkrete.util.threading.KonkreteExecutors;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Polls reflected Watermedia MRLs on one shared daemon executor with a caller-bounded deadline.
 * Future continuations run on that resolver thread unless the caller selects another executor.
 */
public final class WatermediaMrlResolver {

    private static final ScheduledExecutorService EXECUTOR = KonkreteExecutors.newSingleThreadScheduledExecutor("Konkrete-Watermedia-MRL");

    private WatermediaMrlResolver() {}

    /** Begins polling a Watermedia-owned MRL with the configured deadline. */
    @NotNull
    public static Resolution resolve(@NotNull Object mrl) {
        return resolve(mrl, WatermediaIntegrationConfig.getResolutionTimeout());
    }

    /** Begins polling a Watermedia-owned MRL with a positive caller deadline. */
    @NotNull
    public static Resolution resolve(@NotNull Object mrl, @NotNull Duration timeout) {
        Objects.requireNonNull(mrl, "mrl");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        final long timeoutNanos;
        try {
            timeoutNanos = timeout.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("timeout is too large", exception);
        }
        Resolution resolution = new Resolution();
        long startedAtNanos = System.nanoTime();
        ScheduledFuture<?> task = EXECUTOR.scheduleWithFixedDelay(() -> poll(mrl, startedAtNanos, timeoutNanos, resolution), 0L, 25L, TimeUnit.MILLISECONDS);
        resolution.attach(task);
        return resolution;
    }

    private static void poll(Object mrl, long startedAtNanos, long timeoutNanos, Resolution resolution) {
        if (resolution.future.isDone()) return;
        if (WatermediaReflectionBridge.isMrlLoaded(mrl)) resolution.complete(State.LOADED);
        else if (WatermediaReflectionBridge.isMrlFailed(mrl)) resolution.complete(State.FAILED);
        else if (System.nanoTime() - startedAtNanos >= timeoutNanos) resolution.complete(State.TIMED_OUT);
    }

    /** Terminal outcomes reported by a resolution handle. */
    public enum State {
        /** Watermedia reported a loaded source. */
        LOADED,
        /** Watermedia reported a terminal source failure. */
        FAILED,
        /** The caller-supplied deadline elapsed. */
        TIMED_OUT,
        /** The owning resolution handle was closed. */
        CANCELLED
    }

    /** Thread-safe ownership handle for one scheduled resolution; closing it is terminal and idempotent. */
    public static final class Resolution implements AutoCloseable {
        private final CompletableFuture<State> future = new CompletableFuture<>();
        private final AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();

        private Resolution() {}

        /** Returns the single future completed on the resolver thread with this handle's terminal state. */
        @NotNull public CompletableFuture<State> future() { return this.future; }

        private void attach(ScheduledFuture<?> scheduledTask) {
            if (!this.task.compareAndSet(null, scheduledTask)) scheduledTask.cancel(false);
            if (this.future.isDone()) this.cancelTask();
        }

        private void complete(State state) {
            if (this.future.complete(state)) this.cancelTask();
        }

        private void cancelTask() {
            ScheduledFuture<?> scheduledTask = this.task.get();
            if (scheduledTask != null) scheduledTask.cancel(false);
        }

        /** Cancels polling and completes the future with {@link State#CANCELLED}. */
        @Override
        public void close() {
            this.complete(State.CANCELLED);
        }
    }
}
