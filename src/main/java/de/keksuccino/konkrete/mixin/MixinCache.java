package de.keksuccino.konkrete.mixin;

import net.minecraft.client.gui.screens.Screen;

public class MixinCache {
	
	public static int currentMouseButton = -1;
	
	public static int currentKeyboardKey = -1;
	public static int currentKeyboardScancode = -1;
	public static int currentKeyboardModifiers = -1;
	public static int currentKeyboardAction = -1;
	
	public static int currentKeyboardChar = -1;
	public static int currentKeyboardCharModifiers = -1;

	public static boolean triggerInitCompleted = false;

	public static Screen cachedCurrentScreen = null;

}
