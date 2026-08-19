package de.keksuccino.konkrete.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.konkrete.platform.services.IPlatformHelper;
import de.keksuccino.konkrete.util.mod.UniversalModContainer;
import de.keksuccino.konkrete.util.resource.ClientResourceIndex;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fabric implementation of loader, metadata, registry, and client-resource access.
 */
public class FabricPlatformHelper implements IPlatformHelper {

    /** {@inheritDoc} */
    @Override
    public String getPlatformName() {
        return "fabric";
    }

    /** {@inheritDoc} */
    @Override
    public String getPlatformDisplayName() {
        return "Fabric";
    }

    /** {@inheritDoc} */
    @Override
    public String getLoaderVersion() {
        return this.getModVersion("fabricloader");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    /** {@inheritDoc} */
    @Override
    public String getModVersion(String modId) {
        try {
            Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(modId);
            if (container.isPresent()) {
                return container.get().getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "0.0.0";
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getLoadedModIds() {
        List<String> modIds = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            modIds.add(mod.getMetadata().getId());
        }
        return modIds;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOnClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    /** {@inheritDoc} */
    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return InputConstants.getKey(keyMapping.saveString());
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull List<UniversalModContainer> getLoadedMods() {
        List<UniversalModContainer> mods = new ArrayList<>();
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            ModMetadata metadata = mod.getMetadata();
            List<String> authors = new ArrayList<>();
            metadata.getAuthors().forEach(person -> authors.add(person.getName()));
            mods.add(new UniversalModContainer(metadata.getId(), metadata.getName(), metadata.getDescription(), String.join("\n", metadata.getLicense()), authors));
        });
        return mods;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull Set<Identifier> getLoadedClientResourceLocations() {
        if (!this.isOnClient()) return Set.of();
        return ClientResourceIndex.getLoadedLocations(Minecraft.getInstance().getResourceManager());
    }

}
