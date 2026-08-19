package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface AccessorMixinAbstractWidget {

    @Accessor("alpha") float get_alpha_Konkrete();

    @Accessor("height") void set_height_Konkrete(int height);

    @Accessor("message") void set_message_Konkrete(Component message);

    @Accessor("tooltip") WidgetTooltipHolder get_tooltip_Konkrete();

}
