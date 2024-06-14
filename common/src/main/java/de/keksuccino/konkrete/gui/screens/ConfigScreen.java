package de.keksuccino.konkrete.gui.screens;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.config.Config;
import de.keksuccino.konkrete.config.ConfigEntry;
import de.keksuccino.konkrete.config.ConfigEntry.EntryType;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollAreaEntry;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.network.chat.Component;

@SuppressWarnings("all")
@Deprecated(forRemoval = true)
public class ConfigScreen extends Screen {

	protected Config config;
	protected ScrollArea configList;
	protected Screen parent;
	protected String title;
	protected AdvancedButton doneBtn;
	protected String activeDescription = null;
	
	protected Map<String, String> descriptions = new HashMap<String, String>();
	
	protected static final Color ENTRY_BACKGROUND_COLOR = new Color(92, 92, 92);
	protected static final Color SCREEN_BACKGROUND_COLOR = new Color(54, 54, 54);
	protected static final Color HEADER_FOOTER_COLOR = new Color(33, 33, 33);
	
	public ConfigScreen(Config config, String title, Screen parent) {
		
		super(Component.literal(""));
		this.config = config;
		this.parent = parent;
		this.title = title;
		
		this.configList = new ScrollArea(0, 50, 300, 0);
		this.configList.backgroundColor = ENTRY_BACKGROUND_COLOR;
		
		for (String s : this.config.getCategories()) {
			
			this.configList.addEntry(new CategoryConfigScrollAreaEntry(this.configList, s));
			
			for (ConfigEntry e : this.config.getEntriesForCategory(s)) {
				
				if (e.getType() == EntryType.STRING) {
					this.configList.addEntry(new StringConfigScrollAreaEntry(this.configList, e));
				}
				
				if (e.getType() == EntryType.INTEGER) {
					this.configList.addEntry(new IntegerConfigScrollAreaEntry(this.configList, e));
				}
				
				if (e.getType() == EntryType.DOUBLE) {
					this.configList.addEntry(new DoubleConfigScrollAreaEntry(this.configList, e));
				}
				
				if (e.getType() == EntryType.FLOAT) {
					this.configList.addEntry(new FloatConfigScrollAreaEntry(this.configList, e));
				}
				
				if (e.getType() == EntryType.LONG) {
					this.configList.addEntry(new LongConfigScrollAreaEntry(this.configList, e));
				}
				
				if (e.getType() == EntryType.BOOLEAN) {
					this.configList.addEntry(new BooleanConfigScrollAreaEntry(this.configList, e));
				}
				
			}
			
		}

		this.doneBtn = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
			Minecraft.getInstance().setScreen(this.parent);
		});
		colorizeButton(this.doneBtn);
		this.doneBtn.ignoreBlockedInput = true;
		this.doneBtn.ignoreLeftMouseDownClickBlock = true;
		
	}
	
	@Override
	protected void init() {
		
		this.configList.x = (this.width / 2) - 150;
		this.configList.height = this.height - 100;
		
	}
	
	@Override
	public void removed() {
		this.saveConfig();
	}
	
	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.parent);
	}
	
	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}
	
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		
		RenderSystem.enableBlend();
		
		//Draw screen background
		graphics.fill(0, 0, this.width, this.height, SCREEN_BACKGROUND_COLOR.getRGB());
		
		this.configList.render(graphics);
		
		//Draw header
		graphics.fill(0, 0, this.width, 50, HEADER_FOOTER_COLOR.getRGB());
		
		//Draw title
		if (this.title != null) {
			graphics.drawString(font, this.title, (this.width / 2) - (font.width(this.title) / 2), 20, Color.WHITE.getRGB());
		}
		
		//Draw footer
		graphics.fill(0, this.height - 50, this.width, this.height, HEADER_FOOTER_COLOR.getRGB());
		
		this.doneBtn.setX((this.width / 2) - (this.doneBtn.getWidth() / 2));
		this.doneBtn.setY(this.height - 35);
		this.doneBtn.render(graphics, mouseX, mouseY, partialTicks);
		
		super.render(graphics, mouseX, mouseY, partialTicks);
		
		for (ScrollAreaEntry e : this.configList.getEntries()) {
			if (e instanceof ConfigScrollAreaEntry) {
				if (e.isHovered()) {
					String name = ((ConfigScrollAreaEntry) e).configEntry.getName();
					if (this.descriptions.containsKey(name)) {
						if (!ConfigScrollAreaEntry.isHeaderFooterHovered()) {
							renderDescription(graphics, this.descriptions.get(name), mouseX, mouseY);
						}
						break;
					}
				}
			}
		}
		
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
	}

	public void setValueDescription(String valueName, String desc) {
		this.descriptions.put(valueName, desc);
	}
	
	public void setCategoryDisplayName(String categoryName, String displayName) {
		CategoryConfigScrollAreaEntry e = this.getCategoryEntryByName(categoryName);
		if (e != null) {
			e.displayName = displayName;
		}
	}
	
	public void setValueDisplayName(String valueName, String displayName) {
		ConfigScrollAreaEntry e = this.getEntryByValueName(valueName);
		if (e != null) {
			e.displayName = displayName;
		}
	}
	
	protected ConfigScrollAreaEntry getEntryByValueName(String valueName) {
		for (ScrollAreaEntry e : this.configList.getEntries()) {
			if (e instanceof ConfigScrollAreaEntry) {
				if (((ConfigScrollAreaEntry) e).configEntry.getName().equals(valueName)) {
					return (ConfigScrollAreaEntry) e;
				}
			}
		}
		return null;
	}
	
	protected CategoryConfigScrollAreaEntry getCategoryEntryByName(String categoryName) {
		for (ScrollAreaEntry e : this.configList.getEntries()) {
			if (e instanceof CategoryConfigScrollAreaEntry) {
				if (((CategoryConfigScrollAreaEntry) e).category.equals(categoryName)) {
					return (CategoryConfigScrollAreaEntry) e;
				}
			}
		}
		return null;
	}
	
	protected void saveConfig() {
		for (ScrollAreaEntry e : this.configList.getEntries()) {
			if (e instanceof ConfigScrollAreaEntry) {
				((ConfigScrollAreaEntry) e).onSave();
			}
		}
		this.config.syncConfig();
	}
	
	protected static void renderDescription(GuiGraphics graphics, String description, int mouseX, int mouseY) {
		if (description != null) {
				int width = 10;
				int height = 10;
				String[] desc = StringUtils.splitLines(description, "%n%");
				
				//Getting the longest string from the list to render the background with the correct width
				for (String s : desc) {
					int i = Minecraft.getInstance().font.width(s) + 10;
					if (i > width) {
						width = i;
					}
					height += 10;
				}

				mouseX += 5;
				mouseY += 5;
				
				if (Minecraft.getInstance().screen.width < mouseX + width) {
					mouseX -= width + 10;
				}
				
				if (Minecraft.getInstance().screen.height < mouseY + height) {
					mouseY -= height + 10;
				}

				RenderUtils.setZLevelPre(graphics, 600);
				
				renderDescriptionBackground(graphics, mouseX, mouseY, width, height);

				RenderSystem.enableBlend();

				int i2 = 5;
				for (String s : desc) {
					graphics.drawString(Minecraft.getInstance().font, s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
					i2 += 10;
				}

				RenderUtils.setZLevelPost(graphics);
				
				RenderSystem.disableBlend();
		}
	}
	
	protected static void renderDescriptionBackground(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
	}
	
	protected static void colorizeButton(AdvancedButton b) {
		b.setBackgroundColor(new Color(100, 100, 100), new Color(130, 130, 130), new Color(180, 180, 180), new Color(199, 199, 199), 1);
	}
	
	protected static abstract class ConfigScrollAreaEntry extends ScrollAreaEntry {

		protected ConfigEntry configEntry;
		protected Font font = Minecraft.getInstance().font;
		protected String displayName;
		
		public ConfigScrollAreaEntry(ScrollArea parent, ConfigEntry configEntry) {
			super(parent);
			this.configEntry = configEntry;
		}
		
		@Override
		public void renderEntry(GuiGraphics graphics) {
			
			int center = this.x + (this.getWidth() / 2);
			
			graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), ENTRY_BACKGROUND_COLOR.getRGB());
			
			//Render config entry name
			if (this.displayName != null) {
				int nameWidth = font.width(this.displayName);
				graphics.drawString(font, this.displayName, center - nameWidth - 10 , this.y + 10, Color.WHITE.getRGB());
			} else {
				int nameWidth = font.width(this.configEntry.getName());
				graphics.drawString(font, this.configEntry.getName(), center - nameWidth - 10 , this.y + 10, Color.WHITE.getRGB());
			}
			
			
		}

		@Override
		public int getHeight() {
			return 26;
		}
		
		protected abstract void onSave();

		public static boolean isHeaderFooterHovered() {
			Screen s = Minecraft.getInstance().screen;
			if (s != null) {
				int mouseX = MouseInput.getMouseX();
				int mouseY = MouseInput.getMouseY();
				int minXHeaderFooter = 0;
				int maxXHeaderFooter = s.width;
				int minYHeader = 0;
				int maxYHeader = 50;
				int minYFooter = s.height - 50;
				int maxYFooter = s.height;
				//HEADER
				if ((mouseX >= minXHeaderFooter) && (mouseX <= maxXHeaderFooter) && (mouseY >= minYHeader) && (mouseY <= maxYHeader)) {
					return true;
				}
				//FOOTER
				if ((mouseX >= minXHeaderFooter) && (mouseX <= maxXHeaderFooter) && (mouseY >= minYFooter) && (mouseY <= maxYFooter)) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	protected static class StringConfigScrollAreaEntry extends ConfigScrollAreaEntry {

		private AdvancedTextField input;
		
		public StringConfigScrollAreaEntry(ScrollArea parent, ConfigEntry configEntry) {
			
			super(parent, configEntry);
			
			input = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 100, 20, true, null);
			input.setMaxLength(10000);
			input.setValue(configEntry.getValue());
			
		}
		
		@Override
		public void render(GuiGraphics graphics) {
			super.render(graphics);
			
			int center = this.x + (this.getWidth() / 2);
			
			input.setX(center + 10);
			input.setY(this.y + 3);
			input.render(graphics, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
			
		}
		
		@Override
		protected void onSave() {
			
			configEntry.setValue(input.getValue());
			
		}
		
	}
	
	protected static class IntegerConfigScrollAreaEntry extends ConfigScrollAreaEntry {

		private AdvancedTextField input;
		
		public IntegerConfigScrollAreaEntry(ScrollArea parent, ConfigEntry configEntry) {
			
			super(parent, configEntry);
			
			input = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 100, 20, true, CharacterFilter.getIntegerCharacterFiler());
			input.setMaxLength(10000);
			input.setValue(configEntry.getValue());
			
		}
		
		@Override
		public void render(GuiGraphics graphics) {
			super.render(graphics);
			
			int center = this.x + (this.getWidth() / 2);
			
			input.setX(center + 10);
			input.setY(this.y + 3);
			input.render(graphics, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
			
		}
		
		@Override
		protected void onSave() {
			
			if (MathUtils.isInteger(this.input.getValue())) {
				
				configEntry.setValue(input.getValue());
				
			} else {
				
				System.out.println("################ ERROR [KONKRETE] ################");
				System.out.println("Unable to save value to config! Invalid value type!");
				System.out.println("Value: " + this.input.getValue());
				System.out.println("Variable Type: INTEGER");
				System.out.println("##################################################");
				
			}
			
		}
		
	}
	
	protected static class DoubleConfigScrollAreaEntry extends ConfigScrollAreaEntry {

		private AdvancedTextField input;
		
		public DoubleConfigScrollAreaEntry(ScrollArea parent, ConfigEntry configEntry) {
			
			super(parent, configEntry);
			
			input = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 100, 20, true, CharacterFilter.getDoubleCharacterFiler());
			input.setMaxLength(10000);
			input.setValue(configEntry.getValue());
			
		}
		
		@Override
		public void render(GuiGraphics graphics) {
			super.render(graphics);
			
			int center = this.x + (this.getWidth() / 2);
			
			input.setX(center + 10);
			input.setY(this.y + 3);
			input.render(graphics, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
			
		}
		
		@Override
		protected void onSave() {
			
			if (MathUtils.isDouble(this.input.getValue())) {
				
				configEntry.setValue(input.getValue());
				
			} else {
				
				System.out.println("################ ERROR [KONKRETE] ################");
				System.out.println("Unable to save value to config! Invalid value type!");
				System.out.println("Value: " + this.input.getValue());
				System.out.println("Variable Type: DOUBLE");
				System.out.println("##################################################");
				
			}
			
		}
		
	}
	
	protected static class LongConfigScrollAreaEntry extends ConfigScrollAreaEntry {

		private AdvancedTextField input;
		
		public LongConfigScrollAreaEntry(ScrollArea parent, ConfigEntry configEntry) {
			
			super(parent, configEntry);
			
			input = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 100, 20, true, CharacterFilter.getIntegerCharacterFiler());
			input.setMaxLength(10000);
			input.setValue(configEntry.getValue());
			
		}
		
		@Override
		public void render(GuiGraphics graphics) {
			super.render(graphics);
			
			int center = this.x + (this.getWidth() / 2);
			
			input.setX(center + 10);
			input.setY(this.y + 3);
			input.render(graphics, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
			
		}
		
		@Override
		protected void onSave() {
			
			if (MathUtils.isLong(this.input.getValue())) {
				
				configEntry.setValue(input.getValue());
				
			} else {
				
				System.out.println("################ ERROR [KONKRETE] ################");
				System.out.println("Unable to save value to config! Invalid value type!");
				System.out.println("Value: " + this.input.getValue());
				System.out.println("Variable Type: LONG");
				System.out.println("##################################################");
				
			}
			
		}
		
	}
	
	protected static class FloatConfigScrollAreaEntry extends ConfigScrollAreaEntry {

		private AdvancedTextField input;
		
		public FloatConfigScrollAreaEntry(ScrollArea parent, ConfigEntry configEntry) {
			
			super(parent, configEntry);
			
			input = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 100, 20, true, CharacterFilter.getDoubleCharacterFiler());
			input.setMaxLength(10000);
			input.setValue(configEntry.getValue());
			
		}
		
		@Override
		public void render(GuiGraphics graphics) {
			super.render(graphics);
			
			int center = this.x + (this.getWidth() / 2);
			
			input.setX(center + 10);
			input.setY(this.y + 3);
			input.render(graphics, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
			
		}
		
		@Override
		protected void onSave() {
			
			if (MathUtils.isFloat(this.input.getValue())) {
				
				configEntry.setValue(input.getValue());
				
			} else {
				
				System.out.println("################ ERROR [KONKRETE] ################");
				System.out.println("Unable to save value to config! Invalid value type!");
				System.out.println("Value: " + this.input.getValue());
				System.out.println("Variable Type: FLOAT");
				System.out.println("##################################################");
				
			}
			
		}
		
	}
	
	protected static class BooleanConfigScrollAreaEntry extends ConfigScrollAreaEntry {

		private AdvancedButton toggleBtn;
		private boolean state = false;
		
		public BooleanConfigScrollAreaEntry(ScrollArea parent, ConfigEntry configEntry) {
			
			super(parent, configEntry);
			
			if (configEntry.getValue().equalsIgnoreCase("true")) {
				state = true;
			}
			
			toggleBtn = new AdvancedButton(0, 0, 102, 20, "", true, (press) -> {
				if (!isHeaderFooterHovered()) {
					state = !state;
					if (state) {
						toggleBtn.setMessage(Component.literal("§a" + Locals.localize("configscreen.boolean.enabled")));
					} else {
						toggleBtn.setMessage(Component.literal("§c" + Locals.localize("configscreen.boolean.disabled")));
					}
				}
			});
			if (state) {
				toggleBtn.setMessage(Component.literal("§a" + Locals.localize("configscreen.boolean.enabled")));
			} else {
				toggleBtn.setMessage(Component.literal("§c" + Locals.localize("configscreen.boolean.disabled")));
			}
			
			colorizeButton(this.toggleBtn);
			
		}
		
		@Override
		public void render(GuiGraphics graphics) {
			super.render(graphics);
			
			int center = this.x + (this.getWidth() / 2);
			
			toggleBtn.setX(center + 9);
			toggleBtn.setY(this.y + 3);
			toggleBtn.render(graphics, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
			
		}
		
		@Override
		protected void onSave() {
			
			configEntry.setValue("" + this.state);
			
		}
		
	}
	
	protected static class CategoryConfigScrollAreaEntry extends ScrollAreaEntry {

		protected String category;
		protected Font textRenderer = Minecraft.getInstance().font;
		protected String displayName;
		
		public CategoryConfigScrollAreaEntry(ScrollArea parent, String category) {
			super(parent);
			this.category = category;
		}
		
		@Override
		public void renderEntry(GuiGraphics graphics) {
			
			int center = this.x + (this.getWidth() / 2);
			
			graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), ENTRY_BACKGROUND_COLOR.getRGB());
			
			//Render category title
			if (this.displayName != null) {
				int nameWidth = textRenderer.width(this.displayName);
				graphics.drawString(textRenderer, this.displayName, center - (nameWidth / 2) , this.y + 10, Color.WHITE.getRGB());
			} else {
				int nameWidth = textRenderer.width(this.category);
				graphics.drawString(textRenderer, this.category, center - (nameWidth / 2) , this.y + 10, Color.WHITE.getRGB());
			}

		}

		@Override
		public int getHeight() {
			return 30;
		}
		
	}

}
