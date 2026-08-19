package de.keksuccino.konkrete.util.rendering.ui.screen;

import de.keksuccino.konkrete.util.ObjectHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/** Queues a widget focus request until its parent screen reaches the next render pass. */
public interface InitialWidgetFocusScreen {

    /** Shared flag indicating that a queued focus request is ready to apply. */
    final ObjectHolder<Boolean> doInitialWidgetFocusAction = ObjectHolder.of(false);
    /** Parent screen that will receive the queued focus target. */
    final ObjectHolder<Screen> parentScreenOfInitialFocusWidget = ObjectHolder.of(null);
    /** Widget scheduled to become the parent's focused listener. */
    final ObjectHolder<GuiEventListener> initialFocusWidget = ObjectHolder.of(null);

    /** Queues the supplied widget to receive focus during the next render pass. */
    default void setupInitialFocusWidget(@NotNull Screen parentScreen, @NotNull GuiEventListener widget) {
        parentScreenOfInitialFocusWidget.set(Objects.requireNonNull(parentScreen));
        initialFocusWidget.set(Objects.requireNonNull(widget));
        doInitialWidgetFocusAction.set(true);
    }

    /** Applies and clears the focus request queued for the next render pass. */
    default void performInitialWidgetFocusActionInRender() {
        if (doInitialWidgetFocusAction.get()) {
            doInitialWidgetFocusAction.set(false);
            Screen s = parentScreenOfInitialFocusWidget.get();
            GuiEventListener l = initialFocusWidget.get();
            if ((s != null) && (l != null)) {
                s.setFocused(l);
            }
        }
    }

}
