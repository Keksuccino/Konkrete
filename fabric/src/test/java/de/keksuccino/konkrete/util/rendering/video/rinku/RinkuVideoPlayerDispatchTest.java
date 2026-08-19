package de.keksuccino.konkrete.util.rendering.video.rinku;

import de.keksuccino.konkrete.util.rinku.RinkuIntegrationConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RinkuVideoPlayerDispatchTest {

    private RinkuIntegrationConfig.BrowserTaskDispatcher previousDispatcher;

    @org.junit.jupiter.api.BeforeEach
    void captureDispatcher() {
        this.previousDispatcher = RinkuIntegrationConfig.getBrowserTaskDispatcher();
    }

    @AfterEach
    void restoreClientThreadExecutor() {
        RinkuIntegrationConfig.setBrowserTaskDispatcher(this.previousDispatcher);
    }

    @Test
    void browserCallsUseTheConfiguredExecutor() {
        AtomicReference<Runnable> queued = new AtomicReference<>();
        AtomicBoolean executed = new AtomicBoolean();
        RinkuIntegrationConfig.setBrowserExecutor(queued::set);

        RinkuVideoPlayer.dispatchBrowserTask(() -> executed.set(true));

        assertFalse(executed.get());
        assertNotNull(queued.get());
        queued.get().run();
        assertTrue(executed.get());
    }

    @Test
    void browserCallsRunInlineWhenAlreadyOnTheConfiguredThread() {
        AtomicBoolean executed = new AtomicBoolean();
        RinkuIntegrationConfig.setBrowserTaskDispatcher(new RinkuIntegrationConfig.BrowserTaskDispatcher() {
            @Override
            public boolean isCurrentThread() {
                return true;
            }

            @Override
            public void execute(Runnable command) {
                throw new AssertionError("already-current dispatch must not self-queue");
            }
        });

        RinkuVideoPlayer.dispatchBrowserTask(() -> executed.set(true));

        assertTrue(executed.get());
    }

    @Test
    void protectedJavaScriptExtensionPointDispatchesOffThreadCalls() {
        AtomicReference<Runnable> queued = new AtomicReference<>();
        AtomicInteger dispatches = new AtomicInteger();
        RinkuIntegrationConfig.setBrowserExecutor(task -> {
            dispatches.incrementAndGet();
            queued.set(task);
        });
        TestPlayer player = new TestPlayer();

        player.executeFromSubclass("window.test = true;");

        assertEquals(1, dispatches.get());
        assertNotNull(queued.get());
        queued.getAndSet(null).run();
        assertEquals(1, dispatches.get());
        assertNull(queued.get());
    }

    private static final class TestPlayer extends RinkuVideoPlayer {

        /** Suppresses optional browser initialization so this test isolates the protected dispatch contract. */
        @Override
        public void initialize() {
        }

        private void executeFromSubclass(String code) {
            this.executeJavaScript(code);
        }
    }
}
