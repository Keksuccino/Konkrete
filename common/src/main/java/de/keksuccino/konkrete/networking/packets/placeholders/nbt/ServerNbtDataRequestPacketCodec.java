package de.keksuccino.konkrete.networking.packets.placeholders.nbt;

import de.keksuccino.konkrete.networking.PacketCodec;
import net.minecraft.resources.Identifier;

/** Codec for serverbound NBT placeholder requests. */
public final class ServerNbtDataRequestPacketCodec extends PacketCodec<ServerNbtDataRequestPacket> {

    /** Creates the fixed Konkrete server-NBT-request codec. */
    public ServerNbtDataRequestPacketCodec() {
        super(Identifier.fromNamespaceAndPath("konkrete", "nbt_placeholder_request"), ServerNbtDataRequestPacket.class);
    }

}
