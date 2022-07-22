package de.keksuccino.konkrete.gui.screens.popup;

import java.awt.Color;
import java.util.function.Consumer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class TextInputPopup extends Popup {
	
	protected Consumer<String> callback;
	protected String input = null;
	protected AdvancedTextField textField;
	protected AdvancedButton doneButton;
	protected Color color;
	protected int width = 250;
	protected String title = "";
	
	public TextInputPopup(Color color, String title, CharacterFilter filter, int alpha) {
		super(alpha);
		this.init(color, title, filter, null);
	}
	
	public TextInputPopup(Color color, String title, CharacterFilter filter, int backgroundAlpha, Consumer<String> callback) {
		super(backgroundAlpha);
		this.init(color, title, filter, callback);
	}
	
	protected void init(Color color, String title, CharacterFilter filter, Consumer<String> callback) {
		this.textField = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 200, 20, true, filter);
		this.textField.setCanLoseFocus(true);
		this.textField.setFocus(false);
		this.textField.setMaxLength(1000);
		
		this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
			this.input = this.textField.getValue();
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(this.input);
			}
		});
		this.addButton(this.doneButton);
		
		if (title != null) {
			this.title = title;
		}
		
		this.color = color;
		this.callback = callback;
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			int height = 100;
			
			RenderSystem.enableBlend();
			IngameGui.fill(matrix, (renderIn.width / 2) - (this.width / 2), (renderIn.height / 2) - (height / 2), (renderIn.width / 2) + (this.width / 2), (renderIn.height / 2) + (height / 2), this.color.getRGB());
			RenderSystem.disableBlend();
			
			AbstractGui.drawCenteredString(matrix, Minecraft.getInstance().font, new StringTextComponent(title), renderIn.width / 2, (renderIn.height / 2) - (height / 2) + 10, Color.WHITE.getRGB());
			
			this.textField.setX((renderIn.width / 2) - (this.textField.getWidth() / 2));
			this.textField.setY((renderIn.height / 2) - (this.textField.getHeight() / 2));
			this.textField.renderButton(matrix, mouseX, mouseY, Minecraft.getInstance().getFrameTime());
			
			this.doneButton.setX((renderIn.width / 2) - (this.doneButton.getWidth() / 2));
			this.doneButton.setY(((renderIn.height / 2) + (height / 2)) - this.doneButton.getHeight() - 5);
			
			this.renderButtons(matrix, mouseX, mouseY);
		}
	}
	
	public void setText(String text) {
		this.textField.setValue("");
		this.textField.insertText(text);
	}
	
	public String getInput() {
		return this.input;
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
			this.input = this.textField.getValue();
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(this.input);
			}
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(null);
			}
		}
	}

}
