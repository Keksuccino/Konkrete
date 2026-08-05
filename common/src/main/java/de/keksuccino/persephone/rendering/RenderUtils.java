package de.keksuccino.persephone.rendering;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

public class RenderUtils {

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

}
