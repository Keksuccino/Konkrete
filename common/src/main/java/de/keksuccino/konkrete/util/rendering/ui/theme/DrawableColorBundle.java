package de.keksuccino.konkrete.util.rendering.ui.theme;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import org.jetbrains.annotations.NotNull;

/** Groups a base color with its focused, hovered, and disabled variants. */
public class DrawableColorBundle {

    /** Color used on opaque surfaces. */
    protected DrawableColor normal;
    /** Color used on blurred surfaces. */
    protected DrawableColor blur;

    /** Creates a bundle whose interaction variants start from the normal color. */
    @NotNull
    public static DrawableColorBundle of(@NotNull DrawableColor normal, @NotNull DrawableColor blur) {
        return new DrawableColorBundle(normal, blur);
    }

    /** Creates an empty drawable color bundle with default state. */
    protected DrawableColorBundle() {
    }

    /** Pairs opaque and blurred-surface variants of the same theme color. */
    protected DrawableColorBundle(@NotNull DrawableColor normal, @NotNull DrawableColor blur) {
        this.normal = normal;
        this.blur = blur;
    }

    /** Replaces the value held by this object. */
    public DrawableColorBundle set(@NotNull DrawableColor normal, @NotNull DrawableColor blur) {
        this.normal = normal;
        this.blur = blur;
        return this;
    }

    /** Returns the configured callback value. */
    public DrawableColor get() {
        return this.get(UIBase.shouldBlur());
    }

    /** Returns the configured callback value. */
    public DrawableColor get(boolean blur) {
        return blur ? this.blur() : this.normal();
    }

    /** Returns the configured default value. */
    public DrawableColor normal() {
        return this.normal;
    }

    /** Returns the color used to tint blurred UI surfaces. */
    public DrawableColor blur() {
        return this.blur;
    }

}
