package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(targets = "net.minecraft.client.gui.render.GuiRenderer$Draw")
public interface AccessorMixinGuiRendererDraw {

    @Accessor("draw") StagedVertexBuffer.Draw get_draw_Konkrete();

    @Accessor("pipeline") RenderPipeline get_pipeline_Konkrete();

    @Accessor("textureSetup") TextureSetup get_textureSetup_Konkrete();

    @Accessor("scissorArea") @Nullable ScreenRectangle get_scissorArea_Konkrete();

}
