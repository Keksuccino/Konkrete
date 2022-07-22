package de.keksuccino.konkrete.resources;

import de.keksuccino.konkrete.annotations.OptifineFix;
import de.keksuccino.konkrete.mixin.client.IMixinDynamicTexture;
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
	public void upload() {
		super.upload();
		
		//Clearing all NativeImage data to free memory
		clearTextureData(this);
	}
	
	private static void clearTextureData(DynamicTexture texture) {
		((IMixinDynamicTexture)texture).setPixelsKonkrete(new NativeImage(1, 1, true));
	}

}
