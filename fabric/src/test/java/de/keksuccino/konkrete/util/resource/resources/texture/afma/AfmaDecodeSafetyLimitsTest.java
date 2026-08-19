package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.ResourceSafetyLimits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AfmaDecodeSafetyLimitsTest {

    private ResourceSafetyLimits previousLimits;

    @BeforeEach
    void installSmallLimits() {
        this.previousLimits = ResourceRuntime.getSafetyLimits();
        ResourceRuntime.setSafetyLimits(new ResourceSafetyLimits(16, 16, 256L, 2, 3, 128L, 8L));
    }

    @AfterEach
    void restoreLimits() {
        ResourceRuntime.setSafetyLimits(this.previousLimits);
    }

    @Test
    void rejectsFrameCountBeforeAllocatingDescriptorList() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(AfmaBinaryFrameIndexHelper.FRAME_INDEX_MAGIC);
            out.writeByte(AfmaBinaryFrameIndexHelper.FRAME_INDEX_VERSION);
            writeVarInt(out, 3);
        }

        assertThrows(IOException.class, () -> AfmaBinaryFrameIndexHelper.decodeFrameIndex(bytes.toByteArray()));
    }

    @Test
    void rejectsPayloadCountsBeforeAllocatingChunkArray() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(AfmaChunkedPayloadHelper.PAYLOAD_INDEX_MAGIC);
            out.writeByte(AfmaChunkedPayloadHelper.PAYLOAD_INDEX_VERSION);
            writeVarInt(out, 4);
            writeVarInt(out, 0);
        }

        assertThrows(IOException.class, () -> AfmaChunkedPayloadHelper.decodePayloadIndex(bytes.toByteArray()));
    }

    @Test
    void rejectsCumulativeChunkBytesAndOversizedContainerSections() throws Exception {
        byte[] descriptorBytes;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            writeDescriptor(out, 4);
            writeDescriptor(out, 5);
            descriptorBytes = bytes.toByteArray();
        }

        assertThrows(IOException.class, () -> AfmaContainerV2.readChunkDescriptors(new DataInputStream(new ByteArrayInputStream(descriptorBytes)), 2));
        AfmaContainerV2.Header header = new AfmaContainerV2.Header(4, 4, 1, 0);
        assertThrows(IOException.class, () -> AfmaDecoder.validateContainerHeaderLayout(header, 64L));
    }

    private static void writeDescriptor(DataOutputStream out, int uncompressedLength) throws IOException {
        out.writeLong(AfmaContainerV2.HEADER_BYTES);
        out.writeInt(1);
        out.writeInt(uncompressedLength);
        out.writeByte(AfmaContainerV2.COMPRESSION_STORED);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }
}
