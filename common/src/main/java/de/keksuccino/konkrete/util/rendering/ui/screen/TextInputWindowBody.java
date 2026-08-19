package de.keksuccino.konkrete.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import de.keksuccino.konkrete.util.input.InputConstants;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

/** Implements the interactive window body for text input. */
public class TextInputWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    /** Width in GUI units for PiP window. */
    public static final int PIP_WINDOW_WIDTH = 331;
    /** Height in GUI units for PiP window. */
    public static final int PIP_WINDOW_HEIGHT = 133;

    /** Callback receiving validated text when the dialog completes. */
    @NotNull
    protected Consumer<String> callback;
    /** Edit box containing the current value. */
    protected ExtendedEditBox input;
    /** Whole-value validator used to enable or reject completion. */
    protected ConsumingSupplier<TextInputWindowBody, Boolean> textValidator = null;
    /** Tooltip shown for text validator feedback. */
    protected UITooltip textValidatorFeedbackUITooltip = null;
    /** Character-level acceptance filter for the input. */
    @Nullable
    protected CharacterFilter filter;

    @Nullable
    private String cachedValue;

    /** Initializes a filtered single-line input and its completion callback. */
    public TextInputWindowBody(@Nullable CharacterFilter filter, @NotNull Consumer<String> callback) {
        super(Component.empty());
        this.callback = callback;
        this.filter = filter;
    }

    /** Initializes resources required by this text input window body. */
    @Override
    protected void init() {

        String val = "";
        if (this.input != null) {
            val = this.input.getValue();
        } else if (this.cachedValue != null) {
            val = this.cachedValue;
            this.cachedValue = null;
        }
        int inputHeight = 20;
        int inputWidth = Math.max(160, this.width - 80);
        int inputX = (this.width - inputWidth) / 2;
        int buttonY = this.height - 40;
        int contentBottom = buttonY - 12;
        int inputY = Math.max(16, (contentBottom - inputHeight) / 2);

        this.input = this.addRenderableWidget(new ExtendedEditBox(Minecraft.getInstance().font, inputX, inputY, inputWidth, inputHeight, Component.empty()));
        this.input.setMaxLength(10000);
        this.input.setCharacterFilter(this.filter);
        this.input.setValue(val);
        UIBase.applyDefaultWidgetSkinTo(this.input, UIBase.shouldBlur());
        this.setupInitialFocusWidget(this, this.input);

        ExtendedButton cancelButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) - 5 - 100, buttonY, 100, 20, Component.translatable("konkrete.common_components.cancel"), button -> {
            this.callback.accept(null);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(cancelButton, UIBase.shouldBlur());

        ExtendedButton doneButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) + 5, buttonY, 100, 20, Component.translatable("konkrete.common_components.done"), button -> {
            if (this.isTextValid()) {
                this.callback.accept(this.input.getValue());
                this.closeWindow();
            }
        })).setIsActiveSupplier(consumes -> this.isTextValid())
                .setUITooltip(this.textValidatorFeedbackUITooltip);
        UIBase.applyDefaultWidgetSkinTo(doneButton, UIBase.shouldBlur());

    }

    /** Handles window closed externally for this text input window body. */
    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();

    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == InputConstants.KEY_ENTER) && this.isTextValid() && (this.input != null) && this.input.isFocused()) {
            this.callback.accept(this.input.getValue());
            this.closeWindow();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Sets text for this text input window body. */
    public TextInputWindowBody setText(@Nullable String text) {
        if (text == null) text = "";
        if (this.input != null) {
            this.input.setValue(text);
        } else {
            this.cachedValue = text;
        }
        return this;
    }

    /** Returns text. */
    public String getText() {
        return this.input.getValue();
    }

    /** Reports whether all configured validators accept the current text. */
    protected boolean isTextValid() {
        if (this.textValidator != null) return this.textValidator.get(this);
        return true;
    }

    /** Sets text validator for this text input window body. */
    public TextInputWindowBody setTextValidator(@Nullable ConsumingSupplier<TextInputWindowBody, Boolean> textValidator) {
        this.textValidator = textValidator;
        return this;
    }

    /** Sets text validator user feedback for this text input window body. */
    public TextInputWindowBody setTextValidatorUserFeedback(@Nullable UITooltip feedback) {
        this.textValidatorFeedbackUITooltip = feedback;
        return this;
    }

}
