package de.keksuccino.konkrete.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.lwjgl.input.Keyboard;

import de.keksuccino.konkrete.math.MathUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class KeyboardHandler {
	
	private static boolean ctrlPressed = false;
	private static boolean altPressed = false;
	
	private static int keycode = 0;
	private static char typedChar = " ".charAt(0);
	private static List<Integer> ids = new ArrayList<Integer>();
	
	//Using seperated lists to add new listeners to prevent Comodifications
	private static Map<Integer, Consumer<KeyboardData>> pressedRaw = new HashMap<Integer, Consumer<KeyboardData>>();
	private static Map<Integer, Consumer<KeyboardData>> releasedRaw = new HashMap<Integer, Consumer<KeyboardData>>();
	
	private static Map<Integer, Consumer<KeyboardData>> keyPressedListeners = new HashMap<Integer, Consumer<KeyboardData>>();
	private static Map<Integer, Consumer<KeyboardData>> keyReleasedListeners = new HashMap<Integer, Consumer<KeyboardData>>();
	
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			MinecraftForge.EVENT_BUS.register(new KeyboardHandler());
			
			addKeyPressedListener((key) -> {
				if ((key.keycode == 29) || (key.keycode == 157)) {
					ctrlPressed = true;
				}
				if (key.keycode == 56) {
					altPressed = true;
				}
			});
			addKeyReleasedListener((key) -> {
				if ((key.keycode == 29) || (key.keycode == 157)) {
					ctrlPressed = false;
				}
				if (key.keycode == 56) {
					altPressed = false;
				}
			});
			
			init = true;
		}
	}
	
	@SubscribeEvent
	public void onKeyPressPost(GuiScreenEvent.KeyboardInputEvent.Post e) {
		keycode = Keyboard.getEventKey();
		typedChar = Keyboard.getEventCharacter();
		
		keyPressedListeners.clear();
		keyPressedListeners.putAll(pressedRaw);
		
		keyReleasedListeners.clear();
		keyReleasedListeners.putAll(releasedRaw);
		
		if (((Keyboard.getEventKey() == 0) && (typedChar >= ' ')) || Keyboard.getEventKeyState()) {
			for (Consumer<KeyboardData> c : keyPressedListeners.values()) {
				c.accept(new KeyboardData(keycode, typedChar));
			}
		}
		if (!Keyboard.getEventKeyState()) {
			for (Consumer<KeyboardData> c : keyReleasedListeners.values()) {
				c.accept(new KeyboardData(keycode, typedChar));
			}
		}
	}
	
	public static int getCurrentKeyCode() {
		return keycode;
	}
	
	public static char getCurrentChar() {
		return typedChar;
	}
	
	/**
	 * Adds a new listener and returns its unique ID.
	 */
	public static int addKeyPressedListener(Consumer<KeyboardData> c) {
		int id = generateUniqueId();
		pressedRaw.put(id, c);
		return id;
	}
	
	/**
	 * Adds a new listener and returns its unique ID.
	 */
	public static int addKeyReleasedListener(Consumer<KeyboardData> c) {
		int id = generateUniqueId();
		releasedRaw.put(id, c);
		return id;
	}
	
	public static void removeKeyPressedListener(int id) {
		if (pressedRaw.containsKey(id)) {
			pressedRaw.remove(id);
		}
	}
	
	public static void removeKeyReleasedListener(int id) {
		if (releasedRaw.containsKey(id)) {
			releasedRaw.remove(id);
		}
	}

	public static boolean isCtrlPressed() {
		return ctrlPressed;
	}

	public static boolean isAltPressed() {
		return altPressed;
	}
	
	private static int generateUniqueId() {
		int i = MathUtils.getRandomNumberInRange(100000000, 999999999);
		while(ids.contains(i)) {
			i = MathUtils.getRandomNumberInRange(100000000, 999999999);
		}
		ids.add(i);
		return i;
	}

}
