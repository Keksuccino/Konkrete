package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface IMixinMouseHandler {

    @Accessor("lastClickButton") int get_lastClickButton_Konkrete();

    @Invoker("onMove") void invoke_onMove_Konkrete(long windowHandle, double xPos, double yPos);

}
