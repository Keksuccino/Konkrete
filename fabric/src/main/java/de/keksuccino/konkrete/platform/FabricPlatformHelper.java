package de.keksuccino.konkrete.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinKeyMapping;
import de.keksuccino.konkrete.platform.services.IPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class FabricPlatformHelper implements IPlatformHelper {

    private static final AtomicReference<MinecraftServer> CURRENT_SERVER = new AtomicReference<>();

    static {
        ServerLifecycleEvents.SERVER_STARTING.register(CURRENT_SERVER::set);
        // Only clear the instance stopped by this callback, so a delayed old-server event cannot erase a newer server.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CURRENT_SERVER.compareAndSet(server, null));
    }

    @Override
    public String getPlatformName() {
        return "fabric";
    }

    @Override
    public String getPlatformDisplayName() {
        return "Fabric";
    }

    @Override
    public String getLoaderVersion() {
        return this.getModVersion("fabric");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        try {
            Optional<ModContainer> o = FabricLoader.getInstance().getModContainer(modId);
            if (o.isPresent()) {
                ModContainer c = o.get();
                return c.getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0";
    }

    @Override
    public List<String> getLoadedModIds() {
        List<String> l = new ArrayList<>();
        for (ModContainer info : FabricLoader.getInstance().getAllMods()) {
            l.add(info.getMetadata().getId());
        }
        return l;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isOnClient() {
        return (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return CURRENT_SERVER.get();
    }

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return ((AccessorMixinKeyMapping) keyMapping).get_key_Konkrete();
    }

    @Override
    public void setKeyMappingKey(KeyMapping keyMapping, InputConstants.Key key) {
        keyMapping.setKey(key);
    }

}
