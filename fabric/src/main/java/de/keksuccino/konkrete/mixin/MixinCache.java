package de.keksuccino.konkrete.mixin;

import net.minecraft.client.gui.GuiGraphics;

public class MixinCache {

	public static GuiGraphics cachedRenderScreenGuiGraphics = null;
	public static Integer cachedRenderScreenMouseX = null;
	public static Integer cachedRenderScreenMouseY = null;
	public static Float cachedRenderScreenPartial = null;

	public static int currentMouseButton = -1;
	
	public static int currentKeyboardKey = -1;
	public static int currentKeyboardScancode = -1;
	public static int currentKeyboardModifiers = -1;
	public static int currentKeyboardAction = -1;
	
	public static int currentKeyboardChar = -1;
	public static int currentKeyboardCharModifiers = -1;

}
