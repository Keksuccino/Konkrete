//TODO übernehmen 1.4.0
package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public class ClientPlayerLoginEvent extends EventBase {

    public final LocalPlayer player;
    public final Connection connection;

    public ClientPlayerLoginEvent(LocalPlayer player, Connection connection) {
        this.player = player;
        this.connection = connection;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
