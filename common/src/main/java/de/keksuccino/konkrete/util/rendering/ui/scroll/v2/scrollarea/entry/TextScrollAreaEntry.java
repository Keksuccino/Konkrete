package de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry;

import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/** Represents one renderable, focusable text scroll area entry. */
@SuppressWarnings("unused")
public class TextScrollAreaEntry extends ScrollAreaEntry {

    /** Text rendered by this entry. */
    protected Component text;
    /** Width in GUI units for text. */
    protected int textWidth;
    /** Receives this entry when it is activated. */
    protected Consumer<TextScrollAreaEntry> onClickCallback;
    /** Packed base color for the entry text. */
    protected int textBaseColor = UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();

    /** Creates a clickable text entry in the supplied scroll area. */
    public TextScrollAreaEntry(ScrollArea parent, @NotNull Component text, @NotNull Consumer<TextScrollAreaEntry> onClick) {
        super(parent, 0, 14);
        this.onClickCallback = onClick;
        this.setText(text);
    }

    /** Renders entry into the active GUI extraction pass. */
    @Override
    public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        float centerY = this.getY() + (this.getHeight() / 2f);
        UIBase.renderText(graphics, this.text, this.getX() + 5f, centerY - (UIBase.getUITextHeightNormal() / 2f), this.textBaseColor);
    }

    /** Handles click for this text scroll area entry. */
    @Override
    public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        this.onClickCallback.accept((TextScrollAreaEntry) entry);
    }

    /** Sets text for this text scroll area entry. */
    public void setText(@NotNull Component text) {
        this.text = text;
        this.textWidth = (int)UIBase.getUITextWidthNormal(this.text);
        this.setWidth(5 + this.textWidth + 5);
    }

    /** Returns text. */
    public Component getText() {
        return this.text;
    }

    /** Returns the measured text width in GUI units. */
    public int getTextWidth() {
        return this.textWidth;
    }

    /** Returns the packed base color applied before text styling. */
    public int getTextBaseColor() {
        return textBaseColor;
    }

    /** Sets text base color for this text scroll area entry. */
    public void setTextBaseColor(int textBaseColor) {
        this.textBaseColor = textBaseColor;
    }

}
