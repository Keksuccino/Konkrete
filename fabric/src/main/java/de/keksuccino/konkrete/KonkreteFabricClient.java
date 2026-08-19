package de.keksuccino.konkrete;

import de.keksuccino.konkrete.networking.PacketsFabricClient;
import net.fabricmc.api.ClientModInitializer;

/** Fabric's client-only Konkrete entrypoint. */
public final class KonkreteFabricClient implements ClientModInitializer {

    /** Initializes common client systems before Minecraft constructs its resource manager. */
    @Override
    public void onInitializeClient() {
        KonkreteClient.init();
        PacketsFabricClient.init();
        KonkreteFabricClientEvents.registerAll();
    }
}
