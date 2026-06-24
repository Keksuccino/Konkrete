package de.keksuccino.konkrete.rendering;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import javax.annotation.Nullable;

public class GuiUtils {

    /**
     * This method makes it possible to set the active {@link Screen} directly without the need to go through {@link Gui#setScreen(Screen)}.<br>
     * The method will only update the {@code screen} field without doing anything else.
     */
    public static void setScreenDirect(@Nullable Screen screen) {
        ((AccessorMixinGui)Minecraft.getInstance().gui).set_screen_Konkrete(screen);
    }

}
