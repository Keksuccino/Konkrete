package de.keksuccino.konkrete.networking;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketCodecTest {

    private static final Identifier ID = Identifier.fromNamespaceAndPath("consumer", "sample");
    private static final PacketCodec<SamplePacket> CODEC = new PacketCodec<>(ID, SamplePacket.class);

    @Test
    void namespacedEnvelopeRoundTripsWithoutConfusingTheNamespaceSeparator() {
        SamplePacket packet = new SamplePacket("Grüße", 7);

        String envelope = CODEC.serialize(packet);
        PacketEnvelope.Parsed parsed = PacketEnvelope.parse(envelope);

        assertNotNull(parsed);
        assertEquals(ID, parsed.identifier());
        SamplePacket decoded = CODEC.deserialize(parsed.body());
        assertEquals("Grüße", decoded.message);
        assertEquals(7, decoded.number);
    }

    @Test
    void envelopeRejectsMissingNonCanonicalAndOversizedIdentifiers() {
        assertNull(PacketEnvelope.parse("{}"));
        assertNull(PacketEnvelope.parse("sample\n{}"));
        assertNull(PacketEnvelope.parse("Consumer:sample\n{}"));
        assertNull(PacketEnvelope.parse("consumer:sample\n"));
        assertNull(Identifier.tryParse("consumer:bad\nidentifier"));
        String oversizedPath = "x".repeat(PacketRegistry.MAX_PACKET_IDENTIFIER_BYTES);
        assertThrows(IllegalArgumentException.class, () -> new PacketCodec<>(Identifier.fromNamespaceAndPath("consumer", oversizedPath), SamplePacket.class));
    }

    @Test
    void strictCodecRejectsMalformedNonObjectAndTrailingJson() {
        assertThrows(RuntimeException.class, () -> CODEC.deserialize("{\"message\":\"x\",}"));
        assertThrows(RuntimeException.class, () -> CODEC.deserialize("{message:\"x\"}"));
        assertThrows(RuntimeException.class, () -> CODEC.deserialize("{\"message\":\"x\"} trailing"));
        assertThrows(IllegalArgumentException.class, () -> CODEC.deserialize("[]"));
        assertThrows(IllegalArgumentException.class, () -> CODEC.deserialize("null"));
    }

    @Test
    void codecRejectsTheWrongRuntimeClassBeforeSerialization() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        PacketCodec rawCodec = CODEC;

        assertThrows(IllegalArgumentException.class, () -> rawCodec.serialize(new OtherPacket()));
    }

    @Test
    void registryRejectsIdentifierAndTypeTakeovers() {
        PacketCodec<RegisteredPacket> codec = new PacketCodec<>(Identifier.fromNamespaceAndPath("consumer", "registered"), RegisteredPacket.class);
        PacketRegistry.register(codec);

        assertEquals(codec, PacketRegistry.getCodec(codec.getPacketIdentifier()));
        assertEquals(codec, PacketRegistry.getCodecFor(new RegisteredPacket()));
        assertThrows(IllegalArgumentException.class, () -> PacketRegistry.register(new PacketCodec<>(codec.getPacketIdentifier(), OtherPacket.class)));
        assertThrows(IllegalArgumentException.class, () -> PacketRegistry.register(new PacketCodec<>(Identifier.fromNamespaceAndPath("consumer", "other"), RegisteredPacket.class)));
        assertThrows(IllegalArgumentException.class, () -> PacketRegistry.register(new PacketCodec<>(Identifier.withDefaultNamespace("reserved"), OtherPacket.class)));
        assertThrows(IllegalArgumentException.class, () -> PacketRegistry.register(new PacketCodec<>(Identifier.fromNamespaceAndPath("konkrete", "reserved"), OtherPacket.class)));
        assertTrue(PacketRegistry.getPacketIdentifiers().contains(codec.getPacketIdentifier()));
        PacketRegistry.freezeRegistrations();
        assertTrue(PacketRegistry.areRegistrationsFrozen());
        assertThrows(IllegalStateException.class, () -> PacketRegistry.register(new PacketCodec<>(Identifier.fromNamespaceAndPath("consumer", "late"), OtherPacket.class)));
    }

    private static final class SamplePacket extends Packet {

        private String message;
        private int number;

        private SamplePacket() {
        }

        private SamplePacket(String message, int number) {
            this.message = message;
            this.number = number;
        }

        @Override
        public boolean processPacket(ServerPlayer sender) {
            return true;
        }
    }

    private static final class RegisteredPacket extends Packet {

        @Override
        public boolean processPacket(ServerPlayer sender) {
            return true;
        }
    }

    private static final class OtherPacket extends Packet {

        @Override
        public boolean processPacket(ServerPlayer sender) {
            return true;
        }
    }
}
