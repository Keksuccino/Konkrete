package de.keksuccino.konkrete.util.resource;

/**
 * Bounds untrusted image and archive inputs before they can allocate excessive heap or native memory.
 *
 * @param maxImageWidth maximum decoded image width
 * @param maxImageHeight maximum decoded image height
 * @param maxImagePixels maximum width-times-height pixel count
 * @param maxFrameCount maximum combined main and intro frame count
 * @param maxArchiveEntries maximum number of non-directory archive entries
 * @param maxArchiveBytes maximum compressed/container bytes accepted from a stream
 * @param maxDecompressedBytes maximum declared archive total and individual decompressed payload size
 */
public record ResourceSafetyLimits(int maxImageWidth, int maxImageHeight, long maxImagePixels, int maxFrameCount, int maxArchiveEntries, long maxArchiveBytes, long maxDecompressedBytes) {

    /** Conservative defaults suitable for interactive client resources. */
    public static final ResourceSafetyLimits DEFAULT = new ResourceSafetyLimits(8192, 8192, 33_554_432L, 10_000, 50_000, 536_870_912L, 536_870_912L);

    /** Validates that every configured limit is positive. */
    public ResourceSafetyLimits {
        if (maxImageWidth <= 0 || maxImageHeight <= 0 || maxImagePixels <= 0L || maxFrameCount <= 0 || maxArchiveEntries <= 0 || maxArchiveBytes <= 0L || maxDecompressedBytes <= 0L) {
            throw new IllegalArgumentException("Resource safety limits must be positive");
        }
    }

    /** Rejects dimensions that exceed width, height, or pixel-count limits. */
    public void validateImageDimensions(int width, int height, String description) {
        long pixels = (long) width * (long) height;
        if (width <= 0 || height <= 0 || width > this.maxImageWidth || height > this.maxImageHeight || pixels > this.maxImagePixels) {
            throw new IllegalArgumentException(description + " dimensions exceed resource safety limits: " + width + "x" + height);
        }
    }

    /** Rejects a combined frame count above the configured limit. */
    public void validateFrameCount(long frameCount, String description) {
        if (frameCount < 0L || frameCount > this.maxFrameCount) throw new IllegalArgumentException(description + " frame count exceeds resource safety limits: " + frameCount);
    }
}
