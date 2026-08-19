package de.keksuccino.konkrete.util.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientShutdownHandlerTest {

    @Test
    void executesInOrderOnceAndIsolatesFailures() {
        ClientShutdownHandler.CleanupRegistry registry = new ClientShutdownHandler.CleanupRegistry();
        List<String> calls = new ArrayList<>();
        registry.register("second", 20, () -> calls.add("second"));
        registry.register("failing", 10, () -> {
            calls.add("failing");
            throw new IllegalStateException("expected");
        });
        registry.register("first", 10, () -> calls.add("first"));

        registry.shutdown();
        registry.shutdown();

        assertEquals(List.of("failing", "first", "second"), calls);
        assertTrue(registry.isShuttingDown());
    }

    @Test
    void closeCancelsBeforeShutdownAndLateRegistrationRunsImmediately() {
        ClientShutdownHandler.CleanupRegistry registry = new ClientShutdownHandler.CleanupRegistry();
        AtomicInteger calls = new AtomicInteger();
        ClientShutdownHandler.CleanupRegistration registration = registry.register("cancelled", 0, calls::incrementAndGet);
        registration.close();

        registry.shutdown();
        registry.register("late", 0, calls::incrementAndGet);

        assertEquals(1, calls.get());
    }

}
