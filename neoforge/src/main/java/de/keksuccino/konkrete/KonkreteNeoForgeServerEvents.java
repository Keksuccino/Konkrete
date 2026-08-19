package de.keksuccino.konkrete;

import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.WebUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Registers NeoForge callbacks for services owned by a dedicated server process. */
final class KonkreteNeoForgeServerEvents {

    private static boolean registered;

    private KonkreteNeoForgeServerEvents() {}

    static synchronized void registerAll() {
        if (registered) return;
        NeoForge.EVENT_BUS.register(new KonkreteNeoForgeServerEvents());
        registered = true;
    }

    /** Stops process-wide web services only when the stopping server owns the whole process. */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (!Services.PLATFORM.isOnClient()) WebUtils.shutdown();
    }
}
