package de.keksuccino.konkrete.util.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ResourceLock("ResourceRuntime callbacks")
class ResourceRuntimeTest {

    @Test
    void callbackRegistrationsAreRemovableAndFailuresAreIsolated() {
        AtomicInteger animatedCalls = new AtomicInteger();
        ResourceRuntime.Registration failing = ResourceRuntime.addAnimatedTextureListener((source, sourceType, restarts, status) -> {
            throw new IllegalStateException("expected test failure");
        });
        ResourceRuntime.Registration counting = ResourceRuntime.addAnimatedTextureListener((source, sourceType, restarts, status) -> animatedCalls.incrementAndGet());
        try {
            ResourceRuntime.fireAnimatedTextureEvent("example:test", ResourceSourceType.LOCATION, false, ResourceRuntime.AnimatedTextureStatus.STARTED);
            assertEquals(1, animatedCalls.get());
        } finally {
            failing.close();
            counting.close();
        }

        assertFalse(ResourceRuntime.hasAnimatedTextureListeners());
    }

}
