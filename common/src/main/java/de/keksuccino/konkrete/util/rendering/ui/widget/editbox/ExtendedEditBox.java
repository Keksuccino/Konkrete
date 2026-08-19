package de.keksuccino.konkrete.util.rendering.ui.widget.editbox;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinEditBox;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.VanillaEvents;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import de.keksuccino.konkrete.util.input.InputUtils;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.slider.UIWidget;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import net.minecraft.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;
import java.util.function.Supplier;

/** Adds configurable styling, clipping, hints, formatting, and suggestion support to Minecraft's edit box. */
@SuppressWarnings("unused")
public class ExtendedEditBox extends EditBox implements UniqueWidget, NavigatableWidget, UIWidget {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Character-level acceptance filter for edited values. */
    protected CharacterFilter characterFilter;
    /** Per-character formatter applied while extracting visible text. */
    protected CharacterRenderFormatter characterRenderFormatter;
    /** Edit-box background color. */
    protected DrawableColor backgroundColor = DrawableColor.of(new Color(0, 0, 0));
    /** Border color while unfocused. */
    protected DrawableColor borderNormalColor = DrawableColor.of(new Color(-6250336));
    /** Border color while focused. */
    protected DrawableColor borderFocusedColor = DrawableColor.of(new Color(255, 255, 255));
    /** Color of editable text. */
    protected DrawableColor textColor = DrawableColor.of(new Color(14737632));
    /** Color of text while editing is disabled. */
    protected DrawableColor textColorUneditable = DrawableColor.of(new Color(7368816));
    /** Color of inline suggestion text. */
    protected DrawableColor suggestionTextColor = DrawableColor.of(new Color(-8355712));
    /** Whether editable text is drawn with a shadow. */
    protected boolean textShadow = true;
    /** Whether labels use UIBase text metrics and rendering. */
    protected boolean renderLabelWithUiBase = false;
    /** Font used to measure and draw editable text. */
    protected final Font font;
    /** Optional stable identifier exposed through {@link UniqueWidget}. */
    @Nullable
    protected String identifier;
    /** Whether this control may receive focus. */
    protected boolean focusable = true;
    /** Whether keyboard navigation may target this control. */
    protected boolean navigatable = true;
    /** Whether the edit box may consume keyboard and pointer input. */
    protected boolean canConsumeUserInput = true;
    /** Whether to draw the configurable background with rounded corners. */
    protected boolean roundedColorBackground = false;
    /** Rounded-background radius, or a negative value to derive it from the box height. */
    protected float roundedColorBackgroundRadius = -1.0F;
    /** Optional color overriding the hint text style. */
    @Nullable
    protected DrawableColor hintTextColor = null;
    /** Non-editable text drawn immediately before the current value. */
    @Nullable
    protected String inputPrefix;
    /** Non-editable text drawn immediately after the current value. */
    @Nullable
    protected String inputSuffix;
    /** Whether an edit action may delete the complete value. */
    protected boolean deleteAllAllowed = true;
    /** Whether the primary mouse button is currently held. */
    protected boolean leftMouseDown = false;
    /** Computes whether the control is active. */
    @Nullable
    protected ConsumingSupplier<ExtendedEditBox, Boolean> isActiveSupplier = null;
    /** Computes whether the control is visible. */
    @Nullable
    protected ConsumingSupplier<ExtendedEditBox, Boolean> isVisibleSupplier = null;
    /** Optionally supplies the edit-box tooltip. */
    @Nullable
    protected Supplier<UITooltip> uiTooltip;
    /** Optionally computes hint text from the edit box's current state. */
    @Nullable
    protected ConsumingSupplier<ExtendedEditBox, Component> customHintSupplier = null;

    /** Initializes an edit box with bounds, narration, and optional vanilla state to copy. */
    public ExtendedEditBox(Font font, int x, int y, int width, int height, Component narrationMessage) {
        super(font, x, y, width, height, narrationMessage);
        this.font = font;
    }

    /** Initializes an edit box with bounds, narration, and optional vanilla state to copy. */
    public ExtendedEditBox(Font font, int x, int y, int width, int height, @Nullable EditBox editBox, Component narrationMessage) {
        super(font, x, y, width, height, editBox, narrationMessage);
        this.font = font;
    }

    /** Renders edit box into the active GUI extraction pass. */
    protected void renderEditBox(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        AccessorMixinEditBox access = ((AccessorMixinEditBox)this);
        boolean bordered = access.get_bordered_Konkrete();

        if (this.isVisible()) {

            float radius = this.roundedColorBackground ? this.resolveRoundedColorBackgroundRadius() : 0.0F;
            boolean useRoundedBackground = this.roundedColorBackground && radius > 0.0F;

            if (useRoundedBackground) {
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        this.getX(),
                        this.getY(),
                        this.width,
                        this.height,
                        radius,
                        radius,
                        radius,
                        radius,
                        this.backgroundColor.getColorInt(),
                        partial
                );
            } else {
                graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.backgroundColor.getColorInt());
            }
            if (bordered) {
                int borderColor = this.isFocused() ? this.borderFocusedColor.getColorInt() : this.borderNormalColor.getColorInt();
                if (useRoundedBackground) {
                    float borderThickness = 1.0F;
                    float borderRadius = radius > 0.0F ? radius + borderThickness : 0.0F;
                    SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(
                            graphics,
                            this.getX() - 1,
                            this.getY() - 1,
                            this.width + 2,
                            this.height + 2,
                            borderThickness,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            borderColor,
                            partial
                    );
                } else {
                    UIBase.renderBorder(graphics, this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, 1, borderColor, true, true, true, true);
                }
            }

            int textColor = access.get_isEditable_Konkrete() ? this.textColor.getColorInt() : this.textColorUneditable.getColorInt();
            int cursorPos = this.getCursorPosition() - access.get_displayPos_Konkrete();
            int highlightPos = access.get_highlightPos_Konkrete() - access.get_displayPos_Konkrete();
            boolean renderWithUiBase = this.renderLabelWithUiBase;
            String text = this.getVisibleText(access.get_displayPos_Konkrete(), this.getInnerWidth(), renderWithUiBase);
            boolean isCursorInsideVisibleText = cursorPos >= 0 && cursorPos <= text.length();
            boolean isCursorVisible = this.isFocused() && (Util.getMillis() - ((AccessorMixinEditBox)this).get_focusedTime_Konkrete()) / 300L % 2L == 0L && isCursorInsideVisibleText;
            float textHeight = renderWithUiBase ? UIBase.getUITextHeightNormal() : 9.0F;
            float textCenterOffset = renderWithUiBase ? textHeight : 8.0F;
            float textX = bordered ? this.getX() + 4.0F : this.getX();
            float textY = bordered ? this.getY() + (this.height - textCenterOffset) / 2F : this.getY();
            float textXAfterCursor = textX;
            if (highlightPos > text.length()) {
                highlightPos = text.length();
            }

            int textCharacterRenderIndex = access.get_displayPos_Konkrete();
            MutableComponent beforeCursorComp = null;
            MutableComponent afterCursorComp = null;
            boolean renderAfterCursor = false;

            if (!text.isEmpty()) {
                String textBeforeCursor = isCursorInsideVisibleText ? text.substring(0, cursorPos) : text;
                beforeCursorComp = Component.literal("");
                if (this.characterRenderFormatter == null) {
                    beforeCursorComp = Component.literal(textBeforeCursor);
                } else {
                    for (char c : textBeforeCursor.toCharArray()) {
                        MutableComponent comp = this.characterRenderFormatter.formatComponent(this, Component.literal(String.valueOf(c)), textCharacterRenderIndex, c, text, this.getValue());
                        beforeCursorComp.append(comp);
                        textCharacterRenderIndex++;
                    }
                }
                textXAfterCursor = textX + (renderWithUiBase ? UIBase.getUITextWidthNormal(beforeCursorComp) : this.font.width(beforeCursorComp));

                if (isCursorInsideVisibleText && cursorPos < text.length()) {
                    String textAfterCursor = text.substring(cursorPos);
                    MutableComponent afterCursor = Component.literal("");
                    if (this.characterRenderFormatter == null) {
                        afterCursor = Component.literal(textAfterCursor);
                    } else {
                        for (char c : textAfterCursor.toCharArray()) {
                            MutableComponent comp = this.characterRenderFormatter.formatComponent(this, Component.literal(String.valueOf(c)), textCharacterRenderIndex, c, text, this.getValue());
                            afterCursor.append(comp);
                            textCharacterRenderIndex++;
                        }
                    }
                    afterCursorComp = afterCursor;
                    renderAfterCursor = true;
                }
            }

            boolean renderSmallCursor = (this.getCursorPosition() < this.getValue().length()) || (this.getValue().length() >= access.get_maxLength_Konkrete());
            float finalTextXAfterCursor = textXAfterCursor;
            if (!isCursorInsideVisibleText) {
                finalTextXAfterCursor = (cursorPos > 0) ? (textX + this.width) : textX;
            } else if (renderSmallCursor) {
                finalTextXAfterCursor = textXAfterCursor - 1;
                if (!renderWithUiBase && this.textShadow) {
                    textXAfterCursor--;
                }
            }

            if (!text.isEmpty() && beforeCursorComp != null) {
                if (renderWithUiBase) {
                    UIBase.renderText(graphics, beforeCursorComp, textX, textY, textColor);
                } else {
                    graphics.text(this.font, beforeCursorComp, (int) textX, (int) textY, textColor, this.textShadow);
                }
                if (renderAfterCursor && afterCursorComp != null) {
                    if (renderWithUiBase) {
                        UIBase.renderText(graphics, afterCursorComp, textXAfterCursor, textY, textColor);
                    } else {
                        graphics.text(this.font, afterCursorComp, (int) textXAfterCursor, (int) textY, textColor, this.textShadow);
                    }
                }
            }

            // Vanilla Hint
            Component hint = access.get_hint_Konkrete();
            boolean vanillaHintRendered = false;
            if ((hint != null) && text.isEmpty() && !this.isFocused()) {
                graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
                if (this.renderLabelWithUiBase) {
                    float hintY = this.getY() + (this.getHeight() / 2F) - (UIBase.getUITextHeightNormal() / 2F);
                    UIBase.renderText(graphics, hint, textXAfterCursor, hintY, textColor);
                } else {
                    graphics.text(this.font, hint, (int) textXAfterCursor, (int) textY, textColor, this.textShadow);
                }
                graphics.disableScissor();
                vanillaHintRendered = true;
            }

            // Konkrete's Custom Hint Implementation
            Component customHint = this.getCustomHint();
            if (!vanillaHintRendered && (customHint != null) && text.isEmpty()) {
                graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
                if (this.renderLabelWithUiBase) {
                    float hintY = this.getY() + (this.getHeight() / 2F) - (UIBase.getUITextHeightNormal() / 2F);
                    UIBase.renderText(graphics, customHint, this.getX() + 4, hintY, -1);
                } else {
                    graphics.text(this.font, customHint, this.getX() + 4, this.getY() + (this.getHeight() / 2) - (this.font.lineHeight / 2), -1, false);
                }
                graphics.disableScissor();
            }

            if (!renderSmallCursor && access.get_suggestion_Konkrete() != null) {
                if (renderWithUiBase) {
                    UIBase.renderText(graphics, access.get_suggestion_Konkrete(), finalTextXAfterCursor - 1, textY, this.suggestionTextColor.getColorInt());
                } else {
                    graphics.text(this.font, access.get_suggestion_Konkrete(), (int) (finalTextXAfterCursor - 1), (int) textY, this.suggestionTextColor.getColorInt(), this.textShadow);
                }
            }

            if (isCursorVisible) {
                if (renderSmallCursor) {
                    graphics.fill((int) finalTextXAfterCursor, (int) (textY - 1), (int) finalTextXAfterCursor + 1, (int) (textY + 1 + textHeight), textColor);
                } else {
                    graphics.fill((int) finalTextXAfterCursor, (int) (textY + textHeight - 1), (int) finalTextXAfterCursor + 5, (int) (textY + textHeight), textColor);
                }
            }

            if (highlightPos != cursorPos) {
                float highlightWidth = UIBase.getUITextWidth(text.substring(0, highlightPos));
                int highlightEndX = (int) (textX + highlightWidth) - 1;
                graphics.textHighlight((int) finalTextXAfterCursor, (int) (textY - 1), highlightEndX, (int) (textY + 1 + textHeight), access.get_invertHighlightedTextColor_Konkrete());
            }

        }

    }

    /** Adds this widget's draw state to the active GUI extraction pass. */
    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (this.isActiveSupplier != null) this.active = this.isActiveSupplier.get(this);

        if (this.isVisibleSupplier != null) this.visible = this.isVisibleSupplier.get(this);

        this.renderEditBox(graphics, mouseX, mouseY, partial);

        if ((this.uiTooltip != null) && this.visible && this.isHovered()) {
            UITooltip tt = this.uiTooltip.get();
            if (tt != null) {
                TooltipHandler.INSTANCE.addRenderTickTooltip(tt, () -> true);
            }
        }

    }

    /** Plays down sound through Minecraft's sound system. */
    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        if (this instanceof CustomizableWidget w) {
            IAudio sound = w.getCustomClickSoundKonkrete();
            if (sound != null) {
                sound.stop();
                sound.play();
            }
        }
    }

    /** Sets height for this extended edit box. */
    public void setHeight(int height) {
        this.height = height;
    }

    /** Returns display position. */
    public int getDisplayPosition() {
        return ((AccessorMixinEditBox)this).get_displayPos_Konkrete();
    }

    /** Sets display position for this extended edit box. */
    public void setDisplayPosition(int position) {
        ((AccessorMixinEditBox)this).set_displayPos_Konkrete(position);
    }

    /** Returns highlight position. */
    public int getHighlightPosition() {
        return ((AccessorMixinEditBox)this).get_highlightPos_Konkrete();
    }

    /** Returns character filter. */
    public @Nullable CharacterFilter getCharacterFilter() {
        return this.characterFilter;
    }

    /** Sets character filter for this extended edit box. */
    public ExtendedEditBox setCharacterFilter(@Nullable CharacterFilter characterFilter) {
        this.characterFilter = characterFilter;
        return this;
    }

    /** Sets delete all allowed for this extended edit box. */
    @NotNull
    public ExtendedEditBox setDeleteAllAllowed(boolean allowed) {
        this.deleteAllAllowed = allowed;
        return this;
    }

    /** Returns whether delete all allowed. */
    public boolean isDeleteAllAllowed() {
        return this.deleteAllAllowed;
    }

    /** Reports whether editable text is drawn with a shadow. */
    public boolean hasTextShadow() {
        return this.textShadow;
    }

    /**
     * Had to rename this in 1.21.1+, because NeoForge seems to add its own setTextShadow() method to the {@link EditBox} class.
     */
    public ExtendedEditBox setTextShadow_Konkrete(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    /** Returns whether label rendered with UI base. */
    public boolean isLabelRenderedWithUiBase() {
        return this.renderLabelWithUiBase;
    }

    /** Sets label rendered with UI base for this extended edit box. */
    public ExtendedEditBox setLabelRenderedWithUiBase(boolean renderLabelWithUiBase) {
        this.renderLabelWithUiBase = renderLabelWithUiBase;
        return this;
    }

    /** Returns the background color resolved for the current state. */
    @NotNull
    public DrawableColor getBackgroundColor() {
        return this.backgroundColor;
    }

    /** Sets background color for this extended edit box. */
    public ExtendedEditBox setBackgroundColor(@NotNull DrawableColor backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /** Returns border normal color. */
    @NotNull
    public DrawableColor getBorderNormalColor() {
        return borderNormalColor;
    }

    /** Sets border normal color for this extended edit box. */
    public ExtendedEditBox setBorderNormalColor(@NotNull DrawableColor borderNormalColor) {
        this.borderNormalColor = borderNormalColor;
        return this;
    }

    /** Returns border focused color. */
    @NotNull
    public DrawableColor getBorderFocusedColor() {
        return this.borderFocusedColor;
    }

    /** Sets border focused color for this extended edit box. */
    public ExtendedEditBox setBorderFocusedColor(@NotNull DrawableColor borderFocusedColor) {
        this.borderFocusedColor = borderFocusedColor;
        return this;
    }

    /** Returns character render formatter. */
    @Nullable
    public ExtendedEditBox.CharacterRenderFormatter getCharacterRenderFormatter() {
        return this.characterRenderFormatter;
    }

    /** Sets character render formatter for this extended edit box. */
    public ExtendedEditBox setCharacterRenderFormatter(@Nullable ExtendedEditBox.CharacterRenderFormatter characterRenderFormatter) {
        this.characterRenderFormatter = characterRenderFormatter;
        return this;
    }

    /** Returns the packed color used to draw the current text. */
    @NotNull
    public DrawableColor getTextColor() {
        return this.textColor;
    }

    /** Sets text color for this extended edit box. */
    public ExtendedEditBox setTextColor(@NotNull DrawableColor textColor) {
        this.textColor = textColor;
        return this;
    }

    /** Returns text color uneditable. */
    @NotNull
    public DrawableColor getTextColorUneditable() {
        return this.textColorUneditable;
    }

    /** Sets text color uneditable for this extended edit box. */
    public ExtendedEditBox setTextColorUneditable(@NotNull DrawableColor textColorUneditable) {
        this.textColorUneditable = textColorUneditable;
        return this;
    }

    /** Returns suggestion text color. */
    @NotNull
    public DrawableColor getSuggestionTextColor() {
        return this.suggestionTextColor;
    }

    /** Sets suggestion text color for this extended edit box. */
    public ExtendedEditBox setSuggestionTextColor(@NotNull DrawableColor suggestionTextColor) {
        this.suggestionTextColor = suggestionTextColor;
        return this;
    }

    /** Returns whether consume user input. */
    public boolean canConsumeUserInput() {
        return this.canConsumeUserInput;
    }

    /** Sets can consume user input for this extended edit box. */
    public ExtendedEditBox setCanConsumeUserInput(boolean canConsumeUserInput) {
        this.canConsumeUserInput = canConsumeUserInput;
        return this;
    }

    /** Returns whether rounded color background enabled. */
    public boolean isRoundedColorBackgroundEnabled() {
        return this.roundedColorBackground;
    }

    /** Sets rounded color background enabled for this extended edit box. */
    public ExtendedEditBox setRoundedColorBackgroundEnabled(boolean roundedColorBackground) {
        this.roundedColorBackground = roundedColorBackground;
        return this;
    }

    /** Returns rounded color background radius. */
    public float getRoundedColorBackgroundRadius() {
        return this.roundedColorBackgroundRadius;
    }

    /** Sets rounded color background radius for this extended edit box. */
    public ExtendedEditBox setRoundedColorBackgroundRadius(float roundedColorBackgroundRadius) {
        this.roundedColorBackgroundRadius = roundedColorBackgroundRadius;
        return this;
    }

    /** Computes rounded color background radius from the supplied inputs. */
    protected float resolveRoundedColorBackgroundRadius() {
        if (this.roundedColorBackgroundRadius >= 0.0F) {
            return this.roundedColorBackgroundRadius;
        }
        return UIBase.getWidgetCornerRoundingRadius();
    }

    /** Returns input prefix. */
    public @Nullable String getInputPrefix() {
        return inputPrefix;
    }

    /** Sets input prefix for this extended edit box. */
    public ExtendedEditBox setInputPrefix(@Nullable String inputPrefix) {
        this.inputPrefix = inputPrefix;
        this.setValue(this.getValueWithoutPrefixSuffix());
        return this;
    }

    /** Returns input suffix. */
    public @Nullable String getInputSuffix() {
        return inputSuffix;
    }

    /** Sets input suffix for this extended edit box. */
    public ExtendedEditBox setInputSuffix(@Nullable String inputSuffix) {
        this.inputSuffix = inputSuffix;
        this.setValue(this.getValueWithoutPrefixSuffix());
        return this;
    }

    /** Applies input prefix suffix character render formatter to the supplied target. */
    public ExtendedEditBox applyInputPrefixSuffixCharacterRenderFormatter() {
        this.setCharacterRenderFormatter((editBox, component, characterIndex, character, visiblePartOfLine, fullLine) -> {
            if ((this.inputSuffix != null) && (characterIndex > Math.max(0, (editBox.getValue().length() - this.inputSuffix.length())-1))) {
                component.withStyle(Style.EMPTY.withColor(this.getTextColorUneditable().getColorInt()));
            }
            if ((this.inputPrefix != null) && (characterIndex < this.inputPrefix.length())) {
                component.withStyle(Style.EMPTY.withColor(this.getTextColorUneditable().getColorInt()));
            }
            return component;
        });
        return this;
    }

    /** Sets is active supplier for this extended edit box. */
    public void setIsActiveSupplier(@Nullable ConsumingSupplier<ExtendedEditBox, Boolean> isActiveSupplier) {
        this.isActiveSupplier = isActiveSupplier;
    }

    /** Sets is visible supplier for this extended edit box. */
    public void setIsVisibleSupplier(@Nullable ConsumingSupplier<ExtendedEditBox, Boolean> isVisibleSupplier) {
        this.isVisibleSupplier = isVisibleSupplier;
    }

    /** Sets text color for this extended edit box. */
    @Deprecated
    @Override
    public void setTextColor(int color) {
        this.textColor = DrawableColor.of(new Color(color));
    }

    /** Sets text color uneditable for this extended edit box. */
    @Deprecated
    @Override
    public void setTextColorUneditable(int color) {
        this.textColorUneditable = DrawableColor.of(new Color(color));
    }

    /** Sets value for this extended edit box. */
    @Override
    public void setValue(@NotNull String value) {
        String v = this.getWithoutPrefixSuffix(value);
        if (this.inputPrefix != null) v = this.inputPrefix + v;
        if (this.inputSuffix != null) v = v + this.inputSuffix;
        super.setValue(v);
    }

    /** Routes typed character input and reports whether it was consumed. */
    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), 0);
    }

    /** Routes typed character input and reports whether it was consumed. */
    public boolean charTyped(char character, int modifiers) {
        if ((this.characterFilter != null) && !this.characterFilter.isAllowedChar(character)) {
            return false;
        }
        return super.charTyped(VanillaEvents.characterEvent(character, modifiers));
    }

    /** Handles click for this extended edit box. */
    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        if (!this.renderLabelWithUiBase) {
            super.onClick(event, isDoubleClick);
            return;
        }

        this.moveCursorTo(this.getCursorPosFromMouseX(event.x()), event.hasShiftDown());
    }

    /** Handles click for this extended edit box. */
    public void onClick(double mouseX, double mouseY) {
        this.onClick(VanillaEvents.mouseButtonEvent(mouseX, mouseY, 0, 0), false);
    }

    /** Inserts text at the current edit position. */
    @Override
    public void insertText(@NotNull String textToWrite) {
        if (this.isInPrefixSuffix(this.getCursorPosition(), 0, 0)) return;
        if (this.isInPrefixSuffix(this.getHighlightPosition(), 0, 0)) return;
        if (this.characterFilter != null) {
            textToWrite = this.characterFilter.filterForAllowedChars(textToWrite);
        }
        super.insertText(textToWrite);
    }

    /** Removes chars from the editable text. */
    @Override
    public void deleteChars(int i) {
        if (this.isInPrefixSuffix(this.getCursorPosition(), -1, -1) || this.isInPrefixSuffix(this.getCursorPosition(), 0, 0)) return;
        if (this.isInPrefixSuffix(this.getHighlightPosition(), 0, 0)) return;
        super.deleteChars(i);
    }

    /** Returns whether in prefix suffix. */
    @SuppressWarnings("all")
    public boolean isInPrefixSuffix(int index, int prefixIndexOffset, int suffixIndexOffset) {
        int cursorPrefix = index + prefixIndexOffset;
        int cursorSuffix = index + suffixIndexOffset;
        if (this.inputPrefix != null) {
            if (cursorPrefix < this.inputPrefix.length()) return true;
        }
        if (this.inputSuffix != null) {
            int i = (this.inputPrefix != null) ? this.inputPrefix.length() + this.getValueWithoutPrefixSuffix().length() : this.getValueWithoutPrefixSuffix().length();
            if (cursorSuffix > i) return true;
        }
        return false;
    }

    /** Returns the editable portion after stripping a configured matching prefix and suffix. */
    public String getValueWithoutPrefixSuffix() {
        return this.getWithoutPrefixSuffix(this.getValue());
    }

    /** Strips configured prefix and suffix text when each is present. */
    protected String getWithoutPrefixSuffix(@NotNull String value) {
        if (value.isEmpty()) return value;
        boolean containsPrefix = (this.inputPrefix != null) && value.startsWith(this.inputPrefix);
        boolean containsSuffix = (this.inputSuffix != null) && value.endsWith(this.inputSuffix);
        String v = containsPrefix ? value.substring(this.inputPrefix.length()) : value;
        if (containsSuffix) v = v.substring(0, Math.max(0, v.length() - this.inputSuffix.length()));
        return v;
    }

    /** Removes text from the editable text. */
    @Override
    public void deleteText(int i, boolean words) {
        if (this.deleteAllAllowed) {
            super.deleteText(i, words);
        } else {
            this.deleteChars(i);
        }
    }

    /** Removes text from the editable text. */
    public void deleteText(int i) {
        this.deleteText(i, false);
    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (!this.canConsumeUserInput) return false;
        if (!net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
            int cursorPos = this.getCursorPosition();
            int highlightPos = this.getHighlightPosition();
            if (cursorPos != highlightPos) {
                if (keycode == 263) {
                    this.moveCursorTo(Math.min(cursorPos, highlightPos), false);
                    return true;
                }
                if (keycode == 262) {
                    this.moveCursorTo(Math.max(cursorPos, highlightPos), false);
                    return true;
                }
            }
        }
        //If select all, only select parts that are not prefix or suffix
        if (((keycode) == 65 && InputUtils.isGuiShortcutModifierDown(modifiers)) && ((this.inputPrefix != null) || (this.inputSuffix != null))) {
            if (this.inputSuffix != null) {
                this.moveCursorTo(this.getValue().length() - this.inputSuffix.length(), false);
            } else {
                this.moveCursorToEnd(false);
            }
            this.setHighlightPos((this.inputPrefix != null) ? this.inputPrefix.length() : 0);
            return true;
        }
        return super.keyPressed(VanillaEvents.keyEvent(keycode, scancode, modifiers));
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.handleMouseClicked(event, isDoubleClick);
    }

    private boolean handleMouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (!this.canConsumeUserInput) return false;
        boolean handled = super.mouseClicked(event, isDoubleClick);
        if (handled && event.button() == 0) this.leftMouseDown = true;
        return handled;
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.handleMouseClicked(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button, 0), false);
    }

    //This is to make the edit box work in FocuslessEventHandlers
    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.leftMouseDown = false;
        return false;
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.canConsumeUserInput) return false;
        if (!this.leftMouseDown || (button != 0)) return false;
        this.moveCursorTo(this.getCursorPosFromMouseX(mouseX), true);
        return true;
    }

    /** Returns cursor pos from mouse x. */
    protected int getCursorPosFromMouseX(double mouseX) {
        int displayPos = Math.max(0, this.getDisplayPosition());
        String value = this.getValue();
        if (value.isEmpty() || (displayPos >= value.length())) {
            return value.length();
        }

        int localX = Mth.floor(mouseX) - this.getX();
        if (((AccessorMixinEditBox)this).get_bordered_Konkrete()) {
            localX -= 4;
        }
        if (localX <= 0) {
            return displayPos;
        }

        float maxWidth = this.getInnerWidth();
        float targetWidth = (maxWidth > 0.0F) ? Math.min(localX, maxWidth) : localX;
        String remaining = value.substring(displayPos);
        int offset = this.renderLabelWithUiBase
                ? this.getTextIndexByWidthAtUIScale(remaining, targetWidth)
                : this.font.plainSubstrByWidth(remaining, (int) targetWidth).length();
        return Math.min(value.length(), displayPos + offset);
    }

    /** Returns visible text. */
    protected String getVisibleText(int displayPos, int innerWidth, boolean renderWithUiBase) {
        String remaining = this.getValue().substring(displayPos);
        if (!renderWithUiBase) {
            return this.font.plainSubstrByWidth(remaining, innerWidth);
        }
        int length = this.getTextIndexByWidthAtUIScale(remaining, innerWidth);
        return remaining.substring(0, length);
    }

    /** Returns text index by width at UI scale. */
    protected int getTextIndexByWidthAtUIScale(@NotNull String text, float targetWidth) {
        if (text.isEmpty()) {
            return 0;
        }
        if (!Float.isFinite(targetWidth)) {
            return targetWidth > 0.0F ? text.length() : 0;
        }
        if (targetWidth <= 0.0F) {
            return 0;
        }
        if (UIBase.isCurrentlyRenderingAtUIScale()) {
            return this.getTextIndexByWidthAtUIScaleInternal(text, targetWidth);
        }
        UIBase.startUIScaleRendering();
        try {
            return this.getTextIndexByWidthAtUIScaleInternal(text, targetWidth);
        } finally {
            UIBase.stopUIScaleRendering();
        }
    }

    /** Returns text index by width at UI scale internal. */
    protected int getTextIndexByWidthAtUIScaleInternal(@NotNull String text, float targetWidth) {
        float fullWidth = UIBase.getUITextWidth(text);
        if (targetWidth >= fullWidth) {
            return text.length();
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            float width = UIBase.getUITextWidth(text.substring(0, mid));
            if (width <= targetWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /** Sets widget identifier for this extended edit box. */
    @Override
    public ExtendedEditBox setWidgetIdentifierKonkrete(@Nullable String identifier) {
        this.identifier = identifier;
        return this;
    }

    /** Returns the optional stable identifier used for widget lookup. */
    @Override
    public @Nullable String getWidgetIdentifierKonkrete() {
        return this.identifier;
    }

    /** Sets focused for this extended edit box. */
    @Override
    public void setFocused(boolean focused) {
        if (!this.focusable) {
            super.setFocused(false);
            return;
        }
        super.setFocused(focused);
    }

    /** Reports whether keyboard focus currently targets this control. */
    @Override
    public boolean isFocused() {
        if (!this.focusable) return false;
        return super.isFocused();
    }

    /** Reports whether this control may receive keyboard focus. */
    @Override
    public boolean isFocusable() {
        return this.focusable;
    }

    /** Sets focusable for this extended edit box. */
    @Override
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    /** Reports whether keyboard navigation may target this control. */
    @Override
    public boolean isNavigatable() {
        return this.navigatable;
    }

    /** Sets navigatable for this extended edit box. */
    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

    /** Sets UI tooltip for this extended edit box. */
    @NotNull
    public ExtendedEditBox setUITooltip(@Nullable Supplier<UITooltip> tooltip) {
        this.uiTooltip = tooltip;
        return this;
    }

    /** Sets custom hint for this extended edit box. */
    @NotNull
    public ExtendedEditBox setCustomHint(@Nullable ConsumingSupplier<ExtendedEditBox, Component> hint) {
        this.customHintSupplier = hint;
        return this;
    }

    /** Sets hint text color for this extended edit box. */
    @NotNull
    public ExtendedEditBox setHintTextColor(@Nullable DrawableColor hintTextColor) {
        this.hintTextColor = hintTextColor;
        return this;
    }

    /** Returns custom hint. */
    @Nullable
    protected MutableComponent getCustomHint() {
        if (this.customHintSupplier == null) return null;
        Component c = this.customHintSupplier.get(this);
        if (c != null) {
            DrawableColor color = (this.hintTextColor != null)
                    ? this.hintTextColor
                    : UIBase.getUITheme().ui_interface_input_field_text_color_uneditable;
            return c.copy().withColor(color.getColorInt());
        }
        return null;
    }

    /** Rewrites the component used to draw one visible character. */
    @FunctionalInterface
    public interface CharacterRenderFormatter {
        /** Applies per-character formatting using both visible and complete line context. */
        @NotNull MutableComponent formatComponent(@NotNull ExtendedEditBox editBox, @NotNull MutableComponent component, int characterIndex, char character, @NotNull String visiblePartOfLine, @NotNull String fullLine);
    }

}
