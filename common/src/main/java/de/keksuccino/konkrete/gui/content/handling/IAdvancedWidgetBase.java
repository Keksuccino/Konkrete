package de.keksuccino.konkrete.gui.content.handling;

import de.keksuccino.konkrete.input.CharData;
import de.keksuccino.konkrete.input.KeyboardData;

@Deprecated(forRemoval = true)
public interface IAdvancedWidgetBase {

    /**
     * Does not work anymore. Don't use this.
     */
    @Deprecated
    default void onTick() {}

    void onKeyPress(KeyboardData d);

    /**
     * Does not work anymore. Don't use this.
     */
    @Deprecated
    default void onKeyReleased(KeyboardData d) {}

    void onCharTyped(CharData d);

    /**
     * Does not work anymore. Don't use this.
     */
    @Deprecated
    default void onMouseClicked(double mouseX, double mouseY, int mouseButton) {}

}
