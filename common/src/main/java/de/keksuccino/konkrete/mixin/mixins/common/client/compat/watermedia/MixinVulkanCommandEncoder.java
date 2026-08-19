package de.keksuccino.konkrete.mixin.mixins.common.client.compat.watermedia;

import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import de.keksuccino.konkrete.util.watermedia.vulkan.WatermediaVulkanInterop;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanCommandEncoder.class)
public class MixinVulkanCommandEncoder {

    @Shadow @Final private VulkanDevice device;

    /** @reason Move cross-thread Watermedia retirement requests onto Minecraft's render-thread destruction queue. */
    @Inject(method = {"submit", "destroy"}, at = @At("HEAD"))
    private void before_submitOrDestroy_Konkrete(CallbackInfo info) {
        WatermediaVulkanInterop.drainRetirements(this.device);
    }

}
