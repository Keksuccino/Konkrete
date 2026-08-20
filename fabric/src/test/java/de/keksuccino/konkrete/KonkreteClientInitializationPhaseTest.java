package de.keksuccino.konkrete;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KonkreteClientInitializationPhaseTest {

    @Test
    void successfulPhaseRunsExactlyOnce() {
        KonkreteClient.InitializationPhase phase = new KonkreteClient.InitializationPhase("test phase");
        AtomicInteger executions = new AtomicInteger();

        assertTrue(phase.runOnce(executions::incrementAndGet));
        assertFalse(phase.runOnce(executions::incrementAndGet));

        assertEquals(1, executions.get());
    }

    @Test
    void reentrantInvocationDoesNotDuplicateWork() {
        KonkreteClient.InitializationPhase phase = new KonkreteClient.InitializationPhase("test phase");
        AtomicInteger executions = new AtomicInteger();

        assertTrue(phase.runOnce(() -> {
            executions.incrementAndGet();
            assertFalse(phase.runOnce(executions::incrementAndGet));
        }));

        assertEquals(1, executions.get());
    }

    @Test
    void failedPhaseCannotPartiallyRunAgain() {
        KonkreteClient.InitializationPhase phase = new KonkreteClient.InitializationPhase("test phase");
        AtomicInteger executions = new AtomicInteger();
        IllegalArgumentException failure = new IllegalArgumentException("expected");

        assertEquals(failure, assertThrows(IllegalArgumentException.class, () -> phase.runOnce(() -> {
            executions.incrementAndGet();
            throw failure;
        })));
        IllegalStateException repeatedFailure = assertThrows(IllegalStateException.class, () -> phase.runOnce(executions::incrementAndGet));

        assertEquals(failure, repeatedFailure.getCause());
        assertEquals(1, executions.get());
    }

}
