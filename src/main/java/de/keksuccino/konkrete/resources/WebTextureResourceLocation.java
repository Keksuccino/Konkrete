package de.keksuccino.konkrete.resources;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.keksuccino.konkrete.input.CharacterFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class WebTextureResourceLocation implements ITextureResourceLocation {

	private String url;
	private ResourceLocation location;
	private boolean loaded = false;
	private int width = 0;
	private int height = 0;
	
	public WebTextureResourceLocation(String url) {
		this.url = url;
	}
	
	/**
	 * Loads the web texture to a {@link ResourceLocation}.<br>
	 * The main instance of minecraft's {@link TextureManager} needs to be loaded before calling this method.<br><br>
	 * 
	 * After loading the texture, {@code WebTextureResourceLocation.isReady()} will return true.
	 */
	public void loadTexture() {
		if (this.loaded) {
			return;
		}

		try {
			if (Minecraft.getInstance().getTextureManager() == null) {
				System.out.println("################################ WARNING ################################");
				System.out.println("Can't load texture '" + this.url + "'! Minecraft TextureManager instance not ready yet!");
				return;
			}
			
			URL u = new URL(this.url);
			HttpURLConnection httpcon = (HttpURLConnection) u.openConnection();
		    httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
			InputStream s = httpcon.getInputStream();
			if (s == null) {
				return;
			}
			NativeImage i = NativeImage.read(s);
			this.width = i.getWidth();
			this.height = i.getHeight();
			location = Minecraft.getInstance().getTextureManager().getDynamicTextureLocation(this.filterUrl(url), new SelfcleaningDynamicTexture(i));
			s.close();
			loaded = true;
		} catch (Exception e) {
			System.out.println("######################### ERROR #########################");
			System.out.println("Can't load texture '" + this.url + "'! Invalid URL!");
			System.out.println("#########################################################");
			loaded = false;
			e.printStackTrace();
		}
	}
	
	public ResourceLocation getResourceLocation() {
		return this.location;
	}
	
	public String getURL() {
		return this.url;
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
	
	private String filterUrl(String url) {
		CharacterFilter c = new CharacterFilter();
		c.addAllowedCharacters("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", 
				"o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".");
		return c.filterForAllowedChars(url.toLowerCase());
	}

}
