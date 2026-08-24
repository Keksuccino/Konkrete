package de.keksuccino.konkrete.config.v2;

import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * A named JSON object used to organize related configuration values.
 */
public final class ConfigSection {

    private final JsonConfig config;
    private final String name;

    ConfigSection(@NotNull JsonConfig config, @NotNull String name) {
        this.config = Objects.requireNonNull(config);
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Defines a value and infers its stored type from the default value.
     */
    @NotNull
    public <T> ConfigValue<T> option(@NotNull String key, @NotNull T defaultValue) {
        return this.config.option(this.name, key, defaultValue);
    }

    /**
     * Defines a value with an explicit type. This overload supports parameterized types from a Gson TypeToken.
     */
    @NotNull
    public <T> ConfigValue<T> option(@NotNull String key, @NotNull Type type, @NotNull T defaultValue) {
        return this.config.option(this.name, key, type, defaultValue);
    }

    /**
     * Defines a value that is absent until explicitly set and can be read with a call-site fallback.
     */
    @NotNull
    public <T> ConfigValue<T> optional(@NotNull String key, @NotNull Class<T> type) {
        return this.config.optional(this.name, key, type);
    }

    /**
     * Defines an optional value with a parameterized or otherwise non-Class type.
     */
    @NotNull
    public <T> ConfigValue<T> optional(@NotNull String key, @NotNull Type type) {
        return this.config.optional(this.name, key, type);
    }

    @NotNull
    public String getName() {
        return this.name;
    }

}
