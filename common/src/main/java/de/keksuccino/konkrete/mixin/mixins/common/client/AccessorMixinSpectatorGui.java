package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpectatorGui.class)
public interface AccessorMixinSpectatorGui {

    @Accessor("menu") SpectatorMenu get_menu_Konkrete();

    @Invoker("getHotbarAlpha") float invoke_getHotbarAlpha_Konkrete();

}
