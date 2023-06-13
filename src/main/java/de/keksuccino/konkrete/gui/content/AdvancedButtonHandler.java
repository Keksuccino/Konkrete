package de.keksuccino.konkrete.gui.content;

import java.awt.Color;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import de.keksuccino.konkrete.rendering.RenderUtils;

public class AdvancedButtonHandler {

	private static AdvancedButton activeDescBtn;
	private static int garbageCheck = 0;
	
	public static void init() {
		Konkrete.getEventHandler().registerEventsFrom(new AdvancedButtonHandler());
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawScreen(DrawScreenEvent.Post e) {
		if (activeDescBtn != null) {
			if (activeDescBtn.isHoveredOrFocused()) {
				if ((Minecraft.getInstance() != null) && (Minecraft.getInstance().screen != null)) {
					renderDescription(e.getGuiGraphics(), e.getMouseX(), e.getMouseY());
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
	
	private static void renderDescriptionBackground(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
	}
	
	private static void renderDescription(GuiGraphics graphics, int mouseX, int mouseY) {
		if (activeDescBtn != null) {
			if (activeDescBtn.getDescription() != null) {
				int width = 10;
				int height = 10;
				
				//Getting the longest string from the list to render the background with the correct width
				for (String s : activeDescBtn.getDescription()) {
					int i = Minecraft.getInstance().font.width(s) + 10;
					if (i > width) {
						width = i;
					}
					height += 10;
				}

				mouseX += 5;
				mouseY += 5;
				
				if (Minecraft.getInstance().screen.width < mouseX + width) {
					mouseX -= width + 10;
				}
				
				if (Minecraft.getInstance().screen.height < mouseY + height) {
					mouseY -= height + 10;
				}
				
				RenderUtils.setZLevelPre(graphics.pose(), 600);

				renderDescriptionBackground(graphics, mouseX, mouseY, width, height);

				RenderSystem.enableBlend();

				int i2 = 5;
				for (String s : activeDescBtn.getDescription()) {
					graphics.drawString(Minecraft.getInstance().font, s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
					i2 += 10;
				}
				
				RenderUtils.setZLevelPost(graphics.pose());
				
				RenderSystem.disableBlend();
			}
		}
	}
	
}
