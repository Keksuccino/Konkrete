package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;

public class ScreenTickEvent extends EventBase {

    @Override
    public boolean isCancelable() {
        return false;
    }

}