package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;

public class ScreenMouseClickedEvent extends EventBase {

    public final double mouseX;
    public final double mouseY;
    public final int mouseButton;

    public ScreenMouseClickedEvent(double mouseX, double mouseY, int mouseButton) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseButton = mouseButton;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
