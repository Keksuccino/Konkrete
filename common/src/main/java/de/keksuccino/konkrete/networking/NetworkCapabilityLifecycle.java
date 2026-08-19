package de.keksuccino.konkrete.networking;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns negotiated Konkrete capability state for live client and server networking sessions.
 * Connection and server keys deliberately use identity semantics: equal addresses, UUIDs, or wrapper objects do not
 * prove that two play sessions are the same session.
 */
final class NetworkCapabilityLifecycle {

    private final AtomicReference<ClientSession> clientSession = new AtomicReference<>();
    private final ConcurrentMap<IdentityKey, ServerSession> serverSessions = new ConcurrentHashMap<>();

    /**
     * Replaces the client session atomically. Repeated callbacks for the same connection are idempotent and must not
     * erase a handshake that was already accepted for that connection.
     */
    boolean beginClientSession(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        while (true) {
            ClientSession current = this.clientSession.get();
            if (current != null && current.connection == connection) return false;
            if (this.clientSession.compareAndSet(current, new ClientSession(connection, null))) return true;
        }
    }

    /**
     * Marks only the expected live session. This prevents a delayed packet from an old connection from granting
     * capability to a replacement connection.
     */
    boolean markClientPeerCapable(@NotNull Object connection) {
        return this.markClientPeerCapable(connection, 0, Set.of());
    }

    boolean markClientPeerCapable(@NotNull Object connection, int bridgeProtocolVersion) {
        return this.markClientPeerCapable(connection, bridgeProtocolVersion, Set.of());
    }

    boolean markClientPeerCapable(@NotNull Object connection, int bridgeProtocolVersion, @NotNull Set<Identifier> packetIdentifiers) {
        Objects.requireNonNull(connection);
        PeerCapabilities capabilities = new PeerCapabilities(bridgeProtocolVersion, packetIdentifiers);
        while (true) {
            ClientSession current = this.clientSession.get();
            if (current == null || current.connection != connection || current.capabilities != null) return false;
            if (this.clientSession.compareAndSet(current, new ClientSession(connection, capabilities))) return true;
        }
    }

    boolean isClientSessionActive(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        ClientSession current = this.clientSession.get();
        return current != null && current.connection == connection;
    }

    boolean isClientPeerCapable(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        ClientSession current = this.clientSession.get();
        return current != null && current.connection == connection && current.capabilities != null;
    }

    boolean supportsClientBridgeProtocol(@NotNull Object connection, int minimumVersion) {
        Objects.requireNonNull(connection);
        if (minimumVersion < 1) throw new IllegalArgumentException("Minimum bridge protocol version must be positive");
        ClientSession current = this.clientSession.get();
        return current != null && current.connection == connection && current.capabilities != null && current.capabilities.bridgeProtocolVersion >= minimumVersion;
    }

    boolean supportsClientPacket(@NotNull Object connection, @NotNull Identifier packetIdentifier) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(packetIdentifier);
        ClientSession current = this.clientSession.get();
        return current != null && current.connection == connection && current.capabilities != null && current.capabilities.packetIdentifiers.contains(packetIdentifier);
    }

    boolean endClientSession(@Nullable Object connection) {
        if (connection == null) return false;
        while (true) {
            ClientSession current = this.clientSession.get();
            if (current == null || current.connection != connection) return false;
            if (this.clientSession.compareAndSet(current, null)) return true;
        }
    }

    boolean beginServerSession(@NotNull Object server) {
        Objects.requireNonNull(server);
        return this.serverSessions.putIfAbsent(new IdentityKey(server), new ServerSession()) == null;
    }

    boolean beginServerConnection(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.beginConnection(connection);
    }

    boolean markServerPeerCapable(@NotNull Object server, @NotNull Object connection) {
        return this.markServerPeerCapable(server, connection, 0, Set.of());
    }

    boolean markServerPeerCapable(@NotNull Object server, @NotNull Object connection, int bridgeProtocolVersion) {
        return this.markServerPeerCapable(server, connection, bridgeProtocolVersion, Set.of());
    }

    boolean markServerPeerCapable(@NotNull Object server, @NotNull Object connection, int bridgeProtocolVersion, @NotNull Set<Identifier> packetIdentifiers) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.markPeerCapable(connection, new PeerCapabilities(bridgeProtocolVersion, packetIdentifiers));
    }

    boolean isServerPeerCapable(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.isPeerCapable(connection);
    }

    boolean isServerConnectionActive(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.isConnectionActive(connection);
    }

    boolean supportsServerBridgeProtocol(@NotNull Object server, @NotNull Object connection, int minimumVersion) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        if (minimumVersion < 1) throw new IllegalArgumentException("Minimum bridge protocol version must be positive");
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.supportsBridgeProtocol(connection, minimumVersion);
    }

    boolean supportsServerPacket(@NotNull Object server, @NotNull Object connection, @NotNull Identifier packetIdentifier) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        Objects.requireNonNull(packetIdentifier);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.supportsPacket(connection, packetIdentifier);
    }

    boolean endServerConnection(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.endConnection(connection);
    }

    boolean endServerSession(@NotNull Object server) {
        Objects.requireNonNull(server);
        ServerSession session = this.serverSessions.remove(new IdentityKey(server));
        if (session == null) return false;
        session.close();
        return true;
    }

    int serverSessionCount() {
        return this.serverSessions.size();
    }

    int liveServerConnectionCount(@NotNull Object server) {
        ServerSession session = this.serverSessions.get(new IdentityKey(Objects.requireNonNull(server)));
        return session == null ? 0 : session.liveConnectionCount();
    }

    int capableServerConnectionCount(@NotNull Object server) {
        ServerSession session = this.serverSessions.get(new IdentityKey(Objects.requireNonNull(server)));
        return session == null ? 0 : session.capableConnectionCount();
    }

    private record ClientSession(Object connection, @Nullable PeerCapabilities capabilities) {
    }

    private record PeerCapabilities(int bridgeProtocolVersion, Set<Identifier> packetIdentifiers) {

        private PeerCapabilities(int bridgeProtocolVersion, @NotNull Set<Identifier> packetIdentifiers) {
            this.bridgeProtocolVersion = Math.max(bridgeProtocolVersion, 0);
            this.packetIdentifiers = Set.copyOf(Objects.requireNonNull(packetIdentifiers));
        }
    }

    /**
     * Synchronizing each server session makes connection admission, logout, and stop one ordered transition. In
     * particular, a handshake racing logout can either complete before cleanup or be rejected after cleanup, but it
     * can never recreate capability state for a connection that is no longer live.
     */
    private static final class ServerSession {

        private final Set<IdentityKey> liveConnections = new HashSet<>();
        private final Map<IdentityKey, PeerCapabilities> capableConnections = new HashMap<>();
        private boolean active = true;

        private synchronized boolean beginConnection(@NotNull Object connection) {
            if (!this.active) return false;
            return this.liveConnections.add(new IdentityKey(connection));
        }

        private synchronized boolean markPeerCapable(@NotNull Object connection, @NotNull PeerCapabilities capabilities) {
            if (!this.active) return false;
            IdentityKey connectionKey = new IdentityKey(connection);
            if (!this.liveConnections.contains(connectionKey)) return false;
            return this.capableConnections.putIfAbsent(connectionKey, capabilities) == null;
        }

        private synchronized boolean isPeerCapable(@NotNull Object connection) {
            return this.active && this.capableConnections.containsKey(new IdentityKey(connection));
        }

        private synchronized boolean isConnectionActive(@NotNull Object connection) {
            return this.active && this.liveConnections.contains(new IdentityKey(connection));
        }

        private synchronized boolean supportsBridgeProtocol(@NotNull Object connection, int minimumVersion) {
            if (!this.active) return false;
            PeerCapabilities capabilities = this.capableConnections.get(new IdentityKey(connection));
            return capabilities != null && capabilities.bridgeProtocolVersion >= minimumVersion;
        }

        private synchronized boolean supportsPacket(@NotNull Object connection, @NotNull Identifier packetIdentifier) {
            if (!this.active) return false;
            PeerCapabilities capabilities = this.capableConnections.get(new IdentityKey(connection));
            return capabilities != null && capabilities.packetIdentifiers.contains(packetIdentifier);
        }

        private synchronized boolean endConnection(@NotNull Object connection) {
            IdentityKey connectionKey = new IdentityKey(connection);
            boolean removed = this.liveConnections.remove(connectionKey);
            return this.capableConnections.remove(connectionKey) != null || removed;
        }

        private synchronized void close() {
            this.active = false;
            this.liveConnections.clear();
            this.capableConnections.clear();
        }

        private synchronized int liveConnectionCount() {
            return this.liveConnections.size();
        }

        private synchronized int capableConnectionCount() {
            return this.capableConnections.size();
        }
    }

    private static final class IdentityKey {

        @NotNull private final Object value;
        private final int hashCode;

        private IdentityKey(@NotNull Object value) {
            this.value = Objects.requireNonNull(value);
            this.hashCode = System.identityHashCode(value);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return this == other || other instanceof IdentityKey otherKey && this.value == otherKey.value;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
