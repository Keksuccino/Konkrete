package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphicsExtractor.class)
public interface AccessorMixinGuiGraphicsExtractor {

    @Accessor("guiRenderState") GuiRenderState get_guiRenderState_Konkrete();

    @Accessor("scissorStack") GuiGraphicsExtractor.ScissorStack get_scissorStack_Konkrete();

}
