//TODO übernehmen 1.5.1
package de.keksuccino.konkrete.mixin.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface IMixinClientPacketListener {

    @Accessor("commands") public CommandDispatcher<SharedSuggestionProvider> getCommandsKonkrete();

    @Accessor("commands") public void setCommandsKonkrete(CommandDispatcher<SharedSuggestionProvider> commands);

}
