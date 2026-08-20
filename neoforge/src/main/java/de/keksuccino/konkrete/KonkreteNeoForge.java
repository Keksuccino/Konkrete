package de.keksuccino.konkrete;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Konkrete.MOD_ID)
public class KonkreteNeoForge {

    public KonkreteNeoForge(IEventBus eventBus) {

        Konkrete.init();

    }

}