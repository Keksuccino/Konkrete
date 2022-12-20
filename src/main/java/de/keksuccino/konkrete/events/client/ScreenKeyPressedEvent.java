package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;

public class ScreenKeyPressedEvent extends EventBase {

    public final int keyCode;
    public final int scanCode;
    public final int modifiers;

    public ScreenKeyPressedEvent(int keyCode, int scanCode, int modifiers) {
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}