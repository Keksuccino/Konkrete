package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface IMixinAbstractWidget {

    @Accessor("height") public void setHeightKonkrete(int height);

}
