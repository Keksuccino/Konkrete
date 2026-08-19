package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Models {@code AfmaContainerV2} state used by the AFMA codec. */
public final class AfmaContainerV2 {

    /** Serialized magic value identifying magic data. */
    public static final int MAGIC = 0x41464D32; // AFM2
    /** Serialized version value for version data. */
    public static final int VERSION = 1;
    /** Byte budget for header in the AFMA codec. */
    public static final int HEADER_BYTES = 21;
    /** Byte budget for chunk descriptor in the AFMA codec. */
    public static final int CHUNK_DESCRIPTOR_BYTES = 17;
    /** Serialized discriminator for compression stored. */
    public static final int COMPRESSION_STORED = 0;
    /** Serialized discriminator for compression raw deflate. */
    public static final int COMPRESSION_RAW_DEFLATE = 1;

    private AfmaContainerV2() {
    }

    /** Returns whether an integer matches the AFMA v2 container magic. */
    public static boolean isMagic(int magic) {
        return magic == MAGIC;
    }

    /** Reads the header from the AFMA codec input. */
    @NotNull
    public static Header readHeader(@NotNull DataInput in) throws IOException {
        Objects.requireNonNull(in);
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new IOException("AFMA v2 container is missing its magic header");
        }

        int version = in.readUnsignedByte();
        if (version != VERSION) {
            throw new IOException("Unsupported AFMA v2 container version: " + version);
        }

        int metadataLength = in.readInt();
        int frameIndexLength = in.readInt();
        int payloadTableLength = in.readInt();
        int chunkCount = in.readInt();
        if (metadataLength < 0 || frameIndexLength < 0 || payloadTableLength < 0 || chunkCount < 0) {
            throw new IOException("AFMA v2 container header contains invalid lengths");
        }
        return new Header(metadataLength, frameIndexLength, payloadTableLength, chunkCount);
    }

    /** Writes the header to the AFMA codec output. */
    public static void writeHeader(@NotNull DataOutput out, @NotNull Header header) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(header);
        out.writeInt(MAGIC);
        out.writeByte(VERSION);
        out.writeInt(header.metadataLength());
        out.writeInt(header.frameIndexLength());
        out.writeInt(header.payloadTableLength());
        out.writeInt(header.chunkCount());
    }

    /** Reads the chunk descriptors from the AFMA codec input. */
    @NotNull
    public static List<ChunkDescriptor> readChunkDescriptors(@NotNull DataInput in, int chunkCount) throws IOException {
        Objects.requireNonNull(in);
        if (chunkCount < 0 || chunkCount > ResourceRuntime.getSafetyLimits().maxArchiveEntries()) throw new IOException("AFMA v2 chunk count exceeds resource safety limits");
        ArrayList<ChunkDescriptor> descriptors = new ArrayList<>(Math.min(chunkCount, 256));
        long decompressedBytes = 0L;
        for (int i = 0; i < chunkCount; i++) {
            long fileOffset = in.readLong();
            int compressedLength = in.readInt();
            int uncompressedLength = in.readInt();
            int compressionMode = in.readUnsignedByte();
            if (fileOffset < 0L || compressedLength < 0 || compressedLength > ResourceRuntime.getSafetyLimits().maxArchiveBytes() || uncompressedLength < 0 || uncompressedLength > ResourceRuntime.getSafetyLimits().maxDecompressedBytes()) {
                throw new IOException("AFMA v2 chunk descriptor exceeds resource safety limits");
            }
            decompressedBytes = Math.addExact(decompressedBytes, uncompressedLength);
            if (decompressedBytes > ResourceRuntime.getSafetyLimits().maxDecompressedBytes()) throw new IOException("AFMA v2 chunks exceed decompressed-byte safety limits");
            if ((compressionMode != COMPRESSION_STORED) && (compressionMode != COMPRESSION_RAW_DEFLATE)) {
                throw new IOException("Unsupported AFMA v2 chunk compression mode: " + compressionMode);
            }
            descriptors.add(new ChunkDescriptor(fileOffset, compressedLength, uncompressedLength, compressionMode));
        }
        return descriptors;
    }

    /** Writes the chunk descriptor to the AFMA codec output. */
    public static void writeChunkDescriptor(@NotNull DataOutput out, @NotNull ChunkDescriptor descriptor) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(descriptor);
        out.writeLong(descriptor.fileOffset());
        out.writeInt(descriptor.compressedLength());
        out.writeInt(descriptor.uncompressedLength());
        out.writeByte(descriptor.compressionMode());
    }

    /** Describes serialized {@code Header} structure consumed by the AFMA codec. */
    public record Header(int metadataLength, int frameIndexLength, int payloadTableLength, int chunkCount) {
    }

    /** Describes serialized {@code ChunkDescriptor} structure consumed by the AFMA codec. */
    public record ChunkDescriptor(long fileOffset, int compressedLength, int uncompressedLength, int compressionMode) {
    }

}
