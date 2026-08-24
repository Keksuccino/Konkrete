package de.keksuccino.konkrete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShutdownHelper {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<Pair<String, Runnable>> SHUTDOWN_TASKS = new ArrayList<>();

    public static void registerShutdownTask(@NotNull String name, @NotNull Runnable task) {
        SHUTDOWN_TASKS.add(Pair.of(name, task));
    }

    @ApiStatus.Internal
    public static void runAll() {
        LOGGER.info("[KONKRETE] Running shutdown tasks..");
        for (Pair<String, Runnable> task : SHUTDOWN_TASKS) {
            try {
                task.getSecond().run();
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to run shutdown task: " + task.getFirst(), ex);
            }
        }
        LOGGER.info("[KONKRETE] Finished running shutdown tasks.");
    }

}
