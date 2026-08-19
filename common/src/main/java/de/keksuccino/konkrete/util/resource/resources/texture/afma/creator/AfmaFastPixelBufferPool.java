package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

/** Retains or reuses {@code AfmaFastPixelBufferPool} state while the AFMA creator is active. */
public final class AfmaFastPixelBufferPool {

    /** Default max cached buffers used by the AFMA creator. */
    protected static final int DEFAULT_MAX_CACHED_BUFFERS_KONKRETE = 4;

    private final int maxCachedBuffers;
    @NotNull
    private final ArrayDeque<int[]> cachedPixelBuffers = new ArrayDeque<>();

    /** Initializes a new {@code AfmaFastPixelBufferPool} for AFMA creator use. */
    public AfmaFastPixelBufferPool() {
        this(DEFAULT_MAX_CACHED_BUFFERS_KONKRETE);
    }

    /** Initializes a new {@code AfmaFastPixelBufferPool} for AFMA creator use. */
    public AfmaFastPixelBufferPool(int maxCachedBuffers) {
        if (maxCachedBuffers <= 0) {
            throw new IllegalArgumentException("AFMA pixel buffer pool size must be greater than zero");
        }
        this.maxCachedBuffers = maxCachedBuffers;
    }

    /** Returns the smallest cached pixel array meeting the requested length, allocating when none qualifies. */
    @NotNull
    public synchronized int[] acquirePixels(int minimumLength) {
        if (minimumLength <= 0) {
            throw new IllegalArgumentException("AFMA pixel buffer length must be greater than zero");
        }

        int[] bestBuffer = null;
        for (int[] candidateBuffer : this.cachedPixelBuffers) {
            if ((candidateBuffer.length < minimumLength)
                    || ((bestBuffer != null) && (candidateBuffer.length >= bestBuffer.length))) {
                continue;
            }
            bestBuffer = candidateBuffer;
        }

        if (bestBuffer != null) {
            this.cachedPixelBuffers.remove(bestBuffer);
            return bestBuffer;
        }
        return new int[minimumLength];
    }

    /** Returns a non-empty pixel array to the MRU cache, evicting the least-recently returned array at capacity. */
    public synchronized void releasePixels(@NotNull int[] pixels) {
        Objects.requireNonNull(pixels);
        if (pixels.length == 0) {
            return;
        }

        if (this.cachedPixelBuffers.size() >= this.maxCachedBuffers) {
            Iterator<int[]> iterator = this.cachedPixelBuffers.descendingIterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        this.cachedPixelBuffers.addFirst(pixels);
    }

    /** Clears the retained state from this AFMA creator component. */
    public synchronized void clear() {
        this.cachedPixelBuffers.clear();
    }

    /** Returns the cached buffer count produced by the AFMA creator. */
    public synchronized int cachedBufferCount() {
        return this.cachedPixelBuffers.size();
    }

}
