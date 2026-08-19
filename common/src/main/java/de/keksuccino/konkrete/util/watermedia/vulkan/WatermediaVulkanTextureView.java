package de.keksuccino.konkrete.util.watermedia.vulkan;

import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import org.lwjgl.vulkan.VK12;

/**
 * Makes WaterMedia's borrowed VkImageView usable by Minecraft's Vulkan render pass without transferring ownership.
 * The superclass-created placeholder view satisfies Minecraft's concrete texture-view contract and is the only view
 * destroyed here; the rotating external handle always remains owned and retired by WaterMedia.
 */
public final class WatermediaVulkanTextureView extends VulkanGpuTextureView {

    private final VulkanDevice device;
    private final long placeholderImageView;
    private volatile long externalImageView;

    /** Creates an owned placeholder on the render thread for a borrowed external image-view facade. */
    public WatermediaVulkanTextureView(VulkanDevice device, VulkanGpuTexture texture) {
        super(device, texture, 0, 1);
        this.device = device;
        this.placeholderImageView = super.vkImageView();
    }

    /** Publishes Watermedia's latest borrowed VkImageView on the render thread without taking ownership. */
    public void setExternalImageView(long externalImageView) {
        this.externalImageView = externalImageView;
    }

    /** Returns whether a non-zero external view is currently published. */
    public boolean hasExternalImageView() {
        return this.externalImageView != 0L;
    }

    /** Returns the current external view, falling back to the owned placeholder. */
    @Override
    public long vkImageView() {
        long handle = this.externalImageView;
        return handle != 0L ? handle : this.placeholderImageView;
    }

    /** Destroys only the owned placeholder on the device thread and never Watermedia's external view. */
    @Override
    public void destroy() {
        VK12.vkDestroyImageView(this.device.vkDevice(), this.placeholderImageView, null);
    }

}
