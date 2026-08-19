package de.keksuccino.konkrete.util.reload;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientReloadListenerRegistrationTest {

    @Test
    void sameLoaderRegistersExpensiveSequenceOnce() {
        AtomicInteger creations = new AtomicInteger();
        AtomicInteger invocations = new AtomicInteger();
        List<Runnable> listeners = new ArrayList<>();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> {
            creations.incrementAndGet();
            return invocations::incrementAndGet;
        });

        assertTrue(registration.register(KonkreteResourceReload.ClientLoader.NEOFORGE, listeners::add));
        assertFalse(registration.register(KonkreteResourceReload.ClientLoader.NEOFORGE, listeners::add));
        listeners.getFirst().run();

        assertEquals(1, creations.get());
        assertEquals(1, listeners.size());
        assertEquals(1, invocations.get());
    }

    @Test
    void eitherLoaderCanOwnAnIsolatedRegistration() {
        for (KonkreteResourceReload.ClientLoader loader : KonkreteResourceReload.ClientLoader.values()) {
            AtomicInteger registrations = new AtomicInteger();
            ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});

            assertTrue(registration.register(loader, listener -> registrations.incrementAndGet()));
            assertEquals(1, registrations.get());
        }
    }

    @Test
    void differentLoaderCannotReplaceOwner() {
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});

        assertTrue(registration.register(KonkreteResourceReload.ClientLoader.FABRIC, listener -> {}));
        assertThrows(IllegalStateException.class, () -> registration.register(KonkreteResourceReload.ClientLoader.NEOFORGE, listener -> {}));
    }

    @Test
    void registrarAndFactoryFailuresRemainRetryable() {
        AtomicInteger factoryAttempts = new AtomicInteger();
        ClientReloadListenerRegistration<Runnable> factoryRegistration = new ClientReloadListenerRegistration<>(() -> {
            if (factoryAttempts.incrementAndGet() == 1) throw new IllegalStateException("not ready");
            return () -> {};
        });
        ClientReloadListenerRegistration<Runnable> registrarRegistration = new ClientReloadListenerRegistration<>(() -> () -> {});
        Consumer<Runnable> failingRegistrar = listener -> {
            throw new IllegalStateException("not ready");
        };

        assertThrows(IllegalStateException.class, () -> factoryRegistration.register(KonkreteResourceReload.ClientLoader.FABRIC, listener -> {}));
        assertTrue(factoryRegistration.register(KonkreteResourceReload.ClientLoader.FABRIC, listener -> {}));
        assertThrows(IllegalStateException.class, () -> registrarRegistration.register(KonkreteResourceReload.ClientLoader.NEOFORGE, failingRegistrar));
        assertTrue(registrarRegistration.register(KonkreteResourceReload.ClientLoader.NEOFORGE, listener -> {}));
    }

    @Test
    void reentrantRegistrarCannotCreateSecondListenerBeforeCommit() {
        AtomicInteger registrations = new AtomicInteger();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});
        Consumer<Runnable> reentrantRegistrar = listener -> {
            registrations.incrementAndGet();
            assertThrows(IllegalStateException.class, () -> registration.register(KonkreteResourceReload.ClientLoader.FABRIC, nested -> registrations.incrementAndGet()));
        };

        assertTrue(registration.register(KonkreteResourceReload.ClientLoader.FABRIC, reentrantRegistrar));
        assertEquals(1, registrations.get());
    }

    @Test
    void nullDependenciesAndFactoryResultsAreRejectedWithoutCommit() {
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});
        ClientReloadListenerRegistration<Runnable> nullFactoryResult = new ClientReloadListenerRegistration<>(() -> null);

        assertThrows(NullPointerException.class, () -> registration.register(null, listener -> {}));
        assertThrows(NullPointerException.class, () -> registration.register(KonkreteResourceReload.ClientLoader.FABRIC, null));
        assertThrows(NullPointerException.class, () -> nullFactoryResult.register(KonkreteResourceReload.ClientLoader.FABRIC, listener -> {}));
        assertThrows(NullPointerException.class, () -> new ClientReloadListenerRegistration<Runnable>(null));
        assertTrue(registration.register(KonkreteResourceReload.ClientLoader.FABRIC, listener -> {}));
    }

}
