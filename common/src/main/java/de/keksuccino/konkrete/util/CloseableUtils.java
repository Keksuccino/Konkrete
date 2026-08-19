package de.keksuccino.konkrete.util;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import java.io.Closeable;

/** Utility methods for closeable. */
public class CloseableUtils {

    /** Closes quietly. */
    public static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable == null) return;
        if (closeable instanceof Closeable c) {
            IOUtils.closeQuietly(c);
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignore) {}
    }

}
