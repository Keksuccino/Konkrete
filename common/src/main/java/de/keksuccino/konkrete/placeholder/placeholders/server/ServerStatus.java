package de.keksuccino.konkrete.placeholder.placeholders.server;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Immutable server-list status consumed by the built-in server placeholders. */
public record ServerStatus(boolean online, long pingMillis, @NotNull String motd, @NotNull String playerCount, @NotNull String version) {
    /** Rejects null display fields while preserving provider-defined ping and online values verbatim. */
    public ServerStatus {
        Objects.requireNonNull(motd, "motd");
        Objects.requireNonNull(playerCount, "playerCount");
        Objects.requireNonNull(version, "version");
    }

    /** Creates the stable offline result used after timeout or connection failure. */
    @NotNull
    public static ServerStatus offline() {
        return new ServerStatus(false, -1L, "", "0/0", "");
    }
}
