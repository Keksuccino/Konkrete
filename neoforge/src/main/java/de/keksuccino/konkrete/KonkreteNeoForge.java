package de.keksuccino.konkrete;

import de.keksuccino.konkrete.networking.PacketsNeoForge;
import de.keksuccino.konkrete.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod(Konkrete.MOD_ID)
public class KonkreteNeoForge {

    public KonkreteNeoForge(@NotNull IEventBus eventBus) {

        Konkrete.init();

        PacketsNeoForge.init(eventBus);

        if (Services.PLATFORM.isOnClient()) KonkreteNeoForgeClientEvents.registerAll(eventBus);

        KonkreteNeoForgeServerEvents.registerAll();

    }

}
