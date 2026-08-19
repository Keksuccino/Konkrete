package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface AccessorMixinNativeImage {

    @Accessor("pixels") long get_pixels_Konkrete();

    @Invoker("writeToChannel") boolean invoke_writeToChannel_Konkrete(WritableByteChannel channel);

}
