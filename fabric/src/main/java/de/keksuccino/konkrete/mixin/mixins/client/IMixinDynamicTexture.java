package de.keksuccino.konkrete.mixin.mixins.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DynamicTexture.class)
public interface IMixinDynamicTexture {

    @Accessor("pixels") public NativeImage getPixelsKonkrete();

    @Accessor("pixels") public void setPixelsKonkrete(NativeImage pixels);

}
