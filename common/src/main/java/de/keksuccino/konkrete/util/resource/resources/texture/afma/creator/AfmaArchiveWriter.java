package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaBinaryFrameIndexHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaContainerV2;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaIoHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaPayloadArchiveLayout;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaStoredPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.zip.Deflater;

/** Writes an AFMA archive from a validated encode plan and reports deterministic progress. */
public class AfmaArchiveWriter {

    /** Configured deflate level used by the AFMA creator. */
    protected static final int DEFLATE_LEVEL = 9;
    /** Byte budget for io buffer in the AFMA creator. */
    protected static final int IO_BUFFER_BYTES = 8192;

    /** Writes resource data to the AFMA creator output. */
    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile) throws IOException {
        this.write(plan, outputFile, null);
    }

    /** Writes resource data to the AFMA creator output. */
    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile, @Nullable BooleanSupplier cancellationRequested) throws IOException {
        this.write(plan, outputFile, cancellationRequested, null);
    }

    /** Writes resource data to the AFMA creator output. */
    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile, @Nullable BooleanSupplier cancellationRequested,
                      @Nullable ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(plan);
        Objects.requireNonNull(outputFile);

        File parent = outputFile.getParentFile();
        if ((parent != null) && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create AFMA output directory: " + parent.getAbsolutePath());
        }

        LinkedHashMap<String, AfmaStoredPayload> payloads = plan.getPayloads();
        HashMap<String, AfmaStoredPayload> payloadsByNormalizedPath = buildNormalizedPayloadMap(payloads);
        AfmaPayloadArchiveLayout payloadArchive = plan.getPayloadArchive();

        byte[] metadataBytes = encodeMetadataJson(plan.getMetadata());
        byte[] frameIndexBytes = AfmaBinaryFrameIndexHelper.encodeFrameIndex(plan.getFrameIndex(), payloadArchive.payloadIdsByPath());
        byte[] payloadTableBytes = payloadArchive.encodePayloadTable();

        int totalEntries = payloadArchive.chunkPlans().size() + 3;
        int writtenEntries = 0;

        checkCancelled(cancellationRequested);
        writtenEntries++;
        reportProgress(progressListener, "metadata.json", writtenEntries, totalEntries);
        checkCancelled(cancellationRequested);
        writtenEntries++;
        reportProgress(progressListener, AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH, writtenEntries, totalEntries);
        checkCancelled(cancellationRequested);
        writtenEntries++;
        reportProgress(progressListener, "payload_table.bin", writtenEntries, totalEntries);

        File blobTempFile = File.createTempFile("afma_v2_blobs_", ".tmp", parent);
        ArrayList<RelativeChunkDescriptor> relativeChunkDescriptors = new ArrayList<>(payloadArchive.chunkPlans().size());
        try {
            long blobOffset = 0L;
            try (BufferedOutputStream blobOut = new BufferedOutputStream(new FileOutputStream(blobTempFile))) {
                for (AfmaPayloadArchiveLayout.ChunkPlan chunkPlan : payloadArchive.chunkPlans()) {
                    checkCancelled(cancellationRequested);
                    ChunkBlob chunkBlob = this.buildChunkBlob(chunkPlan, payloadsByNormalizedPath, cancellationRequested);
                    blobOut.write(chunkBlob.bytes());
                    relativeChunkDescriptors.add(new RelativeChunkDescriptor(
                            blobOffset,
                            chunkBlob.bytes().length,
                            chunkPlan.uncompressedLength(),
                            chunkBlob.compressionMode()
                    ));
                    blobOffset += chunkBlob.bytes().length;
                    writtenEntries++;
                    reportProgress(progressListener, AfmaChunkedPayloadHelper.chunkEntryPath(chunkPlan.chunkId()), writtenEntries, totalEntries);
                }
                blobOut.flush();
            }

            long chunkDataOffset = AfmaContainerV2.HEADER_BYTES
                    + metadataBytes.length
                    + frameIndexBytes.length
                    + payloadTableBytes.length
                    + ((long) relativeChunkDescriptors.size() * (long) AfmaContainerV2.CHUNK_DESCRIPTOR_BYTES);

            ArrayList<AfmaContainerV2.ChunkDescriptor> chunkDescriptors = new ArrayList<>(relativeChunkDescriptors.size());
            for (RelativeChunkDescriptor relativeDescriptor : relativeChunkDescriptors) {
                chunkDescriptors.add(new AfmaContainerV2.ChunkDescriptor(
                        chunkDataOffset + relativeDescriptor.relativeOffset(),
                        relativeDescriptor.compressedLength(),
                        relativeDescriptor.uncompressedLength(),
                        relativeDescriptor.compressionMode()
                ));
            }

            checkCancelled(cancellationRequested);
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
                 InputStream blobIn = new BufferedInputStream(new FileInputStream(blobTempFile))) {
                AfmaContainerV2.writeHeader(out, new AfmaContainerV2.Header(
                        metadataBytes.length,
                        frameIndexBytes.length,
                        payloadTableBytes.length,
                        chunkDescriptors.size()
                ));
                out.write(metadataBytes);
                out.write(frameIndexBytes);
                out.write(payloadTableBytes);
                for (AfmaContainerV2.ChunkDescriptor chunkDescriptor : chunkDescriptors) {
                    AfmaContainerV2.writeChunkDescriptor(out, chunkDescriptor);
                }
                out.flush();
                blobIn.transferTo(out);
                out.flush();
            }
        } finally {
            if (blobTempFile.exists()) {
                blobTempFile.delete();
            }
        }
    }

    /** Builds the normalized payload map for the AFMA creator. */
    @NotNull
    protected static HashMap<String, AfmaStoredPayload> buildNormalizedPayloadMap(@NotNull Map<String, AfmaStoredPayload> payloads) {
        HashMap<String, AfmaStoredPayload> payloadsByNormalizedPath = new HashMap<>(payloads.size());
        for (Map.Entry<String, AfmaStoredPayload> entry : payloads.entrySet()) {
            payloadsByNormalizedPath.putIfAbsent(normalizePath(entry.getKey()), entry.getValue());
        }
        return payloadsByNormalizedPath;
    }

    /** Builds the chunk blob for the AFMA creator. */
    @NotNull
    protected ChunkBlob buildChunkBlob(@NotNull AfmaPayloadArchiveLayout.ChunkPlan chunkPlan,
                                       @NotNull Map<String, AfmaStoredPayload> payloadsByNormalizedPath,
                                       @Nullable BooleanSupplier cancellationRequested) throws IOException {
        byte[] rawChunkBytes = this.materializeChunkBytes(chunkPlan, payloadsByNormalizedPath, cancellationRequested);
        if (rawChunkBytes.length <= 0) {
            return new ChunkBlob(rawChunkBytes, AfmaContainerV2.COMPRESSION_STORED);
        }

        Deflater deflater = new Deflater(DEFLATE_LEVEL, true);
        byte[] deflateBuffer = new byte[IO_BUFFER_BYTES];
        try {
            ByteArrayOutputStream compressedOut = new ByteArrayOutputStream(Math.max(32, Math.min(rawChunkBytes.length, AfmaPayloadArchiveLayout.TARGET_CHUNK_BYTES)));
            deflater.setInput(rawChunkBytes);
            deflater.finish();
            while (!deflater.finished()) {
                drainDeflater(compressedOut, deflater, deflateBuffer);
            }
            byte[] compressedChunkBytes = compressedOut.toByteArray();
            if (compressedChunkBytes.length >= rawChunkBytes.length) {
                return new ChunkBlob(rawChunkBytes, AfmaContainerV2.COMPRESSION_STORED);
            }
            return new ChunkBlob(compressedChunkBytes, AfmaContainerV2.COMPRESSION_RAW_DEFLATE);
        } finally {
            deflater.end();
        }
    }

    /** Materializes the chunk bytes for the AFMA creator. */
    @NotNull
    protected byte[] materializeChunkBytes(@NotNull AfmaPayloadArchiveLayout.ChunkPlan chunkPlan,
                                           @NotNull Map<String, AfmaStoredPayload> payloadsByNormalizedPath,
                                           @Nullable BooleanSupplier cancellationRequested) throws IOException {
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream(Math.max(32, chunkPlan.uncompressedLength()));
        byte[] readBuffer = new byte[IO_BUFFER_BYTES];
        for (String payloadPath : chunkPlan.payloadPaths()) {
            checkCancelled(cancellationRequested);
            AfmaStoredPayload payload = Objects.requireNonNull(payloadsByNormalizedPath.get(normalizePath(payloadPath)),
                    "AFMA payload was NULL for " + payloadPath);
            try (InputStream in = payload.openStream()) {
                int read;
                while ((read = in.read(readBuffer)) >= 0) {
                    if (read <= 0) {
                        continue;
                    }
                    rawOut.write(readBuffer, 0, read);
                }
            }
        }
        byte[] rawChunkBytes = rawOut.toByteArray();
        if (rawChunkBytes.length != chunkPlan.uncompressedLength()) {
            throw new IOException("AFMA v2 chunk length mismatch while materializing " + AfmaChunkedPayloadHelper.chunkEntryPath(chunkPlan.chunkId()));
        }
        return rawChunkBytes;
    }

    /** Drains the deflater buffer for the AFMA creator. */
    protected static int drainDeflater(@NotNull OutputStream out, @NotNull Deflater deflater, @NotNull byte[] deflateBuffer) throws IOException {
        int writtenBytes = 0;
        while (true) {
            int compressed = deflater.deflate(deflateBuffer);
            if (compressed > 0) {
                out.write(deflateBuffer, 0, compressed);
                writtenBytes += compressed;
                continue;
            }
            if (deflater.needsInput() || deflater.finished()) {
                return writtenBytes;
            }
            throw new IOException("AFMA v2 payload deflater stalled unexpectedly");
        }
    }

    /** Normalizes the path for the AFMA creator. */
    @NotNull
    protected static String normalizePath(@NotNull String path) {
        return AfmaIoHelper.normalizeEntryPath(path).toLowerCase(Locale.ROOT);
    }

    /** Checks the cancelled before continuing the AFMA creator operation. */
    protected static void checkCancelled(@Nullable BooleanSupplier cancellationRequested) {
        if ((cancellationRequested != null) && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("AFMA archive writing was cancelled");
        }
    }

    /** Reports the progress from the AFMA creator. */
    protected static void reportProgress(@Nullable ProgressListener progressListener, @NotNull String path, int writtenEntries, int totalEntries) {
        if (progressListener != null) {
            progressListener.update(path, (double) writtenEntries / Math.max(1, totalEntries));
        }
    }

    /** Encodes the metadata json for the AFMA creator. */
    @NotNull
    protected static byte[] encodeMetadataJson(@NotNull AfmaMetadata metadata) {
        StringBuilder json = new StringBuilder(256);
        json.append('{');
        boolean firstProperty = true;
        firstProperty = appendJsonStringProperty(json, firstProperty, "format", metadata.getFormat());
        firstProperty = appendJsonNumberProperty(json, firstProperty, "format_version", metadata.getFormatVersion());
        firstProperty = appendJsonNumberProperty(json, firstProperty, "canvas_width", metadata.getCanvasWidth());
        firstProperty = appendJsonNumberProperty(json, firstProperty, "canvas_height", metadata.getCanvasHeight());
        firstProperty = appendJsonNumberProperty(json, firstProperty, "loop_count", metadata.getLoopCount());
        firstProperty = appendJsonNumberProperty(json, firstProperty, "frame_time", metadata.getFrameTime());
        firstProperty = appendJsonNumberProperty(json, firstProperty, "frame_time_intro", metadata.getFrameTimeIntro());
        firstProperty = appendJsonLongMapProperty(json, firstProperty, "custom_frame_times", metadata.getCustomFrameTimes());
        firstProperty = appendJsonLongMapProperty(json, firstProperty, "custom_frame_times_intro", metadata.getCustomFrameTimesIntro());
        firstProperty = appendJsonNumberProperty(json, firstProperty, "keyframe_interval", metadata.getKeyframeInterval());
        firstProperty = appendEncodingProperty(json, firstProperty, metadata.getEncoding());
        appendCreatorProperty(json, firstProperty, metadata.getCreator());
        json.append('}');
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Appends the encoding property to the AFMA creator output state. */
    protected static boolean appendEncodingProperty(@NotNull StringBuilder json,
                                                    boolean firstProperty,
                                                    @Nullable AfmaMetadata.Encoding encoding) {
        if (encoding == null) {
            return firstProperty;
        }
        appendJsonPropertyPrefix(json, firstProperty, "encoding");
        json.append('{');
        boolean firstEncodingProperty = true;
        firstEncodingProperty = appendJsonStringProperty(json, firstEncodingProperty, "intra_payload_codec", encoding.getIntraPayloadCodec());
        firstEncodingProperty = appendJsonStringProperty(json, firstEncodingProperty, "color_model", encoding.getColorModel());
        firstEncodingProperty = appendJsonBooleanProperty(json, firstEncodingProperty, "rect_copy_enabled", encoding.isRectCopyEnabled());
        appendJsonBooleanProperty(json, firstEncodingProperty, "duplicate_frame_elision", encoding.isDuplicateFrameElision());
        json.append('}');
        return false;
    }

    /** Appends the creator property to the AFMA creator output state. */
    protected static boolean appendCreatorProperty(@NotNull StringBuilder json,
                                                   boolean firstProperty,
                                                   @Nullable AfmaMetadata.Creator creator) {
        if (creator == null) {
            return firstProperty;
        }
        appendJsonPropertyPrefix(json, firstProperty, "creator");
        json.append('{');
        boolean firstCreatorProperty = true;
        firstCreatorProperty = appendJsonStringProperty(json, firstCreatorProperty, "tool", creator.getTool());
        firstCreatorProperty = appendJsonStringProperty(json, firstCreatorProperty, "tool_version", creator.getToolVersion());
        appendJsonStringProperty(json, firstCreatorProperty, "created_at_utc", creator.getCreatedAtUtc());
        json.append('}');
        return false;
    }

    /** Appends the json string property to the AFMA creator output state. */
    protected static boolean appendJsonStringProperty(@NotNull StringBuilder json,
                                                      boolean firstProperty,
                                                      @NotNull String propertyName,
                                                      @Nullable String value) {
        if (value == null) {
            return firstProperty;
        }
        appendJsonPropertyPrefix(json, firstProperty, propertyName);
        appendJsonString(json, value);
        return false;
    }

    /** Appends the json number property to the AFMA creator output state. */
    protected static boolean appendJsonNumberProperty(@NotNull StringBuilder json,
                                                      boolean firstProperty,
                                                      @NotNull String propertyName,
                                                      long value) {
        appendJsonPropertyPrefix(json, firstProperty, propertyName);
        json.append(value);
        return false;
    }

    /** Appends the json boolean property to the AFMA creator output state. */
    protected static boolean appendJsonBooleanProperty(@NotNull StringBuilder json,
                                                       boolean firstProperty,
                                                       @NotNull String propertyName,
                                                       boolean value) {
        appendJsonPropertyPrefix(json, firstProperty, propertyName);
        json.append(value);
        return false;
    }

    /** Appends the json long map property to the AFMA creator output state. */
    protected static boolean appendJsonLongMapProperty(@NotNull StringBuilder json,
                                                       boolean firstProperty,
                                                       @NotNull String propertyName,
                                                       @NotNull Map<Integer, Long> values) {
        if (values.isEmpty()) {
            return firstProperty;
        }
        appendJsonPropertyPrefix(json, firstProperty, propertyName);
        json.append('{');
        boolean firstEntry = true;
        for (Map.Entry<Integer, Long> entry : values.entrySet()) {
            if (!firstEntry) {
                json.append(',');
            }
            appendJsonString(json, Integer.toString(entry.getKey()));
            json.append(':').append(Objects.requireNonNull(entry.getValue()));
            firstEntry = false;
        }
        json.append('}');
        return false;
    }

    /** Appends the json property prefix to the AFMA creator output state. */
    protected static void appendJsonPropertyPrefix(@NotNull StringBuilder json,
                                                   boolean firstProperty,
                                                   @NotNull String propertyName) {
        if (!firstProperty) {
            json.append(',');
        }
        appendJsonString(json, propertyName);
        json.append(':');
    }

    /** Appends the json string to the AFMA creator output state. */
    protected static void appendJsonString(@NotNull StringBuilder json, @NotNull String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> json.append("\\\\");
                case '"' -> json.append("\\\"");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }

    /** Describes serialized {@code RelativeChunkDescriptor} structure consumed by the AFMA creator. */
    protected record RelativeChunkDescriptor(long relativeOffset, int compressedLength, int uncompressedLength, int compressionMode) {
    }

    /** Carries {@code ChunkBlob} data between validated stages of the AFMA creator. */
    protected record ChunkBlob(@NotNull byte[] bytes, int compressionMode) {
    }

    /** Receives {@code ProgressListener} progress or lifecycle notifications from the AFMA creator. */
    @FunctionalInterface
    public interface ProgressListener {
        /** Receives the active payload path and normalized completion fraction. */
        void update(@NotNull String path, double progress);
    }

}
