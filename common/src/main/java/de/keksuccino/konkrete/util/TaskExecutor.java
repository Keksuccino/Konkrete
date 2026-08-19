package de.keksuccino.konkrete.util;

import de.keksuccino.konkrete.util.threading.KonkreteExecutors;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Schedules guarded background work with optional client-thread delivery. */
public class TaskExecutor {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService EXECUTOR = KonkreteExecutors.newScheduledThreadPool(20, "Konkrete-TaskExecutor");

    /** Schedules fixed-rate work; callback exceptions are logged and later repetitions continue. */
    public static void scheduleAtFixedRate(@NotNull Task task, long initialDelay, long period, @NotNull TimeUnit unit, boolean executeInMainThread) {
        ScheduledFuture<?>[] future = new ScheduledFuture[1];
        future[0] = EXECUTOR.scheduleAtFixedRate(() -> {
            final Runnable r = () -> {
                try {
                    ScheduledFuture<?> f = future[0];
                    if (f != null) {
                        task.run(f);
                    }
                } catch (Exception ex) {
                    LOGGER.error("[KONKRETE] Error while trying to execute scheduled (fixed rate) task!", ex);
                }
            };
            if (executeInMainThread) {
                MainThreadTaskExecutor.executeInMainThread(r, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                r.run();
            }
        }, initialDelay, period, unit);
    }

    /** Schedules one delayed callback and logs callback exceptions. */
    public static void schedule(@NotNull Task task, long delay, @NotNull TimeUnit unit, boolean executeInMainThread) {
        ScheduledFuture<?>[] future = new ScheduledFuture[1];
        future[0] = EXECUTOR.schedule(() -> {
            final Runnable r = () -> {
                try {
                    ScheduledFuture<?> f = future[0];
                    if (f != null) {
                        task.run(f);
                    }
                } catch (Exception ex) {
                    LOGGER.error("[KONKRETE] Error while trying to execute scheduled task!", ex);
                }
            };
            if (executeInMainThread) {
                MainThreadTaskExecutor.executeInMainThread(r, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                r.run();
            }
        }, delay, unit);
    }

    /**
     * Schedules cancellable one-shot work. The cancellation guard is checked again inside the main-thread wrapper because
     * the scheduler future is already complete by the time an enqueued callback is eventually drained.
     */
    @NotNull
    public static CancellableTask scheduleCancellable(@NotNull Runnable task, long delay, @NotNull TimeUnit unit, boolean executeInMainThread) {
        CancellableOneShotTask scheduledTask = new CancellableOneShotTask(Objects.requireNonNull(task), executeInMainThread);
        ScheduledFuture<?> future = EXECUTOR.schedule(scheduledTask::dispatch, delay, Objects.requireNonNull(unit));
        scheduledTask.attach(future);
        return scheduledTask;
    }

    /** Submits immediate work in the background or queues its guarded callback for the client thread. */
    public static void execute(@NotNull Runnable task, boolean executeInMainThread) {
        EXECUTOR.execute(() -> {
            final Runnable r = () -> {
                try {
                    task.run();
                } catch (Exception ex) {
                    LOGGER.error("[KONKRETE] Error while trying to execute task!", ex);
                }
            };
            if (executeInMainThread) {
                MainThreadTaskExecutor.executeInMainThread(r, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                r.run();
            }
        });
    }

    /** Runs one scheduled unit of work. */
    @FunctionalInterface
    public interface Task {
        /** Runs the task with its scheduling future. */
        void run(@NotNull ScheduledFuture<?> future);
    }

    /** Controls the lifecycle of one delayed task, including after client-thread handoff. */
    public interface CancellableTask {

        /** Cancels future execution of the task. */
        void cancel();

        /** Returns whether this task has been cancelled. */
        boolean isCancelled();
    }

    static final class CancellableOneShotTask implements CancellableTask {

        private final Runnable task;
        private final boolean executeInMainThread;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile ScheduledFuture<?> future;

        CancellableOneShotTask(@NotNull Runnable task, boolean executeInMainThread) {
            this.task = task;
            this.executeInMainThread = executeInMainThread;
        }

        private void attach(@NotNull ScheduledFuture<?> future) {
            this.future = future;
            if (this.cancelled.get()) future.cancel(false);
        }

        void dispatch() {
            if (this.cancelled.get()) return;
            Runnable guardedTask = () -> {
                if (this.cancelled.get()) return;
                try {
                    this.task.run();
                } catch (Exception ex) {
                    LOGGER.error("[KONKRETE] Error while trying to execute cancellable scheduled task!", ex);
                }
            };
            if (this.executeInMainThread) {
                MainThreadTaskExecutor.executeInMainThread(guardedTask, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                guardedTask.run();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void cancel() {
            if (!this.cancelled.compareAndSet(false, true)) return;
            ScheduledFuture<?> scheduledFuture = this.future;
            if (scheduledFuture != null) scheduledFuture.cancel(false);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCancelled() {
            return this.cancelled.get();
        }
    }

}
