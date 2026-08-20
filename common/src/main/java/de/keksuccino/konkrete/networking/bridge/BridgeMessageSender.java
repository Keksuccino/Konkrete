package de.keksuccino.konkrete.networking.bridge;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Chooses the compatible legacy wire format or the negotiated v1 chunk format before constructing any payload.
 */
public final class BridgeMessageSender {

    private BridgeMessageSender() {
    }

    /**
     * Selects and transmits the compatibility or chunk wire format without constructing unsupported payloads.
     *
     * @param endpoint exact transport endpoint
     * @param direction compatibility direction marker
     * @param message complete logical envelope
     * @param bridgeProtocolV1Advertised whether the exact peer advertised chunk protocol v1
     * @param chunkChannelSupport exact-endpoint chunk-channel query
     * @param legacyTransmitter compatibility payload transmitter
     * @param chunkTransmitter chunk payload transmitter
     * @param <E> endpoint type
     * @return precise send outcome
     */
    public static <E> @NotNull SendResult send(@NotNull E endpoint, @NotNull String direction, @NotNull String message, boolean bridgeProtocolV1Advertised, @NotNull Predicate<? super E> chunkChannelSupport, @NotNull BiPredicate<? super E, ? super BridgePacketPayload> legacyTransmitter, @NotNull BiPredicate<? super E, ? super BridgeChunkPayload> chunkTransmitter) {
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(message);
        Objects.requireNonNull(chunkChannelSupport);
        Objects.requireNonNull(legacyTransmitter);
        Objects.requireNonNull(chunkTransmitter);

        try {
            BridgeProtocol.encodedLength(direction, BridgeProtocol.MAX_LEGACY_DIRECTION_BYTES);
        } catch (IllegalArgumentException ex) {
            return SendResult.INVALID_DIRECTION;
        }

        final int encodedLength;
        try {
            encodedLength = BridgeProtocol.encodedLength(message, BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES);
        } catch (BridgeProtocol.EncodedLengthExceededException ex) {
            return SendResult.MESSAGE_TOO_LARGE;
        } catch (BridgeProtocol.MalformedTextException ex) {
            return SendResult.MALFORMED_TEXT;
        }

        if (encodedLength <= BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES) {
            BridgePacketPayload payload = new BridgePacketPayload(direction, message);
            return legacyTransmitter.test(endpoint, payload) ? SendResult.SENT : SendResult.LEGACY_CHANNEL_UNAVAILABLE;
        }
        if (!bridgeProtocolV1Advertised) return SendResult.CHUNK_PROTOCOL_UNAVAILABLE;
        if (!chunkChannelSupport.test(endpoint)) return SendResult.CHUNK_CHANNEL_UNAVAILABLE;

        List<BridgeChunkPayload> chunks = BridgeChunkEncoder.encode(message, encodedLength);
        for (BridgeChunkPayload chunk : chunks) {
            if (!chunkTransmitter.test(endpoint, chunk)) return SendResult.CHUNK_CHANNEL_UNAVAILABLE;
        }
        return SendResult.SENT;
    }

    /**
     * Transport-level bridge outcome.
     */
    public enum SendResult {

        /** Every required payload was submitted. */
        SENT,
        /** The exact peer lacks the compatibility channel. */
        LEGACY_CHANNEL_UNAVAILABLE,
        /** The exact peer did not advertise chunk protocol v1. */
        CHUNK_PROTOCOL_UNAVAILABLE,
        /** The exact peer lacks the chunk channel. */
        CHUNK_CHANNEL_UNAVAILABLE,
        /** The logical message exceeds the reassembly limit. */
        MESSAGE_TOO_LARGE,
        /** The logical message contains malformed UTF-16. */
        MALFORMED_TEXT,
        /** The compatibility direction marker exceeds its limit. */
        INVALID_DIRECTION

    }

}
