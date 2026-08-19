package de.keksuccino.konkrete.util.window;

import com.mojang.blaze3d.platform.Window;

/**
 * Mixin bridge that augments {@link Window} with a non-integer GUI scale and scaled-size mutation.
 */
public interface PreciseGuiScaleWindow {

    /**
     * Returns the precise scale, falling back to vanilla's integer scale until one is explicitly set.
     *
     * @return current precise scale
     */
    double getPreciseGuiScale_Konkrete();

    /**
     * Updates the precise scale metadata. Call {@link WindowHandler#setGuiScale(double)} instead of invoking this directly.
     *
     * @param scale precise scale
     */
    void setPreciseGuiScale_Konkrete(double scale);

    /**
     * Updates the private vanilla scaled dimensions after a precise-scale calculation.
     *
     * @param width scaled framebuffer width
     * @param height scaled framebuffer height
     */
    void setGuiScaledSize_Konkrete(int width, int height);

}
