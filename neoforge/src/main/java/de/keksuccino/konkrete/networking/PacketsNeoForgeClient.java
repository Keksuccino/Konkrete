package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.bridge.BridgeChunkPayload;
import de.keksuccino.konkrete.networking.bridge.BridgeMessageSender;
import de.keksuccino.konkrete.networking.bridge.BridgePacketPayload;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Physical-client-only NeoForge transport and lifecycle integration.
 */
public final class PacketsNeoForgeClient {

    private static boolean initialized;

    private PacketsNeoForgeClient() {
    }

    /**
     * Registers serverbound sending and client connection callbacks once.
     */
    public static synchronized void init() {
        if (initialized) return;
        PacketHandler.setSendToServerLogic((connection, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(connection, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactConnection -> PacketHandlerNeoForge.canSendToServer(BridgeChunkPayload.TYPE, exactConnection), (exactConnection, payload) -> PacketHandlerNeoForge.sendToServer(payload, exactConnection), (exactConnection, payload) -> PacketHandlerNeoForge.sendToServer(payload, exactConnection)));
        NeoForge.EVENT_BUS.register(new ClientNetworkEventsNeoForge());
        initialized = true;
    }

}
