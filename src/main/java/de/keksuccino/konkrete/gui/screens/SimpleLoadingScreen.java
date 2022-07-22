package de.keksuccino.konkrete.gui.screens;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
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

@SuppressWarnings("deprecation")
@OnlyIn(Dist.CLIENT)
public class SimpleLoadingScreen extends Screen {
	private static ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojangstudios.png");
	private final Minecraft mc;
	private LoadingAnimationRenderer loading = new LoadingAnimationRenderer("keksuccino/animations/loading", 15, true, 0, 0, 16, 16, "konkrete", null);
	private String status = "";
	private boolean darkmode = false;
	
	public SimpleLoadingScreen(Minecraft mc) {
		super(new StringTextComponent(""));
		this.mc = mc;
	}

	@Override
	public void render(MatrixStack matrix, int p_render_1_, int p_render_2_, float p_render_3_) {
		int color = new Color(239, 50, 61).getRGB();
		if (darkmode) {
			color = new Color(26, 26, 26).getRGB();
		}
		fill(matrix, 0, 0, width, height, color);
		
		int j2 = (int)((double)mc.getWindow().getGuiScaledWidth() * 0.5D);
		int i1 = (int)((double)mc.getWindow().getGuiScaledHeight() * 0.5D);
		double d0 = Math.min((double)mc.getWindow().getGuiScaledWidth() * 0.75D, (double)mc.getWindow().getGuiScaledHeight()) * 0.25D;
		int j1 = (int)(d0 * 0.5D);
		double d1 = d0 * 4.0D;
		int k1 = (int)(d1 * 0.5D);
		mc.getTextureManager().bind(MOJANG_LOGO_TEXTURE);
		RenderSystem.enableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		blit(matrix, j2 - k1, i1 - j1, k1, (int)d0, -0.0625F, 0.0F, 120, 60, 120, 120);
		blit(matrix, j2, i1 - j1, k1, (int)d0, 0.0625F, 60.0F, 120, 60, 120, 120);

		this.loading.setPosX((width /2) - (this.loading.getWidth() / 2));
		this.loading.setPosY(height - 80);

		RenderSystem.color4f(0.0F, 0.733F, 1.0F, 1.0F);
		this.loading.render(matrix);

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.drawStatus(this.status, matrix, width / 2, this.loading.getPosY() + this.loading.getHeight() + 20);
		
		super.render(matrix, p_render_1_, p_render_2_, p_render_3_);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
	
	public void setStatusText(String status) {
		this.status = status;
	}

	public void drawStatus(String text, MatrixStack matrix, int width, int height) {
		mc.font.draw(matrix, text, (float) (width - Minecraft.getInstance().font.width(text) / 2), (float) height, Color.WHITE.getRGB());
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
				h = Minecraft.getInstance().screen.height;
				w = Minecraft.getInstance().screen.width;
				x2 = 0;
				y2 = 0;
			}
			
			Minecraft.getInstance().getTextureManager().bind(this.resources.get(this.currentFrame()));
			
			RenderSystem.enableBlend();
			
			float[] colorf = RenderUtils.getColorFromHexString(this.hex).getComponents(new float[4]);
			if (colorf != null) {
				RenderSystem.color4f(colorf[0], colorf[1], colorf[2], colorf[3]);
			} else {
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.0F);
			}
			
			IngameGui.blit(matrix, x2, y2, 0.0F, 0.0F, w, h, w, h);
			
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