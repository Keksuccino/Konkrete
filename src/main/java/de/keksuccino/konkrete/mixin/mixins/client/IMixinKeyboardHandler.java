package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyboardHandler.class)
public interface IMixinKeyboardHandler {

    @Accessor("sendRepeatsToGui") public boolean getSendRepeatsToGuiKonkrete();

}
