package de.keksuccino.persephone.mixin.mixins.common.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface AccessorMixinGui {

    /**
     * @reason Set the screen instance directly without the need to go through the normal {@link Gui#setScreen(Screen)}.
     */
    @Accessor("screen") void set_screen_Persephone(Screen screen);

}
