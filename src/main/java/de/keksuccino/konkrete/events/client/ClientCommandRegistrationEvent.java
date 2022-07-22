package de.keksuccino.konkrete.events.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraftforge.eventbus.api.Event;

public class ClientCommandRegistrationEvent extends Event {

    public final CommandDispatcher<CommandSource> dispatcher;

    public ClientCommandRegistrationEvent(CommandDispatcher<CommandSource> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
