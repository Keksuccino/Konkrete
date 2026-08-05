package de.keksuccino.persephone;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Persephone.MOD_ID)
public class PersephoneNeoForge {

    public PersephoneNeoForge(IEventBus eventBus) {

        Persephone.init();

    }

}