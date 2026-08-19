package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.zip.Deflater;

/** Carries {@code AfmaStoredPayload} data between validated stages of the AFMA codec. */
public final class AfmaStoredPayload implements AutoCloseable {

    /** Byte budget for analysis buffer in the AFMA codec. */
    protected static final int ANALYSIS_BUFFER_BYTES = 8192;
    private static final File TEMP_DIR = AfmaIoHelper.createNamedTempDirectory("encoded_afma_payloads");
    /** Byte budget for inline memory payload in the AFMA codec. */
    protected static final int INLINE_MEMORY_PAYLOAD_BYTES = 128 * 1024;
    /** Lower bound for heap budget bytes accepted by the AFMA codec. */
    protected static final long MIN_HEAP_BUDGET_BYTES = 16L * 1024L * 1024L;
    /** Upper bound for heap budget bytes enforced by the AFMA codec. */
    protected static final long MAX_HEAP_BUDGET_BYTES = 128L * 1024L * 1024L;
    @NotNull
    private static final PayloadStore PAYLOAD_STORE = new PayloadStore(INLINE_MEMORY_PAYLOAD_BYTES, computeHeapBudgetBytes());

    @NotNull
    private final PayloadStorage storage;
    private final int length;
    private final long estimatedArchiveBytes;
    @NotNull
    private final String fingerprint;
    @NotNull
    private final byte[] tailBytes;
    private boolean closed = false;

    private AfmaStoredPayload(@NotNull PayloadStorage storage, int length, long estimatedArchiveBytes,
                              @NotNull String fingerprint, @NotNull byte[] tailBytes) {
        this.storage = Objects.requireNonNull(storage);
        this.length = Math.max(0, length);
        this.estimatedArchiveBytes = Math.max(0L, estimatedArchiveBytes);
        this.fingerprint = Objects.requireNonNull(fingerprint);
        this.tailBytes = Objects.requireNonNull(tailBytes);
    }

    /** Builds the bytes for the AFMA codec. */
    @NotNull
    public static AfmaStoredPayload fromBytes(@NotNull byte[] payloadBytes) throws IOException {
        Objects.requireNonNull(payloadBytes);
        return new AfmaStoredPayload(
                PAYLOAD_STORE.storeBytes(payloadBytes.clone()),
                payloadBytes.length,
                AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes),
                AfmaPayloadMetricsHelper.fingerprintPayload(payloadBytes),
                tailBytes(payloadBytes)
        );
    }

    /** Builds the bytes for the AFMA codec. */
    @NotNull
    public static AfmaStoredPayload fromBytes(@NotNull PayloadSummary payloadSummary, @NotNull byte[] payloadBytes) throws IOException {
        Objects.requireNonNull(payloadSummary);
        Objects.requireNonNull(payloadBytes);
        if (payloadSummary.length() != payloadBytes.length) {
            throw new IOException("AFMA payload summary length does not match the provided payload bytes");
        }

        return new AfmaStoredPayload(
                PAYLOAD_STORE.storeBytes(payloadBytes),
                payloadSummary.length(),
                payloadSummary.estimatedArchiveBytes(),
                payloadSummary.fingerprint(),
                payloadSummary.tailBytes()
        );
    }

    /** Summarizes resource state for the AFMA codec. */
    @NotNull
    public static PayloadSummary summarize(@NotNull byte[] payloadBytes) {
        Objects.requireNonNull(payloadBytes);
        return new PayloadSummary(
                payloadBytes.length,
                AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes),
                AfmaPayloadMetricsHelper.fingerprintPayload(payloadBytes),
                tailBytes(payloadBytes)
        );
    }

    /** Writes resource data to the AFMA codec output. */
    @NotNull
    public static AfmaStoredPayload write(@NotNull Writer writer) throws IOException {
        Objects.requireNonNull(writer);
        SpillablePayloadOutputStream payloadOut = new SpillablePayloadOutputStream(PAYLOAD_STORE);
        boolean success = false;
        try (PayloadAnalysisOutputStream out = new PayloadAnalysisOutputStream(payloadOut)) {
            writer.write(out);
            PayloadAnalysis analysis = out.finishAnalysis();
            success = true;
            return new AfmaStoredPayload(
                    payloadOut.finishPayload(),
                    analysis.length(),
                    analysis.estimatedArchiveBytes(),
                    analysis.fingerprint(),
                    analysis.tailBytes()
            );
        } finally {
            if (!success) {
                payloadOut.abort();
            }
        }
    }

    /** Summarizes resource state for the AFMA codec. */
    @NotNull
    public static PayloadSummary summarize(@NotNull Writer writer) throws IOException {
        Objects.requireNonNull(writer);
        try (PayloadAnalysisOutputStream out = new PayloadAnalysisOutputStream(OutputStream.nullOutputStream())) {
            writer.write(out);
            PayloadAnalysis analysis = out.finishAnalysis();
            return new PayloadSummary(
                    analysis.length(),
                    analysis.estimatedArchiveBytes(),
                    analysis.fingerprint(),
                    analysis.tailBytes()
            );
        }
    }

    /** Captures the capture for the AFMA codec. */
    @NotNull
    public static BufferedPayload capture(@NotNull Writer writer) throws IOException {
        Objects.requireNonNull(writer);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (PayloadAnalysisOutputStream out = new PayloadAnalysisOutputStream(byteOut)) {
            writer.write(out);
            PayloadAnalysis analysis = out.finishAnalysis();
            return new BufferedPayload(
                    byteOut.toByteArray(),
                    new PayloadSummary(
                            analysis.length(),
                            analysis.estimatedArchiveBytes(),
                            analysis.fingerprint(),
                            analysis.tailBytes()
                    )
            );
        }
    }

    /** Returns the length used by the AFMA codec. */
    public int length() {
        this.ensureOpen();
        return this.length;
    }

    /** Returns whether empty. */
    public boolean isEmpty() {
        return this.length() <= 0;
    }

    /** Returns the estimated compressed archive size in bytes. */
    public long estimatedArchiveBytes() {
        this.ensureOpen();
        return this.estimatedArchiveBytes;
    }

    /** Computes a stable fingerprint for resource state in the AFMA codec. */
    @NotNull
    public String fingerprint() {
        this.ensureOpen();
        return this.fingerprint;
    }

    /** Summarizes resource state for the AFMA codec. */
    @NotNull
    public PayloadSummary summarize() {
        this.ensureOpen();
        return new PayloadSummary(this.length, this.estimatedArchiveBytes, this.fingerprint, this.tailBytes);
    }

    /** Opens the stream for the AFMA codec. */
    @NotNull
    public InputStream openStream() throws IOException {
        this.ensureOpen();
        return this.storage.openStream();
    }

    /** Reads and returns a copy of the complete stored payload. */
    @NotNull
    public byte[] readAllBytes() throws IOException {
        this.ensureOpen();
        return this.storage.readAllBytes();
    }

    /** Writes this payload to the supplied output without transferring output ownership. */
    public void writeTo(@NotNull OutputStream out) throws IOException {
        Objects.requireNonNull(out);
        this.ensureOpen();
        this.storage.writeTo(out);
    }

    /** Returns the tail bytes produced by the AFMA codec. */
    @NotNull
    protected byte[] tailBytes() {
        this.ensureOpen();
        return this.tailBytes.clone();
    }

    /** Returns the tail bytes unsafe produced by the AFMA codec. */
    @NotNull
    protected byte[] tailBytesUnsafe() {
        this.ensureOpen();
        return this.tailBytes;
    }

    /** Returns the temp file produced by the AFMA codec. */
    @NotNull
    protected File tempFile() {
        this.ensureOpen();
        return this.storage.requireFileBacked();
    }

    /** Releases the backing heap bytes or spool segment; repeated calls are harmless. */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.storage.release();
    }

    /** Ensures the open is valid for the AFMA codec. */
    protected void ensureOpen() {
        if (this.closed) {
            throw new IllegalStateException("AFMA stored payload has already been closed");
        }
    }

    /** Builds the temp file for the AFMA codec. */
    @NotNull
    protected static File createTempFile() throws IOException {
        return File.createTempFile("afma_payload_", ".bin", TEMP_DIR);
    }

    /** Copies the stream for the AFMA codec. */
    protected static void copyStream(@NotNull InputStream in, @NotNull OutputStream out) throws IOException {
        in.transferTo(out);
    }

    /** Closes a nullable handle and suppresses any exception. */
    protected static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    /** Attempts to delete a nullable temporary file and suppresses any failure. */
    protected static void deleteQuietly(@Nullable File file) {
        if (file == null) {
            return;
        }
        try {
            file.delete();
        } catch (Exception ignored) {
        }
    }

    /** Calculates the heap budget bytes for the AFMA codec. */
    protected static long computeHeapBudgetBytes() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory <= 0L || maxMemory == Long.MAX_VALUE) {
            return MAX_HEAP_BUDGET_BYTES;
        }

        long targetBudget = maxMemory / 8L;
        targetBudget = Math.max(MIN_HEAP_BUDGET_BYTES, targetBudget);
        return Math.min(MAX_HEAP_BUDGET_BYTES, targetBudget);
    }

    /** Returns the tail bytes produced by the AFMA codec. */
    @NotNull
    protected static byte[] tailBytes(@NotNull byte[] payloadBytes) {
        Objects.requireNonNull(payloadBytes);
        int tailLength = Math.min(payloadBytes.length, AfmaChunkedPayloadHelper.PLANNER_DEFLATE_TAIL_BYTES);
        return Arrays.copyOfRange(payloadBytes, payloadBytes.length - tailLength, payloadBytes.length);
    }

    /** Returns the hex digest produced by the AFMA codec. */
    @NotNull
    protected static String hexDigest(@NotNull byte[] digestBytes) {
        StringBuilder builder = new StringBuilder(digestBytes.length * 2);
        for (byte digestByte : digestBytes) {
            builder.append(Character.forDigit((digestByte >>> 4) & 0xF, 16));
            builder.append(Character.forDigit(digestByte & 0xF, 16));
        }
        return builder.toString();
    }

    /** Writes {@code Writer} output for the AFMA codec. */
    @FunctionalInterface
    public interface Writer {
        /** Writes one complete stored payload. */
        void write(@NotNull OutputStream out) throws IOException;
    }

    /** Models {@code PayloadSummary} state used by the AFMA codec. */
    public record PayloadSummary(int length, long estimatedArchiveBytes, @NotNull String fingerprint, @NotNull byte[] tailBytes) {

        /** Validates constructor components before storing this {@code PayloadSummary}. */
        public PayloadSummary {
            fingerprint = Objects.requireNonNull(fingerprint);
            tailBytes = Objects.requireNonNull(tailBytes).clone();
        }

        /** Returns the tail bytes produced by the AFMA codec. */
        @Override
        @NotNull
        public byte[] tailBytes() {
            return this.tailBytes.clone();
        }
    }

    /** Carries {@code BufferedPayload} data between validated stages of the AFMA codec. */
    public record BufferedPayload(@NotNull byte[] payloadBytes, @NotNull PayloadSummary payloadSummary) {

        /** Validates constructor components before storing this {@code BufferedPayload}. */
        public BufferedPayload {
            payloadBytes = Objects.requireNonNull(payloadBytes);
            payloadSummary = Objects.requireNonNull(payloadSummary);
        }
    }

    /** Reports {@code PayloadAnalysis} measurements produced by the AFMA codec. */
    protected record PayloadAnalysis(int length, long estimatedArchiveBytes, @NotNull String fingerprint, @NotNull byte[] tailBytes) {
    }

    // Capture payload metrics while bytes are being written so the encoder does not need a second temp-file read.
    /** Models {@code PayloadAnalysisOutputStream} state used by the AFMA codec. */
    protected static final class PayloadAnalysisOutputStream extends OutputStream {

        /** Holds the delegate handle whose lifecycle follows this AFMA codec instance. */
        @NotNull
        protected final OutputStream delegate;
        /** Current digest state for this AFMA codec instance. */
        @NotNull
        protected final MessageDigest digest;
        /** Current deflater state for this AFMA codec instance. */
        @NotNull
        protected final Deflater deflater = new Deflater(AfmaChunkedPayloadHelper.PLANNER_DEFLATE_LEVEL, true);
        /** Holds the analysisBuffer collection used by this AFMA codec instance. */
        @NotNull
        protected final byte[] analysisBuffer = new byte[ANALYSIS_BUFFER_BYTES];
        /** Holds the deflateBuffer collection used by this AFMA codec instance. */
        @NotNull
        protected final byte[] deflateBuffer = new byte[ANALYSIS_BUFFER_BYTES];
        /** Current tail accumulator state for this AFMA codec instance. */
        @NotNull
        protected final TailAccumulator tailAccumulator = new TailAccumulator(AfmaChunkedPayloadHelper.PLANNER_DEFLATE_TAIL_BYTES);
        /** Current analysis buffer length measured or selected by this AFMA codec instance. */
        protected int analysisBufferLength = 0;
        /** Current length state for this AFMA codec instance. */
        protected int length = 0;
        /** Current compressed bytes measured or selected by this AFMA codec instance. */
        protected long compressedBytes = 0L;
        /** Whether closed currently applies to this AFMA codec instance. */
        protected boolean closed = false;
        /** Current finished analysis state for this AFMA codec instance. */
        @Nullable
        protected PayloadAnalysis finishedAnalysis = null;

        /** Initializes a new {@code PayloadAnalysisOutputStream} for AFMA codec use. */
        protected PayloadAnalysisOutputStream(@NotNull OutputStream delegate) throws IOException {
            this.delegate = Objects.requireNonNull(delegate);
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new IOException("Failed to create AFMA payload fingerprint digest", ex);
            }
        }

        /** Writes resource data to the AFMA codec output. */
        @Override
        public void write(int value) throws IOException {
            this.ensureWritable(1);
            this.delegate.write(value);
            this.length++;
            this.analysisBuffer[this.analysisBufferLength++] = (byte) value;
            if (this.analysisBufferLength >= this.analysisBuffer.length) {
                this.flushAnalysisBuffer();
            }
        }

        /** Writes resource data to the AFMA codec output. */
        @Override
        public void write(@NotNull byte[] source, int offset, int count) throws IOException {
            Objects.requireNonNull(source);
            if (offset < 0 || count < 0 || count > source.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (count <= 0) {
                return;
            }

            this.ensureWritable(count);
            this.delegate.write(source, offset, count);
            this.length += count;
            this.bufferAnalysis(source, offset, count);
        }

        /** Flushes resource state from the AFMA codec buffer. */
        @Override
        public void flush() throws IOException {
            this.delegate.flush();
        }

        /** Closes the destination stream and always releases the native deflater; repeated calls are harmless. */
        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            try {
                this.delegate.close();
            } finally {
                this.deflater.end();
            }
        }

        /** Finishes the analysis lifecycle for the AFMA codec. */
        @NotNull
        public PayloadAnalysis finishAnalysis() throws IOException {
            if (this.finishedAnalysis != null) {
                return this.finishedAnalysis;
            }
            if (this.closed) {
                throw new IOException("AFMA payload output has already been closed");
            }

            this.flushAnalysisBuffer();
            this.delegate.flush();
            this.deflater.finish();
            while (!this.deflater.finished()) {
                this.compressedBytes += this.deflater.deflate(this.deflateBuffer);
            }

            this.finishedAnalysis = new PayloadAnalysis(
                    this.length,
                    Math.max(1L, this.compressedBytes),
                    hexDigest(this.digest.digest()),
                    this.tailAccumulator.toByteArray()
            );
            return this.finishedAnalysis;
        }

        /** Ensures the writable is valid for the AFMA codec. */
        protected void ensureWritable(int additionalBytes) throws IOException {
            if (this.closed) {
                throw new IOException("AFMA payload output has already been closed");
            }
            if (this.finishedAnalysis != null) {
                throw new IOException("AFMA payload output analysis has already been finalized");
            }
            if (additionalBytes > Integer.MAX_VALUE - this.length) {
                throw new IOException("AFMA payload exceeds the supported size limit: " + (((long) this.length) + additionalBytes));
            }
        }

        /** Updates the buffer analysis state in the AFMA codec. */
        protected void bufferAnalysis(@NotNull byte[] source, int offset, int count) {
            int remaining = count;
            int nextOffset = offset;
            if (this.analysisBufferLength > 0) {
                int bytesToCopy = Math.min(remaining, this.analysisBuffer.length - this.analysisBufferLength);
                System.arraycopy(source, nextOffset, this.analysisBuffer, this.analysisBufferLength, bytesToCopy);
                this.analysisBufferLength += bytesToCopy;
                nextOffset += bytesToCopy;
                remaining -= bytesToCopy;
                if (this.analysisBufferLength >= this.analysisBuffer.length) {
                    this.flushAnalysisBuffer();
                }
            }

            if (remaining >= this.analysisBuffer.length) {
                this.analyzeChunk(source, nextOffset, remaining);
                return;
            }

            if (remaining > 0) {
                System.arraycopy(source, nextOffset, this.analysisBuffer, this.analysisBufferLength, remaining);
                this.analysisBufferLength += remaining;
            }
        }

        /** Flushes the analysis buffer from the AFMA codec buffer. */
        protected void flushAnalysisBuffer() {
            if (this.analysisBufferLength <= 0) {
                return;
            }
            this.analyzeChunk(this.analysisBuffer, 0, this.analysisBufferLength);
            this.analysisBufferLength = 0;
        }

        /** Analyzes the chunk for the AFMA codec. */
        protected void analyzeChunk(@NotNull byte[] source, int offset, int count) {
            this.digest.update(source, offset, count);
            this.tailAccumulator.append(source, offset, count);
            this.deflater.setInput(source, offset, count);
            while (!this.deflater.needsInput()) {
                this.compressedBytes += this.deflater.deflate(this.deflateBuffer);
            }
        }
    }

    /** Models {@code SpillablePayloadOutputStream} state used by the AFMA codec. */
    protected static final class SpillablePayloadOutputStream extends OutputStream {

        /** Owned payload store state for this AFMA codec instance. */
        @NotNull
        protected final PayloadStore payloadStore;
        /** Holds the heapBuffer collection used by this AFMA codec instance. */
        @NotNull
        protected byte[] heapBuffer = new byte[Math.min(8192, INLINE_MEMORY_PAYLOAD_BYTES)];
        /** Current heap length measured or selected by this AFMA codec instance. */
        protected int heapLength = 0;
        /** Current spool session state for this AFMA codec instance. */
        @Nullable
        protected SpoolAppendSession spoolSession = null;
        /** Owned finished payload state for this AFMA codec instance. */
        @Nullable
        protected PayloadStorage finishedPayload = null;
        /** Whether closed currently applies to this AFMA codec instance. */
        protected boolean closed = false;

        /** Initializes a new {@code SpillablePayloadOutputStream} for AFMA codec use. */
        protected SpillablePayloadOutputStream(@NotNull PayloadStore payloadStore) {
            this.payloadStore = Objects.requireNonNull(payloadStore);
        }

        /** Writes resource data to the AFMA codec output. */
        @Override
        public void write(int value) throws IOException {
            this.ensureOpen();
            if (this.spoolSession != null) {
                this.spoolSession.write(value);
                return;
            }

            if ((this.heapLength + 1) > INLINE_MEMORY_PAYLOAD_BYTES) {
                this.spillHeapBuffer();
                this.spoolSession.write(value);
                return;
            }

            this.ensureHeapCapacity(1);
            this.heapBuffer[this.heapLength++] = (byte) value;
        }

        /** Writes resource data to the AFMA codec output. */
        @Override
        public void write(@NotNull byte[] source, int offset, int count) throws IOException {
            Objects.requireNonNull(source);
            if (offset < 0 || count < 0 || count > source.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (count <= 0) {
                return;
            }

            this.ensureOpen();
            if (this.spoolSession != null) {
                this.spoolSession.write(source, offset, count);
                return;
            }

            if ((this.heapLength + count) > INLINE_MEMORY_PAYLOAD_BYTES) {
                this.spillHeapBuffer();
                this.spoolSession.write(source, offset, count);
                return;
            }

            this.ensureHeapCapacity(count);
            System.arraycopy(source, offset, this.heapBuffer, this.heapLength, count);
            this.heapLength += count;
        }

        /** Aborts an unfinished spool write while preserving storage already returned by {@link #finishPayload()}. */
        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.finishedPayload == null) {
                this.abort();
            }
        }

        /** Finishes the payload lifecycle for the AFMA codec. */
        @NotNull
        public PayloadStorage finishPayload() throws IOException {
            this.ensureOpen();
            if (this.finishedPayload != null) {
                return this.finishedPayload;
            }

            PayloadStorage payload;
            if (this.spoolSession != null) {
                payload = this.payloadStore.storeSegment(this.spoolSession.finish());
                this.spoolSession = null;
            } else {
                payload = this.payloadStore.storeBytes(Arrays.copyOf(this.heapBuffer, this.heapLength));
                this.heapLength = 0;
            }

            this.finishedPayload = payload;
            return payload;
        }

        /** Aborts resource state and releases its AFMA codec state. */
        public void abort() {
            if (this.finishedPayload != null) {
                return;
            }
            if (this.spoolSession != null) {
                this.spoolSession.abort();
                this.spoolSession = null;
            }
            this.heapLength = 0;
        }

        /** Ensures the heap capacity is valid for the AFMA codec. */
        protected void ensureHeapCapacity(int additionalBytes) {
            int requiredLength = this.heapLength + additionalBytes;
            if (requiredLength <= this.heapBuffer.length) {
                return;
            }

            int nextLength = this.heapBuffer.length;
            while (nextLength < requiredLength) {
                nextLength = Math.min(INLINE_MEMORY_PAYLOAD_BYTES, Math.max(nextLength << 1, requiredLength));
            }
            this.heapBuffer = Arrays.copyOf(this.heapBuffer, nextLength);
        }

        /** Spills the heap buffer to bounded AFMA codec storage. */
        protected void spillHeapBuffer() throws IOException {
            if (this.spoolSession == null) {
                this.spoolSession = this.payloadStore.beginAppendSession();
            }
            if (this.heapLength > 0) {
                this.spoolSession.write(this.heapBuffer, 0, this.heapLength);
                this.heapLength = 0;
            }
        }

        /** Ensures the open is valid for the AFMA codec. */
        protected void ensureOpen() throws IOException {
            if (this.closed) {
                throw new IOException("AFMA spillable payload output has already been closed");
            }
        }
    }

    /** Retains or reuses {@code TailAccumulator} state while the AFMA codec is active. */
    protected static final class TailAccumulator {

        /** Holds the buffer collection used by this AFMA codec instance. */
        @NotNull
        protected final byte[] buffer;
        /** Current length state for this AFMA codec instance. */
        protected int length = 0;

        /** Initializes a new {@code TailAccumulator} for AFMA codec use. */
        protected TailAccumulator(int maxBytes) {
            this.buffer = new byte[Math.max(0, maxBytes)];
        }

        /** Appends resource state to the AFMA codec output state. */
        public void append(@NotNull byte[] source, int offset, int count) {
            if (this.buffer.length <= 0 || count <= 0) {
                return;
            }

            if (count >= this.buffer.length) {
                System.arraycopy(source, offset + count - this.buffer.length, this.buffer, 0, this.buffer.length);
                this.length = this.buffer.length;
                return;
            }

            int nextLength = this.length + count;
            if (nextLength > this.buffer.length) {
                int bytesToDiscard = nextLength - this.buffer.length;
                System.arraycopy(this.buffer, bytesToDiscard, this.buffer, 0, this.length - bytesToDiscard);
                this.length -= bytesToDiscard;
            }
            System.arraycopy(source, offset, this.buffer, this.length, count);
            this.length += count;
        }

        /** Converts the byte array from the AFMA codec representation. */
        @NotNull
        public byte[] toByteArray() {
            if (this.length <= 0) {
                return new byte[0];
            }
            return Arrays.copyOf(this.buffer, this.length);
        }

    }

    /** Models {@code PayloadStorage} state used by the AFMA codec. */
    protected static final class PayloadStorage {

        /** Owned payload store state for this AFMA codec instance. */
        @NotNull
        protected final PayloadStore payloadStore;
        /** Current length state for this AFMA codec instance. */
        protected final int length;
        /** Holds the heapBytes collection used by this AFMA codec instance. */
        @Nullable
        protected byte[] heapBytes;
        /** Current spool segment state for this AFMA codec instance. */
        @Nullable
        protected SpoolSegment spoolSegment;
        /** Whether released currently applies to this AFMA codec instance. */
        protected boolean released = false;

        /** Initializes a new {@code PayloadStorage} for AFMA codec use. */
        protected PayloadStorage(@NotNull PayloadStore payloadStore, @Nullable byte[] heapBytes, @Nullable SpoolSegment spoolSegment, int length) {
            this.payloadStore = Objects.requireNonNull(payloadStore);
            this.heapBytes = heapBytes;
            this.spoolSegment = spoolSegment;
            this.length = Math.max(0, length);
        }

        /** Opens the stream for the AFMA codec. */
        @NotNull
        public InputStream openStream() throws IOException {
            return this.payloadStore.openStream(this);
        }

        /** Reads the complete payload, returning an independent byte array. */
        @NotNull
        public byte[] readAllBytes() throws IOException {
            return this.payloadStore.readAllBytes(this);
        }

        /** Writes the retained heap payload to the supplied output. */
        public void writeTo(@NotNull OutputStream out) throws IOException {
            this.payloadStore.writeTo(this, out);
        }

        /** Discards this payload's heap bytes or spool-segment reference; repeated calls are harmless. */
        public void release() {
            this.payloadStore.release(this);
        }

        /** Requires the file backed before AFMA codec processing. */
        @NotNull
        public File requireFileBacked() {
            return this.payloadStore.requireFileBacked(this);
        }
    }

    /** Retains or reuses {@code PayloadStore} state while the AFMA codec is active. */
    protected static final class PayloadStore {

        /** Current inline memory payload bytes measured or selected by this AFMA codec instance. */
        protected final int inlineMemoryPayloadBytes;
        /** Current max heap bytes measured or selected by this AFMA codec instance. */
        protected final long maxHeapBytes;
        /** Holds the heapPayloads collection used by this AFMA codec instance. */
        @NotNull
        protected final LinkedHashMap<PayloadStorage, Boolean> heapPayloads = new LinkedHashMap<>(16, 0.75F, true);
        /** Current heap bytes measured or selected by this AFMA codec instance. */
        protected long heapBytes = 0L;
        /** Holds the spoolFile handle whose lifecycle follows this AFMA codec instance. */
        @Nullable
        protected File spoolFile = null;
        /** Current spool out state for this AFMA codec instance. */
        @Nullable
        protected FileOutputStream spoolOut = null;
        /** Current spool length measured or selected by this AFMA codec instance. */
        protected long spoolLength = 0L;
        /** Current active spool segments state for this AFMA codec instance. */
        protected int activeSpoolSegments = 0;
        /** Whether append reserved currently applies to this AFMA codec instance. */
        protected boolean appendReserved = false;

        /** Initializes a new {@code PayloadStore} for AFMA codec use. */
        protected PayloadStore(int inlineMemoryPayloadBytes, long maxHeapBytes) {
            this.inlineMemoryPayloadBytes = Math.max(0, inlineMemoryPayloadBytes);
            this.maxHeapBytes = Math.max(0L, maxHeapBytes);
        }

        /** Stores the bytes in the AFMA codec. */
        @NotNull
        public synchronized PayloadStorage storeBytes(@NotNull byte[] payloadBytes) throws IOException {
            Objects.requireNonNull(payloadBytes);
            if (payloadBytes.length <= this.inlineMemoryPayloadBytes) {
                PayloadStorage payload = new PayloadStorage(this, payloadBytes, null, payloadBytes.length);
                this.heapPayloads.put(payload, Boolean.TRUE);
                this.heapBytes += payloadBytes.length;
                this.enforceHeapBudget();
                return payload;
            }
            return this.storeSegment(this.appendBytes(payloadBytes, 0, payloadBytes.length));
        }

        /** Stores the segment in the AFMA codec. */
        @NotNull
        public synchronized PayloadStorage storeSegment(@NotNull SpoolSegment spoolSegment) {
            Objects.requireNonNull(spoolSegment);
            return new PayloadStorage(this, null, spoolSegment, spoolSegment.length());
        }

        /** Begins the append session lifecycle for the AFMA codec. */
        @NotNull
        public synchronized SpoolAppendSession beginAppendSession() throws IOException {
            while (this.appendReserved) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reserving the AFMA payload spool", ex);
                }
            }

            this.ensureSpoolOutput();
            this.appendReserved = true;
            return new SpoolAppendSession(this, this.spoolLength);
        }

        /** Opens the stream for the AFMA codec. */
        @NotNull
        public InputStream openStream(@NotNull PayloadStorage payload) throws IOException {
            PayloadSnapshot payloadSnapshot = this.snapshot(payload);
            if (payloadSnapshot.heapBytes() != null) {
                return new ByteArrayInputStream(payloadSnapshot.heapBytes());
            }
            return this.openSpoolStream(Objects.requireNonNull(payloadSnapshot.spoolSegment()));
        }

        /** Reads the complete heap or spool-backed payload into an independent byte array. */
        @NotNull
        public byte[] readAllBytes(@NotNull PayloadStorage payload) throws IOException {
            PayloadSnapshot payloadSnapshot = this.snapshot(payload);
            if (payloadSnapshot.heapBytes() != null) {
                return payloadSnapshot.heapBytes().clone();
            }
            try (InputStream in = this.openSpoolStream(Objects.requireNonNull(payloadSnapshot.spoolSegment()));
                 ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, payloadSnapshot.length()))) {
                copyStream(in, out);
                return out.toByteArray();
            }
        }

        /** Writes the retained spool segment to the supplied output. */
        public void writeTo(@NotNull PayloadStorage payload, @NotNull OutputStream out) throws IOException {
            Objects.requireNonNull(out);
            PayloadSnapshot payloadSnapshot = this.snapshot(payload);
            if (payloadSnapshot.heapBytes() != null) {
                out.write(payloadSnapshot.heapBytes());
                return;
            }

            try (InputStream in = this.openSpoolStream(Objects.requireNonNull(payloadSnapshot.spoolSegment()))) {
                copyStream(in, out);
            }
        }

        /**
         * Invalidates one payload and removes its heap allocation or spool-segment reference.
         * The shared spool file is deleted after its final segment and append reservation are gone.
         */
        public synchronized void release(@NotNull PayloadStorage payload) {
            if (payload.released) {
                return;
            }

            payload.released = true;
            if (payload.heapBytes != null) {
                if (this.heapPayloads.remove(payload) != null) {
                    this.heapBytes -= payload.heapBytes.length;
                }
                payload.heapBytes = null;
                return;
            }

            if (payload.spoolSegment != null) {
                payload.spoolSegment = null;
                if (this.activeSpoolSegments > 0) {
                    this.activeSpoolSegments--;
                }
                this.cleanupSpoolIfUnused();
            }
        }

        /** Requires the file backed before AFMA codec processing. */
        @NotNull
        public synchronized File requireFileBacked(@NotNull PayloadStorage payload) {
            this.ensureActive(payload);
            if (payload.spoolSegment == null) {
                throw new IllegalStateException("AFMA payload is currently stored in memory");
            }
            return payload.spoolSegment.file();
        }

        /** Appends resource state to the AFMA codec output state. */
        protected synchronized void append(@NotNull SpoolAppendSession session, @NotNull byte[] source, int offset, int count) throws IOException {
            if (count <= 0) {
                return;
            }
            if (!this.appendReserved || session.finished) {
                throw new IOException("AFMA payload spool append session is no longer active");
            }

            Objects.requireNonNull(this.spoolOut).write(source, offset, count);
            session.length += count;
            this.spoolLength += count;
        }

        /** Finishes the append lifecycle for the AFMA codec. */
        protected synchronized SpoolSegment finishAppend(@NotNull SpoolAppendSession session) throws IOException {
            if (session.finished) {
                throw new IOException("AFMA payload spool append session has already been closed");
            }

            Objects.requireNonNull(this.spoolOut).flush();
            session.finished = true;
            this.appendReserved = false;
            this.activeSpoolSegments++;
            this.notifyAll();
            return new SpoolSegment(Objects.requireNonNull(this.spoolFile), session.offset, session.length);
        }

        /** Aborts the append and releases its AFMA codec state. */
        protected synchronized void abortAppend(@NotNull SpoolAppendSession session) {
            if (session.finished) {
                return;
            }

            session.finished = true;
            this.appendReserved = false;
            this.notifyAll();
            this.cleanupSpoolIfUnused();
        }

        /** Ensures the spool output is valid for the AFMA codec. */
        protected synchronized void ensureSpoolOutput() throws IOException {
            if (this.spoolOut != null) {
                return;
            }

            this.spoolFile = createTempFile();
            this.spoolOut = new FileOutputStream(this.spoolFile, true);
            this.spoolLength = 0L;
        }

        /** Cleans up the spool if unused owned by the AFMA codec. */
        protected synchronized void cleanupSpoolIfUnused() {
            if (this.appendReserved || (this.activeSpoolSegments > 0)) {
                return;
            }
            if (this.spoolOut != null) {
                closeQuietly(this.spoolOut);
                this.spoolOut = null;
            }
            if (this.spoolFile != null) {
                deleteQuietly(this.spoolFile);
                this.spoolFile = null;
            }
            this.spoolLength = 0L;
        }

        /** Ensures the active is valid for the AFMA codec. */
        protected synchronized void ensureActive(@NotNull PayloadStorage payload) {
            if (payload.released) {
                throw new IllegalStateException("AFMA payload storage has already been released");
            }
        }

        /** Returns the snapshot used by the AFMA codec. */
        @NotNull
        protected synchronized PayloadSnapshot snapshot(@NotNull PayloadStorage payload) {
            this.ensureActive(payload);
            if (payload.heapBytes != null) {
                this.heapPayloads.get(payload);
                return new PayloadSnapshot(payload.heapBytes, null, payload.length);
            }
            return new PayloadSnapshot(null, Objects.requireNonNull(payload.spoolSegment), payload.length);
        }

        /** Enforces the heap budget limit for the AFMA codec. */
        protected synchronized void enforceHeapBudget() throws IOException {
            if (this.maxHeapBytes <= 0L) {
                this.evictAllHeapPayloads();
                return;
            }

            Iterator<PayloadStorage> iterator = this.heapPayloads.keySet().iterator();
            while ((this.heapBytes > this.maxHeapBytes) && iterator.hasNext()) {
                PayloadStorage payload = iterator.next();
                byte[] heapBytes = payload.heapBytes;
                iterator.remove();
                if (heapBytes == null || payload.released) {
                    continue;
                }

                this.heapBytes -= heapBytes.length;
                payload.heapBytes = null;
                payload.spoolSegment = this.appendBytes(heapBytes, 0, heapBytes.length);
            }
        }

        /** Moves every active heap payload to the shared spool file. */
        protected synchronized void evictAllHeapPayloads() throws IOException {
            Iterator<PayloadStorage> iterator = this.heapPayloads.keySet().iterator();
            while (iterator.hasNext()) {
                PayloadStorage payload = iterator.next();
                byte[] heapBytes = payload.heapBytes;
                iterator.remove();
                if (heapBytes == null || payload.released) {
                    continue;
                }

                this.heapBytes -= heapBytes.length;
                payload.heapBytes = null;
                payload.spoolSegment = this.appendBytes(heapBytes, 0, heapBytes.length);
            }
        }

        /** Appends the bytes to the AFMA codec output state. */
        @NotNull
        protected synchronized SpoolSegment appendBytes(@NotNull byte[] source, int offset, int count) throws IOException {
            SpoolAppendSession session = this.beginAppendSession();
            boolean success = false;
            try {
                session.write(source, offset, count);
                SpoolSegment segment = session.finish();
                success = true;
                return segment;
            } finally {
                if (!success) {
                    session.abort();
                }
            }
        }

        /** Opens the spool stream for the AFMA codec. */
        @NotNull
        protected InputStream openSpoolStream(@NotNull SpoolSegment spoolSegment) throws IOException {
            FileInputStream fileIn = new FileInputStream(spoolSegment.file());
            boolean success = false;
            try {
                fileIn.getChannel().position(spoolSegment.offset());
                InputStream in = new BoundedInputStream(fileIn, spoolSegment.length());
                success = true;
                return new BufferedInputStream(in);
            } finally {
                if (!success) {
                    closeQuietly(fileIn);
                }
            }
        }
    }

    /** Models {@code SpoolAppendSession} state used by the AFMA codec. */
    protected static final class SpoolAppendSession {

        /** Owned payload store state for this AFMA codec instance. */
        @NotNull
        protected final PayloadStore payloadStore;
        /** Holds the singleByteBuffer collection used by this AFMA codec instance. */
        @NotNull
        protected final byte[] singleByteBuffer = new byte[1];
        /** Current offset state for this AFMA codec instance. */
        protected final long offset;
        /** Current length state for this AFMA codec instance. */
        protected int length = 0;
        /** Whether finished currently applies to this AFMA codec instance. */
        protected boolean finished = false;

        /** Initializes a new {@code SpoolAppendSession} for AFMA codec use. */
        protected SpoolAppendSession(@NotNull PayloadStore payloadStore, long offset) {
            this.payloadStore = Objects.requireNonNull(payloadStore);
            this.offset = Math.max(0L, offset);
        }

        /** Writes resource data to the AFMA codec output. */
        public void write(int value) throws IOException {
            this.singleByteBuffer[0] = (byte) value;
            this.write(this.singleByteBuffer, 0, 1);
        }

        /** Writes resource data to the AFMA codec output. */
        public void write(@NotNull byte[] source, int offset, int count) throws IOException {
            this.payloadStore.append(this, source, offset, count);
        }

        /** Finishes the finish lifecycle for the AFMA codec. */
        @NotNull
        public SpoolSegment finish() throws IOException {
            return this.payloadStore.finishAppend(this);
        }

        /** Aborts resource state and releases its AFMA codec state. */
        public void abort() {
            this.payloadStore.abortAppend(this);
        }
    }

    /** Models {@code SpoolSegment} state used by the AFMA codec. */
    protected record SpoolSegment(@NotNull File file, long offset, int length) {

        /** Validates constructor components before storing this {@code SpoolSegment}. */
        public SpoolSegment {
            file = Objects.requireNonNull(file);
            offset = Math.max(0L, offset);
            length = Math.max(0, length);
        }
    }

    /** Models {@code PayloadSnapshot} state used by the AFMA codec. */
    protected record PayloadSnapshot(@Nullable byte[] heapBytes, @Nullable SpoolSegment spoolSegment, int length) {
    }

    /** Models {@code BoundedInputStream} state used by the AFMA codec. */
    protected static final class BoundedInputStream extends InputStream {

        /** Holds the delegate handle whose lifecycle follows this AFMA codec instance. */
        @NotNull
        protected final InputStream delegate;
        /** Current remaining bytes measured or selected by this AFMA codec instance. */
        protected int remainingBytes;

        /** Initializes a new {@code BoundedInputStream} for AFMA codec use. */
        protected BoundedInputStream(@NotNull InputStream delegate, int remainingBytes) {
            this.delegate = Objects.requireNonNull(delegate);
            this.remainingBytes = Math.max(0, remainingBytes);
        }

        /** Reads resource data from the AFMA codec input. */
        @Override
        public int read() throws IOException {
            if (this.remainingBytes <= 0) {
                return -1;
            }
            int read = this.delegate.read();
            if (read >= 0) {
                this.remainingBytes--;
            }
            return read;
        }

        /** Reads resource data from the AFMA codec input. */
        @Override
        public int read(@NotNull byte[] target, int offset, int length) throws IOException {
            Objects.requireNonNull(target);
            if (offset < 0 || length < 0 || length > target.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (this.remainingBytes <= 0) {
                return -1;
            }

            int read = this.delegate.read(target, offset, Math.min(length, this.remainingBytes));
            if (read > 0) {
                this.remainingBytes -= read;
            }
            return read;
        }

        /** Closes the underlying stream; no additional buffer is retained by this bounded view. */
        @Override
        public void close() throws IOException {
            this.delegate.close();
        }
    }

}
