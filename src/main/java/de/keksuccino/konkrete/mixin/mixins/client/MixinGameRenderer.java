package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.AfterRenderScreenEvent;
import de.keksuccino.konkrete.mixin.MixinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void beforeRenderScreenKonkrete(float f, long l, boolean bl, CallbackInfo ci) {
        //Clear cache (gets set in MixinScreen)
        MixinCache.cachedRenderScreenGuiGraphics = null;
        MixinCache.cachedRenderScreenMouseX = null;
        MixinCache.cachedRenderScreenMouseY = null;
        MixinCache.cachedRenderScreenPartial = null;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;handleDelayedNarration()V"))
    private void afterRenderScreenKonkrete(float f, long l, boolean bl, CallbackInfo ci) {
        if ((MixinCache.cachedRenderScreenGuiGraphics != null) && (Minecraft.getInstance().screen != null)) {
            Konkrete.getEventHandler().callEventsFor(new AfterRenderScreenEvent(Minecraft.getInstance().screen, MixinCache.cachedRenderScreenGuiGraphics, MixinCache.cachedRenderScreenMouseX, MixinCache.cachedRenderScreenMouseY, MixinCache.cachedRenderScreenPartial));
        }
    }

}
