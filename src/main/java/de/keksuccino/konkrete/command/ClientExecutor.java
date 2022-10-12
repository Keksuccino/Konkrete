//TODO übernehmen 1.4.0
package de.keksuccino.konkrete.command;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

public class ClientExecutor {

    protected static List<Runnable> commandQueue = new ArrayList<>();

    public static void init() {
        Konkrete.getEventHandler().registerEventsFrom(ClientExecutor.class);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre e) {

        List<Runnable> queue = new ArrayList<>();
        queue.addAll(commandQueue);
        for (Runnable r : queue) {
            r.run();
            commandQueue.remove(r);
        }

    }

    public static void execute(Runnable command) {
        commandQueue.add(command);
    }

}
