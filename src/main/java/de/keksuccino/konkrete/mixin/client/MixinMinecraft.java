package de.keksuccino.konkrete.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    //TODO experimental
    @Inject(at = @At("RETURN"), method = "displayGuiScreen")
    private void onDisplayGuiScreen(GuiScreen screen, CallbackInfo info) {
        //Fixes custom EditBoxes added to screens not reacting to key repeat events in some cases
        if (screen != null) {
            Keyboard.enableRepeatEvents(true);
        } else {
            Keyboard.enableRepeatEvents(false);
        }
    }

}
