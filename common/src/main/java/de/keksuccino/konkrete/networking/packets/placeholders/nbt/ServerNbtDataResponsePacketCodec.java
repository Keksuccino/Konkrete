package de.keksuccino.konkrete.networking.packets.placeholders.nbt;

import de.keksuccino.konkrete.networking.PacketCodec;
import net.minecraft.resources.Identifier;

/** Codec for clientbound NBT placeholder responses. */
public final class ServerNbtDataResponsePacketCodec extends PacketCodec<ServerNbtDataResponsePacket> {

    /** Creates the fixed Konkrete server-NBT-response codec. */
    public ServerNbtDataResponsePacketCodec() {
        super(Identifier.fromNamespaceAndPath("konkrete", "nbt_placeholder_response"), ServerNbtDataResponsePacket.class);
    }

}
