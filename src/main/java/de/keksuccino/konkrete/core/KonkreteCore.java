package de.keksuccino.konkrete.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.launch.MixinBootstrap;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.SortingIndex(1)
public class KonkreteCore implements IFMLLoadingPlugin {

    public KonkreteCore() {

        LogManager.getLogger().info("[KONKRETE] LOADING CORE PLUGIN!");

        MixinBootstrap.init();
        MixinConfigLoader.loadConfigs();

    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}