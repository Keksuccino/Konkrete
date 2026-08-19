package de.keksuccino.konkrete.networking;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Strict textual framing inside the loader-neutral bridge. Newline is not legal in an Identifier, while JSON may
 * contain escaped newlines, making the first newline an unambiguous boundary without re-parsing packet bodies.
 */
final class PacketEnvelope {

    private static final char SEPARATOR = '\n';

    private PacketEnvelope() {
    }

    static @NotNull String encode(@NotNull Identifier identifier, @NotNull String body) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(body);
        if (body.isEmpty()) throw new IllegalArgumentException("Packet body must not be empty");
        validateIdentifier(identifier.toString());
        return identifier + String.valueOf(SEPARATOR) + body;
    }

    static @Nullable Parsed parse(@NotNull String envelope) {
        Objects.requireNonNull(envelope);
        int separatorIndex = envelope.indexOf(SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == envelope.length() - 1) return null;
        String rawIdentifier = envelope.substring(0, separatorIndex);
        Identifier identifier = Identifier.tryParse(rawIdentifier);
        if (identifier == null || !identifier.toString().equals(rawIdentifier)) return null;
        try {
            validateIdentifier(rawIdentifier);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return new Parsed(identifier, envelope.substring(separatorIndex + 1));
    }

    private static void validateIdentifier(@NotNull String identifier) {
        if (identifier.getBytes(StandardCharsets.UTF_8).length > PacketRegistry.MAX_PACKET_IDENTIFIER_BYTES) throw new IllegalArgumentException("Packet identifier exceeds the framework limit");
    }

    record Parsed(@NotNull Identifier identifier, @NotNull String body) {
    }
}
