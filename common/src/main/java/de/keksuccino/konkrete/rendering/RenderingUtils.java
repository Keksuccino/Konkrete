package de.keksuccino.konkrete.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class RenderingUtils {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final String VULKAN_BACKEND_NAME = "Vulkan";

	/**
	 * Returns whether Minecraft's currently initialized render device is using Vulkan.
	 *
	 * @return {@code true} if the active render backend is Vulkan, otherwise {@code false}
	 * @throws IllegalStateException if called before Minecraft has initialized the render device
	 */
	public static boolean isVulkanActive() {
		GpuDevice device = RenderSystem.getDevice();
		return VULKAN_BACKEND_NAME.equals(device.getDeviceInfo().backendName());
	}

    /**
     * Returns the converted color or NULL if the color could not be converted.
     */
	@Nullable
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
		} catch (Exception ex) {
			LOGGER.error("Failed to build Color object from HEX color string!", ex);
		}
		return null;
	}

	public static float getPartialTick() {
		return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	public static boolean isXYInArea(int targetX, int targetY, int x, int y, int width, int height) {
		return isXYInArea((double)targetX, targetY, x, y, width, height);
	}

	public static boolean isXYInArea(double targetX, double targetY, double x, double y, double width, double height) {
		return (targetX >= x) && (targetX < (x + width)) && (targetY >= y) && (targetY < (y + height));
	}

	public static void resetGuiScale() {
		Window m = Minecraft.getInstance().getWindow();
		m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
	}

	/**
	 * @param color The color.
	 * @param newAlpha Value between 0 and 255.
	 * @return The given color with new alpha.
	 */
	public static int replaceAlphaInColor(int color, int newAlpha) {
		newAlpha = Math.min(newAlpha, 255);
		return color & 16777215 | newAlpha << 24;
	}

	/**
	 * @param color The color.
	 * @param newAlpha Value between 0.0F and 1.0F.
	 * @return The given color with new alpha.
	 */
	public static int replaceAlphaInColor(int color, float newAlpha) {
		return replaceAlphaInColor(color, (int)(newAlpha * 255.0F));
	}

	@NotNull
	public static Identifier registerTexture(@NotNull String location, @NotNull AbstractTexture texture) {
		Objects.requireNonNull(location);
		Objects.requireNonNull(texture);
        Identifier loc = location.contains(":") ? Identifier.parse(location) : Identifier.fromNamespaceAndPath("konkrete", location);
		Minecraft.getInstance().getTextureManager().register(loc, texture);
		return loc;
	}

}
