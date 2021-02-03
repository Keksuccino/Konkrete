package de.keksuccino.konkrete.gui.content;

import java.awt.Color;

import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AdvancedButtonHandler {

	private static AdvancedButton activeDescBtn;
	private static int garbageCheck = 0;
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new AdvancedButtonHandler());
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawScreen(DrawScreenEvent.Post e) {
		if (activeDescBtn != null) {
			if (activeDescBtn.isMouseOver()) {
				if ((Minecraft.getMinecraft() != null) && (Minecraft.getMinecraft().currentScreen != null)) {
					renderDescription(e.mouseX, e.mouseY);
				}
			}
			if (garbageCheck == 0) {
				activeDescBtn = null;
			}
			garbageCheck = 0;
		}
	}
	
	public static void setActiveDescriptionButton(AdvancedButton btn) {
		activeDescBtn = btn;
		garbageCheck = 1;
	}
	
	private static void renderDescriptionBackground(int x, int y, int width, int height) {
		Gui.drawRect(x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
	}
	
	private static void renderDescription(int mouseX, int mouseY) {
		if (activeDescBtn != null) {
			if (activeDescBtn.getDescription() != null) {
				int width = 10;
				int height = 10;
				
				//Getting the longest string from the list to render the background with the correct width
				for (String s : activeDescBtn.getDescription()) {
					int i = Minecraft.getMinecraft().fontRendererObj.getStringWidth(s) + 10;
					if (i > width) {
						width = i;
					}
					height += 10;
				}

				mouseX += 5;
				mouseY += 5;
				
				if (Minecraft.getMinecraft().currentScreen.width < mouseX + width) {
					mouseX -= width + 10;
				}
				
				if (Minecraft.getMinecraft().currentScreen.height < mouseY + height) {
					mouseY -= height + 10;
				}

				RenderUtils.setZLevelPre(600);
				
				renderDescriptionBackground(mouseX, mouseY, width, height);

				GlStateManager.enableBlend();

				int i2 = 5;
				for (String s : activeDescBtn.getDescription()) {
					Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
					i2 += 10;
				}
				
				RenderUtils.setZLevelPost();
				
				GlStateManager.disableBlend();
			}
		}
	}
	
}
