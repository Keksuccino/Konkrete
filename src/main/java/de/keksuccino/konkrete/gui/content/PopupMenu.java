package de.keksuccino.konkrete.gui.content;

@Deprecated
/**
 * Deprecated now. Use ContextMenu instead.
 */
public class PopupMenu extends ContextMenu {

	/**
	 * Deprecated now. Use {@link ContextMenu} instead.
	 */
	public PopupMenu(int width, int buttonHeight, int space) {
		super(width, buttonHeight, space);
	}

	public void addChild(PopupMenu menu) {
		super.addChild(menu);
	}

	public void removeChild(PopupMenu menu) {
		super.removeChild(menu);
	}

}
