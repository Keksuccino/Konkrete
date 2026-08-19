package de.keksuccino.konkrete;

import de.keksuccino.konkrete.networking.PacketsNeoForgeClient;
import de.keksuccino.konkrete.util.reload.KonkreteResourceReload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/** Registers NeoForge callbacks that are legal only on the physical client. */
final class KonkreteNeoForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean registered;

    private KonkreteNeoForgeClientEvents() {}

    static synchronized void registerAll(@NotNull IEventBus modEventBus) {
        if (registered) return;
        KonkreteClient.init();
        PacketsNeoForgeClient.init();
        modEventBus.addListener(KonkreteNeoForgeClientEvents::onAddClientReloadListeners);
        NeoForge.EVENT_BUS.register(new KonkreteNeoForgeClientEvents());
        registered = true;
    }

    private static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        if (KonkreteResourceReload.registerClientReloadListener(KonkreteResourceReload.ClientLoader.NEOFORGE, listener -> event.addListener(KonkreteResourceReload.RELOAD_LISTENER_ID, listener))) {
            LOGGER.info("[KONKRETE] Registered Konkrete's client resource reload listener via NeoForge API");
        }
    }

    /** Advances generic client-thread services at the start of each NeoForge client tick. */
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        KonkreteClient.onPreClientTick();
    }

    /** Releases client-owned services before NeoForge tears down Minecraft infrastructure. */
    @SubscribeEvent
    public void onClientStopping(ClientStoppingEvent event) {
        KonkreteClient.shutdown();
    }
}
