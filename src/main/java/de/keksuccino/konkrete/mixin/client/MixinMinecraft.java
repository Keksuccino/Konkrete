//---
package de.keksuccino.konkrete.mixin.client;

import de.keksuccino.konkrete.events.client.ScreenTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow public Screen screen;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "tick")
    private void onScreenTickInTick(CallbackInfo info) {
        Screen.wrapScreenError(() -> {
            MinecraftForge.EVENT_BUS.post(new ScreenTickEvent());
        }, "Konkrete Ticking screen", this.getClass().getCanonicalName());
    }

    //TODO experimental
    @Inject(at = @At("RETURN"), method = "setScreen")
    private void onSetScreen(Screen screen, CallbackInfo info) {
        //Fixes custom EditBoxes added to screens not reacting to key repeat events in some cases
        if (screen != null) {
            Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(true);
        } else {
            Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(false);
        }
    }

}