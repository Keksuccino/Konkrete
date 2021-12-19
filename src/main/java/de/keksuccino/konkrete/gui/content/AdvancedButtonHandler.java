package de.keksuccino.konkrete.gui.content;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AdvancedButtonHandler {

	private static AdvancedButton activeDescBtn;
	private static int garbageCheck = 0;
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new AdvancedButtonHandler());
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawScreen(DrawScreenEvent.Post e) {
		if (activeDescBtn != null) {
			if (activeDescBtn.isHovered()) {
				if ((Minecraft.getInstance() != null) && (Minecraft.getInstance().currentScreen != null)) {
					renderDescription(e.getMatrixStack(), e.getMouseX(), e.getMouseY());
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
	
	private static void renderDescriptionBackground(MatrixStack matrix, int x, int y, int width, int height) {
		IngameGui.fill(matrix, x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
	}
	
	private static void renderDescription(MatrixStack matrix, int mouseX, int mouseY) {
		if (activeDescBtn != null) {
			if (activeDescBtn.getDescription() != null) {
				int width = 10;
				int height = 10;
				
				//Getting the longest string from the list to render the background with the correct width
				for (String s : activeDescBtn.getDescription()) {
					int i = Minecraft.getInstance().fontRenderer.getStringWidth(s) + 10;
					if (i > width) {
						width = i;
					}
					height += 10;
				}

				mouseX += 5;
				mouseY += 5;
				
				if (Minecraft.getInstance().currentScreen.width < mouseX + width) {
					mouseX -= width + 10;
				}
				
				if (Minecraft.getInstance().currentScreen.height < mouseY + height) {
					mouseY -= height + 10;
				}

				RenderUtils.setZLevelPre(matrix, 600);
				
				renderDescriptionBackground(matrix, mouseX, mouseY, width, height);

				RenderSystem.enableBlend();

				int i2 = 5;
				for (String s : activeDescBtn.getDescription()) {
					AbstractGui.drawString(matrix, Minecraft.getInstance().fontRenderer, s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
					i2 += 10;
				}

				RenderUtils.setZLevelPost(matrix);
				
				RenderSystem.disableBlend();
			}
		}
	}
	
}
