package de.keksuccino.konkrete.mixin.mixins.common.client.compat.rinku;

import de.keksuccino.konkrete.util.rinku.BrowserHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Shadow private double xpos;
    @Shadow private double ypos;
    @Unique private final Minecraft minecraft_Konkrete = Minecraft.getInstance();

    @Inject(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void before_mouseMoved_Konkrete(CallbackInfo info) {
        double mouseX = this.xpos * this.minecraft_Konkrete.getWindow().getGuiScaledWidth() / this.minecraft_Konkrete.getWindow().getScreenWidth();
        double mouseY = this.ypos * this.minecraft_Konkrete.getWindow().getGuiScaledHeight() / this.minecraft_Konkrete.getWindow().getScreenHeight();
        BrowserHandler.mouseMoved(mouseX, mouseY);
    }

}
