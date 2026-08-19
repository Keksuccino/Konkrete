package de.keksuccino.konkrete.networking.packets.handshake;

import de.keksuccino.konkrete.networking.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

final class ServerSideHandshakePacketLogic {

    private ServerSideHandshakePacketLogic() {
    }

    static boolean handle(@NotNull ServerPlayer sender, @NotNull HandshakePacket packet) {
        HandshakePacket.NegotiatedCapabilities capabilities = packet.negotiatedCapabilities();
        return capabilities != null && PacketHandler.acceptClientHandshake(sender, capabilities.bridgeProtocolVersion(), capabilities.packetIdentifiers());
    }
}
