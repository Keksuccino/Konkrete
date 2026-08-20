package de.keksuccino.konkrete.placeholder.placeholders.client;

import de.keksuccino.konkrete.util.minecraftoptions.MinecraftOption;
import de.keksuccino.konkrete.util.minecraftoptions.MinecraftOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Configures named Minecraft option access without a hardcoded option catalog. */
public final class MinecraftOptionValueProviders {

    private static final MinecraftOptionValueProvider VANILLA_PROVIDER = MinecraftOptionValueProviders::getVanillaOptionValue;
    private static volatile MinecraftOptionValueProvider provider = VANILLA_PROVIDER;

    private MinecraftOptionValueProviders() {
    }

    /** Atomically replaces the process-wide named-option provider. */
    public static void set(@NotNull MinecraftOptionValueProvider newProvider) {
        provider = Objects.requireNonNull(newProvider, "newProvider");
    }

    /** Returns the latest named-option provider with volatile visibility. */
    @NotNull public static MinecraftOptionValueProvider get() {
        return provider;
    }

    /** Restores the provider backed by Konkrete's vanilla and third-party Minecraft option registry. */
    public static void reset() {
        provider = VANILLA_PROVIDER;
    }

    @org.jetbrains.annotations.Nullable
    private static String getVanillaOptionValue(@NotNull String optionName) {
        MinecraftOption option = MinecraftOptions.getOption(optionName);
        return option != null ? option.get() : null;
    }

}
