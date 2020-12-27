package de.keksuccino.konkrete.gui.content;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class AdvancedImageButton extends AdvancedButton {

	private Identifier image;
	
	public AdvancedImageButton(int x, int y, int widthIn, int heightIn, Identifier image, boolean handleClick, PressAction onPress) {
		super(x, y, widthIn, heightIn, "", handleClick, onPress);
		this.image = image;
	}
	
	public AdvancedImageButton(int x, int y, int widthIn, int heightIn, Identifier image, PressAction onPress) {
		super(x, y, widthIn, heightIn, "", onPress);
		this.image = image;
	}
	
	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		super.render(matrix, mouseX, mouseY, partialTicks);
		
		MinecraftClient.getInstance().getTextureManager().bindTexture(this.image);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		drawTexture(matrix, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
	}

}
