package de.keksuccino.konkrete.util.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesAndAppendsUtf8TextUsingLegacyLineSemantics() throws Exception {
        File file = this.temporaryDirectory.resolve("text.txt").toFile();

        FileUtils.writeTextToFile(file, false, "Grüße", "こんにちは");
        FileUtils.writeTextToFile(file, true, "done");

        assertEquals("Grüße" + System.lineSeparator() + "こんにちは" + System.lineSeparator() + "done", Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void generatesAvailableFilenameAtNormalAndCollisionBoundaries() throws Exception {
        assertEquals("entry.txt", FileUtils.generateAvailableFilename(this.temporaryDirectory.toString(), "entry", ".txt"));
        Files.createFile(this.temporaryDirectory.resolve("entry.txt"));
        Files.createFile(this.temporaryDirectory.resolve("entry_1.txt"));

        assertEquals("entry_2.txt", FileUtils.generateAvailableFilename(this.temporaryDirectory.toString(), "entry", "txt"));
    }

    @Test
    void unpackZipRejectsEntriesOutsideDestination() throws Exception {
        Path archive = this.temporaryDirectory.resolve("malicious.zip");
        Path destination = this.temporaryDirectory.resolve("output");
        Path escaped = this.temporaryDirectory.resolve("escaped.txt");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new ZipEntry("../escaped.txt"));
            output.write("blocked".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        IOException exception = assertThrows(IOException.class, () -> FileUtils.unpackZip(archive.toString(), destination.toString()));

        assertAll(() -> assertTrue(exception.getMessage().contains("escapes output directory")), () -> assertFalse(Files.exists(escaped)));
    }

    @Test
    void readsUtf8LinesWithoutClosingBorrowedStream() throws Exception {
        TrackingInputStream stream = new TrackingInputStream("Grüße\nこんにちは\n".getBytes(StandardCharsets.UTF_8));

        List<String> lines = FileUtils.readTextLinesFrom(stream);

        assertAll(() -> assertEquals(List.of("Grüße", "こんにちは"), lines), () -> assertFalse(stream.closed));
    }

    @Test
    void returnsEmptyListForValidEmptyBorrowedStream() throws Exception {
        TrackingInputStream stream = new TrackingInputStream(new byte[0]);

        List<String> lines = FileUtils.readTextLinesFrom(stream);

        assertAll(() -> assertEquals(List.of(), lines), () -> assertFalse(stream.closed));
    }

    @Test
    void midReadFailurePublishesNoPartialLinesAndLeavesBorrowedStreamOpen() {
        byte[] readablePrefix = "first\nsecond\n".getBytes(StandardCharsets.UTF_8);
        byte[] completeContent = "first\nsecond\nunread\n".getBytes(StandardCharsets.UTF_8);
        FailingInputStream stream = new FailingInputStream(completeContent, readablePrefix.length);
        AtomicReference<List<String>> publishedLines = new AtomicReference<>();

        IOException exception = assertThrows(IOException.class, () -> publishedLines.set(FileUtils.readTextLinesFrom(stream)));

        assertAll(() -> assertEquals("simulated read failure", exception.getMessage()), () -> assertNull(publishedLines.get()), () -> assertFalse(stream.closed));
    }

    @Test
    void fileReadDistinguishesValidEmptyFileFromOpenFailure() throws Exception {
        File emptyFile = Files.createFile(this.temporaryDirectory.resolve("empty.txt")).toFile();
        File missingFile = this.temporaryDirectory.resolve("missing.txt").toFile();

        assertAll(() -> assertEquals(List.of(), FileUtils.readTextLinesFrom(emptyFile)), () -> assertThrows(IOException.class, () -> FileUtils.readTextLinesFrom(missingFile)));
    }

    @Test
    void fileReadClosesItsOwnedStreamAfterSuccess() throws Exception {
        TrackingInputStream stream = new TrackingInputStream("complete\n".getBytes(StandardCharsets.UTF_8));

        List<String> lines = FileUtils.readTextLinesFrom(new File("unused"), file -> stream);

        assertAll(() -> assertEquals(List.of("complete"), lines), () -> assertTrue(stream.closed));
    }

    @Test
    void fileReadClosesItsOwnedStreamAfterReadFailure() {
        byte[] readablePrefix = "partial\n".getBytes(StandardCharsets.UTF_8);
        FailingInputStream stream = new FailingInputStream("partial\nunread\n".getBytes(StandardCharsets.UTF_8), readablePrefix.length);
        AtomicReference<List<String>> publishedLines = new AtomicReference<>();

        assertThrows(IOException.class, () -> publishedLines.set(FileUtils.readTextLinesFrom(new File("unused"), file -> stream)));

        assertAll(() -> assertNull(publishedLines.get()), () -> assertTrue(stream.closed));
    }

    @Test
    void fileReadPropagatesOwnedStreamCloseFailureWithoutPublishingLines() {
        CloseFailingInputStream stream = new CloseFailingInputStream("complete\n".getBytes(StandardCharsets.UTF_8));
        AtomicReference<List<String>> publishedLines = new AtomicReference<>();

        IOException exception = assertThrows(IOException.class, () -> publishedLines.set(FileUtils.readTextLinesFrom(new File("unused"), file -> stream)));

        assertAll(() -> assertEquals("simulated close failure", exception.getMessage()), () -> assertNull(publishedLines.get()), () -> assertTrue(stream.closeAttempted));
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private TrackingInputStream(byte[] content) {
            super(content);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }

    }

    private static final class FailingInputStream extends InputStream {

        private final byte[] content;
        private final int failurePosition;
        private int position;
        private boolean closed;

        private FailingInputStream(byte[] content, int failurePosition) {
            this.content = content;
            this.failurePosition = failurePosition;
        }

        @Override
        public int read() throws IOException {
            if (this.position >= this.failurePosition) throw new IOException("simulated read failure");
            return this.content[this.position++] & 0xFF;
        }

        @Override
        public int read(byte[] target, int offset, int length) throws IOException {
            if (this.position >= this.failurePosition) throw new IOException("simulated read failure");
            int count = Math.min(length, this.failurePosition - this.position);
            System.arraycopy(this.content, this.position, target, offset, count);
            this.position += count;
            return count;
        }

        @Override
        public void close() {
            this.closed = true;
        }

    }

    private static final class CloseFailingInputStream extends ByteArrayInputStream {

        private boolean closeAttempted;

        private CloseFailingInputStream(byte[] content) {
            super(content);
        }

        @Override
        public void close() throws IOException {
            this.closeAttempted = true;
            throw new IOException("simulated close failure");
        }

    }

}
