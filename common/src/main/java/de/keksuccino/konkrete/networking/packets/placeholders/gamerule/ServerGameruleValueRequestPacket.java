package de.keksuccino.konkrete.networking.packets.placeholders.gamerule;

import de.keksuccino.konkrete.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Requests one gamerule value and carries a client-generated correlation ID. */
public final class ServerGameruleValueRequestPacket extends Packet {

    private long requestId;
    @Nullable private String gamerule;

    /** Creates the Gson-compatible empty packet shape. */
    public ServerGameruleValueRequestPacket() {
    }

    /**
     * Creates a correlated gamerule request.
     *
     * @param requestId positive client-generated correlation ID
     * @param gamerule vanilla gamerule name
     */
    public ServerGameruleValueRequestPacket(long requestId, @NotNull String gamerule) {
        if (requestId <= 0) throw new IllegalArgumentException("requestId must be positive");
        this.requestId = requestId;
        this.gamerule = gamerule;
    }

    /**
     * Returns the client-generated correlation ID.
     *
     * @return positive request ID, or zero for malformed wire data
     */
    public long requestId() {
        return this.requestId;
    }

    /**
     * Returns the requested gamerule name.
     *
     * @return gamerule name, or {@code null} for malformed wire data
     */
    public @Nullable String gamerule() {
        return this.gamerule;
    }

    /** {@inheritDoc} */
    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return sender != null && ServerSideServerGameruleValueRequestPacketLogic.handle(sender, this);
    }

}
