package de.keksuccino.konkrete.placeholder.placeholders.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerStatusProvidersTest {

    @AfterEach
    void resetProvider() {
        // Replacing with a caller-owned no-op closes any lazily created Konkrete pinger without taking ownership of the override.
        ServerStatusProviders.set(address -> null);
        ServerStatusProviders.reset();
    }

    @Test
    void overrideIsVisibleAndResetLazilyCreatesAnotherBoundedVanillaProvider() {
        ServerStatusProvider vanilla = ServerStatusProviders.get();
        ServerStatusProvider override = address -> new ServerStatus(true, 42L, "MOTD", "1/20", "26.2");

        assertInstanceOf(VanillaServerStatusProvider.class, vanilla);

        ServerStatusProviders.set(override);
        assertSame(override, ServerStatusProviders.get());
        assertEquals(42L, ServerStatusProviders.get().getStatus("example.test").pingMillis());

        ServerStatusProviders.reset();
        ServerStatusProvider recreated = ServerStatusProviders.get();
        assertInstanceOf(VanillaServerStatusProvider.class, recreated);
        assertNotSame(vanilla, recreated);
        assertTrue(((VanillaServerStatusProvider) vanilla).isClosed());
    }

    @Test
    void offlineStatusHasStableFailureValues() {
        ServerStatus offline = ServerStatus.offline();

        assertFalse(offline.online());
        assertEquals(-1L, offline.pingMillis());
        assertEquals("", offline.motd());
        assertEquals("0/0", offline.playerCount());
        assertEquals("", offline.version());
    }

    @Test
    void vanillaProviderRejectsBlankAndOversizedAddressesWithoutNetworkWork() {
        VanillaServerStatusProvider provider = new VanillaServerStatusProvider();
        try {
            assertNull(provider.getStatus("   "));
            assertEquals(ServerStatus.offline(), provider.getStatus("x".repeat(256)));
        } finally {
            provider.close();
        }
    }

    @Test
    void vanillaProviderCloseIsIdempotent() {
        VanillaServerStatusProvider provider = new VanillaServerStatusProvider();

        provider.close();
        provider.close();

        assertTrue(provider.isClosed());
        assertNull(provider.getStatus("example.test"));
    }

    @Test
    void lifecycleIsLazyStableAndRegistersOneShutdownHook() {
        AtomicInteger factories = new AtomicInteger();
        AtomicInteger registrations = new AtomicInteger();
        AtomicReference<Runnable> cleanup = new AtomicReference<>();
        ServerStatusProviders.ProviderLifecycle lifecycle = new ServerStatusProviders.ProviderLifecycle(() -> { factories.incrementAndGet(); return new FakeOwnedProvider(); }, runnable -> { registrations.incrementAndGet(); cleanup.set(runnable); }, () -> false);

        assertEquals(0, factories.get());
        ServerStatusProvider first = lifecycle.get();
        assertSame(first, lifecycle.get());
        assertEquals(1, factories.get());
        assertEquals(1, registrations.get());

        lifecycle.set(first);
        assertEquals(0, ((FakeOwnedProvider) first).closeCalls.get());
        lifecycle.reset();
        assertSame(first, lifecycle.get());
        assertEquals(1, factories.get());
        assertEquals(1, registrations.get());
        assertTrue(cleanup.get() != null);
    }

    @Test
    void lifecycleClosesOnlyOwnedProvidersAcrossOverrideResetAndShutdown() {
        AtomicReference<Runnable> cleanup = new AtomicReference<>();
        AtomicReference<FakeOwnedProvider> latestOwned = new AtomicReference<>();
        ServerStatusProviders.ProviderLifecycle lifecycle = new ServerStatusProviders.ProviderLifecycle(() -> { FakeOwnedProvider provider = new FakeOwnedProvider(); latestOwned.set(provider); return provider; }, cleanup::set, () -> false);
        FakeOwnedProvider firstOwned = (FakeOwnedProvider) lifecycle.get();
        CallerOwnedProvider firstOverride = new CallerOwnedProvider();
        CallerOwnedProvider secondOverride = new CallerOwnedProvider();

        lifecycle.set(firstOverride);
        assertEquals(1, firstOwned.closeCalls.get());
        assertSame(firstOverride, lifecycle.get());
        lifecycle.set(secondOverride);
        assertEquals(0, firstOverride.closeCalls.get());
        lifecycle.reset();
        FakeOwnedProvider secondOwned = (FakeOwnedProvider) lifecycle.get();
        assertNotSame(firstOwned, secondOwned);
        assertEquals(0, firstOverride.closeCalls.get());
        assertEquals(0, secondOverride.closeCalls.get());

        cleanup.get().run();
        cleanup.get().run();
        assertEquals(1, secondOwned.closeCalls.get());
        assertNull(lifecycle.get().getStatus("example.test"));
        assertEquals(0, firstOverride.closeCalls.get());
        assertEquals(0, secondOverride.closeCalls.get());
        assertSame(secondOwned, latestOwned.get());
    }

    private static final class FakeOwnedProvider implements ServerStatusProviders.OwnedProvider {

        private final AtomicInteger closeCalls = new AtomicInteger();

        @Override
        public ServerStatus getStatus(String address) {
            return ServerStatus.offline();
        }

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
        }

    }

    private static final class CallerOwnedProvider implements ServerStatusProvider, AutoCloseable {

        private final AtomicInteger closeCalls = new AtomicInteger();

        @Override
        public ServerStatus getStatus(String address) {
            return ServerStatus.offline();
        }

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
        }

    }

}
