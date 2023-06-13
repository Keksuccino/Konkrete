package de.keksuccino.konkrete.gui.content.scrollarea;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.resources.ResourceLocation;

public class ScrollArea {
	
	public Color backgroundColor = new Color(0, 0, 0, 240);
	public Color grabberColorNormal = Color.LIGHT_GRAY;
	public Color grabberColorHover = Color.GRAY;
	public ResourceLocation grabberTextureNormal = null;
	public ResourceLocation grabberTextureHover = null;
	public int x;
	public int y;
	public int width;
	public int height;
	public int grabberheight = 20;
	public int grabberwidth = 10;
	public boolean enableScrolling = true;
	private List<ScrollAreaEntry> entries = new ArrayList<ScrollAreaEntry>();
	
	private boolean grabberHovered = false;
	private boolean grabberPressed = false;
	
	private int scrollpos = 0;
	private int entryheight = 0;
	
	private int startY = 0;
	private int startPos = 0;
	
	public ScrollArea(int x, int y, int width, int height) {
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		
		Konkrete.getEventHandler().registerEventsFrom(this);
	}
	
	public void render(GuiGraphics graphics) {
		
		RenderSystem.enableBlend();

		this.renderBackground(graphics);
		
		this.renderScrollbar(graphics);

		Window win = Minecraft.getInstance().getWindow();
		double scale = win.getGuiScale();
		int sciBottom = this.height + this.y;
		RenderSystem.enableScissor((int)(x * scale), (int)(win.getHeight() - (sciBottom * scale)), (int)(width * scale), (int)(height * scale));
		
		int i = 0;
		for (ScrollAreaEntry e : this.entries) {
			int i2 = (this.height - this.grabberheight);
			if (i2 == 0) {
				i2 = 1;
			}
			int scroll = this.scrollpos * (this.entryheight / i2);
			e.x = this.x;
			e.y = this.y + i - scroll;
			e.render(graphics);
			
			i += e.getHeight();
		}

		RenderSystem.disableScissor();
		
	}
	
	protected void renderScrollbar(GuiGraphics graphics) {
		if (this.height < this.entryheight) {
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();

			//update grabber hover state
			if (((this.x + this.width) <= mouseX) && ((this.x + this.width + grabberwidth) >= mouseX) && ((this.y + this.scrollpos) <= mouseY) && ((this.y + this.scrollpos + grabberheight) >= mouseY)) {
				this.grabberHovered = true;
			} else {
				this.grabberHovered = false;
			}
			
			//Update grabber pressed state
			if (this.isGrabberHovered() && MouseInput.isLeftMouseDown()) {
				this.grabberPressed = true;
			}
			if (!MouseInput.isLeftMouseDown()) {
				this.grabberPressed = false;
			}
					
			//Render scroll grabber
			if (this.enableScrolling) {
				RenderSystem.enableBlend();
				int scrollXStart = this.x + this.width;
				int scrollYStart = this.y + this.scrollpos;
				int scrollXEnd = this.x + this.width + grabberwidth;
				int scrollYEnd = this.y + this.scrollpos + grabberheight;
				if (!this.isGrabberHovered()) {
					if (this.grabberTextureNormal == null) {
						graphics.fill(scrollXStart, scrollYStart, scrollXEnd, scrollYEnd, this.grabberColorNormal.getRGB());
					} else {
//						RenderUtils.bindTexture(this.grabberTextureNormal);
						graphics.blit(this.grabberTextureNormal, scrollXStart, scrollYStart, 0.0F, 0.0F, grabberwidth, grabberheight, grabberwidth, grabberheight);
					}
				} else {
					if (this.grabberTextureHover == null) {
						graphics.fill(scrollXStart, scrollYStart, scrollXEnd, scrollYEnd, this.grabberColorHover.getRGB());
					} else {
//						RenderUtils.bindTexture(this.grabberTextureHover);
						graphics.blit(this.grabberTextureHover, scrollXStart, scrollYStart, 0.0F, 0.0F, grabberwidth, grabberheight, grabberwidth, grabberheight);
					}
				}
			}
			
			//Handle scroll
			if (this.enableScrolling) {
				if (this.isGrabberPressed()) {
					this.handleGrabberScrolling();
				} else {
					this.startY = MouseInput.getMouseY();
					this.startPos = this.scrollpos;
				}
			} else {
				this.scrollpos = 0;
			}

		}
	}
	
	public boolean isAreaHovered() {
		int mouseX = MouseInput.getMouseX();
		int mouseY = MouseInput.getMouseY();
		if ((this.x <= mouseX) && ((this.x + this.width + this.grabberwidth) >= mouseX) && (this.y <= mouseY) && ((this.y + this.height) >= mouseY)) {
			return true;
		}
		return false;
	}
	
	protected void handleGrabberScrolling() {
		int i = this.startY - MouseInput.getMouseY();
		int scroll = this.startPos - i;

		if (scroll < 0) {
			this.scrollpos = 0;
		} else if (scroll > this.height - this.grabberheight) {
			this.scrollpos = this.height - this.grabberheight;
		} else {
			this.scrollpos = scroll;
		}
	}
	
	protected void renderBackground(GuiGraphics graphics) {
		graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, this.backgroundColor.getRGB());
	}
	
	public void addEntry(ScrollAreaEntry e) {
		this.entries.add(e);
		this.scrollpos = 0;
		this.entryheight += e.getHeight();
	}
	
	public void removeEntry(ScrollAreaEntry e) {
		if (this.entries.contains(e)) {
			this.entries.remove(e);
			this.scrollpos = 0;
			this.entryheight -= e.getHeight();
		}
	}
	
	public List<ScrollAreaEntry> getEntries() {
		return this.entries;
	}
	
	public int getStackedEntryHeight() {
		return this.entryheight;
	}
	
	public boolean isGrabberHovered() {
		return this.grabberHovered;
	}
	
	public boolean isGrabberPressed() {
		return this.grabberPressed;
	}
	
	@SubscribeEvent
	public void onMouseScrollPre(GuiScreenEvent.MouseScrollEvent.Pre e) {
		if (this.isAreaHovered()) {
			int scroll = this.scrollpos - (int) e.getScrollDelta() * 7;
			if (scroll < 0) {
				this.scrollpos = 0;
			} else if (scroll > this.height - this.grabberheight) {
				this.scrollpos = this.height - this.grabberheight;
			} else {
				this.scrollpos = scroll;
			}
		}
	}

}
