package de.keksuccino.konkrete.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.Pair;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import de.keksuccino.konkrete.util.input.InputConstants;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.widget.TextWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

/** Implements the interactive window body for dual text input. */
public class DualTextInputWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    /** Width in GUI units for PiP window. */
    public static final int PIP_WINDOW_WIDTH = 420;
    /** Height in GUI units for PiP window. */
    public static final int PIP_WINDOW_HEIGHT = 220;

    /** Callback receiving both validated input values. */
    @NotNull
    protected Consumer<Pair<String, String>> callback;
    /** First editable value. */
    protected ExtendedEditBox input_one;
    /** Second editable value. */
    protected ExtendedEditBox input_two;
    /** Button that cancels the current operation. */
    protected ExtendedButton cancelButton;
    /** Button that accepts both values after validation. */
    protected ExtendedButton doneButton;
    /** Combined-input validator used to enable or reject completion. */
    protected ConsumingSupplier<DualTextInputWindowBody, Boolean> textValidator = null;
    /** Tooltip shown for text validator feedback. */
    protected UITooltip textValidatorFeedbackUITooltip = null;
    /** Label preceding the first input. */
    @NotNull
    protected MutableComponent firstInputLabel;
    /** Label preceding the second input. */
    @NotNull
    protected MutableComponent secondInputLabel;
    /** Character-level acceptance filter shared by both inputs. */
    @Nullable
    protected CharacterFilter filter;
    /** Optional initial contents of the first input. */
    @Nullable
    protected String initialValueOne = null;
    /** Optional initial contents of the second input. */
    @Nullable
    protected String initialValueTwo = null;
    /** Whether placeholder insertion is allowed. */
    protected boolean allowPlaceholders = true;

    /** Creates a two-field input dialog with labels, filtering, and completion callback. */
    @NotNull
    public static DualTextInputWindowBody build(@NotNull Component title, @NotNull Component firstInputLabel, @NotNull Component secondInputLabel, @Nullable CharacterFilter filter, @NotNull Consumer<Pair<String, String>> callback) {
        return new DualTextInputWindowBody(title, firstInputLabel, secondInputLabel, filter, callback);
    }

    /** Initializes two labeled, optionally filtered inputs and their combined completion callback. */
    public DualTextInputWindowBody(@NotNull Component title, @NotNull Component firstInputLabel, @NotNull Component secondInputLabel, @Nullable CharacterFilter filter, @NotNull Consumer<Pair<String, String>> callback) {
        super(Component.empty());
        this.callback = callback;
        this.firstInputLabel = (firstInputLabel instanceof MutableComponent l) ? l : Component.empty();
        this.secondInputLabel = (secondInputLabel instanceof MutableComponent l) ? l : Component.empty();
        this.filter = filter;
    }

    /** Initializes resources required by this dual text input window body. */
    @Override
    protected void init() {

        int editorButtonWidth = 100;
        int editorButtonGap = 5;
        int inputHeight = 20;
        int labelGap = 6;
        int groupGap = 24;

        int totalWidth = Math.max(160, this.width - 80);
        int inputWidth = this.allowPlaceholders ? Math.max(120, totalWidth - editorButtonWidth - editorButtonGap) : totalWidth;
        int groupWidth = inputWidth + (this.allowPlaceholders ? (editorButtonWidth + editorButtonGap) : 0);
        int inputX = (this.width - groupWidth) / 2;
        int editorButtonX = inputX + inputWidth + editorButtonGap;

        int buttonY = this.height - 40;
        int contentHeight = (this.font.lineHeight + labelGap + inputHeight) * 2 + groupGap;
        int contentBottom = buttonY - 12;
        int top = Math.max(16, (contentBottom - contentHeight) / 2);

        int firstLabelY = top;
        int firstInputY = firstLabelY + this.font.lineHeight + labelGap;
        int secondLabelY = firstInputY + inputHeight + groupGap;
        int secondInputY = secondLabelY + this.font.lineHeight + labelGap;

        this.addRenderableWidget(new TextWidget(0, firstLabelY, this.width, 20, this.font, this.firstInputLabel)
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setBaseColor(UIBase.getUITheme().ui_interface_generic_text_color))
                .setShadowEnabled(false)
                .setForUI(true);

        String oldValueOne = "";
        if (this.input_one != null) {
            oldValueOne = this.input_one.getValue();
        } else if (this.initialValueOne != null) {
            oldValueOne = this.initialValueOne;
        }
        this.input_one = new ExtendedEditBox(this.font, inputX, firstInputY, inputWidth, inputHeight, Component.empty());
        this.input_one.setMaxLength(10000000);
        this.input_one.setCharacterFilter(this.filter);
        this.input_one.setValue(oldValueOne);
        UIBase.applyDefaultWidgetSkinTo(this.input_one, UIBase.shouldBlur());
        this.addRenderableWidget(this.input_one);
        this.setupInitialFocusWidget(this, this.input_one);

        if (this.allowPlaceholders) {
            UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(editorButtonX, this.input_one.getY(), editorButtonWidth, inputHeight, Component.translatable("konkrete.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                TextEditorWindowBody s = new TextEditorWindowBody(this.firstInputLabel, (this.filter != null) ? this.filter.convertToLegacyFilter() : null, callback -> {
                    if (callback != null) {
                        this.setFirstText(callback);
                    }
                });
                s.setText(this.getFirstText());
                Dialogs.openGeneric(s, this.firstInputLabel, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
            })), UIBase.shouldBlur());
        }

        this.addRenderableWidget(new TextWidget(0, secondLabelY, this.width, 20, this.font, this.secondInputLabel)
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setBaseColor(UIBase.getUITheme().ui_interface_generic_text_color))
                .setShadowEnabled(false)
                .setForUI(true);

        String oldValueTwo = "";
        if (this.input_two != null) {
            oldValueTwo = this.input_two.getValue();
        } else if (this.initialValueTwo != null) {
            oldValueTwo = this.initialValueTwo;
        }
        this.input_two = new ExtendedEditBox(Minecraft.getInstance().font, inputX, secondInputY, inputWidth, inputHeight, Component.empty());
        this.input_two.setMaxLength(10000000);
        this.input_two.setCharacterFilter(this.filter);
        this.input_two.setValue(oldValueTwo);
        UIBase.applyDefaultWidgetSkinTo(this.input_two, UIBase.shouldBlur());
        this.addRenderableWidget(this.input_two);

        if (this.allowPlaceholders) {
            UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(editorButtonX, this.input_two.getY(), editorButtonWidth, inputHeight, Component.translatable("konkrete.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                TextEditorWindowBody s = new TextEditorWindowBody(this.secondInputLabel, (this.filter != null) ? this.filter.convertToLegacyFilter() : null, callback -> {
                    if (callback != null) {
                        this.setSecondText(callback);
                    }
                });
                s.setText(this.getSecondText());
                Dialogs.openGeneric(s, this.secondInputLabel, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
            })), UIBase.shouldBlur());
        }

        this.cancelButton = new ExtendedButton((this.width / 2) - 5 - 100, buttonY, 100, 20, Component.translatable("konkrete.common_components.cancel"), (button) -> {
            this.callback.accept(null);
            this.closeWindow();
        });
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());
        this.addRenderableWidget(this.cancelButton);

        this.doneButton = new ExtendedButton((this.width / 2) + 5, buttonY, 100, 20, Component.translatable("konkrete.common_components.done"), (button) -> {
            if (this.isTextValid()) {
                this.callback.accept(Pair.of(this.getFirstText(), this.getSecondText()));
                this.closeWindow();
            }
        }).setIsActiveSupplier(consumes -> this.isTextValid())
                .setUITooltipSupplier(consumes -> {
                    return this.textValidatorFeedbackUITooltip;
                });
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());
        this.addRenderableWidget(this.doneButton);

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
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        if ((button == InputConstants.KEY_ENTER) && this.isTextValid() && ((this.input_one != null && this.input_one.isFocused()) || (this.input_two != null && this.input_two.isFocused()))) {
            this.callback.accept(Pair.of(this.getFirstText(), this.getSecondText()));
            this.closeWindow();
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    /** Handles window closed externally for this dual text input window body. */
    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    /** Sets first text for this dual text input window body. */
    public DualTextInputWindowBody setFirstText(@Nullable String text) {
        if (text == null) text = "";
        if (this.input_one != null) {
            this.input_one.setValue(text);
        } else {
            this.initialValueOne = text;
        }
        return this;
    }

    /** Sets second text for this dual text input window body. */
    public DualTextInputWindowBody setSecondText(@Nullable String text) {
        if (text == null) text = "";
        if (this.input_two != null) {
            this.input_two.setValue(text);
        } else {
            this.initialValueTwo = text;
        }
        return this;
    }

    /** Returns first text. */
    @NotNull
    public String getFirstText() {
        if (this.input_one != null) return this.input_one.getValue();
        if (this.initialValueOne != null) return this.initialValueOne;
        return "";
    }

    /** Returns second text. */
    @NotNull
    public String getSecondText() {
        if (this.input_two != null) return this.input_two.getValue();
        if (this.initialValueTwo != null) return this.initialValueTwo;
        return "";
    }

    /** Reports whether all configured validators accept the current text. */
    protected boolean isTextValid() {
        if (this.textValidator != null) return this.textValidator.get(this);
        return true;
    }

    /** Sets text validator for this dual text input window body. */
    public DualTextInputWindowBody setTextValidator(@Nullable ConsumingSupplier<DualTextInputWindowBody, Boolean> textValidator) {
        this.textValidator = textValidator;
        return this;
    }

    /** Sets text validator user feedback for this dual text input window body. */
    public DualTextInputWindowBody setTextValidatorUserFeedback(@Nullable UITooltip feedback) {
        this.textValidatorFeedbackUITooltip = feedback;
        return this;
    }

    /** Returns whether allow placeholders. */
    public boolean isAllowPlaceholders() {
        return allowPlaceholders;
    }

    /** Sets allow placeholders for this dual text input window body. */
    public DualTextInputWindowBody setAllowPlaceholders(boolean allowPlaceholders) {
        this.allowPlaceholders = allowPlaceholders;
        return this;
    }

}
