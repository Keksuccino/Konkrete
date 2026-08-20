package de.keksuccino.konkrete.networking.packets.handshake;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.keksuccino.konkrete.networking.Packet;
import de.keksuccino.konkrete.networking.PacketRegistry;
import de.keksuccino.konkrete.networking.bridge.BridgeProtocol;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Negotiates bridge protocol support and the logical packet identifiers registered by the exact peer.
 */
public final class HandshakePacket extends Packet {

    @Nullable private JsonElement bridgeProtocolVersion;
    @Nullable private JsonElement packetIdentifiers;

    /**
     * Creates the legacy-compatible Gson shape. A missing field advertises no optional capability.
     */
    public HandshakePacket() {
    }

    private HandshakePacket(int bridgeProtocolVersion, @NotNull Set<Identifier> packetIdentifiers) {
        this.bridgeProtocolVersion = new JsonPrimitive(bridgeProtocolVersion);
        JsonArray identifiers = new JsonArray();
        packetIdentifiers.stream().map(Identifier::toString).sorted().forEach(identifiers::add);
        this.packetIdentifiers = identifiers;
    }

    /**
     * Creates a handshake snapshot from all codecs registered at send time.
     *
     * @return current framework capabilities
     */
    public static @NotNull HandshakePacket current() {
        return new HandshakePacket(BridgeProtocol.VERSION, PacketRegistry.getPacketIdentifiers());
    }

    /**
     * Returns the strictly parsed protocol version, or zero for a legacy/malformed value.
     *
     * @return advertised bridge protocol version
     */
    public int bridgeProtocolVersion() {
        Integer version = this.parseBridgeProtocolVersion();
        return version == null ? 0 : version;
    }

    /**
     * Returns the strictly parsed packet identifier snapshot, or an empty set for malformed data.
     *
     * @return immutable advertised identifiers
     */
    public @NotNull Set<Identifier> packetIdentifiers() {
        Set<Identifier> identifiers = this.parsePacketIdentifiers();
        return identifiers == null ? Set.of() : identifiers;
    }

    @Nullable NegotiatedCapabilities negotiatedCapabilities() {
        Integer version = this.parseBridgeProtocolVersion();
        Set<Identifier> identifiers = this.parsePacketIdentifiers();
        return version == null || identifiers == null ? null : new NegotiatedCapabilities(version, identifiers);
    }

    /** {@inheritDoc} */
    @Override
    public boolean processClientPacket(@NotNull Connection connection) {
        return ClientSideHandshakePacketLogic.handle(this, connection);
    }

    /** {@inheritDoc} */
    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return sender != null && ServerSideHandshakePacketLogic.handle(sender, this);
    }

    private @Nullable Integer parseBridgeProtocolVersion() {
        if (this.bridgeProtocolVersion == null) return 0;
        if (!this.bridgeProtocolVersion.isJsonPrimitive() || !this.bridgeProtocolVersion.getAsJsonPrimitive().isNumber()) return null;
        try {
            BigDecimal version = this.bridgeProtocolVersion.getAsBigDecimal().stripTrailingZeros();
            if (version.signum() < 0 || version.scale() > 0 || version.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) return null;
            return version.intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            return null;
        }
    }

    private @Nullable Set<Identifier> parsePacketIdentifiers() {
        if (this.packetIdentifiers == null) return Set.of();
        if (!this.packetIdentifiers.isJsonArray()) return null;
        JsonArray values = this.packetIdentifiers.getAsJsonArray();
        if (values.size() > PacketRegistry.MAX_REGISTERED_CODECS) return null;
        Set<Identifier> identifiers = new LinkedHashSet<>();
        for (JsonElement value : values) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
            String rawIdentifier = value.getAsString();
            if (rawIdentifier.getBytes(StandardCharsets.UTF_8).length > PacketRegistry.MAX_PACKET_IDENTIFIER_BYTES) return null;
            Identifier identifier = Identifier.tryParse(rawIdentifier);
            if (identifier == null || !identifier.toString().equals(rawIdentifier) || !identifiers.add(identifier)) return null;
        }
        return Set.copyOf(identifiers);
    }

    record NegotiatedCapabilities(int bridgeProtocolVersion, @NotNull Set<Identifier> packetIdentifiers) {

    }

}
