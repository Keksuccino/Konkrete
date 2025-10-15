package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface IMixinMouseHandler {

    @Accessor("activeButton") public MouseButtonInfo getActiveButtonKonkrete();

    @Accessor("lastClickButton") public int get_lastClickButton_Konkrete();

}
