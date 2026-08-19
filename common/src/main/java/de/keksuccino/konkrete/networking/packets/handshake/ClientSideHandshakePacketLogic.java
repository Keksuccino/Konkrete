package de.keksuccino.konkrete.networking.packets.handshake;

import de.keksuccino.konkrete.networking.PacketHandler;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.NotNull;

final class ClientSideHandshakePacketLogic {

    private ClientSideHandshakePacketLogic() {
    }

    static boolean handle(@NotNull HandshakePacket packet, @NotNull Connection connection) {
        HandshakePacket.NegotiatedCapabilities capabilities = packet.negotiatedCapabilities();
        return capabilities != null && PacketHandler.acceptServerHandshake(connection, capabilities.bridgeProtocolVersion(), capabilities.packetIdentifiers());
    }
}
