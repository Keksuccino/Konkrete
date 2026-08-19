package de.keksuccino.konkrete.networking.packets.placeholders.nbt;

import de.keksuccino.konkrete.util.nbt.NbtNumericValueFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable arguments for one vanilla server-side NBT query.
 *
 * @param sourceType {@code entity}, {@code block}, or {@code storage}
 * @param entitySelector entity selector used by an entity source
 * @param blockPosition command-style position used by a block source
 * @param storageId namespaced command-storage ID
 * @param nbtPath optional vanilla NBT path
 * @param returnType {@code value}, {@code string}, {@code snbt}, or {@code json}
 * @param scale optional numeric multiplier
 */
public record ServerNbtQuery(@Nullable String sourceType, @Nullable String entitySelector, @Nullable String blockPosition, @Nullable String storageId, @Nullable String nbtPath, @Nullable String returnType, @Nullable Double scale) {

    /**
     * Builds and normalizes a query from placeholder argument values.
     *
     * @param values serialized placeholder arguments
     * @return immutable normalized query
     */
    public static @NotNull ServerNbtQuery fromPlaceholderValues(@NotNull Map<String, String> values) {
        Map<String, String> exactValues = Objects.requireNonNull(values, "values");
        return new ServerNbtQuery(normalize(exactValues.get("source_type")), normalize(exactValues.get("entity_selector")), normalize(exactValues.get("block_pos")), normalize(exactValues.get("storage_id")), normalize(exactValues.get("nbt_path")), normalize(exactValues.get("return_type")), NbtNumericValueFormatter.parseOptionalScale(exactValues.get("scale")));
    }

    private static @Nullable String normalize(@Nullable String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
