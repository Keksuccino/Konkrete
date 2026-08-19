package de.keksuccino.konkrete.networking.packets.handshake;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.keksuccino.konkrete.networking.bridge.BridgeProtocol;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandshakePacketTest {

    private static final Gson GSON = new Gson();

    @Test
    void currentHandshakeAdvertisesProtocolV1AsAnAdditiveJsonField() {
        HandshakePacket packet = HandshakePacket.current();
        JsonObject json = GSON.toJsonTree(packet).getAsJsonObject();

        assertEquals(BridgeProtocol.VERSION, packet.bridgeProtocolVersion());
        assertEquals(BridgeProtocol.VERSION, json.get("bridgeProtocolVersion").getAsInt());
        assertNotNull(json.get("packetIdentifiers"));
        assertNotNull(GSON.fromJson(json, LegacyHandshakeShape.class));
    }

    @Test
    void missingOrInvalidAdvertisementsRemainLegacyOnly() {
        assertEquals(0, parse("{}").bridgeProtocolVersion());
        assertEquals(0, parse("{\"bridgeProtocolVersion\":null}").bridgeProtocolVersion());
        assertEquals(0, parse("{\"bridgeProtocolVersion\":\"1\"}").bridgeProtocolVersion());
        assertEquals(0, parse("{\"bridgeProtocolVersion\":true}").bridgeProtocolVersion());
        assertEquals(0, parse("{\"bridgeProtocolVersion\":1.5}").bridgeProtocolVersion());
        assertEquals(0, parse("{\"bridgeProtocolVersion\":-1}").bridgeProtocolVersion());
        assertEquals(0, parse("{\"bridgeProtocolVersion\":2147483648}").bridgeProtocolVersion());
    }

    @Test
    void futureIntegralAdvertisementsRetainV1Compatibility() {
        assertEquals(2, parse("{\"bridgeProtocolVersion\":2}").bridgeProtocolVersion());
        assertEquals(1, parse("{\"bridgeProtocolVersion\":1.0}").bridgeProtocolVersion());
    }

    @Test
    void defaultConstructorDoesNotAccidentallyAdvertiseCapability() {
        HandshakePacket packet = new HandshakePacket();

        assertEquals(0, packet.bridgeProtocolVersion());
        assertEquals("{}", GSON.toJson(packet));
    }

    @Test
    void canonicalPacketIdentifiersAreParsedAsAnImmutableCapabilitySet() {
        HandshakePacket packet = parse("{\"bridgeProtocolVersion\":1,\"packetIdentifiers\":[\"consumer:first\",\"consumer:second\"]}");

        assertEquals(2, packet.packetIdentifiers().size());
        assertTrue(packet.packetIdentifiers().contains(Identifier.fromNamespaceAndPath("consumer", "first")));
        assertNotNull(packet.negotiatedCapabilities());
    }

    @Test
    void malformedOrDuplicatePacketCapabilitiesRejectNegotiation() {
        assertNull(parse("{\"packetIdentifiers\":{}}").negotiatedCapabilities());
        assertNull(parse("{\"packetIdentifiers\":[\"implicit_path\"]}").negotiatedCapabilities());
        assertNull(parse("{\"packetIdentifiers\":[\"consumer:first\",\"consumer:first\"]}").negotiatedCapabilities());
        assertNull(parse("{\"packetIdentifiers\":[1]}").negotiatedCapabilities());
        assertNull(parse("{\"bridgeProtocolVersion\":1.5,\"packetIdentifiers\":[]}").negotiatedCapabilities());
    }

    private static HandshakePacket parse(String json) {
        return GSON.fromJson(json, HandshakePacket.class);
    }

    private static final class LegacyHandshakeShape {
    }
}
