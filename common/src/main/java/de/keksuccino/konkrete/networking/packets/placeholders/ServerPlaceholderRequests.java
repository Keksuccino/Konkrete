package de.keksuccino.konkrete.networking.packets.placeholders;

import de.keksuccino.konkrete.networking.ClientPacketUtils;
import de.keksuccino.konkrete.networking.PacketHandler;
import de.keksuccino.konkrete.networking.packets.placeholders.gamerule.ServerGameruleValueRequestPacket;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtDataRequestPacket;
import de.keksuccino.konkrete.networking.packets.placeholders.nbt.ServerNbtQuery;
import de.keksuccino.konkrete.networking.request.PacketRequestCache;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/** Non-blocking client cache and correlation layer for Konkrete's server-backed placeholders. */
public final class ServerPlaceholderRequests {

    private static final int MAXIMUM_CACHED_VALUES = 512;
    private static final int MAXIMUM_PENDING_REQUESTS = 128;
    private static final Duration CACHE_DURATION = Duration.ofMillis(100);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private static final PacketRequestCache<Connection, String, String> GAMERULES = new PacketRequestCache<>(MAXIMUM_CACHED_VALUES, MAXIMUM_PENDING_REQUESTS, CACHE_DURATION, REQUEST_TIMEOUT);
    private static final PacketRequestCache<Connection, ServerNbtQuery, String> NBT = new PacketRequestCache<>(MAXIMUM_CACHED_VALUES, MAXIMUM_PENDING_REQUESTS, CACHE_DURATION, REQUEST_TIMEOUT);

    private ServerPlaceholderRequests() {
    }

    /**
     * Returns the latest gamerule value and asynchronously refreshes it through the current connection.
     *
     * @param gamerule vanilla gamerule name
     * @return latest value, or an empty string before a response is available
     */
    public static @NotNull String resolveGamerule(@NotNull String gamerule) {
        Connection connection = ClientPacketUtils.getConnectedConnection();
        if (connection == null) return "";
        String normalized = Objects.requireNonNull(gamerule, "gamerule").trim();
        if (normalized.isEmpty()) return "";
        String value = GAMERULES.resolve(connection, normalized, requestId -> PacketHandler.sendToServer(connection, new ServerGameruleValueRequestPacket(requestId, normalized)));
        return value == null ? "" : value;
    }

    /**
     * Returns the latest server-NBT result and asynchronously refreshes it through the current connection.
     *
     * @param query immutable query arguments
     * @return latest value, or an empty string before a response is available
     */
    public static @NotNull String resolveNbt(@NotNull ServerNbtQuery query) {
        Connection connection = ClientPacketUtils.getConnectedConnection();
        if (connection == null) return "";
        ServerNbtQuery exactQuery = Objects.requireNonNull(query, "query");
        String value = NBT.resolve(connection, exactQuery, requestId -> PacketHandler.sendToServer(connection, new ServerNbtDataRequestPacket(requestId, exactQuery)));
        return value == null ? "" : value;
    }

    /**
     * Clears previous-server data and begins caching for an exact play connection.
     *
     * @param connection new play connection
     */
    public static void onClientConnected(@NotNull Connection connection) {
        GAMERULES.beginSession(connection);
        NBT.beginSession(connection);
    }

    /**
     * Clears data only when the disconnected connection owns the current cache session.
     *
     * @param connection disconnected connection, or {@code null} when unknown
     */
    public static void onClientDisconnected(@Nullable Connection connection) {
        GAMERULES.endSession(connection);
        NBT.endSession(connection);
    }

    /**
     * Completes a live gamerule request for the exact receiving connection.
     *
     * @param connection exact receiving connection
     * @param requestId response correlation ID
     * @param value resolved value, or {@code null} when unavailable
     * @return whether a matching live request was completed
     */
    public static boolean handleGameruleResponse(@NotNull Connection connection, long requestId, @Nullable String value) {
        return GAMERULES.complete(connection, requestId, value == null ? "" : value);
    }

    /**
     * Completes a live server-NBT request for the exact receiving connection.
     *
     * @param connection exact receiving connection
     * @param requestId response correlation ID
     * @param value resolved value, or {@code null} when unavailable
     * @return whether a matching live request was completed
     */
    public static boolean handleNbtResponse(@NotNull Connection connection, long requestId, @Nullable String value) {
        return NBT.complete(connection, requestId, value == null ? "" : value);
    }
}
