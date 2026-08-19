package de.keksuccino.konkrete.util.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered serialized properties grouped under a container type. Instances are not thread-safe.
 */
public class PropertyContainer {

    private static final String SERIALIZED_PROPERTY_NEWLINE_TOKEN = "%%!serialized_property_newline!%%";

    private final Map<String, String> entries = new LinkedHashMap<>();
    private String type;
    private boolean invulnerableProperties;

    /**
     * Creates an empty container.
     *
     * @param type container type
     */
    public PropertyContainer(@NotNull String type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Controls whether replacing an existing entry is rejected.
     *
     * @param invulnerableProperties {@code true} to reject replacement
     */
    public void setInvulnerableProperties(boolean invulnerableProperties) {
        this.invulnerableProperties = invulnerableProperties;
    }

    /**
     * Returns whether replacing an existing entry is rejected.
     *
     * @return current replacement policy
     */
    public boolean isInvulnerableProperties() {
        return this.invulnerableProperties;
    }

    /**
     * Stores a serialized property value. Passing {@code null} removes the entry.
     *
     * @param name property name
     * @param value value to serialize
     */
    public void putProperty(@NotNull String name, @Nullable Object value) {
        String checkedName = Objects.requireNonNull(name, "name");
        if (value instanceof Property<?>) throw new IllegalArgumentException("Serialize Property instances through Property.serialize(): " + checkedName);
        if (this.invulnerableProperties && this.entries.containsKey(checkedName)) throw new IllegalStateException("PropertyContainer already contains property: " + checkedName);
        if (value == null) {
            this.entries.remove(checkedName);
            return;
        }
        this.entries.put(checkedName, serializePropertyValue(value.toString()));
    }

    /**
     * Returns an unmodifiable view of the encoded entries.
     *
     * @return encoded entry view
     */
    @NotNull
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(this.entries);
    }

    /**
     * Reads and decodes a property value.
     *
     * @param name property name
     * @return decoded value, or {@code null}
     */
    @Nullable
    public String getValue(@NotNull String name) {
        String raw = this.entries.get(Objects.requireNonNull(name, "name"));
        return raw == null ? null : deserializePropertyValue(raw);
    }

    /**
     * Removes a property.
     *
     * @param name property name
     */
    public void removeProperty(@NotNull String name) {
        this.entries.remove(Objects.requireNonNull(name, "name"));
    }

    /**
     * Tests whether a property exists.
     *
     * @param name property name
     * @return whether the property exists
     */
    public boolean hasProperty(@NotNull String name) {
        return this.entries.containsKey(Objects.requireNonNull(name, "name"));
    }

    /**
     * Returns the container type.
     *
     * @return container type
     */
    @NotNull
    public String getType() {
        return this.type;
    }

    /**
     * Changes the container type.
     *
     * @param type new type
     */
    public void setType(@NotNull String type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    private static String serializePropertyValue(String value) {
        return value.replace("\r\n", SERIALIZED_PROPERTY_NEWLINE_TOKEN).replace("\r", SERIALIZED_PROPERTY_NEWLINE_TOKEN).replace("\n", SERIALIZED_PROPERTY_NEWLINE_TOKEN);
    }

    private static String deserializePropertyValue(String value) {
        return value.replace(SERIALIZED_PROPERTY_NEWLINE_TOKEN, "\n");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "PropertyContainer{type='" + this.type + "', entries=" + this.entries + '}';
    }

}
