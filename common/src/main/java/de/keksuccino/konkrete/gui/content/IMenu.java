package de.keksuccino.konkrete.gui.content;

@Deprecated(forRemoval = true)
public interface IMenu {
	
	public void setUseable(boolean b);
	
	public boolean isUseable();
	
	public void closeMenu();
	
	public boolean isOpen();

}
