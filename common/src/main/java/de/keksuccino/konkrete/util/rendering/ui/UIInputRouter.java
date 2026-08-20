package de.keksuccino.konkrete.util.rendering.ui;

import de.keksuccino.konkrete.util.rendering.ui.widget.slider.UIWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared input routing rules for toolkit components registered in vanilla screens. */
public final class UIInputRouter {

    /** Selects whether mouse releases target only the capture owner or all UI components. */
    public enum MouseReleaseRouting {

        /** Routes mouse releases with broadcast UI components behavior. */
        BROADCAST_UI_COMPONENTS,
        /** Routes mouse releases with captured components only behavior. */
        CAPTURED_COMPONENTS_ONLY

    }

    private UIInputRouter() {
    }

    /** Routes mouse clicked to the active target. */
    @Nullable
    public static GuiEventListener routeMouseClicked(@NotNull Iterable<? extends GuiEventListener> listeners, @NotNull MouseButtonEvent event, boolean isDoubleClick) {
        for (GuiEventListener listener : listeners) {
            if ((listener instanceof RoutableUIComponent) && listener.mouseClicked(event, isDoubleClick)) return listener;
        }
        return null;
    }

    /** Routes container mouse clicked to the active target. */
    @Nullable
    public static GuiEventListener routeContainerMouseClicked(@NotNull Iterable<? extends GuiEventListener> listeners, @NotNull MouseButtonEvent event, boolean isDoubleClick) {
        for (GuiEventListener listener : listeners) {
            boolean uiWidget = listener instanceof UIWidget;
            if (uiWidget && !listener.isMouseOver(event.x(), event.y())) continue;
            if (!uiWidget && !(listener instanceof RoutableUIComponent) && !(listener instanceof MouseButtonCaptureOwner)) continue;
            if (listener.mouseClicked(event, isDoubleClick)) return listener;
        }
        return null;
    }

    /** Routes mouse scrolled to the active target. */
    public static boolean routeMouseScrolled(@NotNull Iterable<? extends GuiEventListener> listeners, double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        for (GuiEventListener listener : listeners) {
            if ((listener instanceof RoutableUIComponent) && listener.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        }
        return false;
    }

    /** Routes mouse released to the active target. */
    public static boolean routeMouseReleased(@NotNull Iterable<? extends GuiEventListener> listeners, @Nullable GuiEventListener focused, @NotNull MouseButtonEvent event, @NotNull MouseReleaseRouting routing) {
        if (routing == MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY) {
            boolean dispatched = false;
            for (GuiEventListener listener : listeners) {
                if ((listener instanceof RoutableUIComponent) && (listener instanceof MouseButtonCaptureOwner owner) && owner.hasMouseButtonCapture(event.button())) {
                    listener.mouseReleased(event);
                    dispatched = true;
                }
            }
            return dispatched;
        }
        boolean consumeRelease = shouldConsumeMouseRelease(listeners, focused, event.button());
        for (GuiEventListener listener : listeners) {
            if (listener instanceof RoutableUIComponent) listener.mouseReleased(event);
        }
        return consumeRelease;
    }

    private static boolean shouldConsumeMouseRelease(@NotNull Iterable<? extends GuiEventListener> listeners, @Nullable GuiEventListener focused, int button) {
        // Captured releases remain owned even if another component took screen focus after the press.
        for (GuiEventListener listener : listeners) {
            if ((listener instanceof RoutableUIComponent) && (listener instanceof MouseButtonCaptureOwner owner) && owner.hasMouseButtonCapture(button)) return true;
        }
        if (!(focused instanceof RoutableUIComponent)) return false;
        return !(focused instanceof MouseButtonCaptureOwner owner) || owner.hasMouseButtonCapture(button);
    }

}
