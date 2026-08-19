package de.keksuccino.konkrete.util.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

import javax.annotation.Nullable;

/**
 * Marker for work that must execute between ordered GUI draw ranges instead of during render-state extraction.
 * Implementations intentionally emit no vertices; {@code MixinGuiRenderer} consumes them as render-phase boundaries.
 */
public interface GuiRenderPhaseAction extends GuiElementRenderState {

    /** Executes this action at its ordered GUI render-phase boundary. */
    void executeRender_Konkrete();

    /** Emits no vertices; the renderer consumes this state as an ordered phase boundary. */
    @Override
    default void buildVertices(VertexConsumer vertexConsumer) {
    }

    /** Returns a placeholder GUI pipeline used only for render-state ordering. */
    @Override
    default RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    /** Returns an empty binding because phase actions issue their own render commands. */
    @Override
    default TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    /** Returns no scissor because the action manages its own viewport state. */
    @Override
    @Nullable
    default ScreenRectangle scissorArea() {
        return null;
    }

}
