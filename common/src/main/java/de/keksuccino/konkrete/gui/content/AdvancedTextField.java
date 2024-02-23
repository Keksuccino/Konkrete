package de.keksuccino.konkrete.gui.content;

import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.mixin.mixins.client.IMixinEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@Deprecated(forRemoval = true)
public class AdvancedTextField extends ExtendedEditBox {

	@Deprecated
	public AdvancedTextField(Font font, int x, int y, int width, int height, boolean handleSelf, @Nullable CharacterFilter characterFilter) {
		super(font, x, y, width, height, Component.literal(""), handleSelf);
		this.handleSelf = handleSelf;
		this.characterFilter = characterFilter;
	}

	public boolean isEditable() {
		return this.getAccessor().getIsEditableKonkrete();
	}
	
	public boolean isLeftClicked() {
		return (this.isHoveredOrFocused() && MouseInput.isLeftMouseDown());
	}

	public IMixinEditBox getAccessor() {
		return ((IMixinEditBox)this);
	}

	@Deprecated
	public boolean isHoveredOrFocused() {
		return this.isHovered();
	}

	@Deprecated
	protected void setResponderEntryValue(String text) {
		this.getAccessor().onValueChangeKonkrete(text);
	}

	@Deprecated
	public int getMaxStringLength() {
		return this.getAccessor().getMaxLengthKonkrete();
	}

	@Deprecated
	public int getSelectionEnd() {
		return this.getAccessor().getHightlightPosKonkrete();
	}

	@Deprecated
	public boolean isEnabled() {
		return this.isEditable();
	}

}
