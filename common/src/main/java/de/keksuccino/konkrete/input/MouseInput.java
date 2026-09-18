package de.keksuccino.konkrete.input;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinMouseHandler;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.sdl.SDLMouse;

public class MouseInput {

    private static boolean useRenderScale = false;
    private static float renderScale = 1.0F;
    @ApiStatus.Internal
    public static boolean mouseHandler_screenLeftMouseDown = false;
    @ApiStatus.Internal
    public static boolean mouseHandler_screenRightMouseDown = false;

    /**
     * Returns Minecraft's last handled screen button using SDL button IDs (left: 1, middle: 2, right: 3).
     */
    public static int getActiveMouseButton() {
        return ((AccessorMixinMouseHandler)Minecraft.getInstance().mouseHandler).get_lastClickButton_Konkrete();
    }

    public static boolean isLeftMouseDown() {
        return mouseHandler_screenLeftMouseDown;
    }

    public static boolean isRightMouseDown() {
        return mouseHandler_screenRightMouseDown;
    }

    /**
     * Records the current screen event, rather than polling the physical mouse. Keep SDL button IDs here:
     * GLFW IDs differ, and middle/extra buttons must not be reported as right-clicks.
     */
    @ApiStatus.Internal
    public static void updateScreenMouseButton(int button, boolean pressed) {
        mouseHandler_screenLeftMouseDown = pressed && button == SDLMouse.SDL_BUTTON_LEFT;
        mouseHandler_screenRightMouseDown = pressed && button == SDLMouse.SDL_BUTTON_RIGHT;
    }

    /**
     * Clears event state before input dispatch and when the screen is replaced or resized.
     */
    @ApiStatus.Internal
    public static void resetScreenMouseButtons() {
        mouseHandler_screenLeftMouseDown = false;
        mouseHandler_screenRightMouseDown = false;
    }

    public static int getMouseX() {
        int x = (int)(Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
        if (useRenderScale) {
            return (int)(x / renderScale);
        } else {
            return x;
        }
    }

    public static int getMouseY() {
        int y = (int)(Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
        if (useRenderScale) {
            return (int)(y / renderScale);
        } else {
            return y;
        }
    }

    public static void setRenderScale(float scale) {
        renderScale = scale;
        useRenderScale = true;
    }

    public static void resetRenderScale() {
        useRenderScale = false;
    }

    /**
     * Does not work anymore. Do not use this.
     */
    @Deprecated
    public static void blockVanillaInput(String category) {
    }

    /**
     * Does not work anymore. Do not use this.
     */
    @Deprecated
    public static void unblockVanillaInput(String category) {
    }

    /**
     * Does not work anymore. Do not use this.
     */
    @Deprecated
    public static boolean isVanillaInputBlocked() {
        return false;
    }

    /**
     * Does not work anymore. Do not use this.
     */
    @Deprecated
    public static void ignoreBlockedVanillaInput(boolean ignore) {
    }

}
