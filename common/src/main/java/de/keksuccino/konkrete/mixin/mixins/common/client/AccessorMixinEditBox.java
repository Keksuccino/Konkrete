package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditBox.class)
public interface AccessorMixinEditBox {

    @Accessor("isEditable") boolean get_isEditable_Konkrete();

    @Accessor("displayPos") int get_displayPos_Konkrete();

    @Accessor("displayPos") void set_displayPos_Konkrete(int displayPos);

    @Accessor("bordered") boolean get_bordered_Konkrete();

    @Accessor("maxLength") int get_maxLength_Konkrete();

    @Accessor("highlightPos") int get_highlightPos_Konkrete();

    @Accessor("textColor") int get_textColor_Konkrete();

    @Accessor("textColorUneditable") int get_textColorUneditable_Konkrete();

    @Accessor("focusedTime") long get_focusedTime_Konkrete();

    @Accessor("hint") Component get_hint_Konkrete();

    @Accessor("suggestion") String get_suggestion_Konkrete();

    @Accessor("invertHighlightedTextColor") boolean get_invertHighlightedTextColor_Konkrete();

}
