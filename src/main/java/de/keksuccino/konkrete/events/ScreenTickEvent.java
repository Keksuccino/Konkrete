package de.keksuccino.konkrete.events;

import net.minecraftforge.eventbus.api.Event;

public class ScreenTickEvent extends Event {

    public ScreenTickEvent() {
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
