package de.keksuccino.konkrete.resources;

import net.minecraft.util.Identifier;

public interface ITextureResourceLocation {
	
	public void loadTexture();
	
	public Identifier getResourceLocation();
	
	public boolean isReady();
	
	public int getHeight();
	
	public int getWidth();

}
