package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fires after the initial Screen.init() call when using Minecraft.setScreen to open a new GUI.
 */
public class GuiOpenedEvent extends EventBase {

	private Screen screen;

	/**
	 * Fires after the initial Screen.init() call when using Minecraft.setScreen to open a new GUI.
	 */
	public GuiOpenedEvent(Screen screen) {
		this.screen = screen;
	}
	
	public Screen getGui() {
		return this.screen;
	}
	
	public void setGui(Screen s) {
		this.screen = s;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}

}
