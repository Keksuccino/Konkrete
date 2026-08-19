package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface AccessorMixinGui {

    /** Sets the screen field without running the normal {@link Gui#setScreen(Screen)} transition logic. */
    @Accessor("screen") void set_screen_Konkrete(Screen screen);

}
