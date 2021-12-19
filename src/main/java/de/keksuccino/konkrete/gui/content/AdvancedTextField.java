package de.keksuccino.konkrete.gui.content;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import de.keksuccino.konkrete.input.CharData;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

public class AdvancedTextField extends EditBox {

	private int tick = 0;
	private boolean handle;
	private CharacterFilter filter;
	private boolean leftDown = false;
	
	public AdvancedTextField(Font fontrenderer, int x, int y, int width, int height, boolean handleTextField, @Nullable CharacterFilter filter) {
		super(fontrenderer, x, y, width, height, new TextComponent(""));
		this.handle = handleTextField;
		this.filter = filter;
		
		if (this.handle) {
			KeyboardHandler.addKeyPressedListener(this::onKeyPress);
			KeyboardHandler.addKeyReleasedListener(this::onKeyReleased);
			KeyboardHandler.addCharTypedListener(this::onCharTyped);
		}
	}
	
	public boolean isHoveredOrFocused() {
		int mouseX = MouseInput.getMouseX();
		int mouseY = MouseInput.getMouseY();
		if ((mouseX >= this.getX()) && (mouseX <= this.getX() + this.getWidth()) && (mouseY >= this.getY()) && mouseY <= this.getY() + this.getHeight()) {
			return true;
		}
		return false;
	}
	
	//charTyped
	@Override
	public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
		if (this.isVisible() && this.isFocused()) {
			if ((this.filter == null) || this.filter.isAllowed(p_charTyped_1_)) {
				if (this.isEnabled()) {
					this.insertText(Character.toString(p_charTyped_1_));
				}
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	
	@Override
	public void insertText(String textToWrite) {
		String s = "";
		String s1 = textToWrite;
		if (this.filter != null) {
			s1 = this.filter.filterForAllowedChars(textToWrite);
		}
		int i = this.getCursorPosition() < this.getSelectionEnd() ? this.getCursorPosition() : this.getSelectionEnd();
		int j = this.getCursorPosition() < this.getSelectionEnd() ? this.getSelectionEnd() : this.getCursorPosition();
		int k = this.getMaxStringLength() - this.getValue().length() - (i - j);
		if (!this.getValue().isEmpty()) {
			s = s + this.getValue().substring(0, i);
		}

		int l;
		if (k < s1.length()) {
			s = s + s1.substring(0, k);
			l = k;
		} else {
			s = s + s1;
			l = s1.length();
		}

		if (!this.getValue().isEmpty() && j < this.getValue().length()) {
			s = s + this.getValue().substring(j);
		}

		this.setValue(s);
		this.setCursorPosition(i + l);
		this.setHighlightPos(this.getCursorPosition());
		this.setResponderEntryValue(this.getValue());
	}
	
	protected void setResponderEntryValue(String text) {
		try {
			//onTextChanged
			Method m = ReflectionHelper.findMethod(EditBox.class, "m_94174_", String.class);
			m.invoke(this, text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getMaxStringLength() {
		try {
			//maxStringLength
			Field f = ReflectionHelper.findField(EditBox.class, "f_94094_");
			return f.getInt(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int getSelectionEnd() {
		try {
			//selectionEnd
			Field f = ReflectionHelper.findField(EditBox.class, "f_94102_");
			return f.getInt(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public boolean isEnabled() {
		try {
			//isEnabled
			Field f = ReflectionHelper.findField(EditBox.class, "f_94098_");
			return f.getBoolean(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isLeftClicked() {
		return (this.isHoveredOrFocused() && MouseInput.isLeftMouseDown());
	}
	
	//renderButton
	@Override
	public void renderButton(PoseStack matrix, int mouseX, int mouseY, float p_renderButton_3_) {
		super.renderButton(matrix, mouseX, mouseY, p_renderButton_3_);
		
		if (this.handle) {
			if (this.tick > 7) {
				this.tick();
				this.tick = 0;
			} else {
				tick++;
			}
			
			if (MouseInput.isLeftMouseDown() && !this.leftDown) {
				super.mouseClicked(mouseX, mouseY, 0);
				this.leftDown = true;
			}
			if (!MouseInput.isLeftMouseDown()) {
				this.leftDown = false;
			}
		}
	}
	
	public void onKeyPress(KeyboardData d) {
		super.keyPressed(d.keycode, d.scancode, d.modfiers);
	}
	
	public void onKeyReleased(KeyboardData d) {
		super.keyReleased(d.keycode, d.scancode, d.modfiers);
	}
	
	public void onCharTyped(CharData d) {
		this.charTyped(d.typedChar, d.modfiers);
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

}
