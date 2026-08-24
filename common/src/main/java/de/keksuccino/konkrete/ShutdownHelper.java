package de.keksuccino.konkrete;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShutdownHelper {

    private static final List<Runnable> SHUTDOWN_TASKS = new ArrayList<>();

    public static void registerShutdownTask(@NotNull Runnable task) {
        SHUTDOWN_TASKS.add(task);
    }

    @NotNull
    public static List<Runnable> getShutdownTasks() {
        return new ArrayList<>(SHUTDOWN_TASKS);
    }

}
