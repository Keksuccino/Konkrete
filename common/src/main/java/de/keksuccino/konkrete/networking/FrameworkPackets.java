package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.packets.handshake.HandshakePacketCodec;

final class FrameworkPackets {

    private static final HandshakePacketCodec HANDSHAKE = new HandshakePacketCodec();
    private static boolean registered;

    private FrameworkPackets() {
    }

    static synchronized void registerAll() {
        if (registered) return;
        PacketRegistry.registerFramework(HANDSHAKE);
        registered = true;
    }
}
