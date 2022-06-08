package de.keksuccino.konkrete.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class CurrentScreenHandler {

	private static Screen lastScreen;
	private static PoseStack currentStack;

	public static void init() {
		Konkrete.getEventHandler().registerEventsFrom(new CurrentScreenHandler());
	}
	
	public static Screen getScreen() {
		return Minecraft.getInstance().screen;
	}
	
	/**
	 * Returns the {@link PoseStack} for the current game tick or a BLANK ONE if no stack was cached.<br><br>
	 * 
	 * <b>IF NO SCREEN IS BEING RENDERED ATM, THIS WILL RETURN THE LAST STACK USED TO RENDER A SCREEN!</b>
	 */
	public static PoseStack getMatrixStack() {
		if (currentStack == null) {
			currentStack = new PoseStack();
		}
		return currentStack;
	}
	
	public static int getWidth() {
		if (getScreen() != null) {
			return getScreen().width;
		}
		return 0;
	}
	
	public static void setWidth(int width) {
		if (getScreen() != null) {
			getScreen().width = width;
		}
	}
	
	public static int getHeight() {
		if (getScreen() != null) {
			return getScreen().height;
		}
		return 0;
	}
	
	public static void setHeight(int height) {
		if (getScreen() != null) {
			getScreen().height = height;
		}
	}

	public static Screen getLastScreen() {
		return lastScreen;
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Pre e) {
		currentStack = e.getMatrixStack();
	}

	@SubscribeEvent
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		lastScreen = e.getGui();
	}

}
