package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EditBox.class)
public interface IMixinEditBox {

    @Accessor("maxLength") public int getMaxLengthKonkrete();

    @Accessor("highlightPos") public int getHighlightPosKonkrete();

    @Accessor("isEditable") public boolean getIsEditableKonkrete();

    @Invoker("onValueChange") public abstract void onValueChangeInvokerKonkrete(String value);

}
