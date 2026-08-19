package de.keksuccino.konkrete;

import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.WebUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/** Registers Fabric callbacks for services owned by a dedicated server process. */
final class KonkreteFabricServerEvents {

    private static boolean registered;

    private KonkreteFabricServerEvents() {}

    static synchronized void registerAll() {
        if (registered) return;
        // An integrated server shares the client process, so its stop event must not terminate client-owned web services.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (!Services.PLATFORM.isOnClient()) WebUtils.shutdown();
        });
        registered = true;
    }
}
