package de.keksuccino.konkrete.networking.packets.handshake;

import de.keksuccino.konkrete.networking.PacketCodec;
import net.minecraft.resources.Identifier;

/**
 * Codec for Konkrete's framework negotiation packet.
 */
public final class HandshakePacketCodec extends PacketCodec<HandshakePacket> {

    /**
     * Creates the fixed framework handshake codec.
     */
    public HandshakePacketCodec() {
        super(Identifier.fromNamespaceAndPath("konkrete", "handshake"), HandshakePacket.class);
    }
}
