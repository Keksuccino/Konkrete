package de.keksuccino.konkrete.util;

import de.keksuccino.konkrete.config.Config;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/** Backs typed configuration options with defaults and persistence. */
public abstract class AbstractOptions {

    /** Reads and writes one typed configuration entry. */
    @SuppressWarnings("unused")
    public static class Option<T> {

        /** Persistent configuration backing this option. */
        protected final Config config;
        /** Stable key used for reads and writes. */
        protected final String key;
        /** Non-null fallback and type token used for registration. */
        protected final T defaultValue;
        /** Configuration category containing this key. */
        protected final String category;

        /** Initializes and registers a configuration option. */
        public Option(@NotNull Config config, @NotNull String key, @NotNull T defaultValue, @NotNull String category) {
            this.config = Objects.requireNonNull(config);
            this.key = Objects.requireNonNull(key);
            this.defaultValue = Objects.requireNonNull(defaultValue);
            this.category = Objects.requireNonNull(category);
            this.register();
        }

        /** Registers the supported primitive-wrapper or string default with the backing configuration. */
        protected void register() {
            boolean unsupported = false;
            try {
                if (this.defaultValue instanceof Integer) {
                    this.config.registerValue(this.key, (int) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof Double) {
                    this.config.registerValue(this.key, (double) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof Long) {
                    this.config.registerValue(this.key, (long) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof Float) {
                    this.config.registerValue(this.key, (float) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof Boolean) {
                    this.config.registerValue(this.key, (boolean) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof String) {
                    this.config.registerValue(this.key, (String) this.defaultValue, this.category);
                } else {
                    unsupported = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (unsupported) throw new UnsupportedOptionTypeException("Tried to register Option with unsupported type: " + this.key + " (" + this.defaultValue.getClass().getName() + ")");
        }

        /** Returns the persisted value, falling back to the registered default. */
        @NotNull
        public T getValue() {
            return this.config.getOrDefault(this.key, this.defaultValue);
        }

        /** Persists a supported value; {@code null} resets to the default. */
        public Option<T> setValue(T value) {
            try {
                if (value == null) value = this.getDefaultValue();
                if (value instanceof Integer) {
                    this.config.setValue(this.key, (int) value);
                } else if (value instanceof Double) {
                    this.config.setValue(this.key, (double) value);
                } else if (value instanceof Long) {
                    this.config.setValue(this.key, (long) value);
                } else if (value instanceof Float) {
                    this.config.setValue(this.key, (float) value);
                } else if (value instanceof Boolean) {
                    this.config.setValue(this.key, (boolean) value);
                } else if (value instanceof String) {
                    this.config.setValue(this.key, (String) value);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return this;
        }

        /** Persists the default value and returns this option. */
        public Option<T> resetToDefault() {
            this.setValue(null);
            return this;
        }

        /** Returns the immutable non-null default. */
        @NotNull
        public T getDefaultValue() {
            return this.defaultValue;
        }

        /** Returns the stable configuration key. */
        @NotNull
        public String getKey() {
            return this.key;
        }

    }

    /**
     * Thrown when trying to register an Option with an unsupported type.
     */
    @SuppressWarnings("unused")
    public static class UnsupportedOptionTypeException extends RuntimeException {

        /** Creates an exception for an unsupported value type. */
        public UnsupportedOptionTypeException() {
            super();
        }

        /** Creates an exception for an unsupported value type. */
        public UnsupportedOptionTypeException(String msg) {
            super(msg);
        }

    }

}
