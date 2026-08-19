package de.keksuccino.konkrete.networking.packets.placeholders.gamerule;

import de.keksuccino.konkrete.networking.PacketCodec;
import net.minecraft.resources.Identifier;

/** Codec for serverbound gamerule placeholder requests. */
public final class ServerGameruleValueRequestPacketCodec extends PacketCodec<ServerGameruleValueRequestPacket> {

    /** Creates the fixed Konkrete gamerule-request codec. */
    public ServerGameruleValueRequestPacketCodec() {
        super(Identifier.fromNamespaceAndPath("konkrete", "gamerule_placeholder_request"), ServerGameruleValueRequestPacket.class);
    }
}
