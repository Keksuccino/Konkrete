package de.keksuccino.konkrete.gui.content.handling;

import de.keksuccino.konkrete.input.CharData;
import de.keksuccino.konkrete.input.KeyboardData;

public interface IAdvancedWidgetBase {

    void onTick();

    void onKeyPress(KeyboardData d);

    void onKeyReleased(KeyboardData d);

    void onCharTyped(CharData d);

    void onMouseClicked(double mouseX, double mouseY, int mouseButton);

}
