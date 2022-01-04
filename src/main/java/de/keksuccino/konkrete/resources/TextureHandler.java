package de.keksuccino.konkrete.resources;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;

public class TextureHandler {
	
	private static Map<String, ITextureResourceLocation> textures = new HashMap<String, ITextureResourceLocation>();
	private static Map<String, ExternalGifAnimationRenderer> gifs = new HashMap<String, ExternalGifAnimationRenderer>();
	
	public static ExternalTextureResourceLocation getResource(String path) {
		File f = new File(path);
		if (!textures.containsKey(f.getAbsolutePath())) {
			if (f.exists() && f.isFile()) {
				ExternalTextureResourceLocation r = new ExternalTextureResourceLocation(f.getAbsolutePath());
				r.loadTexture();
				textures.put(f.getAbsolutePath(), r);
				return r;
			}
			return null;
		}
		return (ExternalTextureResourceLocation) textures.get(f.getAbsolutePath());
	}

	public static WebTextureResourceLocation getWebResource(String url) {
		return getWebResource(url, true);
	}

	public static WebTextureResourceLocation getWebResource(String url, boolean loadTexture) {
		if (!textures.containsKey(url)) {
			try {
				WebTextureResourceLocation r = new WebTextureResourceLocation(url);
				if (loadTexture) {
					r.loadTexture();
					if (!r.isReady()) {
						return null;
					}
				}
				textures.put(url, r);
				return r;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		return (WebTextureResourceLocation) textures.get(url);
	}

	public static ExternalGifAnimationRenderer getGifResource(String path) {
		File f = new File(path);
		if (!gifs.containsKey(f.getAbsolutePath())) {
			if (f.exists() && f.isFile() && f.getPath().toLowerCase().replace(" ", "").endsWith(".gif")) {
				ExternalGifAnimationRenderer r = new ExternalGifAnimationRenderer(f.getPath(), true, 0, 0, 0, 0);
				r.prepareAnimation();
				gifs.put(f.getAbsolutePath(), r);
				return r;
			}
			return null;
		}
		return gifs.get(f.getAbsolutePath());
	}
	
	public static void removeResource(String path) {
		File f = new File(path);
		if (textures.containsKey(f.getAbsolutePath())) {
			textures.remove(f.getAbsolutePath());
		}
	}

}
