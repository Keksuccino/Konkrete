package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.command.ClientCommandHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(at = @At("HEAD"), method = "sendCommand", cancellable = true)
    private void onSendCommand(String s, Component component, CallbackInfo info) {

        if (ClientCommandHandler.executeClientCommand(s)) {
            info.cancel();
        }

    }

    @Inject(at = @At("HEAD"), method = "commandUnsigned", cancellable = true)
    private void onSendCommandUnsigned(String s, CallbackInfoReturnable<Boolean> info) {

        if (ClientCommandHandler.executeClientCommand(s)) {
            info.setReturnValue(true);
            info.cancel();
        }

    }

}
