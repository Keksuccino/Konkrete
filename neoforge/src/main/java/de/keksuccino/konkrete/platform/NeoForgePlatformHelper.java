package de.keksuccino.konkrete.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.konkrete.platform.services.IPlatformHelper;
import de.keksuccino.konkrete.util.mod.UniversalModContainer;
import de.keksuccino.konkrete.util.resource.ClientResourceIndex;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * NeoForge implementation of loader, metadata, registry, and client-resource access.
 */
public class NeoForgePlatformHelper implements IPlatformHelper {

    /** {@inheritDoc} */
    @Override
    public String getPlatformName() {
        return "neoforge";
    }

    /** {@inheritDoc} */
    @Override
    public String getPlatformDisplayName() {
        return "NeoForge";
    }

    /** {@inheritDoc} */
    @Override
    public String getLoaderVersion() {
        return this.getModVersion("neoforge");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** {@inheritDoc} */
    @Override
    public String getModVersion(String modId) {
        try {
            Optional<? extends ModContainer> container = ModList.get().getModContainerById(modId);
            if (container.isPresent()) {
                return container.get().getModInfo().getVersion().toString();
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
        for (IModInfo mod : ModList.get().getMods()) {
            modIds.add(mod.getModId());
        }
        return modIds;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOnClient() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    /** {@inheritDoc} */
    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return keyMapping.getKey();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull List<UniversalModContainer> getLoadedMods() {
        List<UniversalModContainer> mods = new ArrayList<>();
        ModList.get().getMods().forEach(mod -> mods.add(new UniversalModContainer(mod.getModId(), mod.getDisplayName(), mod.getDescription(), mod.getOwningFile().getLicense(), getAuthors(mod))));
        return mods;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull Set<Identifier> getLoadedClientResourceLocations() {
        if (!this.isOnClient()) return Set.of();
        return ClientResourceIndex.getLoadedLocations(Minecraft.getInstance().getResourceManager());
    }

    private static List<String> getAuthors(IModInfo mod) {
        return mod.getConfig().<String>getConfigElement("authors").filter(authors -> !authors.isBlank()).map(List::of).orElseGet(List::of);
    }

}
