package de.keksuccino.konkrete.mixin.mixins.common.client.compat.watermedia;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import org.lwjgl.vulkan.VkQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanQueue.class)
public class MixinVulkanQueue {

    /** @reason Vulkan requires every host operation on a shared queue to use the same external-synchronization monitor. */
    @WrapOperation(method = "waitIdle", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkQueueWaitIdle(Lorg/lwjgl/vulkan/VkQueue;)I"))
    private int wrap_waitIdle_Konkrete(VkQueue queue, Operation<Integer> original) {
        synchronized (queue) {
            return original.call(queue);
        }
    }

}
