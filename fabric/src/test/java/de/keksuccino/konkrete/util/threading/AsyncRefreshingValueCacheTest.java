package de.keksuccino.konkrete.util.threading;

import de.keksuccino.konkrete.placeholder.testing.ManualTaskQueue;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncRefreshingValueCacheTest {

    @Test
    void publishedValuesEvictInInsertionOrderAtEntryBound() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AsyncRefreshingValueCache<String, String> cache = new AsyncRefreshingValueCache<>(2, 100L, (key, value) -> 1L, tasks::add, key -> "value-" + key, (key, exception) -> "failed", (key, exception) -> { }, () -> 0L);

        assertNull(cache.getOrLoad("first", AsyncRefreshingValueCache.NO_REFRESH));
        tasks.runNext();
        assertNull(cache.getOrLoad("second", AsyncRefreshingValueCache.NO_REFRESH));
        tasks.runNext();
        assertNull(cache.getOrLoad("third", AsyncRefreshingValueCache.NO_REFRESH));
        tasks.runNext();

        assertEquals(2, cache.size());
        assertNull(cache.getCached("first"));
        assertEquals("value-second", cache.getCached("second"));
        assertEquals("value-third", cache.getCached("third"));
    }

    @Test
    void overweightValuesAreDeliveredToNoCallerAndAreNotRetained() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AsyncRefreshingValueCache<String, String> cache = new AsyncRefreshingValueCache<>(2, 4L, (key, value) -> value.length(), tasks::add, key -> "oversized", (key, exception) -> "failed", (key, exception) -> { }, () -> 0L);

        assertNull(cache.getOrLoad("key", AsyncRefreshingValueCache.NO_REFRESH));
        tasks.runNext();

        assertEquals(0, cache.size());
        assertNull(cache.getCached("key"));
    }

    @Test
    void staleValueRemainsVisibleWhileSingleRefreshIsQueued() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicLong clock = new AtomicLong();
        AtomicLong version = new AtomicLong(1L);
        AsyncRefreshingValueCache<String, String> cache = new AsyncRefreshingValueCache<>(tasks::add, key -> "v" + version.get(), (key, exception) -> "failed", (key, exception) -> { }, clock::get);

        assertNull(cache.getOrLoad("key", 10L));
        tasks.runNext();
        assertEquals("v1", cache.getOrLoad("key", 10L));
        assertFalse(cache.isLoading("key"));

        clock.set(10L);
        version.set(2L);
        assertEquals("v1", cache.getOrLoad("key", 10L));
        assertEquals("v1", cache.getOrLoad("key", 10L));
        assertTrue(cache.isLoading("key"));
        assertEquals(1, tasks.size());
        tasks.runNext();
        assertEquals("v2", cache.getCached("key"));
    }
}
