package de.keksuccino.konkrete.input;

import java.util.function.Consumer;

/**
 * Does not work anymore. Do not use this class.
 */
@Deprecated
public class KeyboardHandler {

	private static boolean ctrlPressed = false;
	private static boolean altPressed = false;
	private static boolean keyPressed = false;
	private static int keycode = 0;
	private static int scancode = 0;
	private static char typedChar = ' ';
	private static int charModifiers = 0;
	private static int modifiers = 0;

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static boolean isKeyPressed() {
		return keyPressed;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static int getCurrentKeyCode() {
		return keycode;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static int getCurrentKeyScanCode() {
		return scancode;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static int getCurrentKeyModifiers() {
		return modifiers;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static char getCurrentChar() {
		return typedChar;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static int getCurrentCharModifiers() {
		return charModifiers;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static int addKeyPressedListener(Consumer<KeyboardData> c) {
		return 0;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static int addKeyReleasedListener(Consumer<KeyboardData> c) {
		return 0;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static int addCharTypedListener(Consumer<CharData> c) {
		return 0;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static void removeKeyPressedListener(int id) {
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static void removeKeyReleasedListener(int id) {
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static void removeCharTypedListener(int id) {
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static boolean isCtrlPressed() {
		return ctrlPressed;
	}

	/**
	 * Does not work anymore. Do not use this.
	 */
	@Deprecated
	public static boolean isAltPressed() {
		return altPressed;
	}

}
