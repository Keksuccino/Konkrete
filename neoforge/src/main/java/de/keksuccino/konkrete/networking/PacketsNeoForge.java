package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.bridge.BridgeChunkPayload;
import de.keksuccino.konkrete.networking.bridge.BridgeMessageSender;
import de.keksuccino.konkrete.networking.bridge.BridgePacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * NeoForge transport and lifecycle integration for Konkrete networking.
 */
public final class PacketsNeoForge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean initialized;

    private PacketsNeoForge() {
    }

    /**
     * Registers optional payloads, senders, and exact-connection lifecycle callbacks once.
     *
     * @param modEventBus Konkrete's mod event bus
     */
    public static synchronized void init(@NotNull IEventBus modEventBus) {
        if (initialized) return;
        FrameworkPackets.registerAll();
        Objects.requireNonNull(modEventBus).addListener(PacketsNeoForge::registerBridgePackets);
        PacketHandler.setSendToClientLogic((player, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(player, BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactPlayer -> PacketHandlerNeoForge.canSendToClient(BridgeChunkPayload.TYPE, exactPlayer), (exactPlayer, payload) -> PacketHandlerNeoForge.sendToClient(payload, exactPlayer), (exactPlayer, payload) -> PacketHandlerNeoForge.sendToClient(payload, exactPlayer)));
        NeoForge.EVENT_BUS.register(new NetworkEventsNeoForge());
        initialized = true;
    }

    private static void registerBridgePackets(RegisterPayloadHandlersEvent event) {
        // Optional registration is required so vanilla or non-Konkrete peers can still connect.
        PayloadRegistrar registrar = event.registrar("konkrete").optional();
        registrar.playBidirectional(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC, PacketsNeoForge::handleServerboundBridgePacket, PacketsNeoForge::handleClientboundBridgePacket);
        registrar.playBidirectional(BridgeChunkPayload.TYPE, BridgeChunkPayload.CODEC, PacketsNeoForge::handleServerboundBridgeChunk, PacketsNeoForge::handleClientboundBridgeChunk);
    }

    private static void handleServerboundBridgePacket(BridgePacketPayload payload, IPayloadContext context) {
        try {
            if (context.player() instanceof ServerPlayer sender) payload.handle(sender, PacketHandler.PacketDirection.TO_SERVER);
        } catch (RuntimeException ex) {
            LOGGER.error("[KONKRETE] Failed to handle NeoForge bridge packet.", ex);
        }
    }

    private static void handleClientboundBridgePacket(BridgePacketPayload payload, IPayloadContext context) {
        try {
            payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.connection());
        } catch (RuntimeException ex) {
            LOGGER.error("[KONKRETE] Failed to handle NeoForge bridge packet.", ex);
        }
    }

    private static void handleServerboundBridgeChunk(BridgeChunkPayload payload, IPayloadContext context) {
        try {
            if (context.player() instanceof ServerPlayer sender) payload.handle(sender, PacketHandler.PacketDirection.TO_SERVER);
        } catch (RuntimeException ex) {
            LOGGER.error("[KONKRETE] Failed to handle NeoForge bridge chunk.", ex);
        }
    }

    private static void handleClientboundBridgeChunk(BridgeChunkPayload payload, IPayloadContext context) {
        try {
            payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.connection());
        } catch (RuntimeException ex) {
            LOGGER.error("[KONKRETE] Failed to handle NeoForge bridge chunk.", ex);
        }
    }
}
