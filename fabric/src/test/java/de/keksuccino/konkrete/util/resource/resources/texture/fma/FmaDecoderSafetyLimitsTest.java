package de.keksuccino.konkrete.util.resource.resources.texture.fma;

import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.ResourceSafetyLimits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FmaDecoderSafetyLimitsTest {

    @TempDir
    Path temporaryDirectory;

    private ResourceSafetyLimits previousLimits;

    @BeforeEach
    void rememberLimits() {
        this.previousLimits = ResourceRuntime.getSafetyLimits();
    }

    @AfterEach
    void restoreLimits() {
        ResourceRuntime.setSafetyLimits(this.previousLimits);
    }

    @Test
    void rejectsOversizedFrameDimensionsBeforeImageAllocation() throws Exception {
        Path archive = this.writeArchive(new FrameSpec("frames/0.png", 17, 1));
        ResourceRuntime.setSafetyLimits(new ResourceSafetyLimits(16, 16, 256L, 4, 8, 1_000_000L, 1_000_000L));

        try (FmaDecoder decoder = new FmaDecoder()) {
            assertThrows(IOException.class, () -> decoder.read(archive.toFile()));
        }
    }

    @Test
    void rejectsFrameAndEntryCountsAtConfiguredBoundaries() throws Exception {
        Path tooManyFrames = this.writeArchive(new FrameSpec("frames/0.png", 1, 1), new FrameSpec("frames/1.png", 1, 1));
        ResourceRuntime.setSafetyLimits(new ResourceSafetyLimits(16, 16, 256L, 1, 8, 1_000_000L, 1_000_000L));
        try (FmaDecoder decoder = new FmaDecoder()) {
            assertThrows(IOException.class, () -> decoder.read(tooManyFrames.toFile()));
        }

        Path tooManyEntries = this.writeArchive(new FrameSpec("frames/0.png", 1, 1), new FrameSpec("ignored.bin", 1, 1));
        ResourceRuntime.setSafetyLimits(new ResourceSafetyLimits(16, 16, 256L, 4, 2, 1_000_000L, 1_000_000L));
        try (FmaDecoder decoder = new FmaDecoder()) {
            assertThrows(IOException.class, () -> decoder.read(tooManyEntries.toFile()));
        }
    }

    private Path writeArchive(FrameSpec... frames) throws IOException {
        Path archive = this.temporaryDirectory.resolve("test-" + System.nanoTime() + ".fma");
        try (OutputStream fileOut = Files.newOutputStream(archive); ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
            putEntry(zipOut, "metadata.json", "{}".getBytes());
            for (FrameSpec frame : frames) putEntry(zipOut, frame.path(), pngHeader(frame.width(), frame.height()));
        }
        return archive;
    }

    private static void putEntry(ZipOutputStream zipOut, String path, byte[] bytes) throws IOException {
        zipOut.putNextEntry(new ZipEntry(path));
        zipOut.write(bytes);
        zipOut.closeEntry();
    }

    private static byte[] pngHeader(int width, int height) {
        byte[] header = new byte[33];
        byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        System.arraycopy(signature, 0, header, 0, signature.length);
        header[11] = 13;
        header[12] = 'I';
        header[13] = 'H';
        header[14] = 'D';
        header[15] = 'R';
        writeInt(header, 16, width);
        writeInt(header, 20, height);
        header[24] = 8;
        header[25] = 6;
        return header;
    }

    private static void writeInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    private record FrameSpec(String path, int width, int height) {}
}
