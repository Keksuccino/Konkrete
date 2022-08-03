package de.keksuccino.konkrete.rendering;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
	
	private static ResourceLocation WHITE = null;
	private static ResourceLocation BLANK = null;

	public static ResourceLocation getWhiteImageResource() {
		if (WHITE != null) {
			return WHITE;
		}
		if (Minecraft.getMinecraft().getTextureManager() == null) {
			return null;
		}
		BufferedImage i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		i.setRGB(0, 0, Color.WHITE.getRGB());
		ResourceLocation r = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("whiteback", new DynamicTexture(i));
		WHITE = r;
		return r;
	}
	
	public static ResourceLocation getBlankImageResource() {
		if (BLANK != null) {
			return BLANK;
		}
		if (Minecraft.getMinecraft().getTextureManager() == null) {
			return null;
		}
		BufferedImage i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		i.setRGB(0, 0, new Color(255, 255, 255, 0).getRGB());
		ResourceLocation r = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("blankback", new DynamicTexture(i));
		BLANK = r;
		return r;
	}
	
	public static void setScale(float scale) {
        GlStateManager.enableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
    }
	
    public static void postScale() {
    	GlStateManager.popMatrix();
    }
    
    public static void doubleBlit(double x, double y, float f1, float f2, int w, int h) {
    	innerDoubleBlit(x, x + (double)w, y, y + (double)h, 0, (f1 + 0.0F) / (float)w, (f1 + (float)w) / (float)w, (f2 + 0.0F) / (float)h, (f2 + (float)h) / (float)h);
    }

    public static void innerDoubleBlit(double x, double xEnd, double y, double yEnd, int z, float f1, float f2, float f3, float f4) {
    	Tessellator tesselator = Tessellator.getInstance();
    	BufferBuilder bufferbuilder = tesselator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, yEnd, (double)z).tex(f1, f4).endVertex();
        bufferbuilder.pos(xEnd, yEnd, (double)z).tex(f2, f4).endVertex();
        bufferbuilder.pos(xEnd, y, (double)z).tex(f2, f3).endVertex();
        bufferbuilder.pos(x, y, (double)z).tex(f1, f3).endVertex();
        tesselator.draw();
    }

    /**
     * Returns the converted color or NULL if the color could not be converted.
     */
    public static Color getColorFromHexString(String hex) {
		try {
			hex = hex.replace("#", "");
			if (hex.length() == 6) {
				return new Color(
						Integer.valueOf(hex.substring(0, 2), 16),
						Integer.valueOf(hex.substring(2, 4), 16),
						Integer.valueOf(hex.substring(4, 6), 16));
			}
			if (hex.length() == 8) {
				return new Color(
						Integer.valueOf(hex.substring(0, 2), 16),
						Integer.valueOf(hex.substring(2, 4), 16),
						Integer.valueOf(hex.substring(4, 6), 16),
						Integer.valueOf(hex.substring(6, 8), 16));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

    public static void setZLevelPre(int zLevel) {
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableDepth();
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0D, 0.0D, zLevel);
    }

    public static void setZLevelPost() {
    	GlStateManager.popMatrix();
    	GlStateManager.enableRescaleNormal();
    	GlStateManager.enableDepth();
    }

	public static void bindTexture(ResourceLocation texture, boolean depthTest) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GlStateManager.enableBlend();
		if (depthTest) {
			GlStateManager.enableDepth();
		}
	}

	public static void bindTexture(ResourceLocation texture) {
		bindTexture(texture, false);
	}
    
    public static void fill(float minX, float minY, float maxX, float maxY, int color, float opacity) {
    	
		if (minX < maxX) {
			float i = minX;
			minX = maxX;
			maxX = i;
		}
		if (minY < maxY) {
			float j = minY;
			minY = maxY;
			maxY = j;
		}

		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;
		float a = (float)(color >> 24 & 255) / 255.0F;
		
		a = a * opacity;
		
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bb = tessellator.getBuffer();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);
		bb.begin(7, DefaultVertexFormats.POSITION);
		bb.pos(minX, maxY, 0.0F).endVertex();
		bb.pos(maxX, maxY, 0.0F).endVertex();
		bb.pos(maxX, minY, 0.0F).endVertex();
		bb.pos(minX, minY, 0.0F).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		
	}

	public static void enableScissor(int x, int y, int width, int height) {
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(x, y, width, height);
	}

	public static void disableScissor() {
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

}
