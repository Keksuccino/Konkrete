package de.keksuccino.konkrete.util.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Queues work for the Minecraft client thread. */
public class MainThreadTaskExecutor {

    private static final List<Runnable> QUEUED_TASKS_PRE_CLIENT_TICK = Collections.synchronizedList(new ArrayList<>());
    private static final List<Runnable> QUEUED_TASKS_POST_CLIENT_TICK = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean shuttingDown;

    /**
     * Queues a task for the selected client-tick phase; tasks submitted after shutdown are discarded.
     *
     * @throws NullPointerException when the task or timing is {@code null}
     */
    public static void executeInMainThread(Runnable task, ExecuteTiming when) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(when, "when");
        List<Runnable> queue = when == ExecuteTiming.PRE_CLIENT_TICK ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK;
        synchronized (queue) {
            if (!shuttingDown) queue.add(task);
        }
    }

    /** Atomically drains tasks for one non-null client-tick phase and returns them in submission order. */
    public static List<Runnable> getAndClearQueue(ExecuteTiming executeTiming) {
        Objects.requireNonNull(executeTiming, "executeTiming");
        List<Runnable> queue = executeTiming == ExecuteTiming.PRE_CLIENT_TICK ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK;
        synchronized (queue) {
            List<Runnable> tasks = new ArrayList<>(queue);
            queue.clear();
            return tasks;
        }
    }

    /** Permanently stops admission and clears both queues; repeated calls are safe. */
    public static void shutdown() {
        shuttingDown = true;
        synchronized (QUEUED_TASKS_PRE_CLIENT_TICK) {
            QUEUED_TASKS_PRE_CLIENT_TICK.clear();
        }
        synchronized (QUEUED_TASKS_POST_CLIENT_TICK) {
            QUEUED_TASKS_POST_CLIENT_TICK.clear();
        }
    }

    /** Selects whether a handler runs before or after its target call. */
    public enum ExecuteTiming {

        /** Runs before the client tick. */
        PRE_CLIENT_TICK,
        /** Runs after the client tick. */
        POST_CLIENT_TICK

    }

}
