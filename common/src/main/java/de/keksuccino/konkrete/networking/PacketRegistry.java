package de.keksuccino.konkrete.networking;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Global registry for logical packet codecs contributed by Konkrete and consuming mods.
 */
public final class PacketRegistry {

    /** Maximum number of codecs advertised in one bounded pre-negotiation handshake. */
    public static final int MAX_REGISTERED_CODECS = 128;
    /** Maximum UTF-8 length of one canonical packet identifier. */
    public static final int MAX_PACKET_IDENTIFIER_BYTES = 128;

    private static final Map<Identifier, PacketCodec<?>> CODECS_BY_IDENTIFIER = new LinkedHashMap<>();
    private static final Map<Class<? extends Packet>, PacketCodec<?>> CODECS_BY_TYPE = new LinkedHashMap<>();
    private static boolean registrationsFrozen;

    private PacketRegistry() {
    }

    /**
     * Registers a codec without overriding existing ownership. The identifier must use the consuming mod's own
     * namespace. Register during mod initialization, before any play session begins and freezes the registry.
     *
     * @param codec the codec to register
     */
    public static synchronized void register(@NotNull PacketCodec<?> codec) {
        register(codec, false);
    }

    static synchronized void registerFramework(@NotNull PacketCodec<?> codec) {
        register(codec, true);
    }

    private static void register(@NotNull PacketCodec<?> codec, boolean frameworkOwned) {
        Objects.requireNonNull(codec);
        if (registrationsFrozen) throw new IllegalStateException("Packet codec registration is frozen because networking sessions have started");
        Identifier identifier = codec.getPacketIdentifier();
        PacketEnvelope.encode(identifier, "{}");
        String namespace = identifier.getNamespace();
        if (frameworkOwned && !namespace.equals("konkrete")) throw new IllegalArgumentException("Framework codecs must use the konkrete namespace");
        if (!frameworkOwned && (namespace.equals("minecraft") || namespace.equals("realms") || namespace.equals("konkrete"))) throw new IllegalArgumentException("Consuming mods must register codecs in their own namespace");
        PacketCodec<?> identifierConflict = CODECS_BY_IDENTIFIER.get(identifier);
        if (identifierConflict != null) throw new IllegalArgumentException("Packet identifier is already registered: " + identifier);
        PacketCodec<?> typeConflict = CODECS_BY_TYPE.get(codec.getType());
        if (typeConflict != null) throw new IllegalArgumentException("Packet type is already registered as " + typeConflict.getPacketIdentifier());
        if (CODECS_BY_IDENTIFIER.size() >= MAX_REGISTERED_CODECS) throw new IllegalStateException("Konkrete packet codec limit reached");
        CODECS_BY_IDENTIFIER.put(identifier, codec);
        CODECS_BY_TYPE.put(codec.getType(), codec);
    }

    static synchronized void freezeRegistrations() {
        registrationsFrozen = true;
    }

    static synchronized boolean areRegistrationsFrozen() {
        return registrationsFrozen;
    }

    /**
     * Returns a stable snapshot of registered codecs.
     *
     * @return immutable codec snapshot
     */
    public static synchronized @NotNull List<PacketCodec<?>> getCodecs() {
        return List.copyOf(new ArrayList<>(CODECS_BY_IDENTIFIER.values()));
    }

    /**
     * Returns a stable snapshot of identifiers advertised to peers.
     *
     * @return immutable identifier snapshot
     */
    public static synchronized @NotNull Set<Identifier> getPacketIdentifiers() {
        return Set.copyOf(new LinkedHashSet<>(CODECS_BY_IDENTIFIER.keySet()));
    }

    /**
     * Looks up a codec by canonical identifier.
     *
     * @param identifier the packet identifier
     * @return its codec, or {@code null}
     */
    public static synchronized @Nullable PacketCodec<?> getCodec(@NotNull Identifier identifier) {
        return CODECS_BY_IDENTIFIER.get(Objects.requireNonNull(identifier));
    }

    /**
     * Looks up the codec registered for a packet's exact runtime class.
     *
     * @param packet the packet instance
     * @param <T> the packet type
     * @return its codec, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T extends Packet> @Nullable PacketCodec<T> getCodecFor(@NotNull T packet) {
        return (PacketCodec<T>) CODECS_BY_TYPE.get(Objects.requireNonNull(packet).getClass());
    }
}
