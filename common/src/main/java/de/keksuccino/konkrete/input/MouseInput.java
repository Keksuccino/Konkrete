package de.keksuccino.konkrete.input;

import de.keksuccino.konkrete.mixin.mixins.client.IMixinMouseHandler;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

public class MouseInput {

	private static boolean useRenderScale = false;
	private static float renderScale = 1.0F;
	@ApiStatus.Internal
	public static boolean mouseHandler_screenLeftMouseDown = false;
	@ApiStatus.Internal
	public static boolean mouseHandler_screenRightMouseDown = false;
	
	public static int getActiveMouseButton() {
		return ((IMixinMouseHandler)Minecraft.getInstance().mouseHandler).getActiveButtonKonkrete();
	}

	public static boolean isLeftMouseDown() {
		return mouseHandler_screenLeftMouseDown;
	}

	public static boolean isRightMouseDown() {
		return mouseHandler_screenRightMouseDown;
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
