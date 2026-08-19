package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Objects;

/** Validates and normalizes source images to a common bounded AFMA frame size. */
public class AfmaFrameNormalizer {

    /** Loads the frame for the AFMA creator. */
    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file) throws IOException {
        return this.loadFrame(file, null);
    }

    /** Loads the frame for the AFMA creator. */
    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file, @Nullable AfmaFastPixelBufferPool pixelBufferPool) throws IOException {
        Objects.requireNonNull(file);
        DecodedImage decodedImage = this.decodeFrame(file);
        int[] pixels = AfmaPixelFrameHelper.allocatePixels(decodedImage.width(), decodedImage.height(), pixelBufferPool);
        System.arraycopy(decodedImage.pixels(), 0, pixels, 0, decodedImage.pixels().length);
        return new AfmaPixelFrame(decodedImage.width(), decodedImage.height(), pixels, pixelBufferPool);
    }

    /** Loads the frame for the AFMA creator. */
    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file, int expectedWidth, int expectedHeight) throws IOException {
        return this.loadFrame(file, expectedWidth, expectedHeight, null);
    }

    /** Loads the frame for the AFMA creator. */
    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file, int expectedWidth, int expectedHeight,
                                    @Nullable AfmaFastPixelBufferPool pixelBufferPool) throws IOException {
        this.validateFrameDimensions(expectedWidth, expectedHeight, file);
        int[] pixels = AfmaPixelFrameHelper.allocatePixels(expectedWidth, expectedHeight, pixelBufferPool);
        try {
            this.loadFrameInto(file, expectedWidth, expectedHeight, pixels);
            return new AfmaPixelFrame(expectedWidth, expectedHeight, pixels, pixelBufferPool);
        } catch (IOException | RuntimeException | Error throwable) {
            if (pixelBufferPool != null) {
                pixelBufferPool.releasePixels(pixels);
            }
            throw throwable;
        }
    }

    /** Extracts the patch from the AFMA creator data. */
    @NotNull
    public AfmaPixelFrame extractPatch(@NotNull AfmaPixelFrame image, int x, int y, int width, int height) {
        return this.extractPatch(image, x, y, width, height, null);
    }

    /** Extracts the patch from the AFMA creator data. */
    @NotNull
    public AfmaPixelFrame extractPatch(@NotNull AfmaPixelFrame image, int x, int y, int width, int height,
                                       @Nullable AfmaFastPixelBufferPool pixelBufferPool) {
        return AfmaPixelFrameHelper.crop(image, x, y, width, height, pixelBufferPool);
    }

    /** Decodes a source frame into the supplied pixel array after exact dimension validation. */
    public void loadFrameInto(@NotNull File file, int expectedWidth, int expectedHeight, @NotNull int[] targetPixels) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(targetPixels);
        int expectedPixelCount = AfmaPixelFrameHelper.pixelCount(expectedWidth, expectedHeight);
        if (targetPixels.length < expectedPixelCount) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the expected frame size");
        }
        DecodedImage decodedImage = this.decodeFrame(file);
        if ((decodedImage.width() != expectedWidth) || (decodedImage.height() != expectedHeight)) {
            throw new IOException("AFMA source frame dimensions do not match the expected canvas size: " + file.getAbsolutePath());
        }
        System.arraycopy(decodedImage.pixels(), 0, targetPixels, 0, expectedPixelCount);
    }

    /** Decodes the frame for the AFMA creator. */
    @NotNull
    protected DecodedImage decodeFrame(@NotNull File file) throws IOException {
        try {
            return this.decodeFrameWithStb(file);
        } catch (IOException | LinkageError ex) {
            try {
                return this.decodeFrameWithImageIo(file);
            } catch (IOException imageIoEx) {
                imageIoEx.addSuppressed(ex);
                throw imageIoEx;
            }
        }
    }

    /** Decodes the frame with stb for the AFMA creator. */
    @NotNull
    protected DecodedImage decodeFrameWithStb(@NotNull File file) throws IOException {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer widthBuffer = memoryStack.mallocInt(1);
            IntBuffer heightBuffer = memoryStack.mallocInt(1);
            IntBuffer componentBuffer = memoryStack.mallocInt(1);
            if (!STBImage.stbi_info(file.getAbsolutePath(), widthBuffer, heightBuffer, componentBuffer)) {
                throw new IOException("Failed to inspect AFMA source frame dimensions: " + file.getAbsolutePath());
            }
            this.validateFrameDimensions(widthBuffer.get(0), heightBuffer.get(0), file);
            ByteBuffer decodedBuffer = STBImage.stbi_load(
                    file.getAbsolutePath(),
                    widthBuffer,
                    heightBuffer,
                    componentBuffer,
                    STBImage.STBI_rgb_alpha
            );
            if (decodedBuffer == null) {
                String failureReason = STBImage.stbi_failure_reason();
                throw new IOException("Failed to decode AFMA source frame: " + file.getAbsolutePath() + " (" + failureReason + ")");
            }

            try {
                int width = widthBuffer.get(0);
                int height = heightBuffer.get(0);
                this.validateFrameDimensions(width, height, file);
                int[] pixels = new int[AfmaPixelFrameHelper.pixelCount(width, height)];
                this.decodeFrameToPixels(decodedBuffer, width, height, pixels, file);
                return new DecodedImage(width, height, pixels);
            } finally {
                STBImage.stbi_image_free(decodedBuffer);
            }
        }
    }

    /** Decodes the frame with image io for the AFMA creator. */
    @NotNull
    protected DecodedImage decodeFrameWithImageIo(@NotNull File file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (input == null) throw new IOException("Failed to open AFMA source frame: " + file.getAbsolutePath());
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Failed to identify AFMA source frame: " + file.getAbsolutePath());
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                this.validateFrameDimensions(width, height, file);
                BufferedImage image = reader.read(0);
                if (image == null) throw new IOException("Failed to decode AFMA source frame: " + file.getAbsolutePath());
                this.validateFrameDimensions(image.getWidth(), image.getHeight(), file);
                int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
                return new DecodedImage(width, height, pixels);
            } finally {
                reader.dispose();
            }
        }
    }

    /** Validates source dimensions before allocating native or heap-backed frame storage. */
    protected void validateFrameDimensions(int width, int height, @NotNull File file) throws IOException {
        try {
            ResourceRuntime.getSafetyLimits().validateImageDimensions(width, height, "AFMA source frame " + file.getAbsolutePath());
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    /** Decodes the frame to pixels for the AFMA creator. */
    protected void decodeFrameToPixels(@NotNull ByteBuffer decodedBuffer, int width, int height,
                                       @NotNull int[] targetPixels, @NotNull File sourceFile) throws IOException {
        int pixelCount = AfmaPixelFrameHelper.pixelCount(width, height);
        if (targetPixels.length < pixelCount) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the decoded frame");
        }

        IntBuffer rgbaPixels = decodedBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        if (rgbaPixels.remaining() < pixelCount) {
            throw new IOException("Decoded AFMA source frame is shorter than expected: " + sourceFile.getAbsolutePath());
        }

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            int rgbaColor = rgbaPixels.get(pixelIndex);
            targetPixels[pixelIndex] = (rgbaColor & 0xFF00FF00)
                    | ((rgbaColor & 0x00FF0000) >>> 16)
                    | ((rgbaColor & 0x000000FF) << 16);
        }
    }

    /** Carries {@code DecodedImage} data between validated stages of the AFMA creator. */
    protected record DecodedImage(int width, int height, @NotNull int[] pixels) {
    }

}
