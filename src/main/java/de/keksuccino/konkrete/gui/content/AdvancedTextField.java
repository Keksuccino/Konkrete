package de.keksuccino.konkrete.gui.content;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class AdvancedTextField extends GuiTextField {

	private int tick = 0;
	private boolean handleSelf;
	private CharacterFilter filter;
	private boolean leftDown = false;
	
	public AdvancedTextField(FontRenderer fontRenderer, int x, int y, int width, int height, boolean handleSelf, @Nullable CharacterFilter filter) {
		super(MathUtils.getRandomNumberInRange(50, 400), fontRenderer, x, y, width, height);
		this.handleSelf = handleSelf;
		this.filter = filter;
		if (this.handleSelf) {
			KeyboardHandler.addKeyPressedListener(this::onKeyPress);
		}
	}

	@Override
	public void drawTextBox() {
		super.drawTextBox();
		if (this.handleSelf) {
//			if (this.tick > 7) {
				this.updateCursorCounter();
//				this.tick = 0;
//			} else {
//				tick++;
//			}
			if (MouseInput.isLeftMouseDown() && !this.leftDown) {
				this.mouseClicked(MouseInput.getMouseX(), MouseInput.getMouseY(), 0);
				this.leftDown = true;
			}
			if (!MouseInput.isLeftMouseDown()) {
				this.leftDown = false;
			}
		}
	}

	@Override
	public void writeText(String textToWrite) {
		if (this.filter != null) {
			textToWrite = this.filter.filterForAllowedChars(textToWrite);
		}
		super.writeText(textToWrite);
	}

	public boolean isHovered() {
		int mouseX = MouseInput.getMouseX();
		int mouseY = MouseInput.getMouseY();
		if ((mouseX >= this.x) && (mouseX <= this.x + this.width) && (mouseY >= this.y) && mouseY <= this.y + this.height) {
			return true;
		}
		return false;
	}
	
	public boolean isEditable() {
		try {
			Field f = ObfuscationReflectionHelper.findField(GuiTextField.class, "field_146226_p");
			return f.getBoolean(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isLeftClicked() {
		return (this.isHovered() && MouseInput.isLeftMouseDown());
	}

	public void onKeyPress(KeyboardData d) {
		this.textboxKeyTyped(d.typedChar, d.keycode);
	}

	@Deprecated
	public boolean isEnabled() {
		return this.isEditable();
	}
	
//	@Override
//	public boolean textboxKeyTyped(char typedChar, int keyCode) {
//		if (!this.isFocused()) {
//			return false;
//		} else if (GuiScreen.isKeyComboCtrlA(keyCode)) {
//			this.setCursorPositionEnd();
//			this.setSelectionPos(0);
//			return true;
//		} else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
//			GuiScreen.setClipboardString(this.getSelectedText());
//			return true;
//		} else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
//			if (this.isEnabled()) {
//				this.writeText(GuiScreen.getClipboardString());
//			}
//
//			return true;
//		} else if (GuiScreen.isKeyComboCtrlX(keyCode)) {
//			GuiScreen.setClipboardString(this.getSelectedText());
//
//			if (this.isEnabled()) {
//				this.writeText("");
//			}
//
//			return true;
//		} else {
//
//			//Exclude keys like SHIFT, ENTER, TAB, etc.
//			if ((keyCode == 42) || (keyCode == 54) || (keyCode == 58) || (keyCode == 42) || (keyCode == 15) || (keyCode == 28) || (keyCode == 1) || (keyCode == 29) || (keyCode == 56) || (keyCode == 184) || (keyCode == 157) || (keyCode == 219) || (keyCode == 220) || (keyCode == 184)) {
//				return false;
//			}
//
//			if (keyCode == 14) {
//
//				if (GuiScreen.isCtrlKeyDown()) {
//					if (this.isEnabled()) {
//						this.deleteWords(-1);
//					}
//				} else if (this.isEnabled()) {
//					this.deleteFromCursor(-1);
//				}
//
//				return true;
//
//			} else if (keyCode == 199) {
//
//				if (GuiScreen.isShiftKeyDown()) {
//					this.setSelectionPos(0);
//				} else {
//					this.setCursorPositionZero();
//				}
//
//				return true;
//
//			} else if (keyCode == 203) {
//
//				if (GuiScreen.isShiftKeyDown()) {
//					if (GuiScreen.isCtrlKeyDown()) {
//						this.setSelectionPos(this.getNthWordFromPos(-1, this.getSelectionEnd()));
//					} else {
//						this.setSelectionPos(this.getSelectionEnd() - 1);
//					}
//				} else if (GuiScreen.isCtrlKeyDown()) {
//					this.setCursorPosition(this.getNthWordFromCursor(-1));
//				} else {
//					this.moveCursorBy(-1);
//				}
//
//				return true;
//
//			} else if (keyCode == 205) {
//
//				if (GuiScreen.isShiftKeyDown()) {
//					if (GuiScreen.isCtrlKeyDown()) {
//						this.setSelectionPos(this.getNthWordFromPos(1, this.getSelectionEnd()));
//					} else {
//						this.setSelectionPos(this.getSelectionEnd() + 1);
//					}
//				} else if (GuiScreen.isCtrlKeyDown()) {
//					this.setCursorPosition(this.getNthWordFromCursor(1));
//				} else {
//					this.moveCursorBy(1);
//				}
//
//				return true;
//
//			} else if (keyCode == 207) {
//
//				if (GuiScreen.isShiftKeyDown()) {
//					this.setSelectionPos(this.getText().length());
//				} else {
//					this.setCursorPositionEnd();
//				}
//
//				return true;
//
//			} else if (keyCode == 211) {
//
//				if (GuiScreen.isCtrlKeyDown()) {
//					if (this.isEnabled()) {
//						this.deleteWords(1);
//					}
//				} else if (this.isEnabled()) {
//					this.deleteFromCursor(1);
//				}
//
//				return true;
//
//			} else {
//
//				if ((this.filter == null) || this.filter.isAllowed(typedChar)) {
//					if (this.isEnabled()) {
//						this.writeText(Character.toString(typedChar));
//					}
//
//					return true;
//				} else {
//					return false;
//				}
//
//			}
//
//		}
//	}

}
