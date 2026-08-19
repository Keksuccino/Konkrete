package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.packets.handshake.HandshakePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only accessors for the current play connection.
 */
public final class ClientPacketUtils {

    private ClientPacketUtils() {
    }

    static boolean shouldSendToServer(@NotNull Packet packet, @NotNull PacketCodec<?> codec, @NotNull Connection connection) {
        if (!PacketHandler.isClientSessionActive(connection)) return false;
        return packet instanceof HandshakePacket || PacketHandler.doesServerSupport(connection, codec.getPacketIdentifier());
    }

    /**
     * Returns the current client play connection.
     *
     * @return active connection, or {@code null}
     */
    public static @Nullable Connection getConnectedConnection() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener == null ? null : listener.getConnection();
    }

    /**
     * Returns the configured server address, falling back to the remote socket address.
     *
     * @return connected server address, or {@code null}
     */
    public static @Nullable String getConnectedServerAddress() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) return null;
        if (listener.getServerData() != null) return listener.getServerData().ip;
        Connection connection = listener.getConnection();
        return connection.getRemoteAddress() == null ? null : connection.getRemoteAddress().toString();
    }
}
