package de.keksuccino.konkrete.networking.request;

import de.keksuccino.konkrete.networking.PacketSendResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketRequestCacheTest {

    @Test
    void coalescesRequestsAndReturnsStaleValueDuringRefresh() {
        AtomicLong clock = new AtomicLong();
        PacketRequestCache<Object, String, String> cache = cache(clock, 8, 4);
        Object session = new Object();
        List<Long> requests = new ArrayList<>();

        assertNull(cache.resolve(session, "rule", id -> sent(requests, id)));
        assertNull(cache.resolve(session, "rule", id -> sent(requests, id)));
        assertEquals(List.of(1L), requests);
        assertTrue(cache.complete(session, 1L, "true"));
        assertEquals("true", cache.resolve(session, "rule", id -> sent(requests, id)));

        clock.set(11L);
        assertEquals("true", cache.resolve(session, "rule", id -> sent(requests, id)));
        assertEquals(List.of(1L, 2L), requests);
        assertTrue(cache.complete(session, 2L, "false"));
        assertEquals("false", cache.resolve(session, "rule", id -> sent(requests, id)));
    }

    @Test
    void timeoutFreesCapacityAndRejectsLateResponses() {
        AtomicLong clock = new AtomicLong();
        PacketRequestCache<Object, String, String> cache = cache(clock, 8, 1);
        Object session = new Object();
        List<Long> requests = new ArrayList<>();

        cache.resolve(session, "first", id -> sent(requests, id));
        cache.resolve(session, "second", id -> sent(requests, id));
        assertEquals(List.of(1L), requests);

        clock.set(21L);
        cache.resolve(session, "second", id -> sent(requests, id));
        assertEquals(List.of(1L, 2L), requests);
        assertFalse(cache.complete(session, 1L, "late"));
        assertTrue(cache.complete(session, 2L, "current"));
    }

    @Test
    void exactSessionChangesClearValuesAndCorrelations() {
        AtomicLong clock = new AtomicLong();
        PacketRequestCache<Object, String, String> cache = cache(clock, 8, 4);
        Object first = new String("same");
        Object second = new String("same");

        cache.resolve(first, "rule", id -> PacketSendResult.SENT);
        cache.beginSession(second);

        assertFalse(cache.complete(first, 1L, "stale"));
        assertFalse(cache.complete(second, 1L, "stale"));
        assertNull(cache.resolve(second, "rule", id -> PacketSendResult.SENT));
        cache.endSession(first);
        assertEquals(1, cache.pendingCount());
        cache.endSession(second);
        assertEquals(0, cache.pendingCount());
    }

    @Test
    void boundsCachedValuesAndPendingRequests() {
        AtomicLong clock = new AtomicLong();
        PacketRequestCache<Object, String, String> cache = cache(clock, 2, 2);
        Object session = new Object();

        cache.resolve(session, "a", id -> PacketSendResult.SENT);
        cache.resolve(session, "b", id -> PacketSendResult.SENT);
        cache.resolve(session, "c", id -> PacketSendResult.SENT);
        assertEquals(2, cache.pendingCount());
        assertTrue(cache.complete(session, 1L, "a"));
        assertTrue(cache.complete(session, 2L, "b"));
        cache.resolve(session, "c", id -> PacketSendResult.SENT);
        assertTrue(cache.complete(session, 3L, "c"));
        assertEquals(2, cache.entryCount());
    }

    private static PacketRequestCache<Object, String, String> cache(AtomicLong clock, int maximumEntries, int maximumPending) {
        return new PacketRequestCache<>(maximumEntries, maximumPending, Duration.ofNanos(10L), Duration.ofNanos(20L), clock::get);
    }

    private static PacketSendResult sent(List<Long> requests, long requestId) {
        requests.add(requestId);
        return PacketSendResult.SENT;
    }

}
