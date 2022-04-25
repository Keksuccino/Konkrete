package de.keksuccino.konkrete.resources;

import de.keksuccino.konkrete.mixin.mixins.client.IMixinDynamicTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;

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
		try {
			((IMixinDynamicTexture)texture).setPixelsKonkrete(new NativeImage(1, 1, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
