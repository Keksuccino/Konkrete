package de.keksuccino.konkrete.placeholder.placeholders.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Injectable non-blocking source for remotely queried server-list values. Calls may arrive concurrently off the Minecraft client thread. */
@FunctionalInterface
public interface ServerStatusProvider {
    /** Returns the latest immutable snapshot, or {@code null} while unavailable; implementations must schedule rather than await network I/O. */
    @Nullable ServerStatus getStatus(@NotNull String address);
}
