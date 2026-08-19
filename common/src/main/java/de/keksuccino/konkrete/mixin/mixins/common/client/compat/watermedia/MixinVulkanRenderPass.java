package de.keksuccino.konkrete.mixin.mixins.common.client.compat.watermedia;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanRenderPass;
import de.keksuccino.konkrete.util.watermedia.vulkan.WatermediaVulkanTextureView;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPass.class)
public class MixinVulkanRenderPass {

    @Unique private int sampledImageLayout_Konkrete = VK12.VK_IMAGE_LAYOUT_GENERAL;

    /** @reason Remember whether the sampled descriptor borrows Watermedia's shader-read-only image view. */
    @WrapOperation(method = "pushDescriptors", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vulkan/VulkanGpuTextureView;vkImageView()J"))
    private long wrap_imageView_Konkrete(VulkanGpuTextureView textureView, Operation<Long> original) {
        this.sampledImageLayout_Konkrete = textureView instanceof WatermediaVulkanTextureView watermediaView && watermediaView.hasExternalImageView() ? VK12.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL : VK12.VK_IMAGE_LAYOUT_GENERAL;
        return original.call(textureView);
    }

    /** @reason Minecraft assembles imageView immediately before imageLayout; describing Watermedia's borrowed view as GENERAL violates Vulkan's descriptor layout contract. */
    @WrapOperation(method = "pushDescriptors", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkDescriptorImageInfo$Buffer;imageLayout(I)Lorg/lwjgl/vulkan/VkDescriptorImageInfo$Buffer;"))
    private VkDescriptorImageInfo.Buffer wrap_imageLayout_Konkrete(VkDescriptorImageInfo.Buffer imageInfo, int imageLayout, Operation<VkDescriptorImageInfo.Buffer> original) {
        try {
            return original.call(imageInfo, this.sampledImageLayout_Konkrete);
        } finally {
            this.sampledImageLayout_Konkrete = VK12.VK_IMAGE_LAYOUT_GENERAL;
        }
    }

}
