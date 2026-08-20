package de.keksuccino.konkrete.util.input;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenKeyEventDispatcherTest {

    private static final long ACTIVE_WINDOW = 42L;
    private static final Screen SCREEN = new TestScreen();
    private static final KeyEvent EVENT = new KeyEvent(65, 1, 2);

    @Test
    void pressRepeatAndReleasePublishAfterTheScreenCallAndPreserveItsResult() {
        List<String> order = new ArrayList<>();
        try (ScreenKeyEventDispatcher.ListenerRegistration registration = ScreenKeyEventDispatcher.registerListener(input -> order.add("listener:" + input.action()))) {
            assertTrue(dispatch(GLFW.GLFW_PRESS, () -> {
                order.add("screen:PRESS");
                return true;
            }));
            assertFalse(dispatch(GLFW.GLFW_REPEAT, () -> {
                order.add("screen:REPEAT");
                return false;
            }));
            assertTrue(dispatch(GLFW.GLFW_RELEASE, () -> {
                order.add("screen:RELEASE");
                return true;
            }));
        }

        assertEquals(List.of("screen:PRESS", "listener:PRESS", "screen:REPEAT", "listener:REPEAT", "screen:RELEASE", "listener:RELEASE"), order);
    }

    @Test
    void wrongWindowAndUnsupportedActionStillInvokeTheScreenWithoutPublishing() {
        AtomicInteger screenCalls = new AtomicInteger();
        AtomicInteger listenerCalls = new AtomicInteger();
        try (ScreenKeyEventDispatcher.ListenerRegistration registration = ScreenKeyEventDispatcher.registerListener(input -> listenerCalls.incrementAndGet())) {
            assertTrue(ScreenKeyEventDispatcher.dispatchAfterScreenCall(ACTIVE_WINDOW + 1L, ACTIVE_WINDOW, GLFW.GLFW_PRESS, SCREEN, EVENT, () -> {
                screenCalls.incrementAndGet();
                return true;
            }));
            assertFalse(ScreenKeyEventDispatcher.dispatchAfterScreenCall(ACTIVE_WINDOW, ACTIVE_WINDOW, Integer.MAX_VALUE, SCREEN, EVENT, () -> {
                screenCalls.incrementAndGet();
                return false;
            }));
        }

        assertEquals(2, screenCalls.get());
        assertEquals(0, listenerCalls.get());
    }

    @Test
    void screenFailureDoesNotPublishAnInput() {
        AtomicInteger listenerCalls = new AtomicInteger();
        try (ScreenKeyEventDispatcher.ListenerRegistration registration = ScreenKeyEventDispatcher.registerListener(input -> listenerCalls.incrementAndGet())) {
            assertThrows(TestScreenException.class, () -> dispatch(GLFW.GLFW_PRESS, () -> {
                throw new TestScreenException();
            }));
        }

        assertEquals(0, listenerCalls.get());
    }

    @Test
    void listenersRunInRegistrationOrderAndFailuresAreIsolated() {
        List<String> calls = new ArrayList<>();
        ScreenKeyEventDispatcher.ListenerRegistration first = ScreenKeyEventDispatcher.registerListener(input -> calls.add("first"));
        ScreenKeyEventDispatcher.ListenerRegistration failing = ScreenKeyEventDispatcher.registerListener(input -> {
            calls.add("failing");
            throw new IllegalStateException("expected test failure");
        });
        ScreenKeyEventDispatcher.ListenerRegistration last = ScreenKeyEventDispatcher.registerListener(input -> calls.add("last"));
        try (first; failing; last) {
            dispatch(GLFW.GLFW_PRESS, () -> false);
        }

        assertEquals(List.of("first", "failing", "last"), calls);
    }

    @Test
    void registrationHandleControlsListenerLifecycleIdempotently() {
        AtomicInteger listenerCalls = new AtomicInteger();
        ScreenKeyEventDispatcher.ListenerRegistration registration = ScreenKeyEventDispatcher.registerListener(input -> listenerCalls.incrementAndGet());
        assertTrue(registration.isRegistered());
        dispatch(GLFW.GLFW_PRESS, () -> false);

        registration.close();
        registration.close();
        dispatch(GLFW.GLFW_RELEASE, () -> false);

        assertFalse(registration.isRegistered());
        assertEquals(1, listenerCalls.get());
    }

    private static boolean dispatch(int action, java.util.function.BooleanSupplier screenCall) {
        return ScreenKeyEventDispatcher.dispatchAfterScreenCall(ACTIVE_WINDOW, ACTIVE_WINDOW, action, SCREEN, EVENT, screenCall);
    }

    private static final class TestScreen extends Screen {

        private TestScreen() {
            super(null, null, Component.empty());
        }

    }

    private static final class TestScreenException extends RuntimeException {

    }

}
