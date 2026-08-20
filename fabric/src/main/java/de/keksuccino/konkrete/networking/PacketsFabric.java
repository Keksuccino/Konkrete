package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.bridge.BridgeChunkPayload;
import de.keksuccino.konkrete.networking.bridge.BridgeMessageSender;
import de.keksuccino.konkrete.networking.bridge.BridgePacketPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric transport and lifecycle integration for Konkrete networking.
 */
public final class PacketsFabric {

    private static boolean initialized;

    private PacketsFabric() {
    }

    /**
     * Registers common payload types, server receivers, clientbound sending, and server lifecycle callbacks once.
     */
    public static synchronized void init() {
        if (initialized) return;
        FrameworkPackets.registerAll();
        PacketHandler.setSendToClientLogic((player, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(player, BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactPlayer -> ServerPlayNetworking.canSend(exactPlayer, BridgeChunkPayload.TYPE), PacketsFabric::sendToClientIfNegotiated, PacketsFabric::sendToClientIfNegotiated));
        registerBridgePackets();
        registerLifecycleCallbacks();
        initialized = true;
    }

    private static void registerBridgePackets() {
        PayloadTypeRegistry.serverboundPlay().register(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BridgeChunkPayload.TYPE, BridgeChunkPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BridgeChunkPayload.TYPE, BridgeChunkPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BridgePacketPayload.TYPE, (payload, context) -> payload.handle(context.player(), PacketHandler.PacketDirection.TO_SERVER));
        ServerPlayNetworking.registerGlobalReceiver(BridgeChunkPayload.TYPE, (payload, context) -> payload.handle(context.player(), PacketHandler.PacketDirection.TO_SERVER));
    }

    private static void registerLifecycleCallbacks() {
        ServerLifecycleEvents.SERVER_STARTING.register(PacketHandler::onServerStarting);
        // STOPPING clears before player teardown; STOPPED is an intentional idempotent fallback.
        ServerLifecycleEvents.SERVER_STOPPING.register(PacketHandler::onServerStopped);
        ServerLifecycleEvents.SERVER_STOPPED.register(PacketHandler::onServerStopped);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PacketHandler.onServerPlayerConnected(handler.getPlayer());
            PacketHandler.sendHandshakeToClient(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PacketHandler.onServerPlayerDisconnected(handler.getPlayer()));
    }

    private static boolean sendToClientIfNegotiated(ServerPlayer exactPlayer, CustomPacketPayload payload) {
        if (!ServerPlayNetworking.canSend(exactPlayer, payload.type())) return false;
        ServerPlayNetworking.send(exactPlayer, payload);
        return true;
    }

}
