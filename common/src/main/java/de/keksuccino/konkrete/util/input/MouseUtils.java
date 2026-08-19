package de.keksuccino.konkrete.util.input;

import net.minecraft.client.Minecraft;

/** Utility methods for mouse. */
public class MouseUtils {

    /** Returns the scaled mouse x. */
    public static double getScaledMouseX() {
        return Minecraft.getInstance().mouseHandler.getScaledXPos(Minecraft.getInstance().getWindow());
    }

    /** Returns the scaled mouse y. */
    public static double getScaledMouseY() {
        return Minecraft.getInstance().mouseHandler.getScaledYPos(Minecraft.getInstance().getWindow());
    }

}
