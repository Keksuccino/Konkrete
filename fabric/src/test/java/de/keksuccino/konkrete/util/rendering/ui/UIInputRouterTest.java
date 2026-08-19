package de.keksuccino.konkrete.util.rendering.ui;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UIInputRouterTest {

    @Test
    void scrollRoutingSkipsVanillaChildrenAndStopsAtFirstKonkreteConsumer() {
        AtomicInteger vanillaCalls = new AtomicInteger();
        TestComponent first = new TestComponent(false);
        TestComponent consumer = new TestComponent(true);
        TestComponent afterConsumer = new TestComponent(true);
        GuiEventListener vanilla = new TestVanillaComponent(vanillaCalls);

        assertTrue(UIInputRouter.routeMouseScrolled(List.of(vanilla, first, consumer, afterConsumer), 10.0D, 20.0D, 1.0D, -2.0D));
        assertEquals(0, vanillaCalls.get());
        assertEquals(1, first.scrollCalls);
        assertEquals(1, consumer.scrollCalls);
        assertEquals(0, afterConsumer.scrollCalls);
    }

    @Test
    void unconsumedScrollVisitsEachKonkreteComponentExactlyOnce() {
        TestComponent first = new TestComponent(false);
        TestComponent second = new TestComponent(false);

        assertFalse(UIInputRouter.routeMouseScrolled(List.of(first, second), 0.0D, 0.0D, 0.0D, 1.0D));
        assertEquals(1, first.scrollCalls);
        assertEquals(1, second.scrollCalls);
    }

    @Test
    void broadcastReleasePreservesLegacyFocusedComponentConsumption() {
        TestComponent component = new TestComponent(false);
        MouseButtonEvent event = mouseEvent(0);

        assertTrue(UIInputRouter.routeMouseReleased(List.of(component), component, event, UIInputRouter.MouseReleaseRouting.BROADCAST_UI_COMPONENTS));
        assertEquals(1, component.releaseCalls);
        assertFalse(UIInputRouter.routeMouseReleased(List.of(), new TestVanillaComponent(new AtomicInteger()), event, UIInputRouter.MouseReleaseRouting.BROADCAST_UI_COMPONENTS));
        assertFalse(UIInputRouter.routeMouseReleased(List.of(), null, event, UIInputRouter.MouseReleaseRouting.BROADCAST_UI_COMPONENTS));
    }

    @Test
    void broadcastReleaseResolvesCaptureBeforeDispatchClearsIt() {
        CaptureAwareComponent component = new CaptureAwareComponent(1);
        TestVanillaComponent vanilla = new TestVanillaComponent(new AtomicInteger());
        MouseButtonEvent event = mouseEvent(1);

        assertTrue(UIInputRouter.routeMouseReleased(List.of(component, vanilla), vanilla, event, UIInputRouter.MouseReleaseRouting.BROADCAST_UI_COMPONENTS));
        assertEquals(1, component.releaseCalls);
        assertFalse(component.hasMouseButtonCapture(1));
    }

    @Test
    void capturedOnlyReleaseDispatchesOnlyMatchingPointerOwners() {
        CaptureAwareComponent matching = new CaptureAwareComponent(2);
        CaptureAwareComponent other = new CaptureAwareComponent(1);
        TestComponent legacy = new TestComponent(false);
        MouseButtonEvent event = mouseEvent(2);

        assertTrue(UIInputRouter.routeMouseReleased(List.of(other, legacy, matching), null, event, UIInputRouter.MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY));
        assertEquals(0, other.releaseCalls);
        assertEquals(0, legacy.releaseCalls);
        assertEquals(1, matching.releaseCalls);
        assertFalse(matching.hasMouseButtonCapture(2));
    }

    @Test
    void capturedOnlyReleaseReturnsFalseWithoutDispatchWhenButtonIsUnowned() {
        CaptureAwareComponent component = new CaptureAwareComponent(1);

        assertFalse(UIInputRouter.routeMouseReleased(List.of(component), component, mouseEvent(0), UIInputRouter.MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY));
        assertEquals(0, component.releaseCalls);
        assertTrue(component.hasMouseButtonCapture(1));
    }

    private static MouseButtonEvent mouseEvent(int button) {
        return new MouseButtonEvent(5.0D, 6.0D, new MouseButtonInfo(button, 0));
    }

    private static class TestComponent implements GuiEventListener, RoutableUIComponent {

        private final boolean consumeScroll;
        int scrollCalls;
        int releaseCalls;
        private boolean focused;

        private TestComponent(boolean consumeScroll) {
            this.consumeScroll = consumeScroll;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            this.scrollCalls++;
            return this.consumeScroll;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            this.releaseCalls++;
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }
    }

    private static final class CaptureAwareComponent extends TestComponent implements MouseButtonCaptureOwner {

        private int capturedButton;

        private CaptureAwareComponent(int capturedButton) {
            super(false);
            this.capturedButton = capturedButton;
        }

        @Override
        public boolean hasMouseButtonCapture(int button) {
            return button == this.capturedButton;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            super.mouseReleased(event);
            if (event.button() == this.capturedButton) this.capturedButton = -1;
            return true;
        }
    }

    private static final class TestVanillaComponent implements GuiEventListener {

        private final AtomicInteger scrollCalls;
        private boolean focused;

        private TestVanillaComponent(AtomicInteger scrollCalls) {
            this.scrollCalls = scrollCalls;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            this.scrollCalls.incrementAndGet();
            return true;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }
    }

}
