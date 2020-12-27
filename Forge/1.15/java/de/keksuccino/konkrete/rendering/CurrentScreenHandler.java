package de.keksuccino.konkrete.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CurrentScreenHandler {

	private static Screen lastScreen;

	public static void init() {
		MinecraftForge.EVENT_BUS.register(new CurrentScreenHandler());
	}
	
	public static Screen getScreen() {
		return Minecraft.getInstance().currentScreen;
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

	@SubscribeEvent
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		lastScreen = e.getGui();
	}

}
