package de.keksuccino.konkrete;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;

public class TestEvents {
	
	@SubscribeEvent
	public void onDrawPre(DrawScreenEvent.Pre e) {
		System.out.println("DRAW PRE!");
		RenderSystem.enableBlend();
		DrawableHelper.drawCenteredText(e.getMatrixStack(), MinecraftClient.getInstance().textRenderer, "Huhuuuu", 20, 20, -1);
	}
	
	@SubscribeEvent
	public void onDrawPost(DrawScreenEvent.Post e) {
		System.out.println("DRAW POST!");
		RenderSystem.enableBlend();
		DrawableHelper.drawCenteredText(e.getMatrixStack(), MinecraftClient.getInstance().textRenderer, "Huhu", 20, 50, -1);
	}
	
}
