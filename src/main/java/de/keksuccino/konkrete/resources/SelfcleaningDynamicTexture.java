package de.keksuccino.konkrete.resources;

import java.lang.reflect.Field;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.konkrete.annotations.OptifineFix;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;

//TODO übernehmen
@OptifineFix
//fixes:
//- set 'pixels' to empty image instead of null in clearTextureData()
//- remove override of getPixels() method, because OF seems to inject something into this method
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

	//TODO übernehmen
//	/**
//	 * Dummy method to avoid NullPointers.
//	 */
//	@Override
//	public NativeImage getPixels() {
//		return new NativeImage(0, 0, true);
//	}

	private static void clearTextureData(DynamicTexture texture) {
		try {
			Field f = ReflectionHelper.findField(DynamicTexture.class, "f_117977_");
			((NativeImage)f.get(texture)).close();
			//TODO übernehmen
			f.set(texture, new NativeImage(1, 1, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
