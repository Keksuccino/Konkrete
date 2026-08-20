package de.keksuccino.konkrete.util.threading;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KonkreteThreadsTest {

    @Test
    void startsNamedDaemonAndCompletesItsLifecycle() throws Exception {
        CountDownLatch executed = new CountDownLatch(1);

        Thread thread = KonkreteThreads.startDaemonThread(executed::countDown, "LifecycleTest");

        assertTrue(thread.isDaemon());
        assertTrue(thread.getName().matches("Konkrete-LifecycleTest-\\d+"));
        assertTrue(executed.await(5L, TimeUnit.SECONDS));
        thread.join(5000L);
        assertFalse(thread.isAlive());
    }

}
