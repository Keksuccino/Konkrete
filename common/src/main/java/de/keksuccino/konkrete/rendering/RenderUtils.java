package de.keksuccino.konkrete.rendering;

import java.awt.Color;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@SuppressWarnings("all")
public class RenderUtils {

	/**
	 * Doesn't work anymore. Don't use this. Will always return NULL.
	 */
	@Deprecated(forRemoval = true)
	@Nullable
	public static ResourceLocation getWhiteImageResource() {
		return null;
	}

	/**
	 * Doesn't work anymore. Don't use this. Will always return NULL.
	 */
	@Deprecated(forRemoval = true)
	@Nullable
	public static ResourceLocation getBlankImageResource() {
		return null;
	}

	public static void setScale(GuiGraphics graphics, float scale) {
		setScale(graphics.pose(), scale);
	}

	public static void setScale(PoseStack graphics, float scale) {
		graphics.pushPose();
		graphics.scale(scale, scale, scale);
    }

	public static void postScale(GuiGraphics graphics) {
		postScale(graphics.pose());
	}

    public static void postScale(PoseStack graphics) {
    	graphics.popPose();
    }

    public static void doubleBlit(double x, double y, float f1, float f2, int w, int h) {
    	innerDoubleBlit(x, x + (double)w, y, y + (double)h, 0, (f1 + 0.0F) / (float)w, (f1 + (float)w) / (float)w, (f2 + 0.0F) / (float)h, (f2 + (float)h) / (float)h);
    }

    public static void innerDoubleBlit(double x, double xEnd, double y, double yEnd, int z, float f1, float f2, float f3, float f4) {
    	RenderSystem.setShader(GameRenderer::getPositionTexShader);
    	BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex((float) x, (float) yEnd, z).setUv(f1, f4);
        bufferbuilder.addVertex((float) xEnd, (float) yEnd, z).setUv(f2, f4);
        bufferbuilder.addVertex((float) xEnd, (float) y, z).setUv(f2, f3);
        bufferbuilder.addVertex((float) x, (float) y, z).setUv(f1, f3);
        BufferUploader.drawWithShader(bufferbuilder.build());
    }
    
    /**
     * Returns the converted color or NULL if the color could not be converted.
     */
    public static Color getColorFromHexString(@NotNull String hex) {
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

	public static void setZLevelPre(GuiGraphics graphics, int zLevel) {
		setZLevelPre(graphics.pose(), zLevel);
	}

    public static void setZLevelPre(PoseStack graphics, int zLevel) {
		RenderSystem.disableDepthTest();
		graphics.pushPose();
		graphics.translate(0.0D, 0.0D, zLevel);
    }

	public static void setZLevelPost(GuiGraphics graphics) {
		setZLevelPost(graphics.pose());
	}

    public static void setZLevelPost(PoseStack graphics) {
    	graphics.popPose();
    	RenderSystem.enableDepthTest();
    }

    public static void bindTexture(ResourceLocation texture, boolean depthTest) {
    	RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        if (depthTest) {
        	RenderSystem.enableDepthTest();
        }
    }

    public static void bindTexture(ResourceLocation texture) {
    	bindTexture(texture, false);
    }

	public static void fill(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color, float opacity) {
		fill(graphics.pose(), minX, minY, maxX, maxY, color, opacity);
	}

	public static void fill(PoseStack graphics, float minX, float minY, float maxX, float maxY, int color, float opacity) {

		Matrix4f graphics4f = graphics.last().pose();

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

		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		bb.addVertex(graphics4f, minX, maxY, 0.0F).setColor(r, g, b, a);
		bb.addVertex(graphics4f, maxX, maxY, 0.0F).setColor(r, g, b, a);
		bb.addVertex(graphics4f, maxX, minY, 0.0F).setColor(r, g, b, a);
		bb.addVertex(graphics4f, minX, minY, 0.0F).setColor(r, g, b, a);

		BufferUploader.drawWithShader(bb.build());
		RenderSystem.disableBlend();

	}

}
