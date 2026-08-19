package de.keksuccino.konkrete.util.rendering.text.color;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Maps a custom formatting code to a dynamically supplied text color. */
public class TextColorFormatter {

    /** Single-character control code recognized by the component parser. */
    protected final char code;
    /** Fixed color applied when the control code is parsed. */
    protected final DrawableColor color;

    /** Associates a formatting code with a fixed drawable color. */
    public TextColorFormatter(char code, @NotNull DrawableColor color) {
        Objects.requireNonNull(color);
        this.code = code;
        this.color = color;
    }

    /** Returns the single character recognized after the formatting prefix. */
    public char getCode() {
        return this.code;
    }

    /** Returns the control code as a one-character string. */
    public String getCodeString() {
        return "" + this.code;
    }

    /** Returns the color applied by this formatting code. */
    public DrawableColor getColor() {
        return this.color;
    }

    /** Creates a Minecraft style containing this formatter's text color. */
    public Style getStyle() {
        return Style.EMPTY.withColor(this.getColor().getColorInt());
    }

}
