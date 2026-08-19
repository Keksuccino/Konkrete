package de.keksuccino.konkrete.mixin.mixins.common.client.compat.rinku;

import de.keksuccino.konkrete.util.rinku.BrowserHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "tick", at = @At("HEAD"))
    private void before_tick_Konkrete(CallbackInfo info) {
        BrowserHandler.tick();
    }

}
