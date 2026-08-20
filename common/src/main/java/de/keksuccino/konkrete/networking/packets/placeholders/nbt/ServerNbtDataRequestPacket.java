package de.keksuccino.konkrete.networking.packets.placeholders.nbt;

import de.keksuccino.konkrete.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Requests one vanilla NBT query and carries a client-generated correlation ID. */
public final class ServerNbtDataRequestPacket extends Packet {

    private long requestId;
    @Nullable private String sourceType;
    @Nullable private String entitySelector;
    @Nullable private String blockPosition;
    @Nullable private String storageId;
    @Nullable private String nbtPath;
    @Nullable private String returnType;
    @Nullable private Double scale;

    /** Creates the Gson-compatible empty packet shape. */
    public ServerNbtDataRequestPacket() {
    }

    /**
     * Creates a correlated server-NBT request.
     *
     * @param requestId positive client-generated correlation ID
     * @param query immutable query arguments
     */
    public ServerNbtDataRequestPacket(long requestId, @NotNull ServerNbtQuery query) {
        if (requestId <= 0) throw new IllegalArgumentException("requestId must be positive");
        ServerNbtQuery exactQuery = java.util.Objects.requireNonNull(query, "query");
        this.requestId = requestId;
        this.sourceType = exactQuery.sourceType();
        this.entitySelector = exactQuery.entitySelector();
        this.blockPosition = exactQuery.blockPosition();
        this.storageId = exactQuery.storageId();
        this.nbtPath = exactQuery.nbtPath();
        this.returnType = exactQuery.returnType();
        this.scale = exactQuery.scale();
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
     * Returns an immutable query snapshot decoded from this packet.
     *
     * @return decoded query snapshot
     */
    public @NotNull ServerNbtQuery query() {
        return new ServerNbtQuery(this.sourceType, this.entitySelector, this.blockPosition, this.storageId, this.nbtPath, this.returnType, this.scale);
    }

    /** {@inheritDoc} */
    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return sender != null && ServerSideServerNbtDataRequestPacketLogic.handle(sender, this);
    }

}
