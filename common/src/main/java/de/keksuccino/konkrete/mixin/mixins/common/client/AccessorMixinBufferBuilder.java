package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface AccessorMixinBufferBuilder {

    @Accessor("vertexPointer") long get_vertexPointer_Konkrete();

}
