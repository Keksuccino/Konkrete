package de.keksuccino.konkrete.gui.content;

import java.awt.Color;

import javax.annotation.Nullable;

import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

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
	private boolean labelShadow = true;
	public float alpha = 1.0F;
	
	private Color idleColor;
	private Color hoveredColor;
	private Color idleBorderColor;
	private Color hoveredBorderColor;
	private float borderWidth = 2.0F;
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
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (this.visible) {
			
			this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			
			GlStateManager.enableBlend();
			if (this.hasColorBackground()) {
				Color border;
				if (!hovered) {
					drawRect(this.x, this.y, this.x + this.width, this.y + this.height, this.idleColor.getRGB() | MathHelper.ceil(this.alpha * 255.0F) << 24);
					border = this.idleBorderColor;
				} else {
					if (this.enabled) {
						drawRect(this.x, this.y, this.x + this.width, this.y + this.height, this.hoveredColor.getRGB() | MathHelper.ceil(this.alpha * 255.0F) << 24);
						border = this.hoveredBorderColor;
					} else {
						drawRect(this.x, this.y, this.x + this.width, this.y + this.height, this.idleColor.getRGB() | MathHelper.ceil(this.alpha * 255.0F) << 24);
						border = this.idleBorderColor;
					}
				}
				if (this.hasBorder()) {
					//top
					RenderUtils.fill(this.x, this.y, this.x + this.width, this.y + this.borderWidth, border.getRGB(), this.alpha);
					//bottom
					RenderUtils.fill(this.x, this.y + this.height - this.borderWidth, this.x + this.width, this.y + this.height, border.getRGB(), this.alpha);
					//left
					RenderUtils.fill(this.x, this.y + this.borderWidth, this.x + this.borderWidth, this.y + this.height - this.borderWidth, border.getRGB(), this.alpha);
					//right
					RenderUtils.fill(this.x + this.width - this.borderWidth, this.y + this.borderWidth, this.x + this.width, this.y + this.height - this.borderWidth, border.getRGB(), this.alpha);
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
				GlStateManager.color(1.0F, 1.0F, 1.0F, this.alpha);
				drawModalRectWithCustomSizedTexture(this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			} else {
				mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
	            GlStateManager.color(1.0F, 1.0F, 1.0F, this.alpha);
	            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
	            int i = this.getHoverState(this.hovered);
	            GlStateManager.enableBlend();
	            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
	            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	            this.drawTexturedModalRect(this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
	            this.drawTexturedModalRect(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
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
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		int stringWidth = font.getStringWidth(this.displayString);
		int stringHeight = 8;
		int pX = (int) (((this.x + (this.width / 2)) - ((stringWidth * this.labelScale) / 2)) / this.labelScale);
		int pY = (int) (((this.y + (this.height / 2)) - ((stringHeight * this.labelScale) / 2)) / this.labelScale);
		
		GlStateManager.pushMatrix();
		GlStateManager.scale(this.labelScale, this.labelScale, this.labelScale);
		
		if (this.labelShadow) {
			font.drawStringWithShadow(displayString, pX, pY, getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
		} else {
			font.drawString(displayString, pX, pY, getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
		}
		
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
	
	public void setBackgroundColor(@Nullable Color idle, @Nullable Color hovered, @Nullable Color idleBorder, @Nullable Color hoveredBorder, float borderWidth) {
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
	
	public void setBackgroundColor(@Nullable Color idle, @Nullable Color hovered, @Nullable Color idleBorder, @Nullable Color hoveredBorder, int borderWidth) {
		this.setBackgroundColor(idle, hovered, idleBorder, hoveredBorder, (float) borderWidth);
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
	
	public void setLabelShadow(boolean shadow) {
		this.labelShadow = shadow;
	}
	
	public static boolean isAnyButtonLeftClicked() {
		return leftDown;
	}
	
	public interface IPressable {
		void onPress(GuiButton button);
	}

}
