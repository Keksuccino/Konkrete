package de.keksuccino.konkrete.util.resource;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceWaitTest {

    @Test
    void loadingWaitCooperativelyObservesCompletion() throws Exception {
        MutableResource resource = new MutableResource();
        CountDownLatch started = new CountDownLatch(1);
        Thread waiter = Thread.ofPlatform().start(() -> {
            started.countDown();
            resource.waitForLoadingCompletedOrFailed(5_000L);
        });
        assertTrue(started.await(1L, TimeUnit.SECONDS));

        resource.completed = true;
        waiter.join(1_000L);

        assertFalse(waiter.isAlive());
    }

    @Test
    void readyWaitReturnsPromptlyWhenInterrupted() throws Exception {
        MutableResource resource = new MutableResource();
        CountDownLatch started = new CountDownLatch(1);
        Thread waiter = Thread.ofPlatform().start(() -> {
            started.countDown();
            resource.waitForReady(60_000L);
        });
        assertTrue(started.await(1L, TimeUnit.SECONDS));

        waiter.interrupt();
        waiter.join(1_000L);

        assertFalse(waiter.isAlive());
        assertTrue(waiter.isInterrupted());
    }

    private static final class MutableResource implements Resource {

        private volatile boolean ready;
        private volatile boolean completed;

        @Override
        public @Nullable InputStream open() {
            return null;
        }

        @Override
        public boolean isReady() {
            return this.ready;
        }

        @Override
        public boolean isLoadingCompleted() {
            return this.completed;
        }

        @Override
        public boolean isLoadingFailed() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {}
    }
}
