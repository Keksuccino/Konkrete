package de.keksuccino.konkrete.resources;

import net.minecraft.resources.ResourceLocation;

@Deprecated
public interface ITextureResourceLocation {
	
	public void loadTexture();
	
	public ResourceLocation getResourceLocation();
	
	public boolean isReady();
	
	public int getHeight();
	
	public int getWidth();

}
