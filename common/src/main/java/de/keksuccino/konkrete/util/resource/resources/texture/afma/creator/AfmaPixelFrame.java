package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaBinIntraPayloadHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/** Carries {@code AfmaPixelFrame} data between validated stages of the AFMA creator. */
public class AfmaPixelFrame implements AutoCloseable {

    private final int width;
    private final int height;
    private final @NotNull int[] pixels;
    @Nullable
    private final AfmaFastPixelBufferPool pixelBufferPool;
    private boolean returnedToPool;

    /** Initializes a new {@code AfmaPixelFrame} for AFMA creator use. */
    public AfmaPixelFrame(int width, int height, @NotNull int[] pixels) {
        this(width, height, pixels, null);
    }

    AfmaPixelFrame(int width, int height, @NotNull int[] pixels, @Nullable AfmaFastPixelBufferPool pixelBufferPool) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("AFMA frame dimensions must be greater than zero");
        }

        Objects.requireNonNull(pixels);
        if (pixels.length != AfmaPixelFrameHelper.pixelCount(width, height)) {
            throw new IllegalArgumentException("AFMA frame pixel buffer size does not match dimensions");
        }

        this.width = width;
        this.height = height;
        this.pixels = pixels;
        this.pixelBufferPool = pixelBufferPool;
    }

    /** Returns the width used by this AFMA creator instance. */
    public int getWidth() {
        return this.width;
    }

    /** Returns the height used by this AFMA creator instance. */
    public int getHeight() {
        return this.height;
    }

    /** Returns the pixel count used by this AFMA creator instance. */
    public int getPixelCount() {
        return this.pixels.length;
    }

    /** Returns the row stride used by this AFMA creator instance. */
    public int getRowStride() {
        return this.width;
    }

    /** Returns the pixel index used by this AFMA creator instance. */
    public int getPixelIndex(int x, int y) {
        return (y * this.width) + x;
    }

    /** Returns the row offset used by this AFMA creator instance. */
    public int getRowOffset(int y) {
        return y * this.width;
    }

    /** Returns the pixel rgba used by this AFMA creator instance. */
    public int getPixelRGBA(int x, int y) {
        return this.pixels[this.getPixelIndex(x, y)];
    }

    /** Sets the pixel rgba used by subsequent AFMA creator operations. */
    public void setPixelRGBA(int x, int y, int color) {
        this.pixels[this.getPixelIndex(x, y)] = color;
    }

    /** Creates the full view variant for the AFMA creator. */
    @NotNull
    public PixelView fullView() {
        return new PixelView(this.pixels, 0, this.width, this.height, this.width);
    }

    /** Returns the view used by the AFMA creator. */
    @NotNull
    public PixelView view(int x, int y, int width, int height) {
        AfmaPixelFrameHelper.validateContainedRegion(this, x, y, width, height);
        return new PixelView(this.pixels, this.getPixelIndex(x, y), width, height, this.width);
    }

    /** Copies the pixels to for the AFMA creator. */
    public void copyPixelsTo(@NotNull int[] targetPixels) {
        this.copyPixelsTo(targetPixels, 0);
    }

    /** Copies the pixels to for the AFMA creator. */
    public void copyPixelsTo(@NotNull int[] targetPixels, int targetOffset) {
        Objects.requireNonNull(targetPixels);
        if ((targetOffset < 0) || ((targetPixels.length - targetOffset) < this.pixels.length)) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the source frame");
        }
        System.arraycopy(this.pixels, 0, targetPixels, targetOffset, this.pixels.length);
    }

    /** Copies the row to for the AFMA creator. */
    public void copyRowTo(int y, int srcX, @NotNull int[] targetPixels, int targetOffset, int length) {
        Objects.requireNonNull(targetPixels);
        if ((y < 0) || (y >= this.height)) {
            throw new IndexOutOfBoundsException("AFMA row index out of bounds");
        }
        if ((srcX < 0) || (length < 0) || ((srcX + length) > this.width)) {
            throw new IndexOutOfBoundsException("AFMA row copy exceeds frame bounds");
        }
        if ((targetOffset < 0) || ((targetPixels.length - targetOffset) < length)) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the requested row copy");
        }
        System.arraycopy(this.pixels, this.getPixelIndex(srcX, y), targetPixels, targetOffset, length);
    }

    /** Returns whether alpha. */
    public boolean hasAlpha() {
        for (int color : this.pixels) {
            if (((color >>> 24) & 0xFF) != 0xFF) {
                return true;
            }
        }
        return false;
    }

    /** Copies the resource data for the AFMA creator. */
    public @NotNull AfmaPixelFrame copy() {
        return new AfmaPixelFrame(this.width, this.height, Arrays.copyOf(this.pixels, this.pixels.length));
    }

    /** Copies the pixels for the AFMA creator. */
    public @NotNull int[] copyPixels() {
        return Arrays.copyOf(this.pixels, this.pixels.length);
    }

    /**
     * Exposes the backing pixels for read-only hot paths that need to avoid extra copies.
     */
    public @NotNull int[] getPixelsUnsafe() {
        return this.pixels;
    }

    /** Encodes this frame as a standalone BIN_INTRA payload. */
    public @NotNull byte[] asByteArray() throws IOException {
        return AfmaBinIntraPayloadHelper.encodePayload(this.width, this.height, this.pixels);
    }

    /** Returns pooled pixels at most once; standalone pixel arrays require no cleanup. */
    @Override
    public void close() {
        if (!this.returnedToPool && (this.pixelBufferPool != null)) {
            this.returnedToPool = true;
            this.pixelBufferPool.releasePixels(this.pixels);
        }
    }

    /** Models {@code PixelView} state used by the AFMA creator. */
    public static final class PixelView {

        @NotNull
        private final int[] pixels;
        private final int offset;
        private final int width;
        private final int height;
        private final int stride;

        PixelView(@NotNull int[] pixels, int offset, int width, int height, int stride) {
            this.pixels = Objects.requireNonNull(pixels);
            this.offset = offset;
            this.width = width;
            this.height = height;
            this.stride = stride;
        }

        /** Returns the pixels used by the AFMA creator. */
        @NotNull
        public int[] pixels() {
            return this.pixels;
        }

        /** Builds the fset for the AFMA creator. */
        public int offset() {
            return this.offset;
        }

        /** Returns the width used by the AFMA creator. */
        public int width() {
            return this.width;
        }

        /** Returns the height used by the AFMA creator. */
        public int height() {
            return this.height;
        }

        /** Returns the stride produced by the AFMA creator. */
        public int stride() {
            return this.stride;
        }

        /** Returns the pixel rgba used by this AFMA creator instance. */
        public int getPixelRGBA(int x, int y) {
            if ((x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) {
                throw new IndexOutOfBoundsException("AFMA pixel view coordinates out of bounds");
            }
            return this.pixels[this.offset + (y * this.stride) + x];
        }

        /** Copies the row to for the AFMA creator. */
        public void copyRowTo(int y, int srcX, @NotNull int[] targetPixels, int targetOffset, int length) {
            Objects.requireNonNull(targetPixels);
            if ((y < 0) || (y >= this.height)) {
                throw new IndexOutOfBoundsException("AFMA pixel view row index out of bounds");
            }
            if ((srcX < 0) || (length < 0) || ((srcX + length) > this.width)) {
                throw new IndexOutOfBoundsException("AFMA pixel view row copy exceeds bounds");
            }
            if ((targetOffset < 0) || ((targetPixels.length - targetOffset) < length)) {
                throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the requested row copy");
            }
            System.arraycopy(this.pixels, this.offset + (y * this.stride) + srcX, targetPixels, targetOffset, length);
        }
    }

}
