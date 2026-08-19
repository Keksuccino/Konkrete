package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelLoadingScreen.class)
public interface AccessorMixinLevelLoadingScreen {

    @Accessor("loadTracker") LevelLoadTracker get_loadTracker_Konkrete();

}
