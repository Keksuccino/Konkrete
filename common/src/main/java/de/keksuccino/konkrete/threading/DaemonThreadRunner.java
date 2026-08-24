package de.keksuccino.konkrete.threading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@ApiStatus.Internal
public final class DaemonThreadRunner {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private DaemonThreadRunner() {
    }

    @ApiStatus.Internal
    public static @NotNull Thread startDaemonThread(@NotNull Runnable runnable, @NotNull String roleName) {
        String threadName = "Konkrete-" + Objects.requireNonNull(roleName, "roleName") + "-" + THREAD_COUNTER.incrementAndGet();
        Thread thread = new Thread(Objects.requireNonNull(runnable, "runnable"), threadName);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((failedThread, throwable) -> LOGGER.error("[KONKRETE] Uncaught exception in daemon thread '{}'.", failedThread.getName(), throwable));
        thread.start();
        return thread;
    }

}
