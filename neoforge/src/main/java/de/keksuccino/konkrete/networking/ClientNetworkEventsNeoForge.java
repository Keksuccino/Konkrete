package de.keksuccino.konkrete.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

final class ClientNetworkEventsNeoForge {

    /** Starts and negotiates one exact client connection. */
    @SubscribeEvent
    public void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        Connection connection = event.getConnection();
        PacketHandler.onClientConnected(connection);
        Minecraft.getInstance().execute(() -> PacketHandler.sendHandshakeToServer(connection));
    }

    /** Cleans one exact client connection. */
    @SubscribeEvent
    public void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PacketHandler.onClientDisconnected(event.getConnection());
    }
}
