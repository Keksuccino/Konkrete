package de.keksuccino.konkrete.placeholder.placeholders.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Injectable lookup for dynamically named Minecraft option values. */
@FunctionalInterface
public interface MinecraftOptionValueProvider {

    /** Returns the serialized option value, or {@code null} when the name is unsupported. */
    @Nullable String get(@NotNull String optionName);

}
