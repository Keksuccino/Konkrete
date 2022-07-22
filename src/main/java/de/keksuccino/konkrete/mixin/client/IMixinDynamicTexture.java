package de.keksuccino.konkrete.mixin.client;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DynamicTexture.class)
public interface IMixinDynamicTexture {

    @Accessor("pixels") public NativeImage getPixelsKonkrete();

    @Accessor("pixels") public void setPixelsKonkrete(NativeImage pixels);

}
