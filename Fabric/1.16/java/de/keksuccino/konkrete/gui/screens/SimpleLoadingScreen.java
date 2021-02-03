package de.keksuccino.konkrete.gui.screens;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.AnimationRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
public class SimpleLoadingScreen extends Screen {
	private static Identifier MOJANG_LOGO_TEXTURE = new Identifier("textures/gui/title/mojangstudios.png");
	private final MinecraftClient mc;
	private LoadingAnimationRenderer loading = new LoadingAnimationRenderer("keksuccino/animations/loading", 15, true, 0, 0, 16, 16, "konkrete", null);
	private String status = "";
	private boolean darkmode = false;
	
	public SimpleLoadingScreen(MinecraftClient mc) {
		super(new LiteralText(""));
		this.mc = mc;
	}
	
	//TODO update to new rendering logic implemented in forge-konkrete 1.1.1
	@Override
	public void render(MatrixStack matrix, int p_render_1_, int p_render_2_, float p_render_3_) {
		int color = new Color(239, 50, 61).getRGB();
		if (darkmode) {
			color = new Color(26, 26, 26).getRGB();
		}
		fill(matrix, 0, 0, width, height, color);
		
		int j2 = (int)((double)mc.getWindow().getScaledWidth() * 0.5D);
		int i1 = (int)((double)mc.getWindow().getScaledHeight() * 0.5D);
		double d0 = Math.min((double)mc.getWindow().getScaledWidth() * 0.75D, (double)mc.getWindow().getScaledHeight()) * 0.25D;
		int j1 = (int)(d0 * 0.5D);
		double d1 = d0 * 4.0D;
		int k1 = (int)(d1 * 0.5D);
		mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		RenderSystem.enableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		drawTexture(matrix, j2 - k1, i1 - j1, k1, (int)d0, -0.0625F, 0.0F, 120, 60, 120, 120);
		drawTexture(matrix, j2, i1 - j1, k1, (int)d0, 0.0625F, 60.0F, 120, 60, 120, 120);

		this.loading.setPosX((width /2) - (this.loading.getWidth() / 2));
		this.loading.setPosY(height - 80);

		RenderSystem.color4f(0.0F, 0.733F, 1.0F, 1.0F);
		this.loading.render(matrix);

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.drawStatus(this.status, matrix, width / 2, height - 30);
		
		super.render(matrix, p_render_1_, p_render_2_, p_render_3_);
	}
	
	public void setStatusText(String status) {
		this.status = status;
	}

	public void drawStatus(String text, MatrixStack matrix, int width, int height) {
		//drawString
		mc.textRenderer.draw(matrix, text, (float) (width - MinecraftClient.getInstance().textRenderer.getWidth(text) / 2), (float) height, Color.WHITE.getRGB());
	}
	
	public void setDarkmode(boolean b) {
		this.darkmode = b;
	}
	
	public void setLoadingAnimationColor(String hex) {
		this.loading.setHexColor(hex);
	}

	private static class LoadingAnimationRenderer extends AnimationRenderer {

		private String hex = "#ffffffff";
		
		public LoadingAnimationRenderer(String resourceDir, int fps, boolean loop, int posX, int posY, int width, int height, String modid, String hex) {
			super(resourceDir, fps, loop, posX, posY, width, height, modid);
			if (hex != null) {
				this.hex = hex;
			}
		}
		
		@Override
		protected void renderFrame(MatrixStack matrix) {
			int h = this.getHeight();
			int w = this.getWidth();
			int x2 = this.getPosX();
			int y2 = this.getPosY();
			
			if (this.isStretchedToStreensize()) {
				h = MinecraftClient.getInstance().currentScreen.height;
				w = MinecraftClient.getInstance().currentScreen.width;
				x2 = 0;
				y2 = 0;
			}
			
			MinecraftClient.getInstance().getTextureManager().bindTexture(this.resources.get(this.currentFrame()));
			
			RenderSystem.enableBlend();
			
			float[] colorf = RenderUtils.getColorFromHexString(this.hex).getComponents(new float[4]);
			if (colorf != null) {
				RenderSystem.color4f(colorf[0], colorf[1], colorf[2], colorf[3]);
			} else {
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.0F);
			}

			DrawableHelper.drawTexture(matrix, x2, y2, 0.0F, 0.0F, w, h, w, h);
			
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			
			RenderSystem.disableBlend();
		}
		
		public void setHexColor(String hex) {
			if (hex == null) {
				this.hex = "#ffffffff";
			} else {
				this.hex = hex;
			}
		}
		
	}
}