package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screen.Screen;

//TODO implement setGui()
public class GuiOpenEvent extends EventBase {

	private Screen screen; 
	
	public GuiOpenEvent(Screen screen) {
		this.screen = screen;
	}
	
	public Screen getGui() {
		return this.screen;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}

}
