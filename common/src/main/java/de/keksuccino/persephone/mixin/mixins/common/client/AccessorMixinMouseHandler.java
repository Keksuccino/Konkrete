package de.keksuccino.persephone.mixin.mixins.common.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface AccessorMixinMouseHandler {

    @Accessor("lastClickButton") int get_lastClickButton_Persephone();

}
