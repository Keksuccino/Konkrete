package de.keksuccino.konkrete.util.rendering.ui.screen;

import de.keksuccino.konkrete.util.rendering.ui.RoutableUIComponent;
import net.minecraft.client.gui.screens.Screen;

/**
 * Marks a {@link Screen} whose {@link RoutableUIComponent}s should use vanilla child hit testing
 * instead of the toolkit's broadcast routing rules.
 */
public interface VanillaMouseClickHandlingScreen {

}
