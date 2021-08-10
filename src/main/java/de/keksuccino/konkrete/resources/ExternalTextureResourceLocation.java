package de.keksuccino.konkrete.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public class ExternalTextureResourceLocation implements ITextureResourceLocation {

	private InputStream in;
	
	private String path;
	private Identifier location;
	private boolean loaded = false;
	private int width = 0;
	private int height = 0;
	
	public ExternalTextureResourceLocation(String path) {
		this.path = path;
	}

	public ExternalTextureResourceLocation(InputStream in) {
		this.in = in;
	}
	
	/**
	 * Loads the external texture to a {@link Identifier}.<br>
	 * The main instance of minecraft's {@link TextureManager} needs to be loaded before calling this method.<br><br>
	 * 
	 * After loading the texture, {@link ExternalTextureResourceLocation#isReady()} will return true.
	 */
	public void loadTexture() {
		if (this.loaded) {
			return;
		}
		try {
			if (MinecraftClient.getInstance().getTextureManager() == null) {
				System.out.println("################################ WARNING ################################");
				System.out.println("Can't load texture '" + this.path + "'! Minecraft TextureManager instance not ready yet!");
				return;
			}
			if (this.in == null) {
				File f = new File(path);
				this.in = new FileInputStream(f);
			}
			NativeImage i = NativeImage.read(this.in);
			this.width = i.getWidth();
			this.height = i.getHeight();
			SelfcleaningDynamicTexture tex = new SelfcleaningDynamicTexture(i);
			this.location = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("externaltexture", tex);
			this.in.close();
			loaded = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Identifier getResourceLocation() {
		return this.location;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public boolean isReady() {
		return this.loaded;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public int getWidth() {
		return this.width;
	}

}
