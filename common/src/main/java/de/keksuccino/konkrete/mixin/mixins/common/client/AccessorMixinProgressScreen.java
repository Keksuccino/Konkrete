package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.screens.ProgressScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProgressScreen.class)
public interface AccessorMixinProgressScreen {

    @Accessor("progress") int get_progress_Konkrete();

}
