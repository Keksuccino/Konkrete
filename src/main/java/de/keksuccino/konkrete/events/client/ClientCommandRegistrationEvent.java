//TODO übernehmen 1.5.1
package de.keksuccino.konkrete.events.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.eventbus.api.Event;

public class ClientCommandRegistrationEvent extends Event {

    public final CommandDispatcher<CommandSourceStack> dispatcher;

    public ClientCommandRegistrationEvent(CommandDispatcher<CommandSourceStack> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
