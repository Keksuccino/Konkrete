//TODO Rewrite implementieren
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
		if (!textures.containsKey(url)) {
			try {
				WebTextureResourceLocation r = new WebTextureResourceLocation(url);
				r.loadTexture();
				if (!r.isReady()) {
					return null;
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
	
//	private static Map<String, ITextureResourceLocation> textures = new HashMap<String, ITextureResourceLocation>();
//	private static Map<String, ExternalGifAnimationRenderer> gifs = new HashMap<String, ExternalGifAnimationRenderer>();
//	
//	@Deprecated
//	public static ExternalTextureResourceLocation getResource(String path) {
//		return getResource(path, ExternalTextureResourceLocation.class);
//	}
//	
//	@Deprecated
//	public static WebTextureResourceLocation getWebResource(String url) {
//		return getResource(url, WebTextureResourceLocation.class);
//	}
//	
//	@Deprecated
//	public static ExternalGifAnimationRenderer getGifResource(String path) {
//		return getResource(path, ExternalGifAnimationRenderer.class);
//	}
//
//	public static boolean isResourceCached(String pathOrUrl) {
//		File f = new File(pathOrUrl);
//		if (textures.containsKey(f.getAbsolutePath())) {
//			return true;
//		}
//		if (textures.containsKey(pathOrUrl)) {
//			return true;
//		}
//		if (gifs.containsKey(f.getAbsolutePath())) {
//			return true;
//		}
//		return false;
//	}
//
//	/**
//	 * Will manually set a resource for the given key.<br><br>
//	 * 
//	 * <b>This is not needed by default!</b><br>
//	 * {@link TextureHandler#getResource(String, Class)} automatically registers and caches textures!
//	 */
//	public static void setResource(String key, ITextureResourceLocation r) {
//		textures.put(key, r);
//	}
//
//	/**
//	 * Will manually set a resource for the given key.<br><br>
//	 * 
//	 * <b>This is not needed by default!</b><br>
//	 * {@link TextureHandler#getResource(String, Class)} automatically registers and caches textures!
//	 */
//	public static void setResource(String key, ExternalGifAnimationRenderer r) {
//		gifs.put(key, r);
//	}
//
//	/**
//	 * Will return the resource for the given path/Url or NULL if the resource does not exist.<br><br>
//	 * 
//	 * The texture key is either the <b>absolute</b> path or the URL of the registered texture.<br><br>
//	 * 
//	 * Valid texture types are:<br>
//	 * - {@link ExternalTextureResourceLocation}<br>
//	 * - {@link WebTextureResourceLocation}<br>
//	 * - {@link ExternalGifAnimationRenderer}
//	 */
//	public static <T> T getResource(String pathOrUrl, Class<T> textureType) {
//		File f = new File(pathOrUrl);
//		
//		if (textureType == ExternalTextureResourceLocation.class) {
//			if (!textures.containsKey(f.getAbsolutePath())) {
//				if (f.exists() && f.isFile()) {
//					ExternalTextureResourceLocation r = new ExternalTextureResourceLocation(f.getAbsolutePath());
//					r.loadTexture();
//					textures.put(f.getAbsolutePath(), r);
//				}
//			}
//			return (T) textures.get(pathOrUrl);
//		}
//		
//		if (textureType == WebTextureResourceLocation.class) {
//			if (!textures.containsKey(pathOrUrl)) {
//				try {
//					WebTextureResourceLocation r = new WebTextureResourceLocation(pathOrUrl);
//					r.loadTexture();
//					if (!r.isReady()) {
//						return null;
//					}
//					textures.put(pathOrUrl, r);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			return (T) textures.get(pathOrUrl);
//		}
//		
//		if (textureType == ExternalGifAnimationRenderer.class) {
//			if (!gifs.containsKey(f.getAbsolutePath())) {
//				if (f.exists() && f.isFile() && f.getPath().toLowerCase().replace(" ", "").endsWith(".gif")) {
//					ExternalGifAnimationRenderer r = new ExternalGifAnimationRenderer(f.getPath(), true, 0, 0, 0, 0);
//					r.prepareAnimation();
//					gifs.put(f.getAbsolutePath(), r);
//				}
//			}
//			return (T) gifs.get(pathOrUrl);
//		}
//		
//		return null;
//	}
//	
//	public static void removeResource(String pathOrUrl) {
//		File f = new File(pathOrUrl);
//		if (textures.containsKey(f.getAbsolutePath())) {
//			textures.remove(f.getAbsolutePath());
//			return;
//		}
//		if (textures.containsKey(pathOrUrl)) {
//			textures.remove(pathOrUrl);
//			return;
//		}
//		if (gifs.containsKey(f.getAbsolutePath())) {
//			gifs.remove(f.getAbsolutePath());
//		}
//	}

}
