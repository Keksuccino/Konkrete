package de.keksuccino.konkrete.gui.content;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;

public class DropdownMenu implements IMenu {
	
	private int width;
	private int height;
	private int x;
	private int y;
	private List<AdvancedButton> content = new ArrayList<AdvancedButton>();
	private AdvancedButton dropdown;
	private boolean opened = false;
	private boolean hovered = false;
	private boolean autoclose = false;
	private int space;
	
	public DropdownMenu(String label, int width, int height, int x, int y, int space) {
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		this.space = space;
		
		this.dropdown = new AdvancedButton(0, 0, 0, 0, label, true, (press) -> {
			this.toggleMenu();
		});
	}
	
	public void render(int mouseX, int mouseY) {
		
		this.updateHovered(mouseX, mouseY);
		
		this.dropdown.height = this.height;
		this.dropdown.width = this.width;
		this.dropdown.xPosition = this.x;
		this.dropdown.yPosition = this.y;
		
		this.dropdown.drawButton(Minecraft.getMinecraft(), mouseX, mouseY);
		
		int stackedHeight = this.height + this.space;
		if (this.opened) {
			for (AdvancedButton b : this.content) {
				b.setHandleClick(true);
				b.width = this.width;
				b.xPosition = this.x;
				b.yPosition = this.y + stackedHeight;
				b.drawButton(Minecraft.getMinecraft(), mouseX, mouseY);
				
				stackedHeight += b.height + this.space;
			}
		}
		
		if (this.autoclose && !this.isHovered() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
			this.opened = false;
		}
	}
	
	private void updateHovered(int mouseX, int mouseY) {
		if ((mouseX >= this.dropdown.xPosition) && (mouseX <= this.dropdown.xPosition + this.dropdown.width) && (mouseY >= this.dropdown.yPosition) && mouseY <= this.dropdown.yPosition + this.dropdown.height) {
			this.hovered = true;
			return;
		}
		for (AdvancedButton b : this.content) {
			if ((mouseX >= b.xPosition) && (mouseX <= b.xPosition + b.width) && (mouseY >= b.yPosition) && mouseY <= b.yPosition + b.height) {
				this.hovered = true;
				return;
			}
		}
		this.hovered = false;
	}
	
	public boolean isHovered() {
		if (!this.isOpen()) {
			return false;
		}
		return this.hovered;
	}
	
	public void setUseable(boolean b) {
		this.dropdown.setUseable(b);
		for (AdvancedButton bt : this.content) {
			bt.setUseable(b);
		}
		if (!b) {
			this.opened = false;
		}
	}
	
	public boolean isUseable() {
		if (this.dropdown == null) {
			return false;
		}
		return this.dropdown.isUseable();
	}
	
	public void setAutoclose(boolean b) {
		this.autoclose = b;
	}
	
	public boolean isOpen() {
		return this.opened;
	}
	
	public void openMenu() {
		this.opened = true;
	}
	
	public void closeMenu() {
		this.opened = false;
	}
	
	private void toggleMenu() {
		if (this.opened) {
			this.opened = false;
		} else {
			this.opened = true;
		}
	}
	
	public void addContent(AdvancedButton button) {
		this.content.add(button);
	}
	
	public void setLabel(String text) {
		this.dropdown.displayString = text;
	}
	
	public AdvancedButton getDropdownParent() {
		return this.dropdown;
	}

}
