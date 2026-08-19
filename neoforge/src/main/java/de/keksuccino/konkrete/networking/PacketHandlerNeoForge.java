package de.keksuccino.konkrete.networking;

import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Exact-connection NeoForge optional-channel operations.
 */
public final class PacketHandlerNeoForge {

    private PacketHandlerNeoForge() {
    }

    /**
     * Tests whether one client's listener negotiated a payload type.
     *
     * @param type the payload type
     * @param player the exact player
     * @return whether the channel exists
     */
    public static boolean canSendToClient(@NotNull CustomPacketPayload.Type<?> type, @NotNull ServerPlayer player) {
        ServerGamePacketListenerImpl connection = Objects.requireNonNull(player).connection;
        return NetworkRegistry.hasChannel(connection, Objects.requireNonNull(type).id());
    }

    /**
     * Sends only when the exact client listener negotiated the payload type.
     *
     * @param packet the payload
     * @param player the exact player
     * @return whether the payload was submitted
     */
    public static boolean sendToClient(@NotNull CustomPacketPayload packet, @NotNull ServerPlayer player) {
        ServerGamePacketListenerImpl connection = Objects.requireNonNull(player).connection;
        return OptionalPayloadSender.sendIfSupported(connection, Objects.requireNonNull(packet), (listener, payload) -> NetworkRegistry.hasChannel(listener, payload.type().id()), ServerGamePacketListenerImpl::send);
    }

    /**
     * Tests whether one server connection negotiated a payload type.
     *
     * @param type the payload type
     * @param connection the exact connection
     * @return whether the channel exists
     */
    public static boolean canSendToServer(@NotNull CustomPacketPayload.Type<?> type, @NotNull Connection connection) {
        return NetworkRegistry.hasChannel(Objects.requireNonNull(connection), ConnectionProtocol.PLAY, Objects.requireNonNull(type).id());
    }

    /**
     * Sends only when the exact server connection negotiated the payload type.
     *
     * @param packet the payload
     * @param connection the exact connection
     * @return whether the payload was submitted
     */
    public static boolean sendToServer(@NotNull CustomPacketPayload packet, @NotNull Connection connection) {
        return OptionalPayloadSender.sendIfSupported(Objects.requireNonNull(connection), Objects.requireNonNull(packet), (exactConnection, payload) -> NetworkRegistry.hasChannel(exactConnection, ConnectionProtocol.PLAY, payload.type().id()), (exactConnection, payload) -> exactConnection.send(new ServerboundCustomPayloadPacket(payload)));
    }
}
