package de.keksuccino.konkrete.mixin.mixins.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.command.ClientCommandHandler;
import de.keksuccino.konkrete.events.client.ClientPlayerLoginEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Shadow private CommandDispatcher<SharedSuggestionProvider> commands;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetPos()V", shift = At.Shift.AFTER), method = "handleLogin")
    private void onResetPosInHandleLogin(ClientboundLoginPacket clientboundLoginPacket, CallbackInfo info) {

        Konkrete.getEventHandler().callEventsFor(new ClientPlayerLoginEvent(Minecraft.getInstance().player, Minecraft.getInstance().getConnection().getConnection()));

    }

    @Inject(at = @At("TAIL"), method = "handleCommands")
    private void onHandleCommands(ClientboundCommandsPacket packet, CallbackInfo info) {

        this.commands = ClientCommandHandler.mergeWithServerCommands(this.commands);

    }

}
