package de.keksuccino.konkrete.networking;

/**
 * Outcome of one optional logical packet send.
 */
public enum PacketSendResult {
    /** The complete packet was submitted to the connection. */
    SENT,
    /** No current client connection exists. */
    NO_CONNECTION,
    /** The supplied endpoint is not part of the active lifecycle session. */
    SESSION_INACTIVE,
    /** The exact peer did not advertise this logical packet identifier. */
    PEER_UNAVAILABLE,
    /** No codec owns the packet's exact runtime class. */
    CODEC_NOT_REGISTERED,
    /** Packet serialization failed. */
    SERIALIZATION_FAILED,
    /** The peer did not negotiate the legacy bridge channel. */
    LEGACY_CHANNEL_UNAVAILABLE,
    /** The peer did not negotiate bridge protocol v1 for an oversized packet. */
    CHUNK_PROTOCOL_UNAVAILABLE,
    /** The peer did not negotiate the chunk bridge channel. */
    CHUNK_CHANNEL_UNAVAILABLE,
    /** The logical packet exceeds the bridge's maximum encoded size. */
    MESSAGE_TOO_LARGE,
    /** The logical packet contains malformed UTF-16 text. */
    MALFORMED_TEXT,
    /** Loader-specific transport initialization has not completed. */
    TRANSPORT_NOT_INITIALIZED
}
