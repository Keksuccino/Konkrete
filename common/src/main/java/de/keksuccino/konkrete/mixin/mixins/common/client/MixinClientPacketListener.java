package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.placeholder.placeholders.player.LastDeathMessageTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    /** @reason Capture the local death message only after vanilla successfully opens the corresponding death screen. */
    @Inject(method = "handlePlayerCombatKill", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", shift = At.Shift.AFTER))
    private void after_setScreen_in_handlePlayerCombatKill_Konkrete(ClientboundPlayerCombatKillPacket packet, CallbackInfo info) {
        LastDeathMessageTracker.record(packet.message());
    }

}
