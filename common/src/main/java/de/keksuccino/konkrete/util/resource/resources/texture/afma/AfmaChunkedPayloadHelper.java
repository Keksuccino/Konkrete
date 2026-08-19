package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.Deflater;

/** Provides {@code AfmaChunkedPayloadHelper} transformations and validation used by the AFMA codec. */
public final class AfmaChunkedPayloadHelper {

    /** Archive entry path for payload index data. */
    public static final String PAYLOAD_INDEX_ENTRY_PATH = "payload_index.bin";
    /** Archive directory containing payload chunk entries. */
    public static final String PAYLOAD_CHUNK_DIRECTORY = "payload_chunks/";
    /** Filename extension used for payload chunk entries. */
    public static final String PAYLOAD_CHUNK_EXTENSION = ".bin";
    /** Serialized magic value identifying payload index data. */
    public static final int PAYLOAD_INDEX_MAGIC = 0x41465049; // AFPI
    /** Serialized version value for payload index data. */
    public static final int PAYLOAD_INDEX_VERSION = 1;
    /** Target chunk bytes used when planning AFMA codec output. */
    public static final int TARGET_CHUNK_BYTES = 512 * 1024;
    /** Target chunk byte candidates used when planning AFMA codec output. */
    public static final int[] TARGET_CHUNK_BYTE_CANDIDATES = {
            256 * 1024,
            384 * 1024,
            TARGET_CHUNK_BYTES,
            768 * 1024,
            1024 * 1024,
            1536 * 1024,
            2048 * 1024
    };
    /** Target chunk size shortlist max used when planning AFMA codec output. */
    protected static final int TARGET_CHUNK_SIZE_SHORTLIST_MAX = 3;
    /** Target chunk size frame headroom used when planning AFMA codec output. */
    protected static final double TARGET_CHUNK_SIZE_FRAME_HEADROOM = 1.18D;
    /** Target chunk size payload grouping multiplier used when planning AFMA codec output. */
    protected static final double TARGET_CHUNK_SIZE_PAYLOAD_GROUPING_MULTIPLIER = 6D;
    /** Target chunk size fallback grouping multiplier used when planning AFMA codec output. */
    protected static final double TARGET_CHUNK_SIZE_FALLBACK_GROUPING_MULTIPLIER = 4D;
    /** Upper bound for cache simulation chunk retained by the AFMA codec. */
    protected static final int CACHE_SIMULATION_CHUNK_LIMIT = AfmaIoHelper.DEFAULT_PAYLOAD_CHUNK_CACHE_SIZE;
    /** Serialized discriminator for sequence kind intro. */
    public static final int SEQUENCE_KIND_INTRO = 0;
    /** Serialized discriminator for sequence kind main. */
    public static final int SEQUENCE_KIND_MAIN = 1;
    /** Byte budget for planner deflate tail in the AFMA codec. */
    public static final int PLANNER_DEFLATE_TAIL_BYTES = 32 * 1024;
    /** Configured planner deflate level used by the AFMA codec. */
    public static final int PLANNER_DEFLATE_LEVEL = 1;
    /** Byte budget for estimated zip chunk overhead in the AFMA codec. */
    protected static final int ESTIMATED_ZIP_CHUNK_OVERHEAD_BYTES = 96;
    /** Scoring or hashing coefficient for same frame edge weight in the AFMA codec. */
    protected static final int SAME_FRAME_EDGE_WEIGHT = 10;
    /** Scoring or hashing coefficient for consecutive frame edge weight in the AFMA codec. */
    protected static final int CONSECUTIVE_FRAME_EDGE_WEIGHT = 5;
    /** Byte budget for chunk cache miss penalty in the AFMA codec. */
    protected static final int CHUNK_CACHE_MISS_PENALTY_BYTES = 1024;
    /** Byte budget for multi chunk frame penalty in the AFMA codec. */
    protected static final int MULTI_CHUNK_FRAME_PENALTY_BYTES = 768;
    /** Shared immutable empty value used by the AFMA codec. */
    protected static final byte[] EMPTY_BYTES = new byte[0];
    private static final ThreadLocal<Deflater> CHUNK_DEFLATER_POOL = ThreadLocal.withInitial(() -> new Deflater(PLANNER_DEFLATE_LEVEL, true));
    private static final ThreadLocal<byte[]> CHUNK_BUFFER_POOL = ThreadLocal.withInitial(() -> new byte[8192]);
    private static final ThreadLocal<byte[]> CHUNK_READ_BUFFER_POOL = ThreadLocal.withInitial(() -> new byte[8192]);

    private AfmaChunkedPayloadHelper() {
    }

    /** Builds the archive layout for the AFMA codec. */
    @NotNull
    public static PackedPayloadArchive buildArchiveLayout(@NotNull Map<String, AfmaStoredPayload> payloads) {
        return buildArchiveLayout(payloads, ArchivePackingHints.empty());
    }

    /** Builds the archive layout for the AFMA codec. */
    @NotNull
    public static PackedPayloadArchive buildArchiveLayout(@NotNull Map<String, AfmaStoredPayload> payloads, @Nullable ArchivePackingHints packingHints) {
        return buildArchiveLayoutInternal(payloads, packingHints, false);
    }

    /** Simulates the archive layout for the AFMA codec. */
    @NotNull
    public static PackedPayloadArchive simulateArchiveLayout(@NotNull Map<String, AfmaStoredPayload> payloads, @Nullable ArchivePackingHints packingHints) {
        return buildArchiveLayoutInternal(payloads, packingHints, true);
    }

    /** Builds the packing hints for the AFMA codec. */
    @NotNull
    public static ArchivePackingHints buildPackingHints(@NotNull AfmaFrameIndex frameIndex, int loopCount) {
        Objects.requireNonNull(frameIndex);
        ArrayList<PayloadAccessFrame> accessFrames = new ArrayList<>();
        int nextRegionId = 0;
        nextRegionId = appendSequenceAccessFrames(accessFrames, frameIndex.getIntroFrames(), SEQUENCE_KIND_INTRO, nextRegionId);
        nextRegionId = appendSequenceAccessFrames(accessFrames, frameIndex.getFrames(), SEQUENCE_KIND_MAIN, nextRegionId);
        if (shouldRepeatMainAccessTrace(frameIndex.getFrames(), loopCount)) {
            appendSequenceAccessFrames(accessFrames, frameIndex.getFrames(), SEQUENCE_KIND_MAIN, nextRegionId);
        }
        return new ArchivePackingHints(accessFrames);
    }

    /** Builds the access frame for the AFMA codec. */
    @NotNull
    public static PayloadAccessFrame createAccessFrame(@NotNull AfmaFrameDescriptor descriptor, int keyframeRegionId, int sequenceKind) {
        Objects.requireNonNull(descriptor);
        ArrayList<String> payloadPaths = new ArrayList<>(2);
        addAccessPayloadPath(payloadPaths, descriptor.getPrimaryPayloadPath());
        addAccessPayloadPath(payloadPaths, descriptor.getSecondaryPayloadPath());
        return new PayloadAccessFrame(payloadPaths, keyframeRegionId, sequenceKind);
    }

    /** Calculates the var int bytes for the AFMA codec. */
    public static int estimateVarIntBytes(int value) {
        if ((value & ~0x7F) == 0) {
            return 1;
        }
        if ((value & ~0x3FFF) == 0) {
            return 2;
        }
        if ((value & ~0x1FFFFF) == 0) {
            return 3;
        }
        if ((value & ~0xFFFFFFF) == 0) {
            return 4;
        }
        return 5;
    }

    /** Calculates the chunk compression delta for the AFMA codec. */
    public static long estimateChunkCompressionDelta(@NotNull byte[] previousTail, @NotNull byte[] payloadBytes) {
        Objects.requireNonNull(previousTail);
        Objects.requireNonNull(payloadBytes);
        if (payloadBytes.length == 0) {
            return 0L;
        }
        if (previousTail.length == 0) {
            return AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes);
        }

        Deflater deflater = CHUNK_DEFLATER_POOL.get();
        byte[] deflateBuffer = CHUNK_BUFFER_POOL.get();
        deflater.reset();
        long combinedEstimate = 0L;
        deflater.setInput(previousTail);
        while (!deflater.needsInput()) {
            combinedEstimate += deflater.deflate(deflateBuffer);
        }

        deflater.setInput(payloadBytes);
        deflater.finish();
        while (!deflater.finished()) {
            combinedEstimate += deflater.deflate(deflateBuffer);
        }

        long tailEstimate = AfmaPayloadMetricsHelper.estimateArchiveBytes(previousTail);
        return Math.max(0L, combinedEstimate - tailEstimate);
    }

    /** Calculates the chunk compression delta for the AFMA codec. */
    public static long estimateChunkCompressionDelta(@NotNull byte[] previousTail, @NotNull AfmaStoredPayload payload) {
        Objects.requireNonNull(previousTail);
        Objects.requireNonNull(payload);
        if (payload.isEmpty()) {
            return 0L;
        }
        if (previousTail.length == 0) {
            return payload.estimatedArchiveBytes();
        }

        Deflater deflater = CHUNK_DEFLATER_POOL.get();
        byte[] readBuffer = CHUNK_READ_BUFFER_POOL.get();
        byte[] deflateBuffer = CHUNK_BUFFER_POOL.get();
        deflater.reset();
        long combinedEstimate = 0L;
        try (InputStream in = payload.openStream()) {
            deflater.setInput(previousTail);
            while (!deflater.needsInput()) {
                combinedEstimate += deflater.deflate(deflateBuffer);
            }

            int read;
            while ((read = in.read(readBuffer)) >= 0) {
                if (read <= 0) {
                    continue;
                }
                deflater.setInput(readBuffer, 0, read);
                while (!deflater.needsInput()) {
                    combinedEstimate += deflater.deflate(deflateBuffer);
                }
            }

            deflater.finish();
            while (!deflater.finished()) {
                combinedEstimate += deflater.deflate(deflateBuffer);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to estimate AFMA payload chunk compression delta", ex);
        }

        long tailEstimate = AfmaPayloadMetricsHelper.estimateArchiveBytes(previousTail);
        return Math.max(0L, combinedEstimate - tailEstimate);
    }

    /** Calculates the chunk compression delta for the AFMA codec. */
    public static long estimateChunkCompressionDelta(@NotNull byte[] previousTail,
                                                     @NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                                     @NotNull AfmaStoredPayload.Writer payloadWriter) {
        Objects.requireNonNull(previousTail);
        Objects.requireNonNull(payloadSummary);
        Objects.requireNonNull(payloadWriter);
        if (payloadSummary.length() <= 0) {
            return 0L;
        }
        if (previousTail.length == 0) {
            return payloadSummary.estimatedArchiveBytes();
        }

        Deflater deflater = CHUNK_DEFLATER_POOL.get();
        byte[] deflateBuffer = CHUNK_BUFFER_POOL.get();
        deflater.reset();
        long[] combinedEstimateHolder = {0L};
        try {
            deflater.setInput(previousTail);
            while (!deflater.needsInput()) {
                combinedEstimateHolder[0] += deflater.deflate(deflateBuffer);
            }

            OutputStream deflaterOut = new OutputStream() {
                @NotNull
                private final byte[] writeBuffer = new byte[8192];
                private int writeBufferLength = 0;

                /** Writes resource data to the AFMA codec output. */
                @Override
                public void write(int value) {
                    if (this.writeBufferLength >= this.writeBuffer.length) {
                        this.flushBuffer();
                    }
                    this.writeBuffer[this.writeBufferLength++] = (byte) value;
                }

                /** Writes resource data to the AFMA codec output. */
                @Override
                public void write(@NotNull byte[] source, int offset, int count) {
                    if (count <= 0) {
                        return;
                    }

                    int remaining = count;
                    int nextOffset = offset;
                    while (remaining > 0) {
                        if (this.writeBufferLength == 0 && remaining >= this.writeBuffer.length) {
                            this.deflate(source, nextOffset, this.writeBuffer.length);
                            nextOffset += this.writeBuffer.length;
                            remaining -= this.writeBuffer.length;
                            continue;
                        }

                        if (this.writeBufferLength >= this.writeBuffer.length) {
                            this.flushBuffer();
                        }

                        int bytesToCopy = Math.min(remaining, this.writeBuffer.length - this.writeBufferLength);
                        System.arraycopy(source, nextOffset, this.writeBuffer, this.writeBufferLength, bytesToCopy);
                        this.writeBufferLength += bytesToCopy;
                        nextOffset += bytesToCopy;
                        remaining -= bytesToCopy;
                    }
                }

                /** Flushes resource state from the AFMA codec buffer. */
                @Override
                public void flush() {
                    this.flushBuffer();
                }

                private void flushBuffer() {
                    if (this.writeBufferLength <= 0) {
                        return;
                    }
                    this.deflate(this.writeBuffer, 0, this.writeBufferLength);
                    this.writeBufferLength = 0;
                }

                private void deflate(@NotNull byte[] source, int offset, int count) {
                    deflater.setInput(source, offset, count);
                    while (!deflater.needsInput()) {
                        combinedEstimateHolder[0] += deflater.deflate(deflateBuffer);
                    }
                }
            };
            payloadWriter.write(deflaterOut);
            deflaterOut.flush();

            deflater.finish();
            while (!deflater.finished()) {
                combinedEstimateHolder[0] += deflater.deflate(deflateBuffer);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to estimate AFMA payload chunk compression delta", ex);
        }

        long tailEstimate = AfmaPayloadMetricsHelper.estimateArchiveBytes(previousTail);
        return Math.max(0L, combinedEstimateHolder[0] - tailEstimate);
    }

    /** Appends the deflate tail to the AFMA codec output state. */
    @NotNull
    public static byte[] appendDeflateTail(@NotNull byte[] currentTail, @NotNull byte[] payloadBytes) {
        Objects.requireNonNull(currentTail);
        Objects.requireNonNull(payloadBytes);
        if (payloadBytes.length == 0) {
            return currentTail;
        }
        int resultLength = Math.min(PLANNER_DEFLATE_TAIL_BYTES, currentTail.length + payloadBytes.length);
        byte[] resultTail = new byte[resultLength];
        int payloadBytesInTail = Math.min(payloadBytes.length, resultLength);
        System.arraycopy(payloadBytes, payloadBytes.length - payloadBytesInTail, resultTail, resultLength - payloadBytesInTail, payloadBytesInTail);
        int carriedPrefixBytes = resultLength - payloadBytesInTail;
        if (carriedPrefixBytes > 0) {
            System.arraycopy(currentTail, currentTail.length - carriedPrefixBytes, resultTail, 0, carriedPrefixBytes);
        }
        return resultTail;
    }

    /** Appends the deflate tail to the AFMA codec output state. */
    @NotNull
    public static byte[] appendDeflateTail(@NotNull byte[] currentTail, @NotNull AfmaStoredPayload payload) {
        Objects.requireNonNull(currentTail);
        Objects.requireNonNull(payload);
        if (payload.isEmpty()) {
            return currentTail;
        }

        int payloadLength = payload.length();
        int resultLength = Math.min(PLANNER_DEFLATE_TAIL_BYTES, currentTail.length + payloadLength);
        byte[] resultTail = new byte[resultLength];
        byte[] payloadTail = payload.tailBytesUnsafe();
        int payloadBytesInTail = Math.min(payloadTail.length, resultLength);
        System.arraycopy(payloadTail, payloadTail.length - payloadBytesInTail, resultTail, resultLength - payloadBytesInTail, payloadBytesInTail);
        int carriedPrefixBytes = resultLength - payloadBytesInTail;
        if (carriedPrefixBytes > 0) {
            System.arraycopy(currentTail, currentTail.length - carriedPrefixBytes, resultTail, 0, carriedPrefixBytes);
        }
        return resultTail;
    }

    /** Appends the deflate tail to the AFMA codec output state. */
    @NotNull
    public static byte[] appendDeflateTail(@NotNull byte[] currentTail, @NotNull AfmaStoredPayload.PayloadSummary payloadSummary) {
        Objects.requireNonNull(currentTail);
        Objects.requireNonNull(payloadSummary);
        if (payloadSummary.length() <= 0) {
            return currentTail;
        }

        int payloadLength = payloadSummary.length();
        int resultLength = Math.min(PLANNER_DEFLATE_TAIL_BYTES, currentTail.length + payloadLength);
        byte[] resultTail = new byte[resultLength];
        byte[] payloadTail = payloadSummary.tailBytes();
        int payloadBytesInTail = Math.min(payloadTail.length, resultLength);
        System.arraycopy(payloadTail, payloadTail.length - payloadBytesInTail, resultTail, resultLength - payloadBytesInTail, payloadBytesInTail);
        int carriedPrefixBytes = resultLength - payloadBytesInTail;
        if (carriedPrefixBytes > 0) {
            System.arraycopy(currentTail, currentTail.length - carriedPrefixBytes, resultTail, 0, carriedPrefixBytes);
        }
        return resultTail;
    }

    /** Builds the archive layout internal for the AFMA codec. */
    @NotNull
    protected static PackedPayloadArchive buildArchiveLayoutInternal(@NotNull Map<String, AfmaStoredPayload> payloads,
                                                                    @Nullable ArchivePackingHints packingHints,
                                                                    boolean approximateCompression) {
        Objects.requireNonNull(payloads);
        LinkedHashMap<String, PayloadData> payloadDataByPath = new LinkedHashMap<>();
        int insertionOrder = 0;
        for (Map.Entry<String, AfmaStoredPayload> entry : payloads.entrySet()) {
            String payloadPath = Objects.requireNonNull(entry.getKey());
            AfmaStoredPayload payload = Objects.requireNonNull(entry.getValue(), "AFMA payload was NULL for " + payloadPath);
            payloadDataByPath.put(payloadPath, new PayloadData(payloadPath, payload, insertionOrder++));
        }

        if (payloadDataByPath.isEmpty()) {
            return new PackedPayloadArchive(
                    Collections.unmodifiableMap(new LinkedHashMap<String, Integer>()),
                    List.of(),
                    List.of(),
                    PackingMetrics.empty(TARGET_CHUNK_BYTES)
            );
        }

        ArchivePackingHints effectiveHints = resolveArchivePackingHints(payloadDataByPath, packingHints);
        PackingModel packingModel = buildPackingModel(payloadDataByPath, effectiveHints);
        int[] targetChunkByteCandidates = resolveTargetChunkByteCandidates(payloadDataByPath, packingModel);

        PackedPayloadArchive bestArchive = null;
        long bestScore = Long.MAX_VALUE;
        for (int targetChunkBytes : targetChunkByteCandidates) {
            PackedPayloadArchive candidate = packPayloads(payloadDataByPath, packingModel, targetChunkBytes, approximateCompression);
            long candidateScore = candidate.packingMetrics().scoredArchiveBytes();
            if (bestArchive == null || candidateScore < bestScore
                    || ((candidateScore == bestScore) && candidate.packingMetrics().predictedArchiveBytes() < bestArchive.packingMetrics().predictedArchiveBytes())) {
                bestArchive = candidate;
                bestScore = candidateScore;
            }
        }

        return Objects.requireNonNull(bestArchive);
    }

    /** Encodes the payload index for the AFMA codec. */
    @NotNull
    public static byte[] encodePayloadIndex(@NotNull PackedPayloadArchive archiveLayout) throws IOException {
        Objects.requireNonNull(archiveLayout);
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeInt(PAYLOAD_INDEX_MAGIC);
            out.writeByte(PAYLOAD_INDEX_VERSION);
            writeVarInt(out, archiveLayout.chunkPlans().size());
            writeVarInt(out, archiveLayout.payloadLocators().size());
            for (ChunkPlan chunkPlan : archiveLayout.chunkPlans()) {
                writeVarInt(out, chunkPlan.uncompressedLength());
            }
            for (PayloadLocator payloadLocator : archiveLayout.payloadLocators()) {
                writeVarInt(out, payloadLocator.chunkId());
                writeVarInt(out, payloadLocator.offset());
                writeVarInt(out, payloadLocator.length());
            }
            out.flush();
            return byteStream.toByteArray();
        }
    }

    /** Decodes the payload index for the AFMA codec. */
    @NotNull
    public static DecodedPayloadIndex decodePayloadIndex(@NotNull byte[] payloadIndexBytes) throws IOException {
        Objects.requireNonNull(payloadIndexBytes);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadIndexBytes))) {
            int magic = in.readInt();
            if (magic != PAYLOAD_INDEX_MAGIC) {
                throw new IOException("AFMA payload index is missing its magic header");
            }

            int version = in.readUnsignedByte();
            if (version != PAYLOAD_INDEX_VERSION) {
                throw new IOException("Unsupported AFMA payload index version: " + version);
            }

            int chunkCount = readVarInt(in);
            int payloadCount = readVarInt(in);
            if (chunkCount < 0 || payloadCount < 0 || chunkCount > ResourceRuntime.getSafetyLimits().maxArchiveEntries() || payloadCount > ResourceRuntime.getSafetyLimits().maxArchiveEntries()) {
                throw new IOException("AFMA payload index counts exceed resource safety limits");
            }

            int[] chunkLengths = new int[chunkCount];
            long decompressedBytes = 0L;
            for (int i = 0; i < chunkCount; i++) {
                chunkLengths[i] = readVarInt(in);
                if (chunkLengths[i] < 0 || chunkLengths[i] > ResourceRuntime.getSafetyLimits().maxDecompressedBytes()) {
                    throw new IOException("AFMA payload chunk " + i + " exceeds resource safety limits");
                }
                decompressedBytes = Math.addExact(decompressedBytes, chunkLengths[i]);
                if (decompressedBytes > ResourceRuntime.getSafetyLimits().maxDecompressedBytes()) throw new IOException("AFMA payload chunks exceed decompressed-byte safety limits");
            }

            LinkedHashMap<String, PayloadLocator> payloadLocatorsByPath = new LinkedHashMap<>();
            for (int payloadId = 0; payloadId < payloadCount; payloadId++) {
                int chunkId = readVarInt(in);
                int offset = readVarInt(in);
                int length = readVarInt(in);
                if (chunkId < 0 || chunkId >= chunkCount) {
                    throw new IOException("AFMA payload " + payloadId + " references an invalid payload chunk");
                }
                if (offset < 0 || length < 0 || ((long) offset + (long) length) > chunkLengths[chunkId]) {
                    throw new IOException("AFMA payload " + payloadId + " has an invalid chunk range");
                }
                payloadLocatorsByPath.put(syntheticPayloadPath(payloadId).toLowerCase(Locale.ROOT), new PayloadLocator(chunkId, offset, length));
            }

            if (in.available() > 0) {
                throw new IOException("AFMA payload index contains trailing data");
            }

            return new DecodedPayloadIndex(payloadLocatorsByPath, chunkLengths);
        }
    }

    /** Builds the synthetic payload path identifier for the AFMA codec. */
    @NotNull
    public static String syntheticPayloadPath(int payloadId) {
        if (payloadId < 0) {
            throw new IllegalArgumentException("AFMA payload id is negative: " + payloadId);
        }
        return "payload/" + Integer.toUnsignedString(payloadId, 36);
    }

    /** Builds the chunk entry path identifier for the AFMA codec. */
    @NotNull
    public static String chunkEntryPath(int chunkId) {
        if (chunkId < 0) {
            throw new IllegalArgumentException("AFMA chunk id is negative: " + chunkId);
        }
        return PAYLOAD_CHUNK_DIRECTORY + "chunk_" + Integer.toUnsignedString(chunkId, 36) + PAYLOAD_CHUNK_EXTENSION;
    }

    /** Returns whether whole chunk payload. */
    public static boolean isWholeChunkPayload(@NotNull PayloadLocator payloadLocator, int[] chunkLengths) {
        Objects.requireNonNull(payloadLocator);
        Objects.requireNonNull(chunkLengths);
        int chunkId = payloadLocator.chunkId();
        return chunkId >= 0
                && chunkId < chunkLengths.length
                && payloadLocator.offset() == 0
                && payloadLocator.length() == chunkLengths[chunkId];
    }

    /** Registers the chunk plan with this AFMA codec component. */
    protected static void addChunkPlan(@NotNull List<ChunkPlan> chunkPlans, @NotNull List<String> chunkPayloadPaths, int chunkLength) {
        int chunkId = chunkPlans.size();
        chunkPlans.add(new ChunkPlan(chunkEntryPath(chunkId), List.copyOf(chunkPayloadPaths), chunkLength));
    }

    /** Returns whether repeat main access trace. */
    protected static boolean shouldRepeatMainAccessTrace(@NotNull List<AfmaFrameDescriptor> mainFrames, int loopCount) {
        return !mainFrames.isEmpty() && mainFrames.size() > 1 && loopCount != 1;
    }

    /** Appends the sequence access frames to the AFMA codec output state. */
    protected static int appendSequenceAccessFrames(@NotNull List<PayloadAccessFrame> accessFrames, @NotNull List<AfmaFrameDescriptor> sequence,
                                                    int sequenceKind, int nextRegionId) {
        if (sequence.isEmpty()) {
            return nextRegionId;
        }

        int currentRegionId = -1;
        for (AfmaFrameDescriptor descriptor : sequence) {
            AfmaFrameDescriptor resolvedDescriptor = Objects.requireNonNull(descriptor);
            if (currentRegionId < 0 || resolvedDescriptor.isKeyframe()) {
                currentRegionId = nextRegionId++;
            }
            accessFrames.add(createAccessFrame(resolvedDescriptor, currentRegionId, sequenceKind));
        }
        return nextRegionId;
    }

    /** Registers the access payload path with this AFMA codec component. */
    protected static void addAccessPayloadPath(@NotNull List<String> payloadPaths, @Nullable String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        for (String existingPath : payloadPaths) {
            if (existingPath.equals(path)) {
                return;
            }
        }
        payloadPaths.add(path);
    }

    /** Resolves the archive packing hints for the AFMA codec. */
    @NotNull
    protected static ArchivePackingHints resolveArchivePackingHints(@NotNull Map<String, PayloadData> payloadDataByPath,
                                                                   @Nullable ArchivePackingHints packingHints) {
        if (packingHints != null && !packingHints.isEmpty()) {
            return packingHints;
        }

        ArrayList<PayloadAccessFrame> fallbackFrames = new ArrayList<>(payloadDataByPath.size());
        for (String payloadPath : payloadDataByPath.keySet()) {
            fallbackFrames.add(new PayloadAccessFrame(List.of(payloadPath), 0, SEQUENCE_KIND_MAIN));
        }
        return new ArchivePackingHints(fallbackFrames);
    }

    /** Builds the packing model for the AFMA codec. */
    @NotNull
    protected static PackingModel buildPackingModel(@NotNull Map<String, PayloadData> payloadDataByPath,
                                                    @NotNull ArchivePackingHints packingHints) {
        LinkedHashMap<String, PayloadPackingStats> statsByPath = new LinkedHashMap<>();
        HashMap<String, String> actualPathsByNormalized = new HashMap<>();
        for (PayloadData payloadData : payloadDataByPath.values()) {
            statsByPath.put(payloadData.path(), new PayloadPackingStats(payloadData));
            actualPathsByNormalized.put(normalizePayloadPath(payloadData.path()), payloadData.path());
        }

        HashMap<String, Map<String, Integer>> edgeWeightsByPath = new HashMap<>();
        ArrayList<PayloadAccessFrame> resolvedAccessFrames = new ArrayList<>(packingHints.accessFrames().size());
        PayloadAccessFrame previousFrame = null;
        int accessIndex = 0;
        for (PayloadAccessFrame accessFrame : packingHints.accessFrames()) {
            ArrayList<String> resolvedPayloadPaths = new ArrayList<>(accessFrame.payloadPaths().size());
            for (String payloadPath : accessFrame.payloadPaths()) {
                String actualPath = actualPathsByNormalized.get(normalizePayloadPath(payloadPath));
                if (actualPath != null && !resolvedPayloadPaths.contains(actualPath)) {
                    resolvedPayloadPaths.add(actualPath);
                }
            }

            PayloadAccessFrame resolvedFrame = new PayloadAccessFrame(resolvedPayloadPaths, accessFrame.keyframeRegionId(), accessFrame.sequenceKind());
            resolvedAccessFrames.add(resolvedFrame);

            for (String payloadPath : resolvedPayloadPaths) {
                PayloadPackingStats stats = statsByPath.get(payloadPath);
                if (stats != null) {
                    stats.recordAccess(accessIndex, accessFrame.keyframeRegionId(), accessFrame.sequenceKind());
                }
            }

            for (int i = 0; i < resolvedPayloadPaths.size(); i++) {
                for (int j = i + 1; j < resolvedPayloadPaths.size(); j++) {
                    addEdgeWeight(edgeWeightsByPath, resolvedPayloadPaths.get(i), resolvedPayloadPaths.get(j), SAME_FRAME_EDGE_WEIGHT);
                }
            }

            if (previousFrame != null) {
                for (String previousPath : previousFrame.payloadPaths()) {
                    for (String currentPath : resolvedPayloadPaths) {
                        addEdgeWeight(edgeWeightsByPath, previousPath, currentPath, CONSECUTIVE_FRAME_EDGE_WEIGHT);
                    }
                }
            }

            previousFrame = resolvedFrame;
            accessIndex++;
        }

        return new PackingModel(List.copyOf(resolvedAccessFrames), statsByPath, edgeWeightsByPath);
    }

    /** Registers the edge weight with this AFMA codec component. */
    protected static void addEdgeWeight(@NotNull Map<String, Map<String, Integer>> edgeWeightsByPath,
                                        @NotNull String firstPath, @NotNull String secondPath, int weight) {
        if (weight <= 0 || firstPath.equals(secondPath)) {
            return;
        }
        edgeWeightsByPath.computeIfAbsent(firstPath, key -> new HashMap<>()).merge(secondPath, weight, Integer::sum);
        edgeWeightsByPath.computeIfAbsent(secondPath, key -> new HashMap<>()).merge(firstPath, weight, Integer::sum);
    }

    /** Resolves the target chunk byte candidates for the AFMA codec. */
    @NotNull
    protected static int[] resolveTargetChunkByteCandidates(@NotNull Map<String, PayloadData> payloadDataByPath,
                                                            @NotNull PackingModel packingModel) {
        if (TARGET_CHUNK_BYTE_CANDIDATES.length <= TARGET_CHUNK_SIZE_SHORTLIST_MAX || payloadDataByPath.isEmpty()) {
            return TARGET_CHUNK_BYTE_CANDIDATES.clone();
        }

        ChunkSizeSelectionStats stats = buildChunkSizeSelectionStats(payloadDataByPath, packingModel);
        int lowerTargetBytes;
        if (stats.accessFrameCount() > 0) {
            lowerTargetBytes = (int) Math.ceil(Math.max((double) stats.maxPayloadBytes(), stats.averageFrameAccessBytes()));
        } else {
            lowerTargetBytes = (int) Math.ceil(Math.max((double) stats.maxPayloadBytes(), stats.averagePayloadBytes() * TARGET_CHUNK_SIZE_FALLBACK_GROUPING_MULTIPLIER));
        }

        int preferredTargetBytes = (int) Math.ceil(Math.max(
                Math.max((double) stats.maxPayloadBytes(), stats.averageFrameAccessBytes() * TARGET_CHUNK_SIZE_FRAME_HEADROOM),
                stats.averagePayloadBytes() * TARGET_CHUNK_SIZE_PAYLOAD_GROUPING_MULTIPLIER
        ));
        if (preferredTargetBytes <= 0) {
            preferredTargetBytes = TARGET_CHUNK_BYTES;
        }

        int upperTargetBytes = (stats.maxFrameAccessBytes() > 0)
                ? Math.max(preferredTargetBytes, stats.maxFrameAccessBytes())
                : Math.max(preferredTargetBytes, stats.maxPayloadBytes());

        LinkedHashSet<Integer> shortlistedCandidates = new LinkedHashSet<>(TARGET_CHUNK_SIZE_SHORTLIST_MAX);
        addClosestTargetChunkCandidate(shortlistedCandidates, lowerTargetBytes);
        addClosestTargetChunkCandidate(shortlistedCandidates, preferredTargetBytes);
        addClosestTargetChunkCandidate(shortlistedCandidates, upperTargetBytes);
        while (shortlistedCandidates.size() < Math.min(TARGET_CHUNK_SIZE_SHORTLIST_MAX, TARGET_CHUNK_BYTE_CANDIDATES.length)) {
            Integer nextCandidate = findClosestUnselectedTargetChunkCandidate(shortlistedCandidates, preferredTargetBytes);
            if (nextCandidate == null) {
                break;
            }
            shortlistedCandidates.add(nextCandidate);
        }

        ArrayList<Integer> orderedCandidates = new ArrayList<>(shortlistedCandidates);
        orderedCandidates.sort(Integer::compareTo);
        int[] resolvedCandidates = new int[orderedCandidates.size()];
        for (int index = 0; index < orderedCandidates.size(); index++) {
            resolvedCandidates[index] = orderedCandidates.get(index);
        }
        return resolvedCandidates;
    }

    /** Registers the closest target chunk candidate with this AFMA codec component. */
    protected static void addClosestTargetChunkCandidate(@NotNull Set<Integer> shortlistedCandidates, int desiredTargetBytes) {
        Integer candidate = findClosestTargetChunkCandidate(shortlistedCandidates, desiredTargetBytes);
        if (candidate != null) {
            shortlistedCandidates.add(candidate);
        }
    }

    /** Resolves the closest unselected target chunk candidate for the AFMA codec. */
    @Nullable
    protected static Integer findClosestUnselectedTargetChunkCandidate(@NotNull Set<Integer> shortlistedCandidates, int desiredTargetBytes) {
        return findClosestTargetChunkCandidate(shortlistedCandidates, desiredTargetBytes);
    }

    /** Resolves the closest target chunk candidate for the AFMA codec. */
    @Nullable
    protected static Integer findClosestTargetChunkCandidate(@NotNull Set<Integer> excludedCandidates, int desiredTargetBytes) {
        Integer bestCandidate = null;
        long bestDistance = Long.MAX_VALUE;
        for (int candidate : TARGET_CHUNK_BYTE_CANDIDATES) {
            if (excludedCandidates.contains(candidate)) {
                continue;
            }

            long distance = Math.abs((long) candidate - (long) desiredTargetBytes);
            if (bestCandidate == null
                    || distance < bestDistance
                    || ((distance == bestDistance) && candidate > bestCandidate)) {
                bestCandidate = candidate;
                bestDistance = distance;
            }
        }
        return bestCandidate;
    }

    /** Builds the chunk size selection stats for the AFMA codec. */
    @NotNull
    protected static ChunkSizeSelectionStats buildChunkSizeSelectionStats(@NotNull Map<String, PayloadData> payloadDataByPath,
                                                                         @NotNull PackingModel packingModel) {
        long totalPayloadBytes = 0L;
        int maxPayloadBytes = 0;
        for (PayloadData payloadData : payloadDataByPath.values()) {
            int payloadLength = payloadData.payload().length();
            totalPayloadBytes += payloadLength;
            maxPayloadBytes = Math.max(maxPayloadBytes, payloadLength);
        }

        long totalFrameAccessBytes = 0L;
        int maxFrameAccessBytes = 0;
        int accessFrameCount = 0;
        for (PayloadAccessFrame accessFrame : packingModel.accessFrames()) {
            if (accessFrame.payloadPaths().isEmpty()) {
                continue;
            }

            int frameAccessBytes = 0;
            for (String payloadPath : accessFrame.payloadPaths()) {
                PayloadData payloadData = payloadDataByPath.get(payloadPath);
                if (payloadData != null) {
                    frameAccessBytes += payloadData.payload().length();
                }
            }
            totalFrameAccessBytes += frameAccessBytes;
            maxFrameAccessBytes = Math.max(maxFrameAccessBytes, frameAccessBytes);
            accessFrameCount++;
        }

        double averagePayloadBytes = (double) totalPayloadBytes / (double) Math.max(1, payloadDataByPath.size());
        double averageFrameAccessBytes = (double) totalFrameAccessBytes / (double) Math.max(1, accessFrameCount);
        return new ChunkSizeSelectionStats(
                payloadDataByPath.size(),
                totalPayloadBytes,
                maxPayloadBytes,
                averagePayloadBytes,
                accessFrameCount,
                averageFrameAccessBytes,
                maxFrameAccessBytes
        );
    }

    /** Packs the payloads into the AFMA codec representation. */
    @NotNull
    protected static PackedPayloadArchive packPayloads(@NotNull Map<String, PayloadData> payloadDataByPath,
                                                       @NotNull PackingModel packingModel,
                                                       int targetChunkBytes,
                                                       boolean approximateCompression) {
        ArrayList<String> orderedPayloadPaths = new ArrayList<>(payloadDataByPath.keySet());
        orderedPayloadPaths.sort(Comparator
                .comparing((String path) -> !packingModel.statsByPath().get(path).wasAccessed())
                .thenComparingDouble(path -> packingModel.statsByPath().get(path).center())
                .thenComparingInt(path -> packingModel.statsByPath().get(path).dominantRegionId())
                .thenComparingInt(path -> payloadDataByPath.get(path).insertionOrder()));

        LinkedHashSet<String> remainingPayloadPaths = new LinkedHashSet<>(orderedPayloadPaths);
        ArrayList<ChunkPlanBuilder> chunkBuilders = new ArrayList<>();
        while (!remainingPayloadPaths.isEmpty()) {
            String seedPayloadPath = Objects.requireNonNull(remainingPayloadPaths.iterator().next());
            remainingPayloadPaths.remove(seedPayloadPath);

            PayloadData seedPayload = Objects.requireNonNull(payloadDataByPath.get(seedPayloadPath));
            PayloadPackingStats seedStats = Objects.requireNonNull(packingModel.statsByPath().get(seedPayloadPath));
            ChunkPlanBuilder chunkBuilder = new ChunkPlanBuilder();
            chunkBuilder.add(seedPayloadPath, seedPayload, seedStats);

            if (seedPayload.payload().length() < targetChunkBytes) {
                while (true) {
                    String nextPayloadPath = chooseNextChunkPayload(chunkBuilder, remainingPayloadPaths, payloadDataByPath, packingModel, targetChunkBytes);
                    if (nextPayloadPath == null) {
                        break;
                    }

                    remainingPayloadPaths.remove(nextPayloadPath);
                    chunkBuilder.add(
                            nextPayloadPath,
                            Objects.requireNonNull(payloadDataByPath.get(nextPayloadPath)),
                            Objects.requireNonNull(packingModel.statsByPath().get(nextPayloadPath))
                    );
                }
            }

            chunkBuilders.add(chunkBuilder);
        }

        LinkedHashMap<String, Integer> payloadIdsByPath = new LinkedHashMap<>();
        ArrayList<PayloadLocator> payloadLocators = new ArrayList<>();
        ArrayList<ChunkPlan> chunkPlans = new ArrayList<>();
        HashMap<String, PayloadLocator> payloadLocatorsByPath = new HashMap<>();
        long payloadCompressedBytes = 0L;
        long payloadIndexBodyBytes = 0L;

        for (ChunkPlanBuilder chunkBuilder : chunkBuilders) {
            int chunkId = chunkPlans.size();
            int offset = 0;
            ArrayList<String> chunkPayloadPaths = new ArrayList<>(chunkBuilder.payloadPaths());
            for (String payloadPath : chunkPayloadPaths) {
                PayloadData payloadData = Objects.requireNonNull(payloadDataByPath.get(payloadPath));
                PayloadLocator locator = new PayloadLocator(chunkId, offset, payloadData.payload().length());
                payloadIdsByPath.put(payloadPath, payloadIdsByPath.size());
                payloadLocators.add(locator);
                payloadLocatorsByPath.put(payloadPath, locator);
                payloadIndexBodyBytes += estimateVarIntBytes(chunkId)
                        + estimateVarIntBytes(offset)
                        + estimateVarIntBytes(payloadData.payload().length());
                offset += payloadData.payload().length();
            }

            chunkPlans.add(new ChunkPlan(chunkEntryPath(chunkId), chunkPayloadPaths, chunkBuilder.uncompressedLength()));
            payloadIndexBodyBytes += estimateVarIntBytes(chunkBuilder.uncompressedLength());
            payloadCompressedBytes += estimateChunkCompressedBytes(chunkPayloadPaths, payloadDataByPath, approximateCompression);
        }

        long payloadIndexBytes = 5L
                + estimateVarIntBytes(chunkPlans.size())
                + estimateVarIntBytes(payloadLocators.size())
                + payloadIndexBodyBytes;
        long chunkOverheadBytes = (long) chunkPlans.size() * ESTIMATED_ZIP_CHUNK_OVERHEAD_BYTES;
        CacheSimulationMetrics cacheMetrics = simulateChunkCache(packingModel.accessFrames(), payloadLocatorsByPath);
        long predictedArchiveBytes = payloadCompressedBytes + chunkOverheadBytes + payloadIndexBytes;
        long localityPenaltyBytes = (cacheMetrics.archiveReads() * CHUNK_CACHE_MISS_PENALTY_BYTES)
                + ((long) cacheMetrics.multiChunkFrameCount() * MULTI_CHUNK_FRAME_PENALTY_BYTES);

        return new PackedPayloadArchive(
                Collections.unmodifiableMap(new LinkedHashMap<>(payloadIdsByPath)),
                List.copyOf(payloadLocators),
                List.copyOf(chunkPlans),
                new PackingMetrics(
                        targetChunkBytes,
                        payloadCompressedBytes,
                        payloadIndexBytes,
                        chunkOverheadBytes,
                        predictedArchiveBytes,
                        predictedArchiveBytes + localityPenaltyBytes,
                        cacheMetrics.cacheHits(),
                        cacheMetrics.cacheMisses(),
                        cacheMetrics.archiveReads(),
                        cacheMetrics.evictions(),
                        cacheMetrics.averageChunkReadsPerFrame(),
                        cacheMetrics.multiChunkFrameCount()
                )
        );
    }

    /** Selects the next chunk payload for the AFMA codec. */
    @Nullable
    protected static String chooseNextChunkPayload(@NotNull ChunkPlanBuilder chunkBuilder,
                                                   @NotNull Set<String> remainingPayloadPaths,
                                                   @NotNull Map<String, PayloadData> payloadDataByPath,
                                                   @NotNull PackingModel packingModel,
                                                   int targetChunkBytes) {
        int remainingCapacity = targetChunkBytes - chunkBuilder.uncompressedLength();
        if (remainingCapacity <= 0 || remainingPayloadPaths.isEmpty()) {
            return null;
        }

        String bestPayloadPath = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (String payloadPath : remainingPayloadPaths) {
            PayloadData payloadData = Objects.requireNonNull(payloadDataByPath.get(payloadPath));
            if (payloadData.payload().length() > remainingCapacity) {
                continue;
            }

            double score = scoreChunkCandidate(chunkBuilder, payloadPath, payloadData, packingModel, targetChunkBytes);
            if (score > bestScore) {
                bestScore = score;
                bestPayloadPath = payloadPath;
            }
        }

        if (bestPayloadPath == null) {
            return null;
        }
        if (chunkBuilder.uncompressedLength() >= (int) Math.round(targetChunkBytes * 0.8D) && bestScore < 96D) {
            return null;
        }
        if (chunkBuilder.uncompressedLength() >= (int) Math.round(targetChunkBytes * 0.55D) && bestScore < 0D) {
            return null;
        }
        return bestPayloadPath;
    }

    /** Scores the chunk candidate for AFMA codec candidate selection. */
    protected static double scoreChunkCandidate(@NotNull ChunkPlanBuilder chunkBuilder,
                                                @NotNull String payloadPath,
                                                @NotNull PayloadData payloadData,
                                                @NotNull PackingModel packingModel,
                                                int targetChunkBytes) {
        PayloadPackingStats stats = Objects.requireNonNull(packingModel.statsByPath().get(payloadPath));
        double score = 0D;
        score += chunkBuilder.affinityWeight(packingModel.edgeWeightsByPath(), payloadPath) * 256D;
        if (stats.dominantRegionId() >= 0 && chunkBuilder.regionIds().contains(stats.dominantRegionId())) {
            score += 192D;
        }
        if (stats.wasAccessed() && chunkBuilder.hasAccessWindow()) {
            if (stats.firstAccessIndex() <= (chunkBuilder.maxAccessIndex() + 2)
                    && stats.lastAccessIndex() >= (chunkBuilder.minAccessIndex() - 2)) {
                score += 96D;
            }
            score -= Math.abs(stats.center() - chunkBuilder.center()) * 1.5D;
        } else if (!stats.wasAccessed()) {
            score -= 64D;
        }

        double fillRatio = (double) (chunkBuilder.uncompressedLength() + payloadData.payload().length()) / Math.max(1, targetChunkBytes);
        score += fillRatio * 128D;
        score -= Math.max(0D, (targetChunkBytes - (chunkBuilder.uncompressedLength() + payloadData.payload().length())) / 1024D);
        if ((chunkBuilder.uncompressedLength() + payloadData.payload().length()) == targetChunkBytes) {
            score += 64D;
        }
        return score;
    }

    /** Calculates the chunk compressed bytes for the AFMA codec. */
    protected static long estimateChunkCompressedBytes(@NotNull List<String> chunkPayloadPaths,
                                                       @NotNull Map<String, PayloadData> payloadDataByPath,
                                                       boolean approximateCompression) {
        if (chunkPayloadPaths.isEmpty()) {
            return 0L;
        }

        if (approximateCompression) {
            byte[] tail = EMPTY_BYTES;
            long compressedBytes = 0L;
            for (String payloadPath : chunkPayloadPaths) {
                AfmaStoredPayload payload = Objects.requireNonNull(payloadDataByPath.get(payloadPath)).payload();
                compressedBytes += estimateChunkCompressionDelta(tail, payload);
                tail = appendDeflateTail(tail, payload);
            }
            return Math.max(1L, compressedBytes);
        }

        Deflater deflater = CHUNK_DEFLATER_POOL.get();
        byte[] readBuffer = CHUNK_READ_BUFFER_POOL.get();
        byte[] deflateBuffer = CHUNK_BUFFER_POOL.get();
        deflater.reset();
        long compressedBytes = 0L;
        try {
            for (String payloadPath : chunkPayloadPaths) {
                AfmaStoredPayload payload = Objects.requireNonNull(payloadDataByPath.get(payloadPath)).payload();
                if (payload.isEmpty()) {
                    continue;
                }

                try (InputStream in = payload.openStream()) {
                    int read;
                    while ((read = in.read(readBuffer)) >= 0) {
                        if (read <= 0) {
                            continue;
                        }
                        deflater.setInput(readBuffer, 0, read);
                        while (!deflater.needsInput()) {
                            compressedBytes += deflater.deflate(deflateBuffer);
                        }
                    }
                }
            }

            deflater.finish();
            while (!deflater.finished()) {
                compressedBytes += deflater.deflate(deflateBuffer);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to estimate AFMA chunk compression from stored payload data", ex);
        }
        return Math.max(1L, compressedBytes);
    }

    /** Simulates the chunk cache for the AFMA codec. */
    @NotNull
    protected static CacheSimulationMetrics simulateChunkCache(@NotNull List<PayloadAccessFrame> accessFrames,
                                                               @NotNull Map<String, PayloadLocator> payloadLocatorsByPath) {
        if (accessFrames.isEmpty()) {
            return new CacheSimulationMetrics(0L, 0L, 0L, 0L, 0D, 0);
        }

        LinkedHashMap<Integer, Integer> cachedChunkIds = new LinkedHashMap<>(CACHE_SIMULATION_CHUNK_LIMIT, 0.75F, true);
        long cacheHits = 0L;
        long cacheMisses = 0L;
        long archiveReads = 0L;
        long evictions = 0L;
        long chunkReads = 0L;
        int multiChunkFrameCount = 0;

        for (PayloadAccessFrame accessFrame : accessFrames) {
            LinkedHashSet<Integer> accessedChunkIds = new LinkedHashSet<>();
            for (String payloadPath : accessFrame.payloadPaths()) {
                PayloadLocator locator = payloadLocatorsByPath.get(payloadPath);
                if (locator != null) {
                    accessedChunkIds.add(locator.chunkId());
                }
            }

            chunkReads += accessedChunkIds.size();
            if (accessedChunkIds.size() > 1) {
                multiChunkFrameCount++;
            }

            for (Integer chunkId : accessedChunkIds) {
                if (cachedChunkIds.get(chunkId) != null) {
                    cacheHits++;
                    continue;
                }

                cacheMisses++;
                archiveReads++;
                if (cachedChunkIds.size() >= CACHE_SIMULATION_CHUNK_LIMIT) {
                    var iterator = cachedChunkIds.entrySet().iterator();
                    if (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                        evictions++;
                    }
                }
                cachedChunkIds.put(chunkId, chunkId);
            }
        }

        return new CacheSimulationMetrics(
                cacheHits,
                cacheMisses,
                archiveReads,
                evictions,
                (double) chunkReads / Math.max(1, accessFrames.size()),
                multiChunkFrameCount
        );
    }

    /** Normalizes the payload path for the AFMA codec. */
    @NotNull
    protected static String normalizePayloadPath(@NotNull String payloadPath) {
        return AfmaIoHelper.normalizeEntryPath(payloadPath).toLowerCase(Locale.ROOT);
    }

    /** Writes the var int to the AFMA codec output. */
    protected static void writeVarInt(@NotNull DataOutputStream out, int value) throws IOException {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }

    /** Reads the var int from the AFMA codec input. */
    protected static int readVarInt(@NotNull DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        while (position < 32) {
            int currentByte = in.readUnsignedByte();
            value |= (currentByte & 0x7F) << position;
            if ((currentByte & 0x80) == 0) {
                return value;
            }
            position += 7;
        }
        throw new IOException("AFMA payload index varint is too large");
    }

    /** Models {@code PayloadLocator} state used by the AFMA codec. */
    public record PayloadLocator(int chunkId, int offset, int length) {
    }

    /** Describes the validated {@code ChunkPlan} work selected by the AFMA codec. */
    public record ChunkPlan(@NotNull String entryPath, @NotNull List<String> payloadPaths, int uncompressedLength) {
        /** Validates constructor components before storing this {@code ChunkPlan}. */
        public ChunkPlan {
            entryPath = Objects.requireNonNull(entryPath);
            payloadPaths = List.copyOf(Objects.requireNonNull(payloadPaths));
        }
    }

    /** Models {@code PackedPayloadArchive} state used by the AFMA codec. */
    public record PackedPayloadArchive(@NotNull Map<String, Integer> payloadIdsByPath,
                                       @NotNull List<PayloadLocator> payloadLocators,
                                       @NotNull List<ChunkPlan> chunkPlans,
                                       @NotNull PackingMetrics packingMetrics) {
        /** Validates and defensively copies every collection before storing this packed archive. */
        public PackedPayloadArchive {
            payloadIdsByPath = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payloadIdsByPath)));
            payloadLocators = List.copyOf(Objects.requireNonNull(payloadLocators));
            chunkPlans = List.copyOf(Objects.requireNonNull(chunkPlans));
            packingMetrics = Objects.requireNonNull(packingMetrics);
        }
    }

    /** Describes serialized {@code DecodedPayloadIndex} structure consumed by the AFMA codec. */
    public record DecodedPayloadIndex(@NotNull Map<String, PayloadLocator> payloadLocatorsByPath,
                                      int[] chunkLengths) {
        /** Validates constructor components before storing this {@code DecodedPayloadIndex}. */
        public DecodedPayloadIndex {
            payloadLocatorsByPath = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payloadLocatorsByPath)));
            chunkLengths = Objects.requireNonNull(chunkLengths).clone();
        }
    }

    /** Carries {@code PayloadAccessFrame} data between validated stages of the AFMA codec. */
    public record PayloadAccessFrame(@NotNull List<String> payloadPaths, int keyframeRegionId, int sequenceKind) {
        /** Validates constructor components before storing this {@code PayloadAccessFrame}. */
        public PayloadAccessFrame {
            payloadPaths = List.copyOf(Objects.requireNonNull(payloadPaths));
        }
    }

    /** Models {@code ArchivePackingHints} state used by the AFMA codec. */
    public record ArchivePackingHints(@NotNull List<PayloadAccessFrame> accessFrames) {
        /** Validates constructor components before storing this {@code ArchivePackingHints}. */
        public ArchivePackingHints {
            accessFrames = List.copyOf(Objects.requireNonNull(accessFrames));
        }

        /** Creates the empty AFMA codec variant. */
        @NotNull
        public static ArchivePackingHints empty() {
            return new ArchivePackingHints(List.of());
        }

        /** Returns whether empty. */
        public boolean isEmpty() {
            return this.accessFrames.isEmpty();
        }

        /** Appends resource state to the AFMA codec output state. */
        @NotNull
        public ArchivePackingHints append(@NotNull PayloadAccessFrame accessFrame) {
            ArrayList<PayloadAccessFrame> nextFrames = new ArrayList<>(this.accessFrames.size() + 1);
            nextFrames.addAll(this.accessFrames);
            nextFrames.add(Objects.requireNonNull(accessFrame));
            return new ArchivePackingHints(nextFrames);
        }
    }

    /** Reports {@code PackingMetrics} measurements produced by the AFMA codec. */
    public record PackingMetrics(int targetChunkBytes,
                                 long estimatedCompressedPayloadBytes,
                                 long estimatedPayloadIndexBytes,
                                 long estimatedChunkOverheadBytes,
                                 long predictedArchiveBytes,
                                 long scoredArchiveBytes,
                                 long predictedChunkCacheHits,
                                 long predictedChunkCacheMisses,
                                 long predictedChunkArchiveReads,
                                 long predictedChunkCacheEvictions,
                                 double averageChunkReadsPerFrame,
                                 int multiChunkFrameCount) {
        /** Creates the empty AFMA codec variant. */
        @NotNull
        public static PackingMetrics empty(int targetChunkBytes) {
            return new PackingMetrics(targetChunkBytes, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0D, 0);
        }
    }

    /** Carries {@code PayloadData} data between validated stages of the AFMA codec. */
    protected record PayloadData(@NotNull String path, @NotNull AfmaStoredPayload payload, int insertionOrder) {
    }

    /** Models {@code PackingModel} state used by the AFMA codec. */
    protected record PackingModel(@NotNull List<PayloadAccessFrame> accessFrames,
                                  @NotNull Map<String, PayloadPackingStats> statsByPath,
                                  @NotNull Map<String, Map<String, Integer>> edgeWeightsByPath) {
    }

    /** Reports {@code ChunkSizeSelectionStats} measurements produced by the AFMA codec. */
    protected record ChunkSizeSelectionStats(int payloadCount,
                                             long totalPayloadBytes,
                                             int maxPayloadBytes,
                                             double averagePayloadBytes,
                                             int accessFrameCount,
                                             double averageFrameAccessBytes,
                                             int maxFrameAccessBytes) {
    }

    /** Reports {@code CacheSimulationMetrics} measurements produced by the AFMA codec. */
    protected record CacheSimulationMetrics(long cacheHits, long cacheMisses, long archiveReads, long evictions,
                                            double averageChunkReadsPerFrame, int multiChunkFrameCount) {
    }

    /** Reports {@code PayloadPackingStats} measurements produced by the AFMA codec. */
    protected static final class PayloadPackingStats {

        /** Current insertion order state for this AFMA codec instance. */
        protected final int insertionOrder;
        /** Current access count measured or selected by this AFMA codec instance. */
        protected long accessCount = 0L;
        /** Current weighted access index sum state for this AFMA codec instance. */
        protected long weightedAccessIndexSum = 0L;
        /** Current first access index measured or selected by this AFMA codec instance. */
        protected int firstAccessIndex = Integer.MAX_VALUE;
        /** Current last access index measured or selected by this AFMA codec instance. */
        protected int lastAccessIndex = -1;
        /** Current dominant region id state for this AFMA codec instance. */
        protected int dominantRegionId = -1;
        /** Current dominant region weight state for this AFMA codec instance. */
        protected int dominantRegionWeight = 0;
        /** Holds the regionWeights collection used by this AFMA codec instance. */
        @NotNull
        protected final Map<Integer, Integer> regionWeights = new HashMap<>();

        /** Initializes a new {@code PayloadPackingStats} for AFMA codec use. */
        protected PayloadPackingStats(@NotNull PayloadData payloadData) {
            this.insertionOrder = payloadData.insertionOrder();
        }

        /** Updates the record access state in the AFMA codec. */
        protected void recordAccess(int accessIndex, int regionId, int sequenceKind) {
            this.accessCount++;
            this.weightedAccessIndexSum += accessIndex;
            this.firstAccessIndex = Math.min(this.firstAccessIndex, accessIndex);
            this.lastAccessIndex = Math.max(this.lastAccessIndex, accessIndex);
            int nextRegionWeight = this.regionWeights.merge(regionId, 1, Integer::sum);
            if (nextRegionWeight > this.dominantRegionWeight) {
                this.dominantRegionWeight = nextRegionWeight;
                this.dominantRegionId = regionId;
            }
        }

        /** Returns whether at least one payload access was recorded. */
        public boolean wasAccessed() {
            return this.accessCount > 0L;
        }

        /** Returns the mean recorded access index, or a stable insertion-order fallback when unused. */
        public double center() {
            if (this.accessCount <= 0L) {
                return this.insertionOrder * 1024D;
            }
            return (double) this.weightedAccessIndexSum / (double) this.accessCount;
        }

        /** Returns the first access index produced by the AFMA codec. */
        public int firstAccessIndex() {
            return this.wasAccessed() ? this.firstAccessIndex : Integer.MAX_VALUE;
        }

        /** Returns the last access index produced by the AFMA codec. */
        public int lastAccessIndex() {
            return this.wasAccessed() ? this.lastAccessIndex : -1;
        }

        /** Returns the dominant region id produced by the AFMA codec. */
        public int dominantRegionId() {
            return this.dominantRegionId;
        }

    }

    /** Accumulates validated inputs and builds {@code ChunkPlanBuilder} state for the AFMA codec. */
    protected static final class ChunkPlanBuilder {

        /** Holds the payloadPaths collection used by this AFMA codec instance. */
        @NotNull
        protected final ArrayList<String> payloadPaths = new ArrayList<>();
        /** Current uncompressed length measured or selected by this AFMA codec instance. */
        protected int uncompressedLength = 0;
        /** Current center sum state for this AFMA codec instance. */
        protected double centerSum = 0D;
        /** Current center count measured or selected by this AFMA codec instance. */
        protected int centerCount = 0;
        /** Current min access index measured or selected by this AFMA codec instance. */
        protected int minAccessIndex = Integer.MAX_VALUE;
        /** Current max access index measured or selected by this AFMA codec instance. */
        protected int maxAccessIndex = -1;
        /** Holds the regionIds collection used by this AFMA codec instance. */
        @NotNull
        protected final Set<Integer> regionIds = new HashSet<>();

        /** Adds resource state to the AFMA codec. */
        public void add(@NotNull String payloadPath, @NotNull PayloadData payloadData, @NotNull PayloadPackingStats stats) {
            this.payloadPaths.add(payloadPath);
            this.uncompressedLength += payloadData.payload().length();
            this.centerSum += stats.center();
            this.centerCount++;
            if (stats.wasAccessed()) {
                this.minAccessIndex = Math.min(this.minAccessIndex, stats.firstAccessIndex());
                this.maxAccessIndex = Math.max(this.maxAccessIndex, stats.lastAccessIndex());
            }
            if (stats.dominantRegionId() >= 0) {
                this.regionIds.add(stats.dominantRegionId());
            }
        }

        /** Returns the payload paths produced by the AFMA codec. */
        @NotNull
        public List<String> payloadPaths() {
            return this.payloadPaths;
        }

        /** Returns the uncompressed length produced by the AFMA codec. */
        public int uncompressedLength() {
            return this.uncompressedLength;
        }

        /** Returns whether this chunk has a non-empty frame-access window. */
        public boolean hasAccessWindow() {
            return this.minAccessIndex != Integer.MAX_VALUE && this.maxAccessIndex >= 0;
        }

        /** Returns the min access index produced by the AFMA codec. */
        public int minAccessIndex() {
            return this.minAccessIndex;
        }

        /** Returns the max access index produced by the AFMA codec. */
        public int maxAccessIndex() {
            return this.maxAccessIndex;
        }

        /** Returns the region ids produced by the AFMA codec. */
        @NotNull
        public Set<Integer> regionIds() {
            return this.regionIds;
        }

        /** Returns the mean center of payloads assigned to this chunk, or zero when empty. */
        public double center() {
            return (this.centerCount > 0) ? (this.centerSum / (double) this.centerCount) : 0D;
        }

        /** Returns the affinity weight produced by the AFMA codec. */
        public int affinityWeight(@NotNull Map<String, Map<String, Integer>> edgeWeightsByPath, @NotNull String candidatePath) {
            int weight = 0;
            for (String payloadPath : this.payloadPaths) {
                Map<String, Integer> edgeWeights = edgeWeightsByPath.get(payloadPath);
                if (edgeWeights != null) {
                    weight += edgeWeights.getOrDefault(candidatePath, 0);
                }
            }
            return weight;
        }

    }

}
