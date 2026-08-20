package de.keksuccino.konkrete.networking.packets.placeholders;

import de.keksuccino.konkrete.networking.PacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.gamerule.ServerGameruleValueRequestPacket;
import de.keksuccino.konkrete.networking.packets.placeholders.gamerule.ServerGameruleValueRequestPacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.gamerule.ServerGameruleValueResponsePacket;
import de.keksuccino.konkrete.networking.packets.placeholders.gamerule.ServerGameruleValueResponsePacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtDataRequestPacket;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtDataRequestPacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtDataResponsePacket;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtDataResponsePacketCodec;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceholderPacketCodecTest {

    @Test
    void gamerulePacketsRoundTripCorrelationAndNullableResults() {
        ServerGameruleValueRequestPacket request = roundTrip(new ServerGameruleValueRequestPacketCodec(), new ServerGameruleValueRequestPacket(41L, "keepInventory"));
        ServerGameruleValueResponsePacket response = roundTrip(new ServerGameruleValueResponsePacketCodec(), new ServerGameruleValueResponsePacket(41L, null));

        assertEquals(41L, request.requestId());
        assertEquals("keepInventory", request.gamerule());
        assertEquals(41L, response.requestId());
        assertNull(response.value());
    }

    @Test
    void nbtPacketsRoundTripEveryQueryArgument() {
        ServerNbtQuery query = new ServerNbtQuery("entity", "@s", null, null, "foodLevel", "value", 2.5D);
        ServerNbtDataRequestPacket request = roundTrip(new ServerNbtDataRequestPacketCodec(), new ServerNbtDataRequestPacket(73L, query));
        ServerNbtDataResponsePacket response = roundTrip(new ServerNbtDataResponsePacketCodec(), new ServerNbtDataResponsePacket(73L, "40"));

        assertEquals(73L, request.requestId());
        assertEquals(query, request.query());
        assertEquals(73L, response.requestId());
        assertEquals("40", response.value());
    }

    @Test
    void requestConstructorsRejectNonPositiveCorrelationIds() {
        ServerNbtQuery query = new ServerNbtQuery("entity", "@s", null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> new ServerGameruleValueRequestPacket(0L, "rule"));
        assertThrows(IllegalArgumentException.class, () -> new ServerNbtDataRequestPacket(-1L, query));
    }

    private static <T extends de.keksuccino.konkrete.networking.Packet> T roundTrip(PacketCodec<T> codec, T packet) {
        String envelope = codec.serialize(packet);
        int separator = envelope.indexOf('\n');
        if (separator < 0) throw new AssertionError("Serialized packet envelope had no separator");
        return codec.deserialize(envelope.substring(separator + 1));
    }

}
