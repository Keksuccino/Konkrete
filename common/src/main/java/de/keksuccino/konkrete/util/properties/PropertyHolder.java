package de.keksuccino.konkrete.util.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Owns an ordered map of typed runtime properties.
 */
public interface PropertyHolder {

    /**
     * Returns the mutable backing map used by this holder.
     *
     * @return property map keyed by serialized property name
     */
    @NotNull
    Map<String, Property<?>> getPropertyMap();

    /**
     * Returns a snapshot of the holder's properties.
     *
     * @return property snapshot in map iteration order
     */
    @NotNull
    default List<Property<?>> getProperties() {
        return new ArrayList<>(this.getPropertyMap().values());
    }

    /**
     * Looks up a property by key.
     *
     * @param key property key
     * @return matching property, or {@code null}
     */
    @Nullable
    default Property<?> getProperty(@NotNull String key) {
        return this.getPropertyMap().get(Objects.requireNonNull(key, "key"));
    }

    /**
     * Adds or replaces a property.
     *
     * @param property property to store
     * @return the supplied property for fluent initialization
     * @param <P> property type
     */
    default <P extends Property<?>> P putProperty(@NotNull P property) {
        P checkedProperty = Objects.requireNonNull(property, "property");
        this.getPropertyMap().put(checkedProperty.getKey(), checkedProperty);
        return checkedProperty;
    }

    /**
     * Removes a property by key.
     *
     * @param key property key
     */
    default void removeProperty(@NotNull String key) {
        this.getPropertyMap().remove(Objects.requireNonNull(key, "key"));
    }

}
