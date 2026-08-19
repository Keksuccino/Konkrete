package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface AccessorMixinHud {

    @Accessor("title") Component get_title_Konkrete();

    @Accessor("subtitle") Component get_subtitle_Konkrete();

    @Accessor("overlayMessageString") Component get_overlayMessageString_Konkrete();

    @Accessor("overlayMessageTime") int get_overlayMessageTime_Konkrete();

    @Accessor("toolHighlightTimer") int get_toolHighlightTimer_Konkrete();

}
