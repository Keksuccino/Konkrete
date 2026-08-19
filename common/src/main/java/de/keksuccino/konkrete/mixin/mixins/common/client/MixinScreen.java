package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {

    /** @reason Deferred reusable UI work must be extracted after the screen, tooltips, and subtitles so it remains the topmost screen stratum. */
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
    private void after_extractRenderStateWithTooltipAndSubtitles_Konkrete(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        RenderingUtils.executeAndClearDeferredScreenRenderingTasks(graphics, mouseX, mouseY, partialTick);
    }

}
