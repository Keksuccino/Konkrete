package de.keksuccino.konkrete.gui.content;

import java.awt.Color;

import javax.annotation.Nullable;

import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class AdvancedButton extends GuiButton {

	private boolean handleClick = false;
	private static boolean leftDown = false;
	private boolean leftDownThis = false;
	private boolean leftDownNotHovered = false;
	public boolean ignoreBlockedInput = false;
	public boolean ignoreLeftMouseDownClickBlock = false;
	public boolean enableRightclick = false;
	public float labelScale = 1.0F;
	private boolean useable = true;
	
	private Color idleColor;
	private Color hoveredColor;
	private Color idleBorderColor;
	private Color hoveredBorderColor;
	private int borderWidth = 2;
	private ResourceLocation backgroundHover;
	private ResourceLocation backgroundNormal;
	String clicksound = null;
	String[] description = null;
	
	private IPressable press;
	
	public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, IPressable onPress) {
		super(MathUtils.getRandomNumberInRange(100, 999), x, y, widthIn, heightIn, buttonText);
		this.press = onPress;
	}
	
	public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, boolean handleClick, IPressable onPress) {
		super(MathUtils.getRandomNumberInRange(100, 999), x, y, widthIn, heightIn, buttonText);
		this.handleClick = handleClick;
		this.press = onPress;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		if (this.visible) {
			
			this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
			
			GlStateManager.enableBlend();
			if (this.hasColorBackground()) {
				Color border;
				if (!hovered) {
					GuiScreen.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, this.idleColor.getRGB());
					border = this.idleBorderColor;
				} else {
					if (this.enabled) {
						GuiScreen.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, this.hoveredColor.getRGB());
						border = this.hoveredBorderColor;
					} else {
						GuiScreen.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, this.idleColor.getRGB());
						border = this.idleBorderColor;
					}
				}
				if (this.hasBorder()) {
					//top
					GuiScreen.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.borderWidth, border.getRGB());
					//bottom
					GuiScreen.drawRect(this.xPosition, this.yPosition + this.height - this.borderWidth, this.xPosition + this.width, this.yPosition + this.height, border.getRGB());
					//left
					GuiScreen.drawRect(this.xPosition, this.yPosition + this.borderWidth, this.xPosition + this.borderWidth, this.yPosition + this.height - this.borderWidth, border.getRGB());
					//right
					GuiScreen.drawRect(this.xPosition + this.width - this.borderWidth, this.yPosition + this.borderWidth, this.xPosition + this.width, this.yPosition + this.height - this.borderWidth, border.getRGB());
				}
			} else if (this.hasCustomTextureBackground()) {
				if (this.isMouseOver()) {
					if (this.enabled) {
						mc.getTextureManager().bindTexture(backgroundHover);
					} else {
						mc.getTextureManager().bindTexture(backgroundNormal);
					}
				} else {
					Minecraft.getMinecraft().getTextureManager().bindTexture(backgroundNormal);
				}
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				drawModalRectWithCustomSizedTexture(this.xPosition, this.yPosition, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			} else {
				mc.getTextureManager().bindTexture(buttonTextures);
	            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
	            int i = this.getHoverState(this.hovered);
	            GlStateManager.enableBlend();
	            GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
	            GlStateManager.blendFunc(770, 771);
	            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + i * 20, this.width / 2, this.height);
	            this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
			}
			
			this.mouseDragged(mc, mouseX, mouseY);
			
			this.renderLabel();
			
			if (this.isMouseOver()) {
				AdvancedButtonHandler.setActiveDescriptionButton(this);
			}
			
		}

		if (!this.isMouseOver() && MouseInput.isLeftMouseDown()) {
			this.leftDownNotHovered = true;
		}
		if (!MouseInput.isLeftMouseDown()) {
			this.leftDownNotHovered = false;
		}
		
		if (this.handleClick && this.useable) {
			if (this.isMouseOver() && (MouseInput.isLeftMouseDown() || (this.enableRightclick && MouseInput.isRightMouseDown())) && (!leftDown || this.ignoreLeftMouseDownClickBlock) && !leftDownNotHovered && !this.isInputBlocked() && this.enabled && this.visible) {
				if (!this.leftDownThis) {
					this.press.onPress(this);
					this.playPressSound(Minecraft.getMinecraft().getSoundHandler());
					leftDown = true;
					this.leftDownThis = true;
				}
			}
			if (!MouseInput.isLeftMouseDown() && !(MouseInput.isRightMouseDown() && this.enableRightclick)) {
				leftDown = false;
				this.leftDownThis = false;
			}
		}
	}
	
	protected void renderLabel() {
		FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
		int stringWidth = font.getStringWidth(this.displayString);
		int stringHeight = 8;
		int pX = (int) (((this.xPosition + (this.width / 2)) - ((stringWidth * this.labelScale) / 2)) / this.labelScale);
		int pY = (int) (((this.yPosition + (this.height / 2)) - ((stringHeight * this.labelScale) / 2)) / this.labelScale);
		
		GlStateManager.pushMatrix();
		GlStateManager.scale(this.labelScale, this.labelScale, this.labelScale);
		
		font.drawStringWithShadow(this.displayString, pX, pY, getFGColor());
		
		GlStateManager.popMatrix();
	}
	
	protected int getFGColor() {
		int j = 14737632;
		if (packedFGColour != 0) {
			j = packedFGColour;
		} else if (!this.enabled) {
			j = 10526880;
		} else if (this.hovered) {
			j = 16777120;
		}
		return j;
	}
	
	private boolean isInputBlocked() {
		if (this.ignoreBlockedInput) {
			return false;
		}
		return MouseInput.isVanillaInputBlocked();
	}
	
	public void setBackgroundColor(@Nullable Color idle, @Nullable Color hovered, @Nullable Color idleBorder, @Nullable Color hoveredBorder, int borderWidth) {
		this.idleColor = idle;
		this.hoveredColor = hovered;
		this.hoveredBorderColor = hoveredBorder;
		this.idleBorderColor = idleBorder;
		
		if (borderWidth >= 0) {
			this.borderWidth = borderWidth;
		} else {
			borderWidth = 0;
		}
	}
	
	public void setBackgroundTexture(ResourceLocation normal, ResourceLocation hovered) {
		this.backgroundNormal = normal;
		this.backgroundHover = hovered;
	}
	
	public void setBackgroundTexture(ExternalTextureResourceLocation normal, ExternalTextureResourceLocation hovered) {
		if (!normal.isReady()) {
			normal.loadTexture();
		}
		if (!hovered.isReady()) {
			hovered.loadTexture();
		}
		this.backgroundHover = hovered.getResourceLocation();
		this.backgroundNormal = normal.getResourceLocation();
	}
	
	public boolean hasBorder() {
		return (this.hasColorBackground() && (this.idleBorderColor != null) && (this.hoveredBorderColor != null));
	}
	
	public boolean hasColorBackground() {
		return ((this.idleColor != null) && (this.hoveredColor != null));
	}
	
	public boolean hasCustomTextureBackground() {
		return ((this.backgroundHover != null) && (this.backgroundNormal != null));
	}
	
	@Override
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		if (!this.handleClick) {
			if (this.useable) {
				if (this.isMouseOver()) {
					this.press.onPress(this);
				}
				return super.mousePressed(mc, mouseX, mouseY);
			}
		}
		return false;
	}
	
	@Override
	public void playPressSound(net.minecraft.client.audio.SoundHandler soundHandlerIn) {
		if (this.clicksound == null) {
			super.playPressSound(soundHandlerIn);
		} else {
			SoundHandler.resetSound(this.clicksound);
			SoundHandler.playSound(this.clicksound);
		}
	}
	
	public void setUseable(boolean b) {
		this.useable = b;
	}
	
	public boolean isUseable() {
		return this.useable;
	}
	
	public void setHandleClick(boolean b) {
		this.handleClick = b;
	}
	
	public void setPressAction(IPressable press) {
		this.press = press;
	}
	
	public void setClickSound(@Nullable String key) {
		this.clicksound = key;
	}

	public void setDescription(String... desc) {
		this.description = desc;
	}

	public String[] getDescription() {
		return this.description;
	}
	
	public static boolean isAnyButtonLeftClicked() {
		return leftDown;
	}
	
	public interface IPressable {
		void onPress(GuiButton button);
	}

}
