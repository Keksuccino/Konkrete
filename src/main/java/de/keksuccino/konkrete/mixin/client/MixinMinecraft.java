package de.keksuccino.konkrete.mixin.client;

import de.keksuccino.konkrete.events.ScreenTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), method = "tick")
    private void onScreenTickInTick(CallbackInfo info) {
        Screen.wrapScreenError(() -> {
            MinecraftForge.EVENT_BUS.post(new ScreenTickEvent());
        }, "Konkrete Ticking screen", this.getClass().getCanonicalName());
    }

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
