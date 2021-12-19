package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screen.Screen;

public class GuiInitCompletedEvent extends EventBase {
	
	protected Screen screen;
	
	public GuiInitCompletedEvent(Screen screen) {
		this.screen = screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public Screen getGui() {
		return this.screen;
	}
	
}