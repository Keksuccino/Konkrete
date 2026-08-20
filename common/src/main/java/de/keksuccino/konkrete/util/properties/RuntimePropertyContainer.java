package de.keksuccino.konkrete.util.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Non-serializable, typed key-value storage for transient runtime state. Instances are not thread-safe.
 */
public class RuntimePropertyContainer {

    private final Map<String, RuntimeProperty<?>> properties = new LinkedHashMap<>();

    /** Creates an empty runtime-property container. */
    public RuntimePropertyContainer() {}

    /**
     * Adds or replaces a runtime property.
     *
     * @param key property key
     * @param value property value
     * @return this container
     * @param <T> value type
     */
    public <T> RuntimePropertyContainer putProperty(@NotNull String key, @Nullable T value) {
        this.properties.put(Objects.requireNonNull(key, "key"), new RuntimeProperty<>(value));
        return this;
    }

    /**
     * Adds a runtime property only when the key is absent.
     *
     * @param key property key
     * @param value property value
     * @return this container
     * @param <T> value type
     */
    public <T> RuntimePropertyContainer putPropertyIfAbsent(@NotNull String key, @Nullable T value) {
        this.properties.putIfAbsent(Objects.requireNonNull(key, "key"), new RuntimeProperty<>(value));
        return this;
    }

    /**
     * Adds a non-null default when absent and returns the stored value using the default's type.
     *
     * @param key property key
     * @param value default value
     * @return stored value
     * @param <T> value type
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T putPropertyIfAbsentAndGet(@NotNull String key, @NotNull T value) {
        T checkedValue = Objects.requireNonNull(value, "value");
        this.putPropertyIfAbsent(key, checkedValue);
        Class<T> valueType = (Class<T>)checkedValue.getClass();
        return Objects.requireNonNull(this.getProperty(key, valueType));
    }

    /**
     * Reads a Boolean property.
     *
     * @param key property key
     * @return stored value, or {@code null}
     */
    @Nullable
    public Boolean getBooleanProperty(@NotNull String key) {
        return this.getProperty(key, Boolean.class);
    }

    /**
     * Reads a String property.
     *
     * @param key property key
     * @return stored value, or {@code null}
     */
    @Nullable
    public String getStringProperty(@NotNull String key) {
        return this.getProperty(key, String.class);
    }

    /**
     * Reads an Integer property.
     *
     * @param key property key
     * @return stored value, or {@code null}
     */
    @Nullable
    public Integer getIntegerProperty(@NotNull String key) {
        return this.getProperty(key, Integer.class);
    }

    /**
     * Reads a property after verifying its runtime type.
     *
     * @param key property key
     * @param propertyType expected type
     * @return stored value, or {@code null} when absent or of a different type
     * @param <T> value type
     */
    @Nullable
    public <T> T getProperty(@NotNull String key, @NotNull Class<T> propertyType) {
        RuntimeProperty<?> property = this.properties.get(Objects.requireNonNull(key, "key"));
        if (property == null || property.value() == null || !propertyType.isInstance(property.value())) return null;
        return propertyType.cast(property.value());
    }

    /**
     * Tests whether a key is present, including a key mapped to {@code null}.
     *
     * @param key property key
     * @return whether the key exists
     */
    public boolean hasProperty(@NotNull String key) {
        return this.properties.containsKey(Objects.requireNonNull(key, "key"));
    }

    /**
     * Removes a property.
     *
     * @param key property key
     * @return this container
     */
    public RuntimePropertyContainer removeProperty(@NotNull String key) {
        this.properties.remove(Objects.requireNonNull(key, "key"));
        return this;
    }

    /** Clears all runtime properties. */
    public void clear() {
        this.properties.clear();
    }

    /**
     * Immutable wrapper preserving whether a key exists independently of a nullable value.
     *
     * @param value wrapped value
     * @param <T> value type
     */
    public record RuntimeProperty<T>(@Nullable T value) {

    }

}
