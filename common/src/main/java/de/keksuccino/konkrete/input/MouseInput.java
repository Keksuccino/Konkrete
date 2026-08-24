package de.keksuccino.konkrete.input;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinMouseHandler;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

public class MouseInput {

	@ApiStatus.Internal
	public static boolean mouseHandler_screenLeftMouseDown = false;
	@ApiStatus.Internal
	public static boolean mouseHandler_screenRightMouseDown = false;
	
	public static int getActiveMouseButton() {
		return ((AccessorMixinMouseHandler)Minecraft.getInstance().mouseHandler).get_lastClickButton_Konkrete();
	}

	public static boolean isLeftMouseDown() {
		return mouseHandler_screenLeftMouseDown;
	}

	public static boolean isRightMouseDown() {
		return mouseHandler_screenRightMouseDown;
	}
	
	public static int getMouseX() {
        return (int)(Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
	}
	
	public static int getMouseY() {
        return (int)(Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
	}

}
