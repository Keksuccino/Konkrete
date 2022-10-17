//TODO übernehmen 1.5.3
package de.keksuccino.konkrete.events.client;

import net.minecraftforge.eventbus.api.Event;

public class ScreenCharTypedEvent extends Event {

    public final char character;
    public final int modifiers;

    public ScreenCharTypedEvent(char character, int modifiers) {
        this.character = character;
        this.modifiers = modifiers;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
