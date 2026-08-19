package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/** Provides {@code AfmaPixelFrameHelper} transformations and validation used by the AFMA creator. */
public final class AfmaPixelFrameHelper {

    private AfmaPixelFrameHelper() {
    }

    /** Validates the dimensions before AFMA creator processing. */
    public static void validateDimensions(int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            throw new IllegalArgumentException("AFMA frame dimensions must be greater than zero");
        }
    }

    /** Calculates the pixel count value for the AFMA creator. */
    public static int pixelCount(int width, int height) {
        validateDimensions(width, height);
        return Math.multiplyExact(width, height);
    }

    /** Ensures the same size is valid for the AFMA creator. */
    public static void ensureSameSize(@NotNull AfmaPixelFrame first, @NotNull AfmaPixelFrame second) {
        if ((first.getWidth() != second.getWidth()) || (first.getHeight() != second.getHeight())) {
            throw new IllegalArgumentException("AFMA frame dimensions do not match");
        }
    }

    /** Returns whether identical. */
    public static boolean isIdentical(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        return new AfmaFramePairAnalysis(previous, next).isIdentical();
    }

    /** Resolves the difference bounds for the AFMA creator. */
    public static @Nullable AfmaRect findDifferenceBounds(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        return new AfmaFramePairAnalysis(previous, next).differenceBounds();
    }

    /** Extracts resource state from the AFMA creator data. */
    public static @NotNull AfmaPixelFrame crop(@NotNull AfmaPixelFrame source, int x, int y, int width, int height) {
        return crop(source, x, y, width, height, null);
    }

    /** Extracts resource state from the AFMA creator data. */
    public static @NotNull AfmaPixelFrame crop(@NotNull AfmaPixelFrame source, int x, int y, int width, int height,
                                               @Nullable AfmaFastPixelBufferPool pixelBufferPool) {
        int[] croppedPixels = allocatePixels(width, height, pixelBufferPool);
        copyRect(source, x, y, width, height, croppedPixels, 0, width);
        return new AfmaPixelFrame(width, height, croppedPixels, pixelBufferPool);
    }

    /** Returns whether region in bounds. */
    public static boolean isRegionInBounds(@NotNull AfmaPixelFrame frame, int x, int y, int width, int height) {
        Objects.requireNonNull(frame);
        if ((width <= 0) || (height <= 0)) {
            return false;
        }
        return (x >= 0) && (y >= 0)
                && (x <= (frame.getWidth() - width))
                && (y <= (frame.getHeight() - height));
    }

    /** Validates the contained region before AFMA creator processing. */
    public static void validateContainedRegion(@NotNull AfmaPixelFrame frame, int x, int y, int width, int height) {
        if (!isRegionInBounds(frame, x, y, width, height)) {
            throw new IndexOutOfBoundsException("AFMA region exceeds frame bounds");
        }
    }

    /** Acquires a suitably sized pooled pixel array, or allocates one when no pool is supplied. */
    @NotNull
    public static int[] allocatePixels(int width, int height, @Nullable AfmaFastPixelBufferPool pixelBufferPool) {
        int pixelCount = pixelCount(width, height);
        return (pixelBufferPool != null)
                ? pixelBufferPool.acquirePixels(pixelCount)
                : new int[pixelCount];
    }

    /** Copies the rect for the AFMA creator. */
    public static void copyRect(@NotNull AfmaPixelFrame source, int x, int y, int width, int height,
                                @NotNull int[] targetPixels, int targetOffset, int targetStride) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(targetPixels);
        validateContainedRegion(source, x, y, width, height);
        if ((targetStride < width) || (targetStride <= 0)) {
            throw new IllegalArgumentException("AFMA target stride must be at least as wide as the copied region");
        }
        long requiredTargetLength = (long) targetOffset + ((long) (height - 1) * (long) targetStride) + width;
        if ((targetOffset < 0) || (requiredTargetLength > targetPixels.length)) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the requested region copy");
        }

        copyRows(source.getPixelsUnsafe(), source.getPixelIndex(x, y), source.getWidth(), targetPixels, targetOffset, targetStride, width, height);
    }

    /** Copies the rows for the AFMA creator. */
    public static void copyRows(@NotNull int[] sourcePixels, int sourceOffset, int sourceStride,
                                @NotNull int[] targetPixels, int targetOffset, int targetStride,
                                int width, int height) {
        Objects.requireNonNull(sourcePixels);
        Objects.requireNonNull(targetPixels);
        if ((width <= 0) || (height <= 0)) {
            throw new IllegalArgumentException("AFMA copy dimensions must be greater than zero");
        }
        if ((sourceStride < width) || (targetStride < width)) {
            throw new IllegalArgumentException("AFMA copy stride must be at least as wide as the copied region");
        }
        if ((sourceOffset < 0) || (targetOffset < 0)) {
            throw new IllegalArgumentException("AFMA copy offsets must not be negative");
        }
        long requiredSourceLength = (long) sourceOffset + ((long) (height - 1) * (long) sourceStride) + width;
        long requiredTargetLength = (long) targetOffset + ((long) (height - 1) * (long) targetStride) + width;
        if ((requiredSourceLength > sourcePixels.length) || (requiredTargetLength > targetPixels.length)) {
            throw new IllegalArgumentException("AFMA copy exceeds the provided pixel buffers");
        }

        for (int row = 0; row < height; row++) {
            System.arraycopy(sourcePixels, sourceOffset + (row * sourceStride), targetPixels, targetOffset + (row * targetStride), width);
        }
    }

    /** Resolves the dirty bounds after copy for the AFMA creator. */
    public static @Nullable AfmaRect findDirtyBoundsAfterCopy(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, @NotNull AfmaCopyRect copyRect) {
        return new AfmaFramePairAnalysis(previous, next).findDirtyBoundsAfterCopy(copyRect);
    }

    /** Applies the copy rect to the AFMA creator. */
    public static void applyCopyRect(@NotNull int[] pixels, int frameWidth, @NotNull AfmaCopyRect copyRect) {
        int startRow = 0;
        int endRow = copyRect.getHeight();
        int rowStep = 1;
        if (copyRect.getDstY() > copyRect.getSrcY()) {
            startRow = copyRect.getHeight() - 1;
            endRow = -1;
            rowStep = -1;
        }

        for (int row = startRow; row != endRow; row += rowStep) {
            int sourceOffset = ((copyRect.getSrcY() + row) * frameWidth) + copyRect.getSrcX();
            int targetOffset = ((copyRect.getDstY() + row) * frameWidth) + copyRect.getDstX();
            System.arraycopy(pixels, sourceOffset, pixels, targetOffset, copyRect.getWidth());
        }
    }

    /** Applies the copy rects to the AFMA creator. */
    public static void applyCopyRects(@NotNull int[] pixels, int frameWidth, @NotNull List<AfmaCopyRect> copyRects) {
        for (AfmaCopyRect copyRect : copyRects) {
            applyCopyRect(pixels, frameWidth, copyRect);
        }
    }

}
