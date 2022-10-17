//TODO übernehmen 1.5.3
package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;

public class ScreenCharTypedEvent extends EventBase {

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
