package de.keksuccino.konkrete.events.client.mixins;

import de.keksuccino.konkrete.events.client.mixinbase.MixinCache;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.impl.client.screen.ScreenExtensions;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEvents.class)
public class MixinScreenEvents {

    //Fixes a crash with FancyMenu and the "joinserver" button action. I still don't fucking know why this happens.
    @Inject(at = @At("HEAD"), method = "afterRender", cancellable = true)
    private static void onAfterRender(Screen screen, CallbackInfoReturnable<Event<ScreenEvents.AfterRender>> info) {
        if (screen == null) {
            info.setReturnValue(ScreenExtensions.getExtensions(MixinCache.cachedCurrentScreen).fabric_getAfterRenderEvent());
        }
    }

}
