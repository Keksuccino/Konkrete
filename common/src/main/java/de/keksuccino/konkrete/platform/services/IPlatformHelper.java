package de.keksuccino.konkrete.platform.services;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.konkrete.util.mod.UniversalModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Provides loader, mod metadata, registry, and client-resource access to common code.
 */
public interface IPlatformHelper {

    /**
     * Returns the stable lowercase loader identifier.
     *
     * @return loader identifier
     */
    String getPlatformName();

    /**
     * Returns the loader's user-facing name.
     *
     * @return loader display name
     */
    String getPlatformDisplayName();

    /**
     * Returns the active loader version.
     *
     * @return loader version, or {@code 0.0.0} when unavailable
     */
    String getLoaderVersion();

    /**
     * Checks whether a mod is loaded.
     *
     * @param modId mod identifier
     * @return whether the mod is loaded
     */
    boolean isModLoaded(String modId);

    /**
     * Returns a loaded mod's version.
     *
     * @param modId mod identifier
     * @return mod version, or {@code 0.0.0} when unavailable
     */
    String getModVersion(String modId);

    /**
     * Returns all loaded mod identifiers.
     *
     * @return loaded mod identifiers
     */
    List<String> getLoadedModIds();

    /**
     * Checks whether the game is running in a development environment.
     *
     * @return whether this is a development environment
     */
    boolean isDevelopmentEnvironment();

    /**
     * Checks whether the current distribution is the client.
     *
     * @return whether this is the client distribution
     */
    boolean isOnClient();

    /**
     * Returns the input key bound to a key mapping.
     *
     * @param keyMapping key mapping
     * @return bound input key
     */
    InputConstants.Key getKeyMappingKey(KeyMapping keyMapping);

    /**
     * Returns an item's registry identifier.
     *
     * @param item item to identify
     * @return registry identifier, or {@code null} when unavailable
     */
    @Nullable
    default Identifier getItemKey(@NotNull Item item) {
        return getRegistryKey(BuiltInRegistries.ITEM, item);
    }

    /**
     * Returns a mob effect's registry identifier.
     *
     * @param effect effect to identify
     * @return registry identifier, or {@code null} when unavailable
     */
    @Nullable
    default Identifier getEffectKey(@NotNull MobEffect effect) {
        return getRegistryKey(BuiltInRegistries.MOB_EFFECT, effect);
    }

    /**
     * Returns an entity type's registry identifier.
     *
     * @param type entity type to identify
     * @return registry identifier, or {@code null} when unavailable
     */
    @Nullable
    default Identifier getEntityKey(@NotNull EntityType<?> type) {
        return getRegistryKey(BuiltInRegistries.ENTITY_TYPE, type);
    }

    @Nullable
    private static <T> Identifier getRegistryKey(@NotNull Registry<T> registry, @NotNull T value) {
        try {
            return registry.getKey(value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Returns metadata for all loaded mods. The default provides immutable minimal snapshots for legacy service
     * implementations; loader implementations should override it when richer metadata is available.
     *
     * @return loaded mod metadata
     */
    @NotNull
    default List<UniversalModContainer> getLoadedMods() {
        return this.getLoadedModIds().stream().map(id -> new UniversalModContainer(id, id, "", "", List.of())).toList();
    }

    /**
     * Returns every loaded client resource identifier contributed by active packs.
     *
     * @return loaded client resource identifiers, or an immutable empty set when unsupported
     */
    @NotNull
    default Set<Identifier> getLoadedClientResourceLocations() {
        return Set.of();
    }

    /**
     * Finds loaded mod metadata by identifier.
     *
     * @param id mod identifier
     * @return matching mod metadata, or {@code null} when absent
     */
    @Nullable
    default UniversalModContainer getLoadedMod(@NotNull String id) {
        for (UniversalModContainer mod : this.getLoadedMods()) {
            if (id.equals(mod.id())) return mod;
        }
        return null;
    }

    /**
     * Returns the current environment type as a display string.
     *
     * @return {@code development} or {@code production}
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

}
