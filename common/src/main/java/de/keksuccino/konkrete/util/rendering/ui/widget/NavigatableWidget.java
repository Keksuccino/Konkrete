package de.keksuccino.konkrete.util.rendering.ui.widget;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/**
 * {@link GuiEventListener}s that implement this interface can control if they should be navigatable and/or focusable in {@link Screen}s.
 */
public interface NavigatableWidget {

    /** Returns whether this widget may receive keyboard focus. */
    boolean isFocusable();

    /** Controls whether this widget may receive keyboard focus. */
    void setFocusable(boolean focusable);

    /** Returns whether directional navigation may select this widget. */
    boolean isNavigatable();

    /** Controls whether directional navigation may select this widget. */
    void setNavigatable(boolean navigatable);

}
