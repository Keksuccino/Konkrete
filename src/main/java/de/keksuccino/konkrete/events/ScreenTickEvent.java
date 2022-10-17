//TODO übernehmen 1.5.3
package de.keksuccino.konkrete.events;

import net.minecraftforge.eventbus.api.Event;

public class ScreenTickEvent extends Event {

    @Override
    public boolean isCancelable() {
        return false;
    }

}
