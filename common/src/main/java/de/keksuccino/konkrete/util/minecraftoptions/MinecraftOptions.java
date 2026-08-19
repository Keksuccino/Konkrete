package de.keksuccino.konkrete.util.minecraftoptions;

import de.keksuccino.konkrete.mixin.support.client.OptionsFieldAccessBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Enumerates and extends Minecraft's runtime option registry without exposing mutable cache state. Calls that touch vanilla options must run on the client thread.
 */
public final class MinecraftOptions {

    private static final Object LOCK = new Object();
    private static final Map<String, MinecraftOption> REGISTERED_OPTIONS = new LinkedHashMap<>();
    private static Map<String, MinecraftOption> cachedOptions = Map.of();
    @Nullable private static Options cachedOwner;

    private MinecraftOptions() {}

    /**
     * Returns an immutable option snapshot in vanilla serialization order followed by caller registrations.
     *
     * @return option snapshot
     */
    @NotNull
    public static Map<String, MinecraftOption> getOptions() {
        synchronized (LOCK) {
            Options options = Minecraft.getInstance().options;
            if (cachedOwner != options || cachedOptions.isEmpty()) rebuildCache(options);
            return cachedOptions;
        }
    }

    /**
     * Looks up an option by its serialized name.
     *
     * @param name serialized name
     * @return option wrapper, or {@code null}
     */
    @Nullable
    public static MinecraftOption getOption(@NotNull String name) {
        return getOptions().get(Objects.requireNonNull(name, "name"));
    }

    /**
     * Registers or replaces an additional option exposed after vanilla options.
     *
     * @param option option wrapper
     * @return prior registration under the same name, or {@code null}
     */
    @Nullable
    public static MinecraftOption registerOption(@NotNull MinecraftOption option) {
        MinecraftOption checkedOption = Objects.requireNonNull(option, "option");
        synchronized (LOCK) {
            MinecraftOption previous = REGISTERED_OPTIONS.put(checkedOption.getName(), checkedOption);
            cachedOptions = Map.of();
            return previous;
        }
    }

    /**
     * Removes a caller-registered option without affecting vanilla enumeration.
     *
     * @param name serialized option name
     * @return removed registration, or {@code null}
     */
    @Nullable
    public static MinecraftOption unregisterOption(@NotNull String name) {
        synchronized (LOCK) {
            MinecraftOption removed = REGISTERED_OPTIONS.remove(Objects.requireNonNull(name, "name"));
            cachedOptions = Map.of();
            return removed;
        }
    }

    /** Forces vanilla and registered options to be enumerated again on the next access. */
    public static void invalidateCache() {
        synchronized (LOCK) {
            cachedOwner = null;
            cachedOptions = Map.of();
        }
    }

    /** Saves options through vanilla, including key mappings and model-part state. */
    public static void save() {
        Minecraft.getInstance().options.save();
    }

    private static void rebuildCache(Options options) {
        Map<String, MinecraftOption> rebuilt = new LinkedHashMap<>();
        OptionsFieldAccessBridge.collect(options, (name, instance) -> rebuilt.put(name, MinecraftOption.of(name, instance, options)));
        for (KeyMapping keyMapping : options.keyMappings) {
            MinecraftOption option = MinecraftOption.of(keyMapping, options);
            rebuilt.put(option.getName(), option);
        }
        for (PlayerModelPart modelPart : PlayerModelPart.values()) {
            MinecraftOption option = MinecraftOption.of(modelPart, options);
            rebuilt.put(option.getName(), option);
        }
        rebuilt.putAll(REGISTERED_OPTIONS);
        cachedOwner = options;
        cachedOptions = Collections.unmodifiableMap(rebuilt);
    }

}
