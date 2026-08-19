package de.keksuccino.konkrete.util.rendering.text.color;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import org.jetbrains.annotations.NotNull;
import java.util.function.Supplier;

/** Maps a custom formatting code to a dynamically supplied text color. */
public class DynamicTextColorFormatter extends TextColorFormatter {

    /** Supplies the color each time this formatting code is resolved. */
    protected Supplier<DrawableColor> colorSupplier;

    /** Associates a formatting code with a color supplier evaluated at use time. */
    public DynamicTextColorFormatter(char code, @NotNull Supplier<DrawableColor> colorSupplier) {
        super(code, DrawableColor.WHITE);
        this.colorSupplier = colorSupplier;
    }

    /** Returns the color resolved for the current state. */
    @Override
    public DrawableColor getColor() {
        return this.colorSupplier.get();
    }

}
