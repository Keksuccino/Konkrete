package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface IMixinMouseHandler {

    @Accessor("lastClickButton") int get_lastClickButton_Konkrete();

}
