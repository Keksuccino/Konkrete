//TODO übernehmen 1.5.3
package de.keksuccino.konkrete.events.client;

import net.minecraftforge.eventbus.api.Event;

public class ScreenMouseClickedEvent extends Event {

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
