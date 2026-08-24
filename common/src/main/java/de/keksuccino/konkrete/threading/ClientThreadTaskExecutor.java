package de.keksuccino.konkrete.threading;

import de.keksuccino.konkrete.ShutdownHelper;
import de.keksuccino.konkrete.side.ClientUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientThreadTaskExecutor {

    private static final List<Runnable> QUEUED_TASKS_PRE_CLIENT_TICK = Collections.synchronizedList(new ArrayList<>());
    private static final List<Runnable> QUEUED_TASKS_POST_CLIENT_TICK = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean shuttingDown;

    @ApiStatus.Internal
    public static void init() {
        ShutdownHelper.registerShutdownTask("Konkrete client thread task cleanup", ClientThreadTaskExecutor::shutdown);
    }

    public static void queueForExecution(@NotNull Runnable task, @NotNull ExecuteTiming timing) {
        ClientUtils.assertIsOnClient();
        List<Runnable> queue = timing == ExecuteTiming.PRE_CLIENT_TICK ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK;
        synchronized (queue) {
            if (!shuttingDown) queue.add(task);
        }
    }

    @ApiStatus.Internal
    public static List<Runnable> getAndClearQueue(@NotNull ExecuteTiming timing) {
        List<Runnable> queue = timing == ExecuteTiming.PRE_CLIENT_TICK ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK;
        synchronized (queue) {
            List<Runnable> tasks = new ArrayList<>(queue);
            queue.clear();
            return tasks;
        }
    }

    public static void shutdown() {
        shuttingDown = true;
        synchronized (QUEUED_TASKS_PRE_CLIENT_TICK) {
            QUEUED_TASKS_PRE_CLIENT_TICK.clear();
        }
        synchronized (QUEUED_TASKS_POST_CLIENT_TICK) {
            QUEUED_TASKS_POST_CLIENT_TICK.clear();
        }
    }

    public enum ExecuteTiming {
        PRE_CLIENT_TICK,
        POST_CLIENT_TICK
    }

}
