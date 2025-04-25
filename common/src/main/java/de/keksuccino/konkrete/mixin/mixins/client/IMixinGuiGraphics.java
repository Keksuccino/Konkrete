package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphics.class)
public interface IMixinGuiGraphics {

    @Accessor("bufferSource") MultiBufferSource.BufferSource get_bufferSource_Konkrete();

}
