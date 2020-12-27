package de.keksuccino.konkrete.resources;

import java.lang.reflect.Field;

import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

public class SelfcleaningDynamicTexture extends NativeImageBackedTexture {

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
	public NativeImage getImage() {
		return new NativeImage(0, 0, true);
	}
	
	private static void clearTextureData(NativeImageBackedTexture texture) {
		try {
			Field f = ReflectionHelper.findField(NativeImageBackedTexture.class, "image", "field_5200");
			((NativeImage)f.get(texture)).close();
			f.set(texture, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
