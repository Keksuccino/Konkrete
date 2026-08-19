package de.keksuccino.konkrete.networking;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

final class NetworkEventsNeoForge {

    /** Starts and negotiates one exact player connection. */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketHandler.onServerPlayerConnected(player);
            PacketHandler.sendHandshakeToClient(player);
        }
    }

    /** Cleans one exact player connection. */
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PacketHandler.onServerPlayerDisconnected(player);
    }

    /** Starts one exact server lifecycle. */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        PacketHandler.onServerStarting(event.getServer());
    }

    /** Clears server state before player teardown. */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PacketHandler.onServerStopped(event.getServer());
    }

    /** Repeats idempotent cleanup for partial shutdown paths. */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        PacketHandler.onServerStopped(event.getServer());
    }
}
