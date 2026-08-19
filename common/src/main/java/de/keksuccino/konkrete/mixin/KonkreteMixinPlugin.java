package de.keksuccino.konkrete.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class KonkreteMixinPlugin implements IMixinConfigPlugin {

    private static final String RINKU_MIXIN_SEGMENT_KONKRETE = ".compat.rinku.";
    private static final String WATERMEDIA_MIXIN_SEGMENT_KONKRETE = ".compat.watermedia.";
    private boolean rinkuPresent_Konkrete;
    private boolean watermediaPresent_Konkrete;

    @Override
    public void onLoad(String mixinPackage) {
        ClassLoader classLoader = KonkreteMixinPlugin.class.getClassLoader();
        this.rinkuPresent_Konkrete = classLoader.getResource("de/keksuccino/rinku/Rinku.class") != null;
        this.watermediaPresent_Konkrete = classLoader.getResource("org/watermedia/api/media/MediaAPI.class") != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(RINKU_MIXIN_SEGMENT_KONKRETE)) return this.rinkuPresent_Konkrete;
        if (mixinClassName.contains(WATERMEDIA_MIXIN_SEGMENT_KONKRETE)) return this.watermediaPresent_Konkrete;
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
