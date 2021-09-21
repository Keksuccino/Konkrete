package de.keksuccino.konkrete.rendering;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

public class RenderUtils {
	
	private static Identifier WHITE = null;
	private static Identifier BLANK = null;

	public static Identifier getWhiteImageResource() {
		if (WHITE != null) {
			return WHITE;
		}
		if (MinecraftClient.getInstance().getTextureManager() == null) {
			return null;
		}
		NativeImage i = new NativeImage(1, 1, true);
		i.setPixelColor(0, 0, Color.WHITE.getRGB());
		Identifier r = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("whiteback", new NativeImageBackedTexture(i));
		WHITE = r;
		return r;
	}
	
	public static Identifier getBlankImageResource() {
		if (BLANK != null) {
			return BLANK;
		}
		if (MinecraftClient.getInstance().getTextureManager() == null) {
			return null;
		}
		NativeImage i = new NativeImage(1, 1, true);
		i.setPixelColor(0, 0, new Color(255, 255, 255, 0).getRGB());
		Identifier r = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("blankback", new NativeImageBackedTexture(i));
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
    	RenderSystem.setShader(GameRenderer::getPositionTexShader);
    	BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferbuilder.vertex(x, yEnd, (double)z).texture(f1, f4).next();
        bufferbuilder.vertex(xEnd, yEnd, (double)z).texture(f2, f4).next();
        bufferbuilder.vertex(xEnd, y, (double)z).texture(f2, f3).next();
        bufferbuilder.vertex(x, y, (double)z).texture(f1, f3).next();
        bufferbuilder.end();
        BufferRenderer.draw(bufferbuilder);
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
    
    public static void setZLevelPre(MatrixStack matrix, int zLevel) {
		RenderSystem.disableDepthTest();
		matrix.push();
		matrix.translate(0.0D, 0.0D, zLevel);
    }
    
    public static void setZLevelPost(MatrixStack matrix) {
    	matrix.pop();
    	RenderSystem.enableDepthTest();
    }

    public static void bindTexture(Identifier texture, boolean depthTest) {
    	RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        if (depthTest) {
        	RenderSystem.enableDepthTest();
        }
    }

    public static void bindTexture(Identifier texture) {
    	bindTexture(texture, false);
    }
    
    public static void fill(MatrixStack matrix, float minX, float minY, float maxX, float maxY, int color, float opacity) {
		Matrix4f matrix4f = matrix.peek().getModel();
		
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
		
		BufferBuilder bb = Tessellator.getInstance().getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
		bb.vertex(matrix4f, minX, maxY, 0.0F).color(r, g, b, a).next();
		bb.vertex(matrix4f, maxX, maxY, 0.0F).color(r, g, b, a).next();
		bb.vertex(matrix4f, maxX, minY, 0.0F).color(r, g, b, a).next();
		bb.vertex(matrix4f, minX, minY, 0.0F).color(r, g, b, a).next();
		bb.end();
		BufferRenderer.draw(bb);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

}
