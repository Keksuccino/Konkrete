package de.keksuccino.konkrete.util.watermedia;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WatermediaMrlResolverTest {

    @Test
    void completesImmediatelyForLoadedAndFailedMrls() throws Exception {
        try (WatermediaMrlResolver.Resolution loaded = WatermediaMrlResolver.resolve(new FakeMrl("LOADED"), Duration.ofSeconds(1));
             WatermediaMrlResolver.Resolution failed = WatermediaMrlResolver.resolve(new FakeMrl("ERROR"), Duration.ofSeconds(1))) {
            assertEquals(WatermediaMrlResolver.State.LOADED, loaded.future().get(1, TimeUnit.SECONDS));
            assertEquals(WatermediaMrlResolver.State.FAILED, failed.future().get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void unresolvedMrlHasABoundedTerminalState() throws Exception {
        try (WatermediaMrlResolver.Resolution resolution = WatermediaMrlResolver.resolve(new FakeMrl("FETCHING"), Duration.ofMillis(10))) {
            assertEquals(WatermediaMrlResolver.State.TIMED_OUT, resolution.future().get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void closeCancelsSharedPollingAndCompletesTheFuture() throws Exception {
        WatermediaMrlResolver.Resolution resolution = WatermediaMrlResolver.resolve(new FakeMrl("FETCHING"), Duration.ofSeconds(30));

        resolution.close();

        assertEquals(WatermediaMrlResolver.State.CANCELLED, resolution.future().get(1, TimeUnit.SECONDS));
    }

    @Test
    void rejectsNonPositiveDeadlines() {
        assertThrows(IllegalArgumentException.class, () -> WatermediaMrlResolver.resolve(new FakeMrl("FETCHING"), Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> WatermediaMrlResolver.resolve(new FakeMrl("FETCHING"), Duration.ofMillis(-1)));
    }

    public static final class FakeMrl {
        private final String status;

        FakeMrl(String status) {
            this.status = status;
        }

        public String status() {
            return this.status;
        }
    }
}
