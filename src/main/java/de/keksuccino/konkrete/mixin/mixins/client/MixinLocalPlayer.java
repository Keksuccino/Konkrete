package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.command.ClientCommandHandler;
import net.minecraft.client.player.LocalPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    private static final Logger MIXIN_LOGGER = LogManager.getLogger("konkrete/MixinClientPlayerEntity");

    @Inject(at = @At("HEAD"), method = "chat", cancellable = true)
    private void onChat(String msg, CallbackInfo info) {

        if (ClientCommandHandler.executeClientCommand(msg)) {
            info.cancel();
        }

    }

}
