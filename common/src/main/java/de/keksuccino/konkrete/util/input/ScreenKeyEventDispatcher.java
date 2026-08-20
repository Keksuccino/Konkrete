package de.keksuccino.konkrete.util.input;

import de.keksuccino.konkrete.util.window.WindowHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Dispatches screen key input to a thread-safe, registration-ordered subscriber list after vanilla screen handling.
 * Listener runtime failures are logged and isolated; JVM errors still propagate.
 */
public final class ScreenKeyEventDispatcher {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final CopyOnWriteArrayList<ListenerEntry> LISTENERS = new CopyOnWriteArrayList<>();

    private ScreenKeyEventDispatcher() {
    }

    /**
     * Invokes the screen callback and then publishes valid key input for the active window.
     * The screen callback's handled result is returned unchanged, and no event is published when that callback throws.
     *
     * @throws NullPointerException when {@code screen}, {@code event}, or {@code screenCall} is {@code null}
     */
    public static boolean dispatchAfterScreenCall(long windowPointer, @KeyEvent.Action int action, @NotNull Screen screen, @NotNull KeyEvent event, @NotNull BooleanSupplier screenCall) {
        return dispatchAfterScreenCall(windowPointer, WindowHandler.getWindowHandle(), action, screen, event, screenCall);
    }

    /** Registers a non-null listener at the end of dispatch order and returns its idempotent removal handle. */
    @NotNull
    public static ListenerRegistration registerListener(@NotNull ScreenKeyListener listener) {
        ListenerEntry entry = new ListenerEntry(Objects.requireNonNull(listener, "listener"));
        LISTENERS.add(entry);
        return new ListenerRegistration(entry);
    }

    static boolean dispatchAfterScreenCall(long windowPointer, long activeWindowPointer, int action, @NotNull Screen screen, @NotNull KeyEvent event, @NotNull BooleanSupplier screenCall) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(screenCall, "screenCall");
        KeyAction keyAction = KeyAction.fromGlfwAction(action);
        boolean shouldDispatch = windowPointer == activeWindowPointer && keyAction != null;
        boolean handled = screenCall.getAsBoolean();
        if (shouldDispatch) notifyListeners(new ScreenKeyInput(screen, event, keyAction));
        return handled;
    }

    private static void notifyListeners(ScreenKeyInput input) {
        // CopyOnWriteArrayList iteration is a stable snapshot, so listener registration/removal affects the next dispatch.
        for (ListenerEntry entry : LISTENERS) {
            try {
                entry.listener().onScreenKey(input);
            } catch (Exception exception) {
                LOGGER.error("[KONKRETE] Screen-key listener failed while handling {} input.", input.action(), exception);
            }
        }
    }

    /** Receives immutable screen-key input after vanilla screen handling. */
    @FunctionalInterface
    public interface ScreenKeyListener {

        /** Receives one immutable press, repeat, or release notification after screen handling. */
        void onScreenKey(@NotNull ScreenKeyInput input);

    }

    /** Identifies the GLFW key action that produced a screen-key notification. */
    public enum KeyAction {

        /** A key was initially pressed. */
        PRESS,
        /** A held key generated a repeat. */
        REPEAT,
        /** A key was released. */
        RELEASE;

        private static KeyAction fromGlfwAction(int action) {
            return switch (action) {
                case GLFW.GLFW_PRESS -> PRESS;
                case GLFW.GLFW_REPEAT -> REPEAT;
                case GLFW.GLFW_RELEASE -> RELEASE;
                default -> null;
            };
        }

    }

    /**
     * Immutable input delivered to screen-key subscribers.
     *
     * @param screen screen that received the key call
     * @param event key and modifier data
     * @param action press, repeat, or release action
     */
    public record ScreenKeyInput(@NotNull Screen screen, @NotNull KeyEvent event, @NotNull KeyAction action) {

        /** Rejects incomplete input before it can reach subscribers. */
        public ScreenKeyInput {
            Objects.requireNonNull(screen, "screen");
            Objects.requireNonNull(event, "event");
            Objects.requireNonNull(action, "action");
        }

    }

    /** Idempotent handle that unregisters one listener without affecting duplicate registrations. */
    public static final class ListenerRegistration implements AutoCloseable {

        private final ListenerEntry entry;
        private final AtomicBoolean registered = new AtomicBoolean(true);

        private ListenerRegistration(ListenerEntry entry) {
            this.entry = entry;
        }

        /** Returns whether this entry remains registered; an already-started concurrent snapshot may still invoke it. */
        public boolean isRegistered() {
            return this.registered.get();
        }

        /** Unregisters the listener once; repeated calls have no effect. */
        @Override
        public void close() {
            if (this.registered.compareAndSet(true, false)) LISTENERS.remove(this.entry);
        }

    }

    private record ListenerEntry(ScreenKeyListener listener) {

    }

}
