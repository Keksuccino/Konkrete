package de.keksuccino.konkrete.resources;

import java.lang.reflect.Field;

import de.keksuccino.konkrete.annotations.OptifineFix;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;

@OptifineFix
//fixes:
//- set 'pixels' to empty image instead of null in clearTextureData()
//- remove override of getPixels() method, because OF seems to inject something into this method
public class SelfcleaningDynamicTexture extends DynamicTexture {

	public SelfcleaningDynamicTexture(NativeImage nativeImageIn) {
		super(nativeImageIn);
	}
	
	@Override
	public void updateDynamicTexture() {
		super.updateDynamicTexture();
		
		//Clearing all NativeImage data to free memory
		clearTextureData(this);
	}
	
	private static void clearTextureData(DynamicTexture texture) {
		try {
			Field f = ReflectionHelper.findField(DynamicTexture.class, "field_110566_b");
			((NativeImage)f.get(texture)).close();
			f.set(texture, new NativeImage(1, 1, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
