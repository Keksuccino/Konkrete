package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.SuggestionsList.class)
public interface AccessorMixinSuggestionsList {

    @Accessor("offset") int get_offset_Konkrete();

    @Accessor("lastMouse") Vec2 get_lastMouse_Konkrete();

    @Accessor("lastMouse") void set_lastMouse_Konkrete(Vec2 lastMouse);

    @Accessor("rect") Rect2i get_rect_Konkrete();

    @Accessor("current") int get_current_Konkrete();

}
