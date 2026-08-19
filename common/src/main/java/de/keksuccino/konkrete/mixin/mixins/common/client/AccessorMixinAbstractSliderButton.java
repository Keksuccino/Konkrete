package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSliderButton.class)
public interface AccessorMixinAbstractSliderButton {

    @Accessor("canChangeValue") boolean get_canChangeValue_Konkrete();

}
