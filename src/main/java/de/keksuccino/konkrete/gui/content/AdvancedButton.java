package de.keksuccino.konkrete.gui.content;

import java.awt.Color;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AdvancedButton extends Button {

	protected boolean handleClick = false;
	protected static boolean leftDown = false;
	protected boolean leftDownThis = false;
	protected boolean leftDownNotHovered = false;
	public boolean ignoreBlockedInput = false;
	public boolean ignoreLeftMouseDownClickBlock = false;
	public boolean enableRightclick = false;
	public float labelScale = 1.0F;
	protected boolean useable = true;
	protected boolean labelShadow = true;
	public boolean renderLabel = true;
	protected Color idleColor;
	protected Color hoveredColor;
	protected Color idleBorderColor;
	protected Color hoveredBorderColor;
	protected float borderWidth = 2.0F;
	protected ResourceLocation backgroundHover;
	protected ResourceLocation backgroundNormal;
	protected IAnimationRenderer backgroundAnimationNormal;
	protected IAnimationRenderer backgroundAnimationHover;
	public boolean loopBackgroundAnimations = true;
	public boolean restartBackgroundAnimationsOnHover = true;
	protected boolean lastHoverState = false;
	protected String clicksound = null;
	protected String[] description = null;

	protected OnPress press;
	
	public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, OnPress onPress) {
		super(x, y, widthIn, heightIn, Component.literal(buttonText), onPress, (narration) -> {
			return Component.literal(buttonText);
		});
		this.press = onPress;
	}
	
	public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, boolean handleClick, OnPress onPress) {
		super(x, y, widthIn, heightIn, Component.literal(buttonText), onPress, (narration) -> {
			return Component.literal(buttonText);
		});
		this.handleClick = handleClick;
		this.press = onPress;
	}

	@Override
	public void onPress() {
		this.press.onPress(this);
	}
	
	//renderButton
	@Override
	public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
		if (this.visible) {
			Minecraft mc = Minecraft.getInstance();

			this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

			if (this.lastHoverState != this.isHoveredOrFocused()) {
				if (this.isHoveredOrFocused()) {
					if (this.restartBackgroundAnimationsOnHover) {
						if (this.backgroundAnimationNormal != null) {
							this.backgroundAnimationNormal.resetAnimation();
						}
						if (this.backgroundAnimationHover != null) {
							this.backgroundAnimationHover.resetAnimation();
						}
					}
				}
			}
			this.lastHoverState = this.isHoveredOrFocused();
			
			RenderSystem.enableBlend();
			if (this.hasColorBackground()) {
				Color border;
				if (!isHoveredOrFocused()) {
					fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, this.idleColor.getRGB() | Mth.ceil(this.alpha * 255.0F) << 24);
					border = this.idleBorderColor;
				} else {
					if (this.active) {
						fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, this.hoveredColor.getRGB() | Mth.ceil(this.alpha * 255.0F) << 24);
						border = this.hoveredBorderColor;
					} else {
						fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, this.idleColor.getRGB() | Mth.ceil(this.alpha * 255.0F) << 24);
						border = this.idleBorderColor;
					}
				}
				if (this.hasBorder()) {
					//top
					RenderUtils.fill(matrix, this.x, this.y, this.x + this.width, this.y + this.borderWidth, border.getRGB(), this.alpha);
					//bottom
					RenderUtils.fill(matrix, this.x, this.y + this.height - this.borderWidth, this.x + this.width, this.y + this.height, border.getRGB(), this.alpha);
					//left
					RenderUtils.fill(matrix, this.x, this.y + this.borderWidth, this.x + this.borderWidth, this.y + this.height - this.borderWidth, border.getRGB(), this.alpha);
					//right
					RenderUtils.fill(matrix, this.x + this.width - this.borderWidth, this.y + this.borderWidth, this.x + this.width, this.y + this.height - this.borderWidth, border.getRGB(), this.alpha);
				}
			} else {
				this.renderBackgroundNormal(matrix);
				this.renderBackgroundHover(matrix);
			}

			this.renderBg(matrix, mc, mouseX, mouseY);

			this.renderLabel(matrix);

			if (this.isHoveredOrFocused()) {
				AdvancedButtonHandler.setActiveDescriptionButton(this);
			}
		}

		if (!this.isHoveredOrFocused() && MouseInput.isLeftMouseDown()) {
			this.leftDownNotHovered = true;
		}
		if (!MouseInput.isLeftMouseDown()) {
			this.leftDownNotHovered = false;
		}
		
		if (this.handleClick && this.useable) {
			if (this.isHoveredOrFocused() && (MouseInput.isLeftMouseDown() || (this.enableRightclick && MouseInput.isRightMouseDown())) && (!leftDown || this.ignoreLeftMouseDownClickBlock) && !leftDownNotHovered && !this.isInputBlocked() && this.active && this.visible) {
				if (!this.leftDownThis) {
					this.onClick(mouseX, mouseY);
					if (this.clicksound == null) {
						this.playDownSound(Minecraft.getInstance().getSoundManager());
					} else {
						SoundHandler.resetSound(this.clicksound);
						SoundHandler.playSound(this.clicksound);
					}
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

	protected void renderBackgroundHover(PoseStack matrix) {
		try {
			if (this.isHoveredOrFocused()) {
				if (this.active) {
					if (this.hasCustomBackgroundHover()) {
						if (this.backgroundHover != null) {
							RenderUtils.bindTexture(this.backgroundHover);
							RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
							blit(matrix, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
						} else {
							int aniX = this.backgroundAnimationHover.getPosX();
							int aniY = this.backgroundAnimationHover.getPosY();
							int aniWidth = this.backgroundAnimationHover.getWidth();
							int aniHeight = this.backgroundAnimationHover.getHeight();
							boolean aniLoop = this.backgroundAnimationHover.isGettingLooped();

							this.backgroundAnimationHover.setPosX(this.x);
							this.backgroundAnimationHover.setPosY(this.y);
							this.backgroundAnimationHover.setWidth(this.width);
							this.backgroundAnimationHover.setHeight(this.height);
							this.backgroundAnimationHover.setLooped(this.loopBackgroundAnimations);
							this.backgroundAnimationHover.setOpacity(this.alpha);

							this.backgroundAnimationHover.render(matrix);

							this.backgroundAnimationHover.setPosX(aniX);
							this.backgroundAnimationHover.setPosY(aniY);
							this.backgroundAnimationHover.setWidth(aniWidth);
							this.backgroundAnimationHover.setHeight(aniHeight);
							this.backgroundAnimationHover.setLooped(aniLoop);
							this.backgroundAnimationHover.setOpacity(1.0F);
						}
					} else {
						this.renderDefaultBackground(matrix);
					}
				} else {
					this.renderBackgroundNormal(matrix);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void renderBackgroundNormal(PoseStack matrix) {
		try {
			if (!this.isHoveredOrFocused()) {
				if (this.hasCustomBackgroundNormal()) {
					if (this.backgroundNormal != null) {
						RenderUtils.bindTexture(this.backgroundNormal);
						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
						blit(matrix, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
					} else {
						int aniX = this.backgroundAnimationNormal.getPosX();
						int aniY = this.backgroundAnimationNormal.getPosY();
						int aniWidth = this.backgroundAnimationNormal.getWidth();
						int aniHeight = this.backgroundAnimationNormal.getHeight();
						boolean aniLoop = this.backgroundAnimationNormal.isGettingLooped();

						this.backgroundAnimationNormal.setPosX(this.x);
						this.backgroundAnimationNormal.setPosY(this.y);
						this.backgroundAnimationNormal.setWidth(this.width);
						this.backgroundAnimationNormal.setHeight(this.height);
						this.backgroundAnimationNormal.setLooped(this.loopBackgroundAnimations);
						this.backgroundAnimationNormal.setOpacity(this.alpha);

						this.backgroundAnimationNormal.render(matrix);

						this.backgroundAnimationNormal.setPosX(aniX);
						this.backgroundAnimationNormal.setPosY(aniY);
						this.backgroundAnimationNormal.setWidth(aniWidth);
						this.backgroundAnimationNormal.setHeight(aniHeight);
						this.backgroundAnimationNormal.setLooped(aniLoop);
						this.backgroundAnimationNormal.setOpacity(1.0F);
					}
				} else {
					this.renderDefaultBackground(matrix);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void renderDefaultBackground(PoseStack matrix) {
		RenderUtils.bindTexture(WIDGETS_LOCATION);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		int i = this.getYImage(this.isHoveredOrFocused());
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		this.blit(matrix, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
		this.blit(matrix, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
		RenderSystem.disableDepthTest();
	}
	
	protected void renderLabel(PoseStack matrix) {
		if (!renderLabel) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		int stringWidth = font.width(getMessageString());
		int stringHeight = 8;
		int pX = (int) (((this.x + (this.width / 2)) - ((stringWidth * this.labelScale) / 2)) / this.labelScale);
		int pY = (int) (((this.y + (this.height / 2)) - ((stringHeight * this.labelScale) / 2)) / this.labelScale);
		
		matrix.pushPose();
		matrix.scale(this.labelScale, this.labelScale, this.labelScale);

		if (this.labelShadow) {
			font.drawShadow(matrix, getMessageString(), pX, pY, getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
		} else {
			font.draw(matrix, getMessageString(), pX, pY, getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
		}
		
		matrix.popPose();
	}
	
	protected boolean isInputBlocked() {
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

	@Deprecated
	public void setBackgroundTexture(ResourceLocation normal, ResourceLocation hovered) {
		this.backgroundNormal = normal;
		this.backgroundHover = hovered;
	}

	@Deprecated
	public void setBackgroundTexture(ExternalTextureResourceLocation normal, ExternalTextureResourceLocation hovered) {
		if (normal != null) {
			if (!normal.isReady()) {
				normal.loadTexture();
			}
			this.backgroundNormal = normal.getResourceLocation();
		} else {
			this.backgroundNormal = null;
		}
		if (hovered != null) {
			if (!hovered.isReady()) {
				hovered.loadTexture();
			}
			this.backgroundHover = hovered.getResourceLocation();
		} else {
			this.backgroundHover = null;
		}
	}

	public void setBackgroundNormal(ResourceLocation texture) {
		this.backgroundNormal = texture;
	}

	public void setBackgroundNormal(IAnimationRenderer animation) {
		if (animation != null) {
			if (!animation.isReady()) {
				animation.prepareAnimation();
			}
		}
		this.backgroundAnimationNormal = animation;
	}

	public void setBackgroundHover(ResourceLocation texture) {
		this.backgroundHover = texture;
	}

	public void setBackgroundHover(IAnimationRenderer animation) {
		if (animation != null) {
			if (!animation.isReady()) {
				animation.prepareAnimation();
			}
		}
		this.backgroundAnimationHover = animation;
	}
	
	public boolean hasBorder() {
		return (this.hasColorBackground() && (this.idleBorderColor != null) && (this.hoveredBorderColor != null));
	}
	
	public boolean hasColorBackground() {
		return ((this.idleColor != null) && (this.hoveredColor != null));
	}

	@Deprecated
	public boolean hasCustomTextureBackground() {
		return this.hasCustomBackground();
	}

	public boolean hasCustomBackground() {
		return (((this.backgroundHover != null) || (this.backgroundAnimationHover != null)) && ((this.backgroundNormal != null) || (this.backgroundAnimationNormal != null)));
	}

	public boolean hasCustomBackgroundNormal() {
		return ((this.backgroundNormal != null) || (this.backgroundAnimationNormal != null));
	}

	public boolean hasCustomBackgroundHover() {
		return ((this.backgroundHover != null) || (this.backgroundAnimationHover != null));
	}

	@Override
	public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
		if (!this.handleClick) {
			if (this.useable) {
				if (this.active && this.visible) {
			         if (this.isValidClickButton(p_mouseClicked_5_)) {
			            boolean flag = this.clicked(p_mouseClicked_1_, p_mouseClicked_3_);
			            if (flag) {
			               if (this.clicksound == null) {
			            	   this.playDownSound(Minecraft.getInstance().getSoundManager());
			               } else {
			            	   SoundHandler.resetSound(this.clicksound);
			            	   SoundHandler.playSound(this.clicksound);
			               }
			               this.onClick(p_mouseClicked_1_, p_mouseClicked_3_);
			               return true;
			            }
			         }

			         return false;
			      } else {
			         return false;
			      }
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (this.handleClick) {
			return false;
		}
		return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
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
	
	public String getMessageString() {
		return this.getMessage().getString();
	}
	
	public void setMessage(String msg) {
		this.setMessage(Component.literal(msg));
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getX() {
		return this.x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return this.y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setHovered(boolean b) {
		this.isHovered = b;
	}

	public void setPressAction(OnPress press) {
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

}