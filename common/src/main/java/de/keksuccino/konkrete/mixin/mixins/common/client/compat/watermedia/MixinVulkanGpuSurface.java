package de.keksuccino.konkrete.mixin.mixins.common.client.compat.watermedia;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vulkan.VulkanGpuSurface;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanGpuSurface.class)
public class MixinVulkanGpuSurface {

    /** @reason Present uses the same graphics VkQueue as Watermedia uploads and therefore shares its external-synchronization monitor. */
    @WrapOperation(method = "present", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/KHRSwapchain;vkQueuePresentKHR(Lorg/lwjgl/vulkan/VkQueue;Lorg/lwjgl/vulkan/VkPresentInfoKHR;)I"))
    private int wrap_present_Konkrete(VkQueue queue, VkPresentInfoKHR presentInfo, Operation<Integer> original) {
        synchronized (queue) {
            return original.call(queue, presentInfo);
        }
    }

}
