package de.keksuccino.konkrete.gui.screens.popup;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.gui.content.AdvancedButton;

/**
 * Does not work anymore. Don't use this.
 */
@Deprecated(forRemoval = true)
public abstract class Popup {
	
	private boolean displayed = false;
	private int alpha;
	private List<AdvancedButton> buttons = new ArrayList<>();

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	public Popup(int backgroundAlpha) {
		this.alpha = backgroundAlpha;
	}

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	public void render(GuiGraphics graphics, int mouseX, int mouseY, Screen renderIn) {
	}

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	public boolean isDisplayed() {
		return false;
	}

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	public void setDisplayed(boolean b) {
	}

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	public List<AdvancedButton> getButtons() {
		return this.buttons;
	}

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	protected void addButton(AdvancedButton b) {
	}

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	protected void removeButton(AdvancedButton b) {
	}

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	protected void renderButtons(GuiGraphics graphics, int mouseX, int mouseY) {
	}

	/**
	 * Does not work anymore. Don't use this.
	 */
	@Deprecated
	protected void colorizePopupButton(AdvancedButton b) {
	}

}
