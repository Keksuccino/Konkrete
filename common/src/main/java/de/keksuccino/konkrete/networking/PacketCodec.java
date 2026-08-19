package de.keksuccino.konkrete.networking;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;

/**
 * Strict Gson codec for one namespaced logical packet type.
 *
 * @param <T> the packet type
 */
public class PacketCodec<T extends Packet> {

    /** Canonical logical identifier owned by this codec. */
    @NotNull protected final Identifier packetIdentifier;
    /** Exact runtime packet class owned by this codec. */
    @NotNull protected final Class<T> type;

    /**
     * Creates a codec for one canonical packet identifier and exact runtime class.
     *
     * @param packetIdentifier the namespaced logical identifier
     * @param type the exact packet class
     */
    public PacketCodec(@NotNull Identifier packetIdentifier, @NotNull Class<T> type) {
        this.packetIdentifier = Objects.requireNonNull(packetIdentifier);
        this.type = Objects.requireNonNull(type);
        PacketEnvelope.encode(packetIdentifier, "{}");
    }

    /**
     * Serializes a packet into the complete bridge envelope.
     *
     * @param packet the exact packet instance
     * @return the encoded envelope
     */
    public @NotNull String serialize(@NotNull T packet) {
        Objects.requireNonNull(packet);
        if (packet.getClass() != this.type) throw new IllegalArgumentException("Packet runtime type does not match its codec");
        JsonElement json = Objects.requireNonNull(this.buildGson().toJsonTree(packet), "Packet serialized to null JSON");
        if (!json.isJsonObject()) throw new IllegalStateException("Packet codecs must serialize JSON objects");
        return PacketEnvelope.encode(this.packetIdentifier, json.toString());
    }

    /**
     * Deserializes a strict JSON object body and rejects trailing data.
     *
     * @param body packet JSON without its envelope identifier
     * @return the decoded packet
     */
    public @NotNull T deserialize(@NotNull String body) {
        Objects.requireNonNull(body);
        try {
            JsonReader reader = new JsonReader(new StringReader(body));
            reader.setStrictness(Strictness.STRICT);
            JsonElement json = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Packet JSON contains trailing data");
            if (!json.isJsonObject()) throw new IllegalArgumentException("Packet JSON must be an object");
            return Objects.requireNonNull(this.buildGson().fromJson(json, this.type), "Packet JSON decoded to null");
        } catch (IOException ex) {
            throw new IllegalArgumentException("Packet JSON could not be read", ex);
        }
    }

    /**
     * Creates the Gson instance used for this codec.
     *
     * @return a configured Gson instance
     */
    protected @NotNull Gson buildGson() {
        return new Gson();
    }

    /**
     * Returns this codec's canonical logical identifier.
     *
     * @return the packet identifier
     */
    public @NotNull Identifier getPacketIdentifier() {
        return this.packetIdentifier;
    }

    /**
     * Returns the exact packet class handled by this codec.
     *
     * @return the packet class
     */
    public @NotNull Class<T> getType() {
        return this.type;
    }
}
