package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaPayloadArchiveLayout;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaStoredPayload;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Describes the validated {@code AfmaEncodePlan} work selected by the AFMA creator. */
public class AfmaEncodePlan implements AutoCloseable {

    /** Current metadata state for this AFMA creator instance. */
    @NotNull
    protected final AfmaMetadata metadata;
    /** Current frame index measured or selected by this AFMA creator instance. */
    @NotNull
    protected final AfmaFrameIndex frameIndex;
    /** Holds the payloads collection used by this AFMA creator instance. */
    @NotNull
    protected final LinkedHashMap<String, AfmaStoredPayload> payloads;
    /** Current payload archive state for this AFMA creator instance. */
    @NotNull
    protected final AfmaPayloadArchiveLayout payloadArchive;
    /** Current total payload bytes measured or selected by this AFMA creator instance. */
    protected final long totalPayloadBytes;

    /** Initializes a new {@code AfmaEncodePlan} for AFMA creator use. */
    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads) {
        this(metadata, frameIndex, payloads, AfmaPayloadArchiveLayout.build(payloads, frameIndex, metadata.getLoopCount()));
    }

    /** Initializes a new {@code AfmaEncodePlan} for AFMA creator use. */
    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex,
                          @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads,
                          @NotNull AfmaPayloadArchiveLayout payloadArchive) {
        this.metadata = Objects.requireNonNull(metadata);
        this.frameIndex = Objects.requireNonNull(frameIndex);
        this.payloads = new LinkedHashMap<>(Objects.requireNonNull(payloads));
        this.payloadArchive = Objects.requireNonNull(payloadArchive);
        this.totalPayloadBytes = calculateTotalPayloadBytes(this.payloads);
    }

    /** Initializes a new {@code AfmaEncodePlan} for AFMA creator use. */
    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex,
                          @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads,
                          @NotNull AfmaChunkedPayloadHelper.PackedPayloadArchive ignoredLegacyArchive) {
        this(metadata, frameIndex, payloads, AfmaPayloadArchiveLayout.build(payloads, frameIndex, metadata.getLoopCount()));
    }

    /** Returns the metadata used by this AFMA creator instance. */
    @NotNull
    public AfmaMetadata getMetadata() {
        return this.metadata;
    }

    /** Returns the frame index used by this AFMA creator instance. */
    @NotNull
    public AfmaFrameIndex getFrameIndex() {
        return this.frameIndex;
    }

    /** Returns the payloads used by this AFMA creator instance. */
    @NotNull
    public LinkedHashMap<String, AfmaStoredPayload> getPayloads() {
        return new LinkedHashMap<>(this.payloads);
    }

    /** Returns the payload archive used by this AFMA creator instance. */
    @NotNull
    public AfmaPayloadArchiveLayout getPayloadArchive() {
        return this.payloadArchive;
    }

    /** Returns the total payload bytes used by this AFMA creator instance. */
    public long getTotalPayloadBytes() {
        return this.totalPayloadBytes;
    }

    /** Counts the frames in the AFMA creator. */
    public int countFrames(@NotNull AfmaFrameOperationType type) {
        int count = 0;
        count += countFrames(type, this.frameIndex.getFrames());
        count += countFrames(type, this.frameIndex.getIntroFrames());
        return count;
    }

    /** Counts the frames in the AFMA creator. */
    protected static int countFrames(@NotNull AfmaFrameOperationType type, @NotNull java.util.List<AfmaFrameDescriptor> frames) {
        int count = 0;
        for (AfmaFrameDescriptor frame : frames) {
            if ((frame != null) && (frame.getType() == type)) {
                count++;
            }
        }
        return count;
    }

    /** Calculates the total payload bytes for the AFMA creator. */
    protected static long calculateTotalPayloadBytes(@NotNull Map<String, AfmaStoredPayload> payloads) {
        long total = 0L;
        for (AfmaStoredPayload payload : payloads.values()) {
            if (payload != null) {
                total += payload.length();
            }
        }
        return total;
    }

    /** Closes every payload selected by this plan; repeated calls are harmless through payload idempotence. */
    @Override
    public void close() {
        for (AfmaStoredPayload payload : this.payloads.values()) {
            if (payload != null) {
                payload.close();
            }
        }
    }

}
