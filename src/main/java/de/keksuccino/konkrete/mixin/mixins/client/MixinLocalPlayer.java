//TODO übernehmen 1.4.0
package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.command.ClientCommandHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSigner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(at = @At("HEAD"), method = "sendCommand", cancellable = true)
    private void onSendCommand(MessageSigner signer, String s, Component c, CallbackInfo info) {

        if (ClientCommandHandler.executeClientCommand(s)) {
            info.cancel();
        }

    }

}
