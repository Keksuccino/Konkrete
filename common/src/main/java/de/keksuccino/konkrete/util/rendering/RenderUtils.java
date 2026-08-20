package de.keksuccino.konkrete.util.rendering;

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
public class RenderUtils {

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

	@NotNull
	public static Identifier register(@NotNull String location, @NotNull AbstractTexture texture) {
		Objects.requireNonNull(location);
		Objects.requireNonNull(texture);
        Identifier loc = location.contains(":") ? Identifier.parse(location) : Identifier.fromNamespaceAndPath("konkrete", location);
		Minecraft.getInstance().getTextureManager().register(loc, texture);
		return loc;
	}

}
