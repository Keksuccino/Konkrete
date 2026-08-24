package de.keksuccino.konkrete.networking.packets.handshake;

import de.keksuccino.konkrete.networking.PacketCodec;

public class HandshakePacketCodec extends PacketCodec<HandshakePacket> {

    public HandshakePacketCodec() {
        super("fancymenu_handshake", HandshakePacket.class);
    }

}
