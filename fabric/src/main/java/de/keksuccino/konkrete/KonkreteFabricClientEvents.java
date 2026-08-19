package de.keksuccino.konkrete;

import de.keksuccino.konkrete.util.reload.KonkreteResourceReload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Registers Fabric callbacks that are legal only on the physical client. */
final class KonkreteFabricClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean registered;

    private KonkreteFabricClientEvents() {}

    static synchronized void registerAll() {
        if (registered) return;
        if (KonkreteResourceReload.registerClientReloadListener(KonkreteResourceReload.ClientLoader.FABRIC, listener -> ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(KonkreteResourceReload.RELOAD_LISTENER_ID, listener))) {
            LOGGER.info("[KONKRETE] Registered Konkrete's client resource reload listener via Fabric API");
        }
        ClientTickEvents.START_CLIENT_TICK.register(client -> KonkreteClient.onPreClientTick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> KonkreteClient.shutdown());
        registered = true;
    }
}
