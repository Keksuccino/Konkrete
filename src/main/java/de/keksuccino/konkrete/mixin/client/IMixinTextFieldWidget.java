package de.keksuccino.konkrete.mixin.client;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextFieldWidget.class)
public interface IMixinTextFieldWidget {

    @Invoker("onValueChange") public void onValueChangeKonkrete(String s);

    @Accessor("maxLength") public int getMaxLengthKonkrete();

    @Accessor("maxLength") public void setMaxLengthKonkrete(int i);

    @Accessor("highlightPos") public int getHighlightPosKonkrete();

    @Accessor("highlightPos") public void setHighlightPosKonkrete(int i);

    @Accessor("isEditable") public boolean getIsEditableKonkrete();

    @Accessor("isEditable") public void setIsEditableKonkrete(boolean b);

}
