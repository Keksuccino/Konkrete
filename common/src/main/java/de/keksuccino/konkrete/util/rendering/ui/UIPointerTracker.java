package de.keksuccino.konkrete.util.rendering.ui;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Tracks the UI component that consumed each active mouse-button press. */
public final class UIPointerTracker {

    private final Map<Integer, GuiEventListener> pressOwners = new HashMap<>();

    /** Routes mouse clicked to the active target. */
    @Nullable
    public GuiEventListener routeMouseClicked(@NotNull Iterable<? extends GuiEventListener> listeners, @NotNull MouseButtonEvent event, boolean isDoubleClick) {
        this.pressOwners.remove(event.button());
        GuiEventListener listener = UIInputRouter.routeContainerMouseClicked(listeners, event, isDoubleClick);
        if (listener != null) this.pressOwners.put(event.button(), listener);
        return listener;
    }

    /** Routes mouse released to the active target. */
    public boolean dispatchMouseReleased(@NotNull MouseButtonEvent event) {
        GuiEventListener listener = this.pressOwners.remove(event.button());
        if (listener == null) return false;
        listener.mouseReleased(event);
        return true;
    }

    /** Routes mouse dragged to the active target. */
    public boolean dispatchMouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        GuiEventListener listener = this.pressOwners.get(event.button());
        if (listener == null) return false;
        listener.mouseDragged(event, dragX, dragY);
        return true;
    }

}
