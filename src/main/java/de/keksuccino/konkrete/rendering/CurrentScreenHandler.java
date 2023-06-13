package de.keksuccino.konkrete.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiOpenedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class CurrentScreenHandler {

	private static Screen lastScreen;
	private static PoseStack currentStack;
	private static GuiGraphics currentGraphics;

	public static void init() {
		Konkrete.getEventHandler().registerEventsFrom(new CurrentScreenHandler());
	}
	
	public static Screen getScreen() {
		return Minecraft.getInstance().screen;
	}

	/**
	 * Returns the {@link GuiGraphics} for the current game tick or a BLANK ONE if no stack was cached.<br><br>
	 *
	 * <b>IF NO SCREEN IS BEING RENDERED ATM, THIS WILL RETURN THE LAST STACK USED TO RENDER A SCREEN!</b>
	 */
	public static PoseStack getPoseStack() {
		if (currentStack == null) {
			currentStack = new PoseStack();
		}
		return currentStack;
	}

	/**
	 * Returns the {@link GuiGraphics} for the current game tick or a BLANK ONE if no stack was cached.<br><br>
	 * 
	 * <b>IF NO SCREEN IS BEING RENDERED ATM, THIS WILL RETURN THE LAST STACK USED TO RENDER A SCREEN!</b>
	 */
	@Deprecated
	public static PoseStack getMatrixStack() {
		if (currentStack == null) {
			currentStack = new PoseStack();
		}
		return currentStack;
	}

	public static GuiGraphics getGuiGraphics() {
		if (currentGraphics == null) currentGraphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
		return currentGraphics;
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
		currentGraphics = e.getGuiGraphics();
	}

	@SubscribeEvent
	public void onInitPost(GuiOpenedEvent e) {
		lastScreen = e.getGui();
	}

}
