package de.keksuccino.konkrete.command;

import java.util.ArrayList;
import java.util.List;

@Deprecated(forRemoval = true)
public class ClientExecutor {

    protected static final List<Runnable> TASK_QUEUE = new ArrayList<>();

    public static void onClientTick() {
        List<Runnable> queue = new ArrayList<>(TASK_QUEUE);
        for (Runnable r : queue) {
            r.run();
            TASK_QUEUE.remove(r);
        }
    }

    public static void execute(Runnable task) {
        TASK_QUEUE.add(task);
    }

}
