package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.bridge.BridgeChunkPayload;
import de.keksuccino.konkrete.networking.bridge.BridgeChunkReassembler;
import de.keksuccino.konkrete.networking.bridge.BridgeMessageSender;
import de.keksuccino.konkrete.networking.bridge.BridgeProtocol;
import de.keksuccino.konkrete.networking.packets.handshake.HandshakePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * Loader-neutral lifecycle, negotiation, dispatch, and sending API for logical packets.
 */
public final class PacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final NetworkCapabilityLifecycle NETWORK_CAPABILITIES = new NetworkCapabilityLifecycle();
    private static final ServerHandshakeNegotiationTracker SERVER_HANDSHAKE_NEGOTIATIONS = new ServerHandshakeNegotiationTracker();
    private static final BridgeChunkReassembler BRIDGE_REASSEMBLER = new BridgeChunkReassembler();

    @Nullable private static volatile BridgeSendLogic<Connection> sendToServerLogic;
    @Nullable private static volatile BridgeSendLogic<ServerPlayer> sendToClientLogic;

    private PacketHandler() {
    }

    /**
     * Begins an exact client play session and clears any replaced session state.
     *
     * @param connection the new play connection
     */
    public static void onClientConnected(@NotNull Connection connection) {
        PacketRegistry.freezeRegistrations();
        Connection exactConnection = Objects.requireNonNull(connection);
        if (NETWORK_CAPABILITIES.beginClientSession(exactConnection)) BRIDGE_REASSEMBLER.beginClientSession(exactConnection);
    }

    /**
     * Ends only the matching client play session.
     *
     * @param connection the disconnected connection, or {@code null} when a loader cannot identify it
     */
    public static void onClientDisconnected(@Nullable Connection connection) {
        // A null or delayed logout must not erase a replacement connection's negotiated state.
        if (NETWORK_CAPABILITIES.endClientSession(connection)) BRIDGE_REASSEMBLER.endSession(connection);
    }

    /**
     * Begins capability tracking for one exact server instance.
     *
     * @param server the starting server
     */
    public static void onServerStarting(@NotNull MinecraftServer server) {
        PacketRegistry.freezeRegistrations();
        NETWORK_CAPABILITIES.beginServerSession(Objects.requireNonNull(server));
    }

    /**
     * Begins capability tracking for one player's exact packet listener.
     *
     * @param player the connected player
     */
    public static void onServerPlayerConnected(@NotNull ServerPlayer player) {
        PacketRegistry.freezeRegistrations();
        Objects.requireNonNull(player);
        MinecraftServer server = getServer(player);
        Object connection = Objects.requireNonNull(player.connection);
        if (NETWORK_CAPABILITIES.beginServerConnection(server, connection)) BRIDGE_REASSEMBLER.beginServerConnection(server, connection);
    }

    /**
     * Removes negotiation and chunk state for one player's exact packet listener.
     *
     * @param player the disconnecting player
     */
    public static void onServerPlayerDisconnected(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        Object connection = Objects.requireNonNull(player.connection);
        NETWORK_CAPABILITIES.endServerConnection(getServer(player), connection);
        SERVER_HANDSHAKE_NEGOTIATIONS.remove(connection);
        BRIDGE_REASSEMBLER.endSession(connection);
    }

    /**
     * Clears every connection owned by one exact server instance.
     *
     * @param server the stopping server
     */
    public static void onServerStopped(@NotNull MinecraftServer server) {
        NETWORK_CAPABILITIES.endServerSession(Objects.requireNonNull(server));
        BRIDGE_REASSEMBLER.endServer(server);
    }

    /**
     * Accepts a parsed server handshake for the current exact client session.
     *
     * @param connection the receiving connection
     * @param bridgeProtocolVersion the advertised bridge protocol
     * @param packetIdentifiers the server's logical packet identifiers
     * @return whether this was the first accepted handshake for the session
     */
    public static boolean acceptServerHandshake(@NotNull Connection connection, int bridgeProtocolVersion, @NotNull Set<Identifier> packetIdentifiers) {
        return NETWORK_CAPABILITIES.markClientPeerCapable(Objects.requireNonNull(connection), bridgeProtocolVersion, Objects.requireNonNull(packetIdentifiers));
    }

    /**
     * Accepts a parsed client handshake for the player's current exact server connection.
     *
     * @param player the sending player
     * @param bridgeProtocolVersion the advertised bridge protocol
     * @param packetIdentifiers the client's logical packet identifiers
     * @return whether this was the first accepted handshake for the connection
     */
    public static boolean acceptClientHandshake(@NotNull ServerPlayer player, int bridgeProtocolVersion, @NotNull Set<Identifier> packetIdentifiers) {
        Objects.requireNonNull(player);
        MinecraftServer server = getServer(player);
        if (!NETWORK_CAPABILITIES.isServerConnectionActive(server, Objects.requireNonNull(player.connection))) return false;
        ServerHandshakeNegotiationTracker.Decision decision = SERVER_HANDSHAKE_NEGOTIATIONS.accept(Objects.requireNonNull(player.connection));
        warnAboutRejectedHandshake(player, decision);
        return decision.isAllowed() && NETWORK_CAPABILITIES.markServerPeerCapable(server, player.connection, bridgeProtocolVersion, Objects.requireNonNull(packetIdentifiers));
    }

    /**
     * Reports whether the exact server connection advertised the framework.
     *
     * @param connection the client connection
     * @return whether negotiation completed
     */
    public static boolean isServerFrameworkAvailable(@NotNull Connection connection) {
        return NETWORK_CAPABILITIES.isClientPeerCapable(Objects.requireNonNull(connection));
    }

    /**
     * Reports whether the exact player connection advertised the framework.
     *
     * @param player the server-side player
     * @return whether negotiation completed
     */
    public static boolean isClientFrameworkAvailable(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        return NETWORK_CAPABILITIES.isServerPeerCapable(getServer(player), Objects.requireNonNull(player.connection));
    }

    /**
     * Reports whether the exact server advertised one logical packet codec.
     *
     * @param connection the client connection
     * @param packetIdentifier the logical packet identifier
     * @return whether the packet may be sent
     */
    public static boolean doesServerSupport(@NotNull Connection connection, @NotNull Identifier packetIdentifier) {
        return NETWORK_CAPABILITIES.supportsClientPacket(Objects.requireNonNull(connection), Objects.requireNonNull(packetIdentifier));
    }

    /**
     * Reports whether the exact client advertised one logical packet codec.
     *
     * @param player the server-side player
     * @param packetIdentifier the logical packet identifier
     * @return whether the packet may be sent
     */
    public static boolean doesClientSupport(@NotNull ServerPlayer player, @NotNull Identifier packetIdentifier) {
        Objects.requireNonNull(player);
        return NETWORK_CAPABILITIES.supportsServerPacket(getServer(player), Objects.requireNonNull(player.connection), Objects.requireNonNull(packetIdentifier));
    }

    /**
     * Sends the framework handshake to one client when its optional channel exists.
     *
     * @param player the target player
     * @return the send outcome
     */
    public static @NotNull PacketSendResult sendHandshakeToClient(@NotNull ServerPlayer player) {
        return sendToClient(player, HandshakePacket.current());
    }

    /**
     * Sends the framework handshake through the current client connection.
     *
     * @return the send outcome
     */
    public static @NotNull PacketSendResult sendHandshakeToServer() {
        Connection connection = ClientPacketUtils.getConnectedConnection();
        return connection == null ? PacketSendResult.NO_CONNECTION : sendHandshakeToServer(connection);
    }

    /**
     * Sends the framework handshake through an exact client connection.
     *
     * @param connection the target server connection
     * @return the send outcome
     */
    public static @NotNull PacketSendResult sendHandshakeToServer(@NotNull Connection connection) {
        return sendToServer(Objects.requireNonNull(connection), HandshakePacket.current());
    }

    /**
     * Sends a logical packet through the current client connection.
     *
     * @param packet the registered packet
     * @param <T> the packet type
     * @return the send outcome
     */
    public static <T extends Packet> @NotNull PacketSendResult sendToServer(@NotNull T packet) {
        Connection connection = ClientPacketUtils.getConnectedConnection();
        return connection == null ? PacketSendResult.NO_CONNECTION : sendToServer(connection, packet);
    }

    /**
     * Sends a logical packet through one exact client connection.
     *
     * @param connection the target server connection
     * @param packet the registered packet
     * @param <T> the packet type
     * @return the send outcome
     */
    public static <T extends Packet> @NotNull PacketSendResult sendToServer(@NotNull Connection connection, @NotNull T packet) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(packet);
        PacketCodec<T> codec = PacketRegistry.getCodecFor(packet);
        if (codec == null) return PacketSendResult.CODEC_NOT_REGISTERED;
        if (!isClientSessionActive(connection)) return PacketSendResult.SESSION_INACTIVE;
        if (!ClientPacketUtils.shouldSendToServer(packet, codec, connection)) return PacketSendResult.PEER_UNAVAILABLE;
        BridgeSendLogic<Connection> sendLogic = sendToServerLogic;
        if (sendLogic == null) return PacketSendResult.TRANSPORT_NOT_INITIALIZED;
        try {
            String serialized = codec.serialize(packet);
            return translate(sendLogic.send(connection, serialized, NETWORK_CAPABILITIES.supportsClientBridgeProtocol(connection, BridgeProtocol.VERSION)));
        } catch (RuntimeException ex) {
            LOGGER.error("[KONKRETE] Failed to serialize or send packet to server.", ex);
            return PacketSendResult.SERIALIZATION_FAILED;
        }
    }

    /**
     * Sends a logical packet to one exact player when that peer advertised its codec.
     *
     * @param player the target player
     * @param packet the registered packet
     * @param <T> the packet type
     * @return the send outcome
     */
    public static <T extends Packet> @NotNull PacketSendResult sendToClient(@NotNull ServerPlayer player, @NotNull T packet) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(packet);
        PacketCodec<T> codec = PacketRegistry.getCodecFor(packet);
        if (codec == null) return PacketSendResult.CODEC_NOT_REGISTERED;
        MinecraftServer server = getServer(player);
        Object connection = Objects.requireNonNull(player.connection);
        if (!NETWORK_CAPABILITIES.isServerConnectionActive(server, connection)) return PacketSendResult.SESSION_INACTIVE;
        if (!(packet instanceof HandshakePacket) && !NETWORK_CAPABILITIES.supportsServerPacket(server, connection, codec.getPacketIdentifier())) return PacketSendResult.PEER_UNAVAILABLE;
        BridgeSendLogic<ServerPlayer> sendLogic = sendToClientLogic;
        if (sendLogic == null) return PacketSendResult.TRANSPORT_NOT_INITIALIZED;
        try {
            String serialized = codec.serialize(packet);
            return translate(sendLogic.send(player, serialized, NETWORK_CAPABILITIES.supportsServerBridgeProtocol(server, connection, BridgeProtocol.VERSION)));
        } catch (RuntimeException ex) {
            LOGGER.error("[KONKRETE] Failed to serialize or send packet to client.", ex);
            return PacketSendResult.SERIALIZATION_FAILED;
        }
    }

    /**
     * Sends a logical packet to every connected client that advertised its codec.
     *
     * @param server the owning server
     * @param packet the registered packet
     * @param <T> the packet type
     * @return number of sends submitted successfully
     */
    public static <T extends Packet> int sendToAllClients(@NotNull MinecraftServer server, @NotNull T packet) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(packet);
        int sent = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (sendToClient(player, packet) == PacketSendResult.SENT) sent++;
        }
        return sent;
    }

    static void setSendToServerLogic(@NotNull BridgeSendLogic<Connection> logic) {
        sendToServerLogic = Objects.requireNonNull(logic);
    }

    static void setSendToClientLogic(@NotNull BridgeSendLogic<ServerPlayer> logic) {
        sendToClientLogic = Objects.requireNonNull(logic);
    }

    /**
     * Decodes and dispatches a serverbound legacy bridge message.
     *
     * @param sender the sending player
     * @param direction the loader-authenticated direction
     * @param message the complete logical envelope
     */
    public static void onPacketReceived(@Nullable ServerPlayer sender, @NotNull PacketDirection direction, @NotNull String message) {
        onPacketReceived(sender, direction, message, null);
    }

    /**
     * Decodes and dispatches a complete bridge message with exact client connection context.
     *
     * @param sender the sender for serverbound traffic
     * @param direction the loader-authenticated direction
     * @param message the complete logical envelope
     * @param clientConnection the exact receiving connection for clientbound traffic
     */
    public static void onPacketReceived(@Nullable ServerPlayer sender, @NotNull PacketDirection direction, @NotNull String message, @Nullable Connection clientConnection) {
        Objects.requireNonNull(direction);
        PacketEnvelope.Parsed envelope = PacketEnvelope.parse(Objects.requireNonNull(message));
        if (envelope == null) return;
        PacketCodec<?> codec = PacketRegistry.getCodec(envelope.identifier());
        if (codec == null) return;
        boolean handshake = codec.getType() == HandshakePacket.class;

        if (direction == PacketDirection.TO_CLIENT) {
            if (clientConnection == null || !NETWORK_CAPABILITIES.isClientSessionActive(clientConnection)) return;
            if (!handshake && !NETWORK_CAPABILITIES.supportsClientPacket(clientConnection, envelope.identifier())) return;
            Packet packet = deserializePacket(codec, envelope.body());
            if (packet == null) return;
            if (handshake) {
                processClientPacket(packet, clientConnection);
            } else {
                Minecraft.getInstance().execute(() -> {
                    if (NETWORK_CAPABILITIES.isClientSessionActive(clientConnection)) processClientPacket(packet, clientConnection);
                });
            }
            return;
        }

        if (sender == null) return;
        MinecraftServer server = sender.level().getServer();
        if (server == null) return;
        Object connection = Objects.requireNonNull(sender.connection);
        if (!NETWORK_CAPABILITIES.isServerConnectionActive(server, connection)) return;
        if (handshake) {
            if (!admitServerHandshake(sender)) return;
        } else if (!NETWORK_CAPABILITIES.supportsServerPacket(server, connection, envelope.identifier())) {
            return;
        }
        Packet packet = deserializePacket(codec, envelope.body());
        if (packet == null) return;
        if (handshake) {
            processServerPacket(packet, sender);
        } else {
            server.execute(() -> {
                if (NETWORK_CAPABILITIES.supportsServerPacket(server, connection, envelope.identifier())) processServerPacket(packet, sender);
            });
        }
    }

    /**
     * Accepts one binary bridge chunk and dispatches only a complete, valid reassembly.
     *
     * @param sender the sender for serverbound traffic
     * @param direction the loader-authenticated direction
     * @param payload the decoded chunk
     * @param clientConnection the exact receiving connection for clientbound traffic
     */
    public static void onBridgeChunkReceived(@Nullable ServerPlayer sender, @NotNull PacketDirection direction, @NotNull BridgeChunkPayload payload, @Nullable Connection clientConnection) {
        Objects.requireNonNull(direction);
        Objects.requireNonNull(payload);
        if (direction == PacketDirection.TO_CLIENT) {
            if (clientConnection == null || !NETWORK_CAPABILITIES.supportsClientBridgeProtocol(clientConnection, BridgeProtocol.VERSION)) return;
            BridgeChunkReassembler.Result result = BRIDGE_REASSEMBLER.accept(clientConnection, payload);
            if (result.status() == BridgeChunkReassembler.Status.COMPLETE) onPacketReceived(null, direction, Objects.requireNonNull(result.message()), clientConnection);
            return;
        }
        if (sender == null) return;
        MinecraftServer server = sender.level().getServer();
        Object connection = sender.connection;
        if (server == null || !NETWORK_CAPABILITIES.supportsServerBridgeProtocol(server, connection, BridgeProtocol.VERSION)) return;
        BridgeChunkReassembler.Result result = BRIDGE_REASSEMBLER.accept(connection, payload);
        if (result.status() == BridgeChunkReassembler.Status.COMPLETE) onPacketReceived(sender, direction, Objects.requireNonNull(result.message()));
    }

    static boolean isClientSessionActive(@NotNull Connection connection) {
        return NETWORK_CAPABILITIES.isClientSessionActive(Objects.requireNonNull(connection));
    }

    private static boolean admitServerHandshake(@NotNull ServerPlayer sender) {
        ServerHandshakeNegotiationTracker.Decision decision = SERVER_HANDSHAKE_NEGOTIATIONS.admitAttempt(Objects.requireNonNull(sender.connection));
        warnAboutRejectedHandshake(sender, decision);
        return decision.isAllowed();
    }

    private static void warnAboutRejectedHandshake(@NotNull ServerPlayer sender, @NotNull ServerHandshakeNegotiationTracker.Decision decision) {
        if (decision.isWarningRequired()) LOGGER.warn("[KONKRETE] Ignoring excessive or replayed handshake traffic from client: " + sender.getScoreboardName());
    }

    private static @Nullable Packet deserializePacket(@NotNull PacketCodec<?> codec, @NotNull String body) {
        try {
            return codec.deserialize(body);
        } catch (RuntimeException ex) {
            LOGGER.warn("[KONKRETE] Rejected malformed packet body for " + codec.getPacketIdentifier() + ".", ex);
            return null;
        }
    }

    private static void processClientPacket(@NotNull Packet packet, @NotNull Connection connection) {
        try {
            packet.processClientPacket(connection);
        } catch (RuntimeException ex) {
            LOGGER.error("[KONKRETE] Failed to process clientbound packet.", ex);
        }
    }

    private static void processServerPacket(@NotNull Packet packet, @NotNull ServerPlayer sender) {
        try {
            packet.processPacket(sender);
        } catch (RuntimeException ex) {
            LOGGER.error("[KONKRETE] Failed to process serverbound packet.", ex);
        }
    }

    private static @NotNull PacketSendResult translate(@NotNull BridgeMessageSender.SendResult result) {
        return switch (result) {
            case SENT -> PacketSendResult.SENT;
            case LEGACY_CHANNEL_UNAVAILABLE -> PacketSendResult.LEGACY_CHANNEL_UNAVAILABLE;
            case CHUNK_PROTOCOL_UNAVAILABLE -> PacketSendResult.CHUNK_PROTOCOL_UNAVAILABLE;
            case CHUNK_CHANNEL_UNAVAILABLE -> PacketSendResult.CHUNK_CHANNEL_UNAVAILABLE;
            case MESSAGE_TOO_LARGE -> PacketSendResult.MESSAGE_TOO_LARGE;
            case MALFORMED_TEXT -> PacketSendResult.MALFORMED_TEXT;
            case INVALID_DIRECTION -> PacketSendResult.SERIALIZATION_FAILED;
        };
    }

    private static @NotNull MinecraftServer getServer(@NotNull ServerPlayer player) {
        return Objects.requireNonNull(player.level().getServer(), "Server instance of player was null");
    }

    /**
     * Authenticated logical direction supplied by loader callback registration.
     */
    public enum PacketDirection {
        /** Client-to-server traffic. */
        TO_SERVER,
        /** Server-to-client traffic. */
        TO_CLIENT
    }
}
