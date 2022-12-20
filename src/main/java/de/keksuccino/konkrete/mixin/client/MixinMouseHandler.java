//---
package de.keksuccino.konkrete.mixin.client;

import de.keksuccino.konkrete.events.client.ScreenMouseClickedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MouseHelper.class)
public class MixinMouseHandler {

    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow @Final private Minecraft minecraft;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MainWindow;getScreenHeight()I", shift = At.Shift.AFTER), method = "onPress", locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void onOnPress(long window, int i1, int i2, int i3, CallbackInfo info, boolean flag, int i, boolean[] aboolean, double d0) {
        if (flag) {
            double d2 = this.xpos * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
            double d3 = this.ypos * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
            Screen.wrapScreenError(() -> {
                MinecraftForge.EVENT_BUS.post(new ScreenMouseClickedEvent(d2, d3, i));
            }, "Konkrete mouseClicked event handler", this.getClass().getCanonicalName());
        }
    }

}
