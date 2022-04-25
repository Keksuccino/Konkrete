package de.keksuccino.konkrete.gui.content;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.resources.ResourceLocation;

public class AdvancedImageButton extends AdvancedButton {

	private ResourceLocation image;
	
	public AdvancedImageButton(int x, int y, int widthIn, int heightIn, ResourceLocation image, boolean handleClick, OnPress onPress) {
		super(x, y, widthIn, heightIn, "", handleClick, onPress);
		this.image = image;
	}
	
	public AdvancedImageButton(int x, int y, int widthIn, int heightIn, ResourceLocation image, OnPress onPress) {
		super(x, y, widthIn, heightIn, "", onPress);
		this.image = image;
	}
	
	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
		super.render(matrix, mouseX, mouseY, partialTicks);

		RenderUtils.bindTexture(this.image);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		blit(matrix, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
	}
	
	public void setImage(ResourceLocation image) {
		this.image = image;
	}

}
