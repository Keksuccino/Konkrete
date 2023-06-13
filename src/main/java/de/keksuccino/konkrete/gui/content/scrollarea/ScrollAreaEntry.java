package de.keksuccino.konkrete.gui.content.scrollarea;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.input.MouseInput;


public abstract class ScrollAreaEntry {
	
	public int x = 0;
	public int y = 0;
	public final ScrollArea parent;
	
	public ScrollAreaEntry(ScrollArea parent) {
		this.parent = parent;
	}
	
	public abstract void renderEntry(GuiGraphics graphics);
	
	public void render(GuiGraphics graphics) {
		if (this.isVisible()) {
			this.renderEntry(graphics);
		}
	}
	
	public abstract int getHeight();
	
	public int getWidth() {
		return this.parent.width;
	}

	public boolean isHovered() {
		int mx = MouseInput.getMouseX();
		int my = MouseInput.getMouseY();
		if ((this.x <= mx) && (this.y <= my) && ((this.x + this.parent.width) >= mx) && ((this.y + this.getHeight()) >= my)) {
			return true;
		}
		return false;
	}
	
	public boolean isVisible() {
		if ((this.parent.y >= this.y + this.getHeight()) || (this.parent.y + this.parent.height <= this.y)) {
			return false;
		}
		return true;
	}
}
