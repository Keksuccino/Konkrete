package de.keksuccino.konkrete.rendering;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

@SuppressWarnings("deprecation")
public class RenderUtils {
	
	private static ResourceLocation WHITE = null;
	private static ResourceLocation BLANK = null;

	public static ResourceLocation getWhiteImageResource() {
		if (WHITE != null) {
			return WHITE;
		}
		if (Minecraft.getInstance().getTextureManager() == null) {
			return null;
		}
		NativeImage i = new NativeImage(1, 1, true);
		i.setPixelRGBA(0, 0, Color.WHITE.getRGB());
		ResourceLocation r = Minecraft.getInstance().getTextureManager().getDynamicTextureLocation("whiteback", new DynamicTexture(i));
		WHITE = r;
		return r;
	}
	
	public static ResourceLocation getBlankImageResource() {
		if (BLANK != null) {
			return BLANK;
		}
		if (Minecraft.getInstance().getTextureManager() == null) {
			return null;
		}
		NativeImage i = new NativeImage(1, 1, true);
		i.setPixelRGBA(0, 0, new Color(255, 255, 255, 0).getRGB());
		ResourceLocation r = Minecraft.getInstance().getTextureManager().getDynamicTextureLocation("blankback", new DynamicTexture(i));
		BLANK = r;
		return r;
	}
	
	public static void setScale(MatrixStack matrix, float scale) {
		matrix.push();
		matrix.scale(scale, scale, scale);
    }
	
    public static void postScale(MatrixStack matrix) {
    	matrix.pop();
    }

    public static void doubleBlit(double x, double y, float f1, float f2, int w, int h) {
    	innerDoubleBlit(x, x + (double)w, y, y + (double)h, 0, (f1 + 0.0F) / (float)w, (f1 + (float)w) / (float)w, (f2 + 0.0F) / (float)h, (f2 + (float)h) / (float)h);
    }

    public static void innerDoubleBlit(double x, double xEnd, double y, double yEnd, int z, float f1, float f2, float f3, float f4) {
    	BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, yEnd, (double)z).tex(f1, f4).endVertex();
        bufferbuilder.pos(xEnd, yEnd, (double)z).tex(f2, f4).endVertex();
        bufferbuilder.pos(xEnd, y, (double)z).tex(f2, f3).endVertex();
        bufferbuilder.pos(x, y, (double)z).tex(f1, f3).endVertex();
        bufferbuilder.finishDrawing();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.draw(bufferbuilder);
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
    
    //TODO übernehmen
    public static void setZLevelPre(MatrixStack matrix, int zLevel) {
		RenderSystem.disableRescaleNormal();
		RenderSystem.disableDepthTest();
		matrix.push();
		matrix.translate(0.0D, 0.0D, zLevel);
    }
    
    //TODO übernehmen
    public static void setZLevelPost(MatrixStack matrix) {
    	matrix.pop();
    	RenderSystem.enableRescaleNormal();
    	RenderSystem.enableDepthTest();
    }

}
