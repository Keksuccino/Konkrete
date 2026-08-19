package de.keksuccino.konkrete.networking.packets.placeholders.gamerule;

import de.keksuccino.konkrete.networking.PacketCodec;
import net.minecraft.resources.Identifier;

/** Codec for clientbound gamerule placeholder responses. */
public final class ServerGameruleValueResponsePacketCodec extends PacketCodec<ServerGameruleValueResponsePacket> {

    /** Creates the fixed Konkrete gamerule-response codec. */
    public ServerGameruleValueResponsePacketCodec() {
        super(Identifier.fromNamespaceAndPath("konkrete", "gamerule_placeholder_response"), ServerGameruleValueResponsePacket.class);
    }
}
