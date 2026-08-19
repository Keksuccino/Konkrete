package de.keksuccino.konkrete.util.event.acara;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventHandlerTest {

    @Test
    void dispatchesExactEventTypeInDescendingPriorityOrder() {
        EventHandler handler = new EventHandler();
        List<String> calls = new ArrayList<>();
        handler.registerListener(event -> calls.add("normal"), TestEvent.class, EventPriority.NORMAL);
        handler.registerListener(event -> calls.add("high"), TestEvent.class, EventPriority.HIGH);

        handler.postEvent(new TestEvent());

        assertEquals(List.of("high", "normal"), calls);
    }

    @Test
    void annotatedListenerRegistrationAndUnregistrationFollowObjectLifecycle() {
        EventHandler handler = new EventHandler();
        AnnotatedListeners listeners = new AnnotatedListeners();
        handler.registerListenersOf(listeners);

        handler.postEvent(new TestEvent());
        handler.unregisterListenersOf(listeners);
        handler.postEvent(new TestEvent());

        assertEquals(1, listeners.validCalls);
        assertFalse(handler.eventsRegisteredForType(TestEvent.class));
    }

    @Test
    void invalidAnnotatedSignaturesAreIgnored() {
        EventHandler handler = new EventHandler();

        handler.registerListenersOf(new InvalidAnnotatedListener());

        assertFalse(handler.eventsRegisteredForType(TestEvent.class));
    }

    @Test
    void listenerFailureDoesNotPreventRemainingListeners() {
        EventHandler handler = new EventHandler();
        List<String> calls = new ArrayList<>();
        handler.registerListener(event -> {
            throw new IllegalStateException("expected test failure");
        }, TestEvent.class, EventPriority.HIGH);
        handler.registerListener(event -> calls.add("continued"), TestEvent.class, EventPriority.NORMAL);

        handler.postEvent(new TestEvent());

        assertEquals(List.of("continued"), calls);
    }

    @Test
    void nullDirectRegistrationInputsAreRejected() {
        EventHandler handler = new EventHandler();

        assertThrows(NullPointerException.class, () -> handler.registerListener(null, TestEvent.class));
        assertThrows(NullPointerException.class, () -> handler.registerListener(event -> {}, null));
        assertThrows(NullPointerException.class, () -> handler.postEvent(null));
    }

    @Test
    void cancelableEventTracksStateTransitions() {
        TestEvent event = new TestEvent();

        event.setCanceled(true);
        assertTrue(event.isCanceled());
        event.setCanceled(false);
        assertFalse(event.isCanceled());
    }

    private static final class TestEvent extends EventBase {

        @Override
        public boolean isCancelable() {
            return true;
        }
    }

    public static final class AnnotatedListeners {

        private int validCalls;

        @EventListener
        public void onEvent(TestEvent event) {
            this.validCalls++;
        }
    }

    public static final class InvalidAnnotatedListener {

        @EventListener
        public void onEvent(TestEvent event, String invalidExtraParameter) {
        }
    }
}
