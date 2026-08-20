package de.keksuccino.konkrete.networking.packets.placeholders.nbt;

import de.keksuccino.konkrete.networking.Packet;
import de.keksuccino.konkrete.networking.packets.placeholders.ServerPlaceholderRequests;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Returns one server-side NBT value for a correlated client request. */
public final class ServerNbtDataResponsePacket extends Packet {

    private long requestId;
    @Nullable private String value;

    /** Creates the Gson-compatible empty packet shape. */
    public ServerNbtDataResponsePacket() {
    }

    /**
     * Creates a correlated NBT response.
     *
     * @param requestId matching positive client correlation ID
     * @param value resolved value, or {@code null} when unavailable
     */
    public ServerNbtDataResponsePacket(long requestId, @Nullable String value) {
        if (requestId <= 0) throw new IllegalArgumentException("requestId must be positive");
        this.requestId = requestId;
        this.value = value;
    }

    /**
     * Returns the matching client-generated correlation ID.
     *
     * @return positive request ID, or zero for malformed wire data
     */
    public long requestId() {
        return this.requestId;
    }

    /**
     * Returns the resolved value.
     *
     * @return value, or {@code null} when the query produced no tag
     */
    public @Nullable String value() {
        return this.value;
    }

    /** Applies the response only to a pending request on the exact receiving connection. */
    @Override
    public boolean processClientPacket(@NotNull Connection connection) {
        return ServerPlaceholderRequests.handleNbtResponse(connection, this.requestId, this.value);
    }

    /** Rejects this clientbound-only packet on the server. */
    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return false;
    }

}
