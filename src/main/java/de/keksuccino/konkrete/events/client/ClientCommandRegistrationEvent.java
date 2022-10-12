package de.keksuccino.konkrete.events.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public class ClientCommandRegistrationEvent extends EventBase {

    public final CommandDispatcher<CommandSourceStack> dispatcher;
    public final CommandBuildContext context;

    public ClientCommandRegistrationEvent(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        this.dispatcher = dispatcher;
        this.context = context;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
