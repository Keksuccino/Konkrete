package de.keksuccino.konkrete;

import de.keksuccino.konkrete.networking.PacketsFabric;
import net.fabricmc.api.ModInitializer;

public class KonkreteFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        Konkrete.init();

        PacketsFabric.init();

        KonkreteFabricServerEvents.registerAll();

    }

}
