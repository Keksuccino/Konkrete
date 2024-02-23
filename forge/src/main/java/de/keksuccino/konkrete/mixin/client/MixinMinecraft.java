package de.keksuccino.konkrete.mixin.client;

import de.keksuccino.konkrete.events.EventHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private void beforeWrapScreenErrorInTickKonkrete(CallbackInfo info) {
        Screen.wrapScreenError(EventHooks::onScreenTick, "Konkrete Ticking screen", this.getClass().getCanonicalName());
    }

}
