package de.keksuccino.konkrete.events.client.mixinbase;

import net.minecraft.client.util.math.MatrixStack;

public class MixinCache {
	
	public static int currentMouseButton = -1;
	
	public static int currentKeyboardKey = -1;
	public static int currentKeyboardScancode = -1;
	public static int currentKeyboardModifiers = -1;
	public static int currentKeyboardAction = -1;
	
	public static int currentKeyboardChar = -1;
	public static int currentKeyboardCharModifiers = -1;
	
	//TODO neu in 1.17
	public static boolean triggerInitCompleted = false;

}
