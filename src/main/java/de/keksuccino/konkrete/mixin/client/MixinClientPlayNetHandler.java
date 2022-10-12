package de.keksuccino.konkrete.mixin.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.konkrete.command.ClientCommandHandler;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.network.play.server.SCommandListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetHandler.class)
public class MixinClientPlayNetHandler {

    @Shadow public CommandDispatcher<ISuggestionProvider> commands;

    @Inject(at = @At("TAIL"), method = "handleCommands")
    private void onHandleCommands(SCommandListPacket packet, CallbackInfo info) {

        this.commands = ClientCommandHandler.mergeWithServerCommands(this.commands);

    }

}
