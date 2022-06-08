package de.keksuccino.konkrete.gui.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.input.MouseInput;

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
	
	public void render(PoseStack matrix, int mouseX, int mouseY) {
		float ticks = Minecraft.getInstance().getFrameTime();
		
		this.updateHovered(mouseX, mouseY);
		
		this.dropdown.setHeight(this.height);;
		this.dropdown.setWidth(this.width);
		this.dropdown.setX(this.x);
		this.dropdown.setY(this.y);
		
		this.dropdown.render(matrix, mouseX, mouseY, ticks);
		
		int stackedHeight = this.height + this.space;
		if (this.opened) {
			for (AdvancedButton b : this.content) {
				b.setHandleClick(true);
				b.setWidth(this.width);
				b.setX(this.x);
				b.setY(this.y + stackedHeight);
				b.render(matrix, mouseX, mouseY, ticks);
				
				stackedHeight += b.getHeight() + this.space;
			}
		}
		
		if (this.autoclose && !this.isHovered() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
			this.opened = false;
		}
	}
	
	private void updateHovered(int mouseX, int mouseY) {
		if ((mouseX >= this.dropdown.getX()) && (mouseX <= this.dropdown.getX() + this.dropdown.getHeight()) && (mouseY >= this.dropdown.getY()) && mouseY <= this.dropdown.getY() + this.dropdown.getHeight()) {
			this.hovered = true;
			return;
		}
		for (AdvancedButton b : this.content) {
			if ((mouseX >= b.getX()) && (mouseX <= b.getX() + b.getWidth()) && (mouseY >= b.getY()) && mouseY <= b.getY() + b.getHeight()) {
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
		this.dropdown.setMessage(text);
	}
	
	public AdvancedButton getDropdownParent() {
		return this.dropdown;
	}

}
