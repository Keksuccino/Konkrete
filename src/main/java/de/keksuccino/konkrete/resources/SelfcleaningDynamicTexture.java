package de.keksuccino.konkrete.resources;

import java.lang.reflect.Field;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;

public class SelfcleaningDynamicTexture extends DynamicTexture {

	public SelfcleaningDynamicTexture(NativeImage nativeImageIn) {
		super(nativeImageIn);
	}
	
	@Override
	public void upload() {
		super.upload();
		
		//Clearing all NativeImage data to free memory
		clearTextureData(this);
	}
	
	/**
	 * Dummy method to avoid NullPointers.
	 */
	@Override
	public NativeImage getPixels() {
		return new NativeImage(0, 0, true);
	}
	
	private static void clearTextureData(DynamicTexture texture) {
		try {
			Field f = ReflectionHelper.findField(DynamicTexture.class, "pixels");
			((NativeImage)f.get(texture)).close();
			f.set(texture, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
