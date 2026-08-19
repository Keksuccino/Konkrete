package de.keksuccino.konkrete.mixin.support.client;

import de.keksuccino.konkrete.util.rendering.GuiRenderPhaseAction;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

/** Immutable boundary between an extracted GUI action and the draw range preceding it. */
public record RenderPhaseActionEntry(int drawIndex, int order, GuiRenderState.TraverseRange range, GuiRenderPhaseAction action) {
}
