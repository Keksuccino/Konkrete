package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.Deflater;

/** Provides {@code AfmaPayloadMetricsHelper} transformations and validation used by the AFMA codec. */
public final class AfmaPayloadMetricsHelper {

    private static final int MAX_CONTENT_CACHE_ENTRIES = 512;
    private static final ThreadLocal<Deflater> DEFLATER_POOL = ThreadLocal.withInitial(() -> new Deflater(AfmaChunkedPayloadHelper.PLANNER_DEFLATE_LEVEL, true));
    private static final ThreadLocal<byte[]> BUFFER_POOL = ThreadLocal.withInitial(() -> new byte[8192]);
    private static final Map<byte[], Long> ESTIMATED_ARCHIVE_BYTES = Collections.synchronizedMap(new WeakHashMap<>());
    // Keep a small recent-content cache so fresh byte arrays can still reuse expensive metric work.
    private static final Map<PayloadBytesKey, Long> ESTIMATED_ARCHIVE_BYTES_BY_CONTENT = Collections.synchronizedMap(newBoundedContentCache());
    private static final Map<byte[], String> FINGERPRINTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<PayloadBytesKey, String> FINGERPRINTS_BY_CONTENT = Collections.synchronizedMap(newBoundedContentCache());

    private AfmaPayloadMetricsHelper() {
    }

    /** Calculates the archive bytes for the AFMA codec. */
    public static long estimateArchiveBytes(@Nullable byte[] payloadBytes) {
        if (payloadBytes == null) {
            return 0L;
        }

        Long cachedValue = ESTIMATED_ARCHIVE_BYTES.get(payloadBytes);
        if (cachedValue != null) {
            return cachedValue;
        }

        PayloadBytesKey cacheKey = new PayloadBytesKey(payloadBytes);
        cachedValue = ESTIMATED_ARCHIVE_BYTES_BY_CONTENT.get(cacheKey);
        if (cachedValue != null) {
            ESTIMATED_ARCHIVE_BYTES.put(payloadBytes, cachedValue);
            return cachedValue;
        }

        long computedValue = estimateArchiveBytesUncached(payloadBytes);
        synchronized (ESTIMATED_ARCHIVE_BYTES_BY_CONTENT) {
            Long existingContentValue = ESTIMATED_ARCHIVE_BYTES_BY_CONTENT.get(cacheKey);
            if (existingContentValue != null) {
                ESTIMATED_ARCHIVE_BYTES.put(payloadBytes, existingContentValue);
                return existingContentValue;
            }
            ESTIMATED_ARCHIVE_BYTES_BY_CONTENT.put(cacheKey, computedValue);
        }
        synchronized (ESTIMATED_ARCHIVE_BYTES) {
            Long existingValue = ESTIMATED_ARCHIVE_BYTES.get(payloadBytes);
            if (existingValue != null) {
                return existingValue;
            }
            ESTIMATED_ARCHIVE_BYTES.put(payloadBytes, computedValue);
        }
        return computedValue;
    }

    /** Computes a stable fingerprint for the payload in the AFMA codec. */
    @NotNull
    public static String fingerprintPayload(@NotNull byte[] payloadBytes) {
        String cachedValue = FINGERPRINTS.get(payloadBytes);
        if (cachedValue != null) {
            return cachedValue;
        }

        PayloadBytesKey cacheKey = new PayloadBytesKey(payloadBytes);
        cachedValue = FINGERPRINTS_BY_CONTENT.get(cacheKey);
        if (cachedValue != null) {
            FINGERPRINTS.put(payloadBytes, cachedValue);
            return cachedValue;
        }

        String computedValue = fingerprintPayloadUncached(payloadBytes);
        synchronized (FINGERPRINTS_BY_CONTENT) {
            String existingContentValue = FINGERPRINTS_BY_CONTENT.get(cacheKey);
            if (existingContentValue != null) {
                FINGERPRINTS.put(payloadBytes, existingContentValue);
                return existingContentValue;
            }
            FINGERPRINTS_BY_CONTENT.put(cacheKey, computedValue);
        }
        synchronized (FINGERPRINTS) {
            String existingValue = FINGERPRINTS.get(payloadBytes);
            if (existingValue != null) {
                return existingValue;
            }
            FINGERPRINTS.put(payloadBytes, computedValue);
        }
        return computedValue;
    }

    /** Calculates the archive bytes uncached for the AFMA codec. */
    protected static long estimateArchiveBytesUncached(@NotNull byte[] payloadBytes) {
        Deflater deflater = DEFLATER_POOL.get();
        byte[] buffer = BUFFER_POOL.get();
        deflater.reset();
        long compressedBytes = 0L;
        deflater.setInput(payloadBytes);
        deflater.finish();
        while (!deflater.finished()) {
            compressedBytes += deflater.deflate(buffer);
        }
        return Math.max(1L, compressedBytes);
    }

    /** Computes a stable fingerprint for the payload uncached in the AFMA codec. */
    @NotNull
    protected static String fingerprintPayloadUncached(@NotNull byte[] payloadBytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payloadBytes);
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte digestByte : digest) {
                builder.append(Character.forDigit((digestByte >>> 4) & 0xF, 16));
                builder.append(Character.forDigit(digestByte & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return payloadBytes.length + ":" + Arrays.hashCode(payloadBytes);
        }
    }

    /** Creates the bounded content cache used by the AFMA codec. */
    @NotNull
    protected static <T> LinkedHashMap<PayloadBytesKey, T> newBoundedContentCache() {
        return new LinkedHashMap<>(MAX_CONTENT_CACHE_ENTRIES, 0.75F, true) {
            /** Removes the eldest entry from this AFMA codec component. */
            @Override
            protected boolean removeEldestEntry(Map.Entry<PayloadBytesKey, T> eldest) {
                return this.size() > MAX_CONTENT_CACHE_ENTRIES;
            }
        };
    }

    /** Provides a value-based {@code PayloadBytesKey} cache key for the AFMA codec. */
    protected static final class PayloadBytesKey {

        /** Holds the payloadBytes collection used by this AFMA codec instance. */
        @NotNull
        protected final byte[] payloadBytes;
        /** Current payload hash state for this AFMA codec instance. */
        protected final int payloadHash;

        /** Initializes a new {@code PayloadBytesKey} for AFMA codec use. */
        protected PayloadBytesKey(@NotNull byte[] payloadBytes) {
            this.payloadBytes = payloadBytes;
            this.payloadHash = Arrays.hashCode(payloadBytes);
        }

        /** Returns a content hash that incorporates payload length and bytes. */
        @Override
        public int hashCode() {
            return (31 * this.payloadBytes.length) + this.payloadHash;
        }

        /** Compares this AFMA codec value with another value. */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PayloadBytesKey other)) {
                return false;
            }
            return this.payloadBytes.length == other.payloadBytes.length
                    && this.payloadHash == other.payloadHash
                    && Arrays.equals(this.payloadBytes, other.payloadBytes);
        }

    }

}
