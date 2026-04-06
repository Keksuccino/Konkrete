package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface IMixinKeyboardHandler {

    @Invoker("keyPress")
    void invoke_keyPress_Konkrete(long handle, int action, KeyEvent event);

    @Invoker("charTyped")
    void invoke_charTyped_Konkrete(long handle, CharacterEvent event);
}
