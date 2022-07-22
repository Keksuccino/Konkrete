package de.keksuccino.konkrete.events.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.commands.CommandSourceStack;

public class ClientCommandRegistrationEvent extends EventBase {

    public final CommandDispatcher<CommandSourceStack> dispatcher;

    public ClientCommandRegistrationEvent(CommandDispatcher<CommandSourceStack> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
