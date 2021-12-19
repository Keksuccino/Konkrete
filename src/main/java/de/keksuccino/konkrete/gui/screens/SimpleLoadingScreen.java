package de.keksuccino.konkrete.gui.screens;

import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.AnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class SimpleLoadingScreen extends GuiScreen {
	private static ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojang.png");
	private final Minecraft mc;
	private LoadingAnimationRenderer loading = new LoadingAnimationRenderer("keksuccino/animations/loading", 15, true, 0, 0, 16, 16, "konkrete", null);
	private String status = "";
	
	public SimpleLoadingScreen(Minecraft mc) {
		super();
		this.mc = mc;
	}
	
	@Override
	public void drawScreen(int p_render_1_, int p_render_2_, float p_render_3_) {

		this.mc.getTextureManager().bindTexture(RenderUtils.getWhiteImageResource());
		this.drawTexturedModalRect(0, 0, width, height, width, height);
		
		int k1 = (width - 256) / 2;
		int i1 = (height - 256) / 2;
		this.mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		this.drawTexturedModalRect(k1, i1, 0, 0, 256, 256);

		this.loading.setPosX((width /2) - (this.loading.getWidth() / 2));
		this.loading.setPosY(height - 80);

		GlStateManager.color(0.0F, 0.733F, 1.0F, 1.0F);
		this.loading.render();

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.drawStatus(this.status, width / 2, this.loading.getPosY() + this.loading.getHeight() + 20);
		
		super.drawScreen(p_render_1_, p_render_2_, p_render_3_);
	}
	
	public void setStatusText(String status) {
		this.status = status;
	}

	public void drawStatus(String text, int width, int height) {
		mc.fontRenderer.drawString(text, (float) (width - Minecraft.getMinecraft().fontRenderer.getStringWidth(text) / 2), (float) height, 14821431, false);
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
				h = Minecraft.getMinecraft().currentScreen.height;
				w = Minecraft.getMinecraft().currentScreen.width;
				x2 = 0;
				y2 = 0;
			}
			
			Minecraft.getMinecraft().getTextureManager().bindTexture(this.resources.get(this.currentFrame()));
			
			GlStateManager.enableBlend();
			
			float[] colorf = RenderUtils.getColorFromHexString(this.hex).getComponents(new float[4]);
			if (colorf != null) {
				GlStateManager.color(colorf[0], colorf[1], colorf[2], colorf[3]);
			} else {
				GlStateManager.color(1.0F, 1.0F, 1.0F, 0.0F);
			}

			Gui.drawModalRectWithCustomSizedTexture(x2, y2, 0.0F, 0.0F, w, h, w, h);
			
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			
			GlStateManager.disableBlend();
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