package de.keksuccino.konkrete.config.v2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A typed handle to one value in a {@link JsonConfig}.
 */
public final class ConfigValue<T> {

    private final JsonConfig config;
    private final JsonConfig.ValuePath path;
    private final Type type;
    @Nullable
    private final T defaultValue;
    private final boolean hasDefaultValue;
    @Nullable
    private volatile T storedValue;

    ConfigValue(@NotNull JsonConfig config, @NotNull JsonConfig.ValuePath path, @NotNull Type type, @Nullable T defaultValue, boolean hasDefaultValue) {
        this.config = Objects.requireNonNull(config);
        this.path = Objects.requireNonNull(path);
        this.type = Objects.requireNonNull(type);
        this.defaultValue = defaultValue;
        this.hasDefaultValue = hasDefaultValue;
    }

    /**
     * Gets the stored value or this option's declared default.
     *
     * @throws IllegalStateException when called for an absent optional value
     */
    @NotNull
    public T getValue() {
        T value = this.storedValue;
        if (value != null) return value;
        if (this.hasDefaultValue) return Objects.requireNonNull(this.defaultValue);
        throw new IllegalStateException("Optional config value '" + this.getQualifiedKey() + "' is absent and has no default value.");
    }

    /**
     * Gets the stored value without requiring this handle to have a declared default.
     */
    @Nullable
    public T getValueOrNull() {
        T value = this.storedValue;
        return value != null ? value : this.defaultValue;
    }

    /**
     * Gets the stored value or the supplied fallback without storing the fallback.
     */
    @NotNull
    public T getValueOrDefault(@NotNull T fallback) {
        T value = this.getValueOrNull();
        return value != null ? value : Objects.requireNonNull(fallback);
    }

    /**
     * Stores a value immediately. Passing {@code null} resets a defaulted option or removes an optional value.
     */
    @NotNull
    public ConfigValue<T> setValue(@Nullable T value) {
        if (value == null) {
            if (this.hasDefaultValue) {
                this.config.setValue(this, Objects.requireNonNull(this.defaultValue));
            } else {
                this.config.removeValue(this);
            }
        } else {
            this.config.setValue(this, value);
        }
        return this;
    }

    /**
     * Atomically derives and stores a new value from the current value.
     */
    @NotNull
    public ConfigValue<T> update(@NotNull UnaryOperator<T> updater) {
        this.config.updateValue(this, Objects.requireNonNull(updater));
        return this;
    }

    /**
     * Restores and stores the declared default value.
     *
     * @throws IllegalStateException when called for an optional value
     */
    @NotNull
    public ConfigValue<T> resetToDefault() {
        if (!this.hasDefaultValue) throw new IllegalStateException("Optional config value '" + this.getQualifiedKey() + "' has no default value.");
        return this.setValue(this.defaultValue);
    }

    /**
     * Removes the stored JSON property. A defaulted option will still resolve to its in-memory default.
     */
    @NotNull
    public ConfigValue<T> remove() {
        this.config.removeValue(this);
        return this;
    }

    public boolean isPresent() {
        return this.storedValue != null;
    }

    public boolean hasDefaultValue() {
        return this.hasDefaultValue;
    }

    @NotNull
    public T getDefaultValue() {
        if (!this.hasDefaultValue) throw new IllegalStateException("Optional config value '" + this.getQualifiedKey() + "' has no default value.");
        return Objects.requireNonNull(this.defaultValue);
    }

    @NotNull
    public String getKey() {
        return this.path.getKey();
    }

    @Nullable
    public String getSection() {
        return this.path.getSection();
    }

    @NotNull
    public String getQualifiedKey() {
        return this.path.toString();
    }

    Type getType() {
        return this.type;
    }

    JsonConfig.ValuePath getPath() {
        return this.path;
    }

    @Nullable
    T getStoredValue() {
        return this.storedValue;
    }

    void setStoredValue(@Nullable T storedValue) {
        this.storedValue = storedValue;
    }

}
