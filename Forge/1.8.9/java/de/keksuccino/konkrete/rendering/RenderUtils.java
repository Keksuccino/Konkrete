package de.keksuccino.konkrete.rendering;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class RenderUtils {
	
	private static ResourceLocation WHITE = null;
	private static ResourceLocation BLANK = null;

	public static float getRenderPartialTicks() {
		try {
			Field f = ReflectionHelper.findField(Minecraft.class, "timer", "field_71428_T");
			if (f != null) {
				Timer t = (Timer) f.get(Minecraft.getMinecraft());
				if (t != null) {
					return t.renderPartialTicks;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
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
    	WorldRenderer bufferbuilder = tesselator.getWorldRenderer();
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

}
