package de.keksuccino.konkrete.gui.screens;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.AnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleLoadingScreen extends Screen {
	private static ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojang.png");
	private static ResourceLocation MOJANG_LOGO_TEXTURE_DARK = new ResourceLocation("keksuccino", "mojang_dark.png");
	private final Minecraft mc;
	private LoadingAnimationRenderer loading = new LoadingAnimationRenderer("keksuccino/animations/loading", 15, true, 0, 0, 16, 16, "konkrete", null);
	private String status = "";
	private boolean darkmode = false;
	
	public SimpleLoadingScreen(Minecraft mc) {
		super(new StringTextComponent(""));
		this.mc = mc;
	}
	
	@Override
	public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
		int color = Color.WHITE.getRGB();
		if (darkmode) {
			color = new Color(26, 26, 26).getRGB();
		}
		fill(0, 0, width, height, color);
		
		int k1 = (width - 256) / 2;
		int i1 = (height - 256) / 2;
		if (darkmode) {
			this.mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE_DARK);
		} else {
			this.mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		}
		RenderSystem.enableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.blit(k1, i1, 0, 0, 256, 256);

		this.loading.setPosX((width /2) - (this.loading.getWidth() / 2));
		this.loading.setPosY(height - 80);

		RenderSystem.color4f(0.0F, 0.733F, 1.0F, 1.0F);
		this.loading.render();

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.drawStatus(this.status, width / 2, this.loading.getPosY() + this.loading.getHeight() + 20);
		
		super.render(p_render_1_, p_render_2_, p_render_3_);
	}
	
	public void setStatusText(String status) {
		this.status = status;
	}

	public void drawStatus(String text, int width, int height) {
		mc.fontRenderer.drawString(text, (float) (width - Minecraft.getInstance().fontRenderer.getStringWidth(text) / 2), (float) height, 14821431);
	}
	
	public void setDarkmode(boolean b) {
		this.darkmode = b;
	}
	
	public void setLoadingAnimationColor(String hex) {
		this.loading.setHexColor(hex);
	}
	
	private static class LoadingAnimationRenderer extends AnimationRenderer {

		private String hex = "#E22837";
		
		public LoadingAnimationRenderer(String resourceDir, int fps, boolean loop, int posX, int posY, int width, int height, String modid, String hex) {
			super(resourceDir, fps, loop, posX, posY, width, height, modid);
			if (hex != null) {
				this.hex = hex;
			}
		}
		
		@Override
		protected void renderFrame() {
			int h = this.getHeight();
			int w = this.getWidth();
			int x2 = this.getPosX();
			int y2 = this.getPosY();
			
			if (this.isStretchedToStreensize()) {
				h = Minecraft.getInstance().currentScreen.height;
				w = Minecraft.getInstance().currentScreen.width;
				x2 = 0;
				y2 = 0;
			}
			
			Minecraft.getInstance().getTextureManager().bindTexture(this.resources.get(this.currentFrame()));
			
			RenderSystem.enableBlend();
			
			float[] colorf = RenderUtils.getColorFromHexString(this.hex).getComponents(new float[4]);
			if (colorf != null) {
				RenderSystem.color4f(colorf[0], colorf[1], colorf[2], colorf[3]);
			} else {
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.0F);
			}
			
			IngameGui.blit(x2, y2, 0.0F, 0.0F, w, h, w, h);
			
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			
			RenderSystem.disableBlend();
		}
		
		public void setHexColor(String hex) {
			if (hex == null) {
				this.hex = "#E22837";
			} else {
				this.hex = hex;
			}
		}
		
	}
	
}