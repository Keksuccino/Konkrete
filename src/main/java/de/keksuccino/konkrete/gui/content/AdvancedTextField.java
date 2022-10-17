package de.keksuccino.konkrete.gui.content;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import de.keksuccino.konkrete.gui.content.handling.AdvancedWidgetsHandler;
import de.keksuccino.konkrete.gui.content.handling.IAdvancedWidgetBase;
import de.keksuccino.konkrete.input.CharData;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.mixin.client.IMixinEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

//TODO übernehmen 1.5.3
public class AdvancedTextField extends EditBox implements IAdvancedWidgetBase {

	private final boolean handleSelf;
	private final CharacterFilter characterFilter;

	public AdvancedTextField(Font font, int x, int y, int width, int height, boolean handleSelf, @Nullable CharacterFilter characterFilter) {

		super(font, x, y, width, height, new TextComponent(""));
		this.handleSelf = handleSelf;
		this.characterFilter = characterFilter;

	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		if ((this.characterFilter == null) || this.characterFilter.isAllowed(character)) {
			return super.charTyped(character, modifiers);
		}
		return false;
	}

	@Override
	public void insertText(String textToWrite) {
		if (this.characterFilter != null) {
			textToWrite = this.characterFilter.filterForAllowedChars(textToWrite);
		}
		super.insertText(textToWrite);
	}

	@Override
	public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partial) {

		if (this.handleSelf) {
			AdvancedWidgetsHandler.handleWidget(this);
		}

		super.renderButton(matrix, mouseX, mouseY, partial);

	}

	@Override
	public void onTick() {
		if (this.handleSelf) {
			this.tick();
		}
	}

	@Override
	public void onKeyPress(KeyboardData d) {
		if (this.handleSelf) {
			this.keyPressed(d.keycode, d.scancode, d.modfiers);
		}
	}

	@Override
	public void onKeyReleased(KeyboardData d) {
		if (this.handleSelf) {
			this.keyReleased(d.keycode, d.scancode, d.modfiers);
		}
	}

	@Override
	public void onCharTyped(CharData d) {
		if (this.handleSelf) {
			this.charTyped(d.typedChar, d.modfiers);
		}
	}

	@Override
	public void onMouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (this.handleSelf) {
			this.mouseClicked(mouseX, mouseY, mouseButton);
		}
	}

	public boolean isHovered() {
		int mouseX = MouseInput.getMouseX();
		int mouseY = MouseInput.getMouseY();
		if ((mouseX >= this.getX()) && (mouseX <= this.getX() + this.getWidth()) && (mouseY >= this.getY()) && mouseY <= this.getY() + this.getHeight()) {
			return true;
		}
		return false;
	}

	public boolean isEditable() {
		return this.getAccessor().getIsEditableKonkrete();
	}

	public boolean isLeftClicked() {
		return (this.isHoveredOrFocused() && MouseInput.isLeftMouseDown());
	}

	public int getWidth() {
		return this.width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getX() {
		return this.x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return this.y;
	}

	public void setY(int y) {
		this.y = y;
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
