package de.keksuccino.konkrete.util.watermedia.vulkan;

import com.mojang.blaze3d.vulkan.VulkanDevice;
import de.keksuccino.konkrete.util.watermedia.WatermediaReflectionBridge;
import org.jetbrains.annotations.Nullable;

/**
 * Render/device-thread lifecycle bridge for Watermedia's optional Vulkan engine.
 * All entry points must remain presence-gated: registration reflects the optional VKContext API, while returned
 * devices and contexts are borrowed and become unavailable as soon as device close begins.
 */
public final class WatermediaVulkanInterop {

    @Nullable private static volatile WatermediaVulkanContext bridge;
    private static volatile boolean deviceClosing;

    private WatermediaVulkanInterop() {}

    /** Registers Minecraft's borrowed live device once on its device thread; Watermedia's VKContext API must exist. */
    public static synchronized void register(VulkanDevice vulkanDevice) {
        if (bridge != null) throw new IllegalStateException("A Watermedia Vulkan bridge is already registered");
        bridge = new WatermediaVulkanContext(vulkanDevice);
        deviceClosing = false;
    }

    /** Begins terminal close on the device thread, synchronously releasing players before native teardown. */
    public static synchronized void beginDeviceClose(VulkanDevice vulkanDevice) {
        WatermediaVulkanContext current = bridge;
        if (current == null || current.device() != vulkanDevice) return;
        deviceClosing = true;
        // Vulkan player release must finish while its borrowed VkDevice is alive; unlike OpenGL, it can never fall
        // through to daemon cleanup after device destruction.
        WatermediaReflectionBridge.releaseVulkanPlayersBeforeDeviceClose();
        current.beginDeviceClose();
    }

    /** Finishes terminal close on the device thread after native teardown, freeing bridge-owned CPU metadata. */
    public static synchronized void finishDeviceClose(VulkanDevice vulkanDevice) {
        WatermediaVulkanContext current = bridge;
        if (current == null || current.device() != vulkanDevice) return;
        bridge = null;
        current.finishDeviceClose();
    }

    /** Returns the borrowed live device, or null before registration and after close begins. */
    @Nullable
    public static VulkanDevice device() {
        WatermediaVulkanContext current = bridge;
        return current != null && !deviceClosing ? current.device() : null;
    }

    /** Returns the borrowed-device VKContext proxy, or null before registration and after close begins. */
    @Nullable
    public static Object context() {
        WatermediaVulkanContext current = bridge;
        return current != null && !deviceClosing ? current.proxy() : null;
    }

    /** Drains Watermedia retirements on the device thread while the borrowed device remains alive. */
    public static void drainRetirements(VulkanDevice vulkanDevice) {
        WatermediaVulkanContext current = bridge;
        if (current != null && current.device() == vulkanDevice) current.drainRetirements();
    }

}
