package de.keksuccino.konkrete.input;

import java.util.HashMap;
import java.util.Map;

import de.keksuccino.konkrete.mixin.client.IMixinMouseHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MouseInput {
	
	private static boolean leftClicked = false;
	private static boolean rightClicked  = false;
	private static Map<String, Boolean> vanillainput = new HashMap<String, Boolean>();
	private static boolean ignoreBlocked = false;

	private static boolean useRenderScale = false;
	private static float renderScale = 1.0F;
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new MouseInput());
	}
	
	public static int getActiveMouseButton() {
		return ((IMixinMouseHelper)Minecraft.getInstance().mouseHandler).getActiveButtonKonkrete();
	}
	
	public static boolean isLeftMouseDown() {
		return leftClicked;
	}

	public static boolean isRightMouseDown() {
		return rightClicked;
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
	 * Blocks the vanilla mouse input.<br>
	 * Blocked inputs are reset when the screen changes or no screen is active.
	 */
	public static void blockVanillaInput(String category) {
		vanillainput.put(category, true);
	}
	
	public static void unblockVanillaInput(String category) {
		vanillainput.put(category, false);
	}

	public static boolean isVanillaInputBlocked() {
		if (ignoreBlocked) {
			return false;
		}
		return vanillainput.containsValue(true);
	}

	public static void ignoreBlockedVanillaInput(boolean ignore) {
		ignoreBlocked = ignore;
	}
	
	@SubscribeEvent
	public void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre e) {
		int i = e.getButton();
		if (i == 0) {
			leftClicked = true;
		}
		if (i == 1) {
			rightClicked = true;
		}
		
		if (isVanillaInputBlocked()) {
			e.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void onMouseReleased(GuiScreenEvent.MouseReleasedEvent.Pre e) {
		int i = e.getButton();
		if (i == 0) {
			leftClicked = false;
		}
		if (i == 1) {
			rightClicked = false;
		}
	}
	
	@SubscribeEvent
	public void onScreenInit(GuiScreenEvent.InitGuiEvent.Pre e) {
		//Reset values on screen init
		leftClicked = false;
		rightClicked = false;

		vanillainput.clear();
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		if ((Minecraft.getInstance() != null) && (Minecraft.getInstance().screen == null)) {
			vanillainput.clear();
		}
	}

}
