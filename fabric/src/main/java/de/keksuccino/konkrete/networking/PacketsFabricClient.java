package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.bridge.BridgeChunkPayload;
import de.keksuccino.konkrete.networking.bridge.BridgeMessageSender;
import de.keksuccino.konkrete.networking.bridge.BridgePacketPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Physical-client-only Fabric transport and lifecycle integration.
 */
public final class PacketsFabricClient {

    private static boolean initialized;

    private PacketsFabricClient() {
    }

    /**
     * Registers client receivers, serverbound sending, and client connection callbacks once.
     */
    public static synchronized void init() {
        if (initialized) return;
        PacketsFabric.init();
        PacketHandler.setSendToServerLogic((connection, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(connection, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactConnection -> canSend(exactConnection, BridgeChunkPayload.TYPE), PacketsFabricClient::sendIfNegotiated, PacketsFabricClient::sendIfNegotiated));
        ClientPlayNetworking.registerGlobalReceiver(BridgePacketPayload.TYPE, (payload, context) -> payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.player().connection.getConnection()));
        ClientPlayNetworking.registerGlobalReceiver(BridgeChunkPayload.TYPE, (payload, context) -> payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.player().connection.getConnection()));
        ClientPlayConnectionEvents.JOIN.register((listener, sender, minecraft) -> {
            Connection connection = listener.getConnection();
            PacketHandler.onClientConnected(connection);
            minecraft.execute(() -> PacketHandler.sendHandshakeToServer(connection));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((listener, minecraft) -> PacketHandler.onClientDisconnected(listener.getConnection()));
        initialized = true;
    }

    private static boolean canSend(Connection exactConnection, CustomPacketPayload.Type<?> type) {
        if (Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().getConnection().getConnection() != exactConnection) return false;
        return ClientPlayNetworking.canSend(type);
    }

    private static boolean sendIfNegotiated(Connection exactConnection, CustomPacketPayload payload) {
        if (!canSend(exactConnection, payload.type())) return false;
        exactConnection.send(ClientPlayNetworking.createServerboundPacket(payload));
        return true;
    }

}
