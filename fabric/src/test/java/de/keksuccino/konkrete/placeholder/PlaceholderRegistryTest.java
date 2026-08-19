package de.keksuccino.konkrete.placeholder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderRegistry global state")
class PlaceholderRegistryTest {

    @AfterEach
    void clearRegistry() {
        PlaceholderRegistry.clear();
    }

    @Test
    void namespacesMayOwnTheSameLocalIdentifier() {
        TestPlaceholder first = new TestPlaceholder("shared", List.of("legacy"));
        TestPlaceholder second = new TestPlaceholder("shared", List.of());

        PlaceholderRegistry.register("first_mod", first);
        PlaceholderRegistry.register("second_mod", second);

        assertSame(first, PlaceholderRegistry.getPlaceholder("first_mod:shared"));
        assertSame(second, PlaceholderRegistry.getPlaceholder("second_mod:shared"));
        assertSame(first, PlaceholderRegistry.getPlaceholder("first_mod:legacy"));
        assertEquals(List.of(first, second), PlaceholderRegistry.getPlaceholders());
    }

    @Test
    void lifecycleAndNamespaceRemovalAreOwnedAndDeterministic() {
        TestPlaceholder first = new TestPlaceholder("first", List.of());
        TestPlaceholder second = new TestPlaceholder("second", List.of());
        TestPlaceholder foreign = new TestPlaceholder("foreign", List.of());

        PlaceholderRegistry.register("owned", first);
        PlaceholderRegistry.register("owned", second);
        PlaceholderRegistry.register("foreign", foreign);

        assertEquals(1, first.registered.get());
        assertEquals(2, PlaceholderRegistry.unregisterNamespace("owned"));
        assertEquals(1, first.unregistered.get());
        assertEquals(1, second.unregistered.get());
        assertEquals(0, foreign.unregistered.get());
        assertEquals(List.of(foreign), PlaceholderRegistry.getPlaceholders());
        assertFalse(PlaceholderRegistry.unregister("owned", "first"));
    }

    @Test
    void collisionsAndMismatchedNamespacesFailClosed() {
        PlaceholderRegistry.register("owner", new TestPlaceholder("first", List.of("alias")));

        assertThrows(IllegalStateException.class, () -> PlaceholderRegistry.register("owner", new TestPlaceholder("second", List.of("alias"))));
        assertThrows(IllegalArgumentException.class, () -> PlaceholderRegistry.register("owner", new TestPlaceholder("other:first", List.of())));
        assertThrows(IllegalArgumentException.class, () -> PlaceholderRegistry.register("bad namespace", new TestPlaceholder("value", List.of())));
        assertThrows(IllegalArgumentException.class, () -> PlaceholderRegistry.unregister("owner", "other:first"));
        assertEquals(1, PlaceholderRegistry.getPlaceholders().size());
        assertTrue(PlaceholderRegistry.unregister("owner", "owner:first"));
    }

    @Test
    void registeringHookRunsOutsideLockWhileIdentifierRemainsReservedAndInvisible() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        LifecyclePlaceholder blocked = new LifecyclePlaceholder("blocked", () -> await(entered, release), () -> { });

        CompletableFuture<Void> registration = CompletableFuture.runAsync(() -> PlaceholderRegistry.register("owner", blocked));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        assertSame(null, PlaceholderRegistry.getPlaceholder("owner:blocked"));
        assertThrows(IllegalStateException.class, () -> PlaceholderRegistry.register("owner", new TestPlaceholder("blocked", List.of())));
        TestPlaceholder unrelated = new TestPlaceholder("unrelated", List.of());
        PlaceholderRegistry.register("owner", unrelated);
        assertSame(unrelated, PlaceholderRegistry.getPlaceholder("owner:unrelated"));

        release.countDown();
        registration.get(5, TimeUnit.SECONDS);
        assertSame(blocked, PlaceholderRegistry.getPlaceholder("owner:blocked"));
    }

    @Test
    void failedRegisteringHookReleasesReservationWithoutPublishing() {
        LifecyclePlaceholder failed = new LifecyclePlaceholder("failed", () -> { throw new IllegalStateException("registration failed"); }, () -> { });

        assertThrows(IllegalStateException.class, () -> PlaceholderRegistry.register("owner", failed));
        assertSame(null, PlaceholderRegistry.getPlaceholder("owner:failed"));
        TestPlaceholder replacement = new TestPlaceholder("failed", List.of());
        PlaceholderRegistry.register("owner", replacement);
        assertSame(replacement, PlaceholderRegistry.getPlaceholder("owner:failed"));
    }

    @Test
    void failedRegisteringHookDoesNotRollBackSuccessfulReentrantRegistration() {
        TestPlaceholder nested = new TestPlaceholder("nested", List.of());
        LifecyclePlaceholder failed = new LifecyclePlaceholder("failed", () -> {
            PlaceholderRegistry.register("owner", nested);
            throw new IllegalStateException("registration failed");
        }, () -> { });

        assertThrows(IllegalStateException.class, () -> PlaceholderRegistry.register("owner", failed));
        assertSame(null, PlaceholderRegistry.getPlaceholder("owner:failed"));
        assertSame(nested, PlaceholderRegistry.getPlaceholder("owner:nested"));
    }

    @Test
    void unregisteringHookRunsOutsideLockAfterRemovalIsVisible() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        LifecyclePlaceholder blocked = new LifecyclePlaceholder("blocked", () -> { }, () -> await(entered, release));
        PlaceholderRegistry.register("owner", blocked);

        CompletableFuture<Boolean> unregistration = CompletableFuture.supplyAsync(() -> PlaceholderRegistry.unregister("owner", "blocked"));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        assertSame(null, PlaceholderRegistry.getPlaceholder("owner:blocked"));
        TestPlaceholder unrelated = new TestPlaceholder("unrelated", List.of());
        PlaceholderRegistry.register("owner", unrelated);
        assertSame(unrelated, PlaceholderRegistry.getPlaceholder("owner:unrelated"));

        release.countDown();
        assertTrue(unregistration.get(5, TimeUnit.SECONDS));
    }

    @Test
    void namespaceRemovalRunsEveryHookAndAggregatesFailures() {
        AtomicInteger hooks = new AtomicInteger();
        PlaceholderRegistry.register("owner", new LifecyclePlaceholder("first", () -> { }, () -> { hooks.incrementAndGet(); throw new IllegalStateException("first"); }));
        PlaceholderRegistry.register("owner", new LifecyclePlaceholder("second", () -> { }, () -> { hooks.incrementAndGet(); throw new IllegalArgumentException("second"); }));

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> PlaceholderRegistry.unregisterNamespace("owner"));

        assertEquals(2, hooks.get());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals(List.of(), PlaceholderRegistry.getRegistrations("owner"));
    }

    private static void await(CountDownLatch entered, CountDownLatch release) {
        entered.countDown();
        try {
            if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for lifecycle test release");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static final class TestPlaceholder extends Placeholder {

        private final List<String> aliases;
        private final AtomicInteger registered = new AtomicInteger();
        private final AtomicInteger unregistered = new AtomicInteger();

        private TestPlaceholder(String identifier, List<String> aliases) {
            super(identifier);
            this.aliases = aliases;
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString placeholder) {
            return this.getIdentifier();
        }

        @Override
        public List<String> getValueNames() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return this.getIdentifier();
        }

        @Override
        public List<String> getDescription() {
            return List.of();
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public DeserializedPlaceholderString getDefaultPlaceholderString() {
            return DeserializedPlaceholderString.build(this.getIdentifier(), null);
        }

        @Override
        public List<String> getAlternativeIdentifiers() {
            return this.aliases;
        }

        @Override
        protected void onRegistered(PlaceholderRegistry.Registration registration) {
            this.registered.incrementAndGet();
        }

        @Override
        protected void onUnregistered(PlaceholderRegistry.Registration registration) {
            this.unregistered.incrementAndGet();
        }
    }

    private static final class LifecyclePlaceholder extends Placeholder {

        private final Runnable registeringHook;
        private final Runnable unregisteringHook;

        private LifecyclePlaceholder(String identifier, Runnable registeringHook, Runnable unregisteringHook) {
            super(identifier);
            this.registeringHook = registeringHook;
            this.unregisteringHook = unregisteringHook;
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString placeholder) {
            return this.getIdentifier();
        }

        @Override
        public List<String> getValueNames() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return this.getIdentifier();
        }

        @Override
        public List<String> getDescription() {
            return List.of();
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public DeserializedPlaceholderString getDefaultPlaceholderString() {
            return DeserializedPlaceholderString.build(this.getIdentifier(), null);
        }

        @Override
        protected void onRegistered(PlaceholderRegistry.Registration registration) {
            this.registeringHook.run();
        }

        @Override
        protected void onUnregistered(PlaceholderRegistry.Registration registration) {
            this.unregisteringHook.run();
        }
    }
}
