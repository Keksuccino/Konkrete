package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface AccessorMixinMinecraft {

    @Accessor("reloadStateTracker") ResourceLoadStateTracker get_reloadStateTracker_Konkrete();

}
