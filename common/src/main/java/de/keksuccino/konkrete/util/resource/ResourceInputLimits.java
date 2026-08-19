package de.keksuccino.konkrete.util.resource;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Shared bounded-I/O primitives used before untrusted inputs reach native or archive decoders. */
public final class ResourceInputLimits {

    private ResourceInputLimits() {}

    /** Copies at most {@code maxBytes}, failing before writing a byte beyond the limit. */
    public static long copy(@NotNull InputStream input, @NotNull OutputStream output, long maxBytes, @NotNull String description) throws IOException {
        validateByteLimit(maxBytes);
        byte[] buffer = new byte[8192];
        long total = 0L;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) continue;
            total = Math.addExact(total, read);
            if (total > maxBytes) throw new IOException(description + " exceeds the configured " + maxBytes + " byte limit");
            output.write(buffer, 0, read);
        }
        return total;
    }

    /** Reads at most {@code maxBytes}, failing before retaining a byte beyond the limit. */
    @NotNull
    public static byte[] readAllBytes(@NotNull InputStream input, long maxBytes, @NotNull String description) throws IOException {
        validateByteLimit(maxBytes);
        int initialCapacity = (int) Math.min(maxBytes, 8192L);
        ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity);
        copy(input, output, maxBytes, description);
        return output.toByteArray();
    }

    /** Wraps a stream so reads fail immediately after the configured byte limit is reached. */
    @NotNull
    public static InputStream bounded(@NotNull InputStream input, long maxBytes, @NotNull String description) {
        validateByteLimit(maxBytes);
        return new FilterInputStream(input) {

            private long remaining = maxBytes;

            /** Reads resource data from the resource runtime input. */
            @Override
            public int read() throws IOException {
                if (this.remaining == 0L) {
                    int overflow = super.read();
                    if (overflow >= 0) throw new IOException(description + " exceeds the configured " + maxBytes + " byte limit");
                    return -1;
                }
                int value = super.read();
                if (value >= 0) this.remaining--;
                return value;
            }

            /** Reads resource data from the resource runtime input. */
            @Override
            public int read(byte @NotNull [] bytes, int offset, int length) throws IOException {
                if (length == 0) return 0;
                if (this.remaining == 0L) return this.read() < 0 ? -1 : 1;
                int read = super.read(bytes, offset, (int) Math.min(length, this.remaining));
                if (read > 0) this.remaining -= read;
                return read;
            }
        };
    }

    private static void validateByteLimit(long maxBytes) {
        if (maxBytes < 0L) throw new IllegalArgumentException("Byte limit must not be negative");
    }
}
