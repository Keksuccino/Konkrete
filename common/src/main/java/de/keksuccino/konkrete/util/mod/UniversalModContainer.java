package de.keksuccino.konkrete.util.mod;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Loader-independent metadata for a loaded mod.
 *
 * @param id mod identifier; {@code null} becomes empty
 * @param name display name; {@code null} falls back to the identifier
 * @param description description text; {@code null} becomes empty
 * @param license declared license text; {@code null} becomes empty
 * @param authors author names; {@code null} and null entries are discarded from the immutable snapshot
 */
public record UniversalModContainer(@NotNull String id, @NotNull String name, @NotNull String description, @NotNull String license, @NotNull List<String> authors) {

    /**
     * Normalizes optional loader metadata and snapshots the author list.
     */
    public UniversalModContainer {
        id = Objects.requireNonNullElse(id, "");
        name = Objects.requireNonNullElse(name, id);
        description = Objects.requireNonNullElse(description, "");
        license = Objects.requireNonNullElse(license, "");
        authors = authors == null ? List.of() : authors.stream().filter(Objects::nonNull).toList();
    }

}
