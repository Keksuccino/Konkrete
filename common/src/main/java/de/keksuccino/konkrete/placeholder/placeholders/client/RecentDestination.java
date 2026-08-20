package de.keksuccino.konkrete.placeholder.placeholders.client;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Immutable consumer-supplied description of the most recently joined local world or multiplayer server. */
public record RecentDestination(@NotNull Type type, @NotNull String identifier, @NotNull String displayName) {

    /** Rejects null fields while retaining empty consumer-specific identifiers and display names. */
    public RecentDestination {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(displayName, "displayName");
    }

    /** Distinguishes local save histories from multiplayer server histories. */
    public enum Type {

        WORLD,
        SERVER

    }

}
