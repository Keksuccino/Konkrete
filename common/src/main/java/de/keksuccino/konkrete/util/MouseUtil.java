package de.keksuccino.konkrete.util;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Caches GUI-scaled mouse state and dispatches mouse callbacks in registration order.
 * All methods are client-thread confined. Listener mutations made during a callback affect the next dispatch;
 * listener exceptions propagate and stop the current dispatch.
 */
@SuppressWarnings("unused")
public class MouseUtil {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Map<Long, MouseButtonListener> CLICK_LISTENERS = new LinkedHashMap<>();
    private static final Map<Long, MouseButtonListener> RELEASE_LISTENERS = new LinkedHashMap<>();
    private static final Map<Long, MouseMoveListener> MOVE_LISTENERS = new LinkedHashMap<>();
    private static final Map<Long, MouseDragListener> DRAG_LISTENERS = new LinkedHashMap<>();

    private static long listenerId = 0L;
    private static boolean mouseStateInitialized = false;
    private static double lastMouseX = 0D;
    private static double lastMouseY = 0D;
    private static boolean cachedLeftMouseDown = false;
    private static boolean cachedRightMouseDown = false;
    private static boolean cachedMousePositionInitialized = false;
    private static double cachedGuiScaledMouseX = 0D;
    private static double cachedGuiScaledMouseY = 0D;

    /** Samples current mouse state, then dispatches movement followed by left/right drag callbacks. */
    public static void tick() {
        double mouseX = getLiveGuiScaledMouseX();
        double mouseY = getLiveGuiScaledMouseY();
        cacheMousePosition(mouseX, mouseY);
        cachedLeftMouseDown = MC.mouseHandler.isLeftPressed();
        cachedRightMouseDown = MC.mouseHandler.isRightPressed();

        if (!mouseStateInitialized) {
            mouseStateInitialized = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return;
        }

        double deltaX = mouseX - lastMouseX;
        double deltaY = mouseY - lastMouseY;

        if ((deltaX != 0D) || (deltaY != 0D)) {
            for (MouseMoveListener listener : new ArrayList<>(MOVE_LISTENERS.values())) {
                listener.onMouseMoved(mouseX, mouseY, deltaX, deltaY);
            }
            if (isLeftMouseDown()) {
                for (MouseDragListener listener : new ArrayList<>(DRAG_LISTENERS.values())) {
                    listener.onMouseDragged(GLFW.GLFW_MOUSE_BUTTON_LEFT, mouseX, mouseY, deltaX, deltaY);
                }
            }
            if (isRightMouseDown()) {
                for (MouseDragListener listener : new ArrayList<>(DRAG_LISTENERS.values())) {
                    listener.onMouseDragged(GLFW.GLFW_MOUSE_BUTTON_RIGHT, mouseX, mouseY, deltaX, deltaY);
                }
            }
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    /** Registers a non-null press listener at the end of dispatch order and returns its lifecycle ID. */
    public static long addClickListener(@NotNull MouseButtonListener listener) {
        Objects.requireNonNull(listener, "listener");
        listenerId++;
        CLICK_LISTENERS.put(listenerId, listener);
        return listenerId;
    }

    /** Registers a non-null release listener at the end of dispatch order and returns its lifecycle ID. */
    public static long addReleaseListener(@NotNull MouseButtonListener listener) {
        Objects.requireNonNull(listener, "listener");
        listenerId++;
        RELEASE_LISTENERS.put(listenerId, listener);
        return listenerId;
    }

    /** Registers a non-null movement listener at the end of dispatch order and returns its lifecycle ID. */
    public static long addMoveListener(@NotNull MouseMoveListener listener) {
        Objects.requireNonNull(listener, "listener");
        listenerId++;
        MOVE_LISTENERS.put(listenerId, listener);
        return listenerId;
    }

    /** Registers a non-null drag listener at the end of dispatch order and returns its lifecycle ID. */
    public static long addDragListener(@NotNull MouseDragListener listener) {
        Objects.requireNonNull(listener, "listener");
        listenerId++;
        DRAG_LISTENERS.put(listenerId, listener);
        return listenerId;
    }

    /** Replaces an existing press listener without changing its dispatch position. */
    public static boolean overrideClickListener(long id, @NotNull MouseButtonListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!CLICK_LISTENERS.containsKey(id)) {
            return false;
        }
        CLICK_LISTENERS.put(id, listener);
        return true;
    }

    /** Replaces an existing release listener without changing its dispatch position. */
    public static boolean overrideReleaseListener(long id, @NotNull MouseButtonListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!RELEASE_LISTENERS.containsKey(id)) {
            return false;
        }
        RELEASE_LISTENERS.put(id, listener);
        return true;
    }

    /** Replaces an existing movement listener without changing its dispatch position. */
    public static boolean overrideMoveListener(long id, @NotNull MouseMoveListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!MOVE_LISTENERS.containsKey(id)) {
            return false;
        }
        MOVE_LISTENERS.put(id, listener);
        return true;
    }

    /** Replaces an existing drag listener without changing its dispatch position. */
    public static boolean overrideDragListener(long id, @NotNull MouseDragListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!DRAG_LISTENERS.containsKey(id)) {
            return false;
        }
        DRAG_LISTENERS.put(id, listener);
        return true;
    }

    /** Replaces a press or release listener by ID, returning {@code false} when neither registry contains it. */
    public static boolean overrideListener(long id, @NotNull MouseButtonListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (CLICK_LISTENERS.containsKey(id)) {
            CLICK_LISTENERS.put(id, listener);
            return true;
        }
        if (RELEASE_LISTENERS.containsKey(id)) {
            RELEASE_LISTENERS.put(id, listener);
            return true;
        }
        return false;
    }

    /** Replaces a movement listener by ID, preserving dispatch order. */
    public static boolean overrideListener(long id, @NotNull MouseMoveListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!MOVE_LISTENERS.containsKey(id)) {
            return false;
        }
        MOVE_LISTENERS.put(id, listener);
        return true;
    }

    /** Replaces a drag listener by ID, preserving dispatch order. */
    public static boolean overrideListener(long id, @NotNull MouseDragListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!DRAG_LISTENERS.containsKey(id)) {
            return false;
        }
        DRAG_LISTENERS.put(id, listener);
        return true;
    }

    /** Removes the first listener registry entry matching {@code id}. */
    public static boolean removeListener(long id) {
        if (CLICK_LISTENERS.remove(id) != null) {
            return true;
        }
        if (RELEASE_LISTENERS.remove(id) != null) {
            return true;
        }
        if (MOVE_LISTENERS.remove(id) != null) {
            return true;
        }
        return DRAG_LISTENERS.remove(id) != null;
    }

    /** Removes every registered mouse listener without resetting cached input state. */
    public static void clearListeners() {
        CLICK_LISTENERS.clear();
        RELEASE_LISTENERS.clear();
        MOVE_LISTENERS.clear();
        DRAG_LISTENERS.clear();
    }

    /** Caches the press, then invokes a stable snapshot of press listeners in registration order. */
    public static void onMouseButtonPressed(int button, double mouseX, double mouseY) {
        cacheMousePosition(mouseX, mouseY);
        cacheMouseButtonState(button, GLFW.GLFW_PRESS);
        MouseButton mouseButton = MouseButton.fromGlfwButton(button);
        for (MouseButtonListener listener : new ArrayList<>(CLICK_LISTENERS.values())) {
            listener.onMouseButton(mouseButton, mouseX, mouseY);
        }
    }

    /** Caches the release, then invokes a stable snapshot of release listeners in registration order. */
    public static void onMouseButtonReleased(int button, double mouseX, double mouseY) {
        cacheMousePosition(mouseX, mouseY);
        cacheMouseButtonState(button, GLFW.GLFW_RELEASE);
        MouseButton mouseButton = MouseButton.fromGlfwButton(button);
        for (MouseButtonListener listener : new ArrayList<>(RELEASE_LISTENERS.values())) {
            listener.onMouseButton(mouseButton, mouseX, mouseY);
        }
    }

    /** Updates cached left/right state; unsupported buttons are ignored and every non-release action means down. */
    public static void cacheMouseButtonState(int button, int action) {
        MouseButton mouseButton = MouseButton.fromGlfwButton(button);
        boolean down = (action != GLFW.GLFW_RELEASE);
        switch (mouseButton) {
            case LEFT -> cachedLeftMouseDown = down;
            case RIGHT -> cachedRightMouseDown = down;
            default -> {}
        }
    }

    /** Replaces the cached GUI-scaled position used by subsequent reads. */
    public static void cacheMousePosition(double mouseX, double mouseY) {
        cachedGuiScaledMouseX = mouseX;
        cachedGuiScaledMouseY = mouseY;
        cachedMousePositionInitialized = true;
    }

    /** Returns whether Minecraft currently owns the mouse cursor. */
    public static boolean isMouseGrabbed() {
        return MC.mouseHandler.isMouseGrabbed();
    }

    /** Returns the last sampled or callback-updated left-button state. */
    public static boolean isLeftMouseDown() {
        return cachedLeftMouseDown;
    }

    /** Returns the last sampled or callback-updated right-button state. */
    public static boolean isRightMouseDown() {
        return cachedRightMouseDown;
    }

    /** Returns cached GUI-scaled X, or samples the live cursor until the cache is initialized. */
    public static double getGuiScaledMouseX() {
        if (cachedMousePositionInitialized) {
            return cachedGuiScaledMouseX;
        }
        return getLiveGuiScaledMouseX();
    }

    /** Returns cached GUI-scaled Y, or samples the live cursor until the cache is initialized. */
    public static double getGuiScaledMouseY() {
        if (cachedMousePositionInitialized) {
            return cachedGuiScaledMouseY;
        }
        return getLiveGuiScaledMouseY();
    }

    private static double getLiveGuiScaledMouseX() {
        return getMouseX() * (double) MC.getWindow().getGuiScaledWidth() / (double) MC.getWindow().getScreenWidth();
    }

    private static double getLiveGuiScaledMouseY() {
        return getMouseY() * (double) MC.getWindow().getGuiScaledHeight() / (double) MC.getWindow().getScreenHeight();
    }

    /** Returns the live raw cursor X reported by Minecraft. */
    public static double getMouseX() {
        return MC.mouseHandler.xpos();
    }

    /** Returns the live raw cursor Y reported by Minecraft. */
    public static double getMouseY() {
        return MC.mouseHandler.ypos();
    }

    /** Maps supported mouse buttons to GLFW codes. */
    public enum MouseButton {
        /** The primary left mouse button. */
        LEFT(GLFW.GLFW_MOUSE_BUTTON_LEFT),
        /** The secondary right mouse button. */
        RIGHT(GLFW.GLFW_MOUSE_BUTTON_RIGHT),
        /** The middle mouse button. */
        MIDDLE(GLFW.GLFW_MOUSE_BUTTON_MIDDLE),
        /** Any mouse button outside the standard three. */
        OTHER(-1);

        private final int glfwButton;

        MouseButton(int glfwButton) {
            this.glfwButton = glfwButton;
        }

        /** Returns the GLFW button code, or {@code -1} for {@link #OTHER}. */
        public int getGlfwButton() {
            return this.glfwButton;
        }

        /** Maps a GLFW code to a supported mouse button. */
        public static @NotNull MouseButton fromGlfwButton(int button) {
            return switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> LEFT;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> RIGHT;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> MIDDLE;
                default -> OTHER;
            };
        }
    }

    /** Receives mouse button listener callbacks. */
    @FunctionalInterface
    public interface MouseButtonListener {
        /** Handles a mouse-button transition at the supplied coordinates. */
        void onMouseButton(@NotNull MouseButton button, double mouseX, double mouseY);
    }

    /** Receives mouse move listener callbacks. */
    @FunctionalInterface
    public interface MouseMoveListener {
        /** Handles mouse movement and its coordinate deltas. */
        void onMouseMoved(double mouseX, double mouseY, double deltaX, double deltaY);
    }

    /** Receives mouse drag listener callbacks. */
    @FunctionalInterface
    public interface MouseDragListener {
        /** Handles a mouse drag for the supplied button and coordinate deltas. */
        void onMouseDragged(int button, double mouseX, double mouseY, double deltaX, double deltaY);
    }

}
