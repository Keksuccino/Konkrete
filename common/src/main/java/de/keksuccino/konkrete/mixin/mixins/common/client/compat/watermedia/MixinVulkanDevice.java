package de.keksuccino.konkrete.mixin.mixins.common.client.compat.watermedia;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanInstance;
import com.mojang.blaze3d.vulkan.VulkanPhysicalDevice;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import de.keksuccino.konkrete.util.watermedia.vulkan.WatermediaVulkanInterop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.vulkan.VkDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(VulkanDevice.class)
public class MixinVulkanDevice {

    @Unique private static final Logger LOGGER_KONKRETE = LogManager.getLogger();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void after_init_Konkrete(ShaderSource shaderSource, VulkanInstance instance, VulkanPhysicalDevice physicalDevice, Set<String> extensions, VkDevice device, long vma, CheckpointExtension checkpointExtension, CallbackInfo info) {
        try {
            WatermediaVulkanInterop.register((VulkanDevice) (Object) this);
        } catch (Throwable throwable) {
            // Optional media integration must never invalidate an otherwise usable Minecraft Vulkan device.
            LOGGER_KONKRETE.error("[KONKRETE] Failed to register Minecraft's Vulkan device with Watermedia", throwable);
        }
    }

    /** @reason Publish device shutdown before Minecraft destroys resources borrowed by the optional Watermedia context. */
    @WrapMethod(method = "close")
    private void wrap_close_Konkrete(Operation<Void> original) {
        VulkanDevice device = (VulkanDevice) (Object) this;
        try {
            WatermediaVulkanInterop.beginDeviceClose(device);
        } catch (Throwable throwable) {
            LOGGER_KONKRETE.error("[KONKRETE] Failed to begin Watermedia Vulkan device shutdown", throwable);
        }
        try {
            original.call();
        } finally {
            try {
                WatermediaVulkanInterop.finishDeviceClose(device);
            } catch (Throwable throwable) {
                LOGGER_KONKRETE.error("[KONKRETE] Failed to finish Watermedia Vulkan device shutdown", throwable);
            }
        }
    }

}
