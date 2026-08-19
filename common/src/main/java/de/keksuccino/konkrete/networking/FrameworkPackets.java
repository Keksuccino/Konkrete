package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.packets.handshake.HandshakePacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.gamerule.ServerGameruleValueRequestPacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.gamerule.ServerGameruleValueResponsePacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtDataRequestPacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtDataResponsePacketCodec;

final class FrameworkPackets {

    private static final HandshakePacketCodec HANDSHAKE = new HandshakePacketCodec();
    private static final ServerGameruleValueRequestPacketCodec GAMERULE_REQUEST = new ServerGameruleValueRequestPacketCodec();
    private static final ServerGameruleValueResponsePacketCodec GAMERULE_RESPONSE = new ServerGameruleValueResponsePacketCodec();
    private static final ServerNbtDataRequestPacketCodec NBT_REQUEST = new ServerNbtDataRequestPacketCodec();
    private static final ServerNbtDataResponsePacketCodec NBT_RESPONSE = new ServerNbtDataResponsePacketCodec();
    private static boolean registered;

    private FrameworkPackets() {
    }

    static synchronized void registerAll() {
        if (registered) return;
        PacketRegistry.registerFramework(HANDSHAKE);
        PacketRegistry.registerFramework(GAMERULE_REQUEST);
        PacketRegistry.registerFramework(GAMERULE_RESPONSE);
        PacketRegistry.registerFramework(NBT_REQUEST);
        PacketRegistry.registerFramework(NBT_RESPONSE);
        registered = true;
    }
}
