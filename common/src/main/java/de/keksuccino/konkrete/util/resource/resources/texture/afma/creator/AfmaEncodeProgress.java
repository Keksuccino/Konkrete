package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Reports mutable or snapshot {@code AfmaEncodeProgress} state for the AFMA creator. */
public record AfmaEncodeProgress(
        @NotNull Phase phase,
        @NotNull String task,
        @Nullable String detail,
        double progress
) {

    /** Validates constructor components before storing this {@code AfmaEncodeProgress}. */
    public AfmaEncodeProgress {
        progress = Math.max(0.0D, Math.min(1.0D, progress));
    }

    /** Returns whether idle. */
    public static @NotNull AfmaEncodeProgress idle() {
        return new AfmaEncodeProgress(Phase.IDLE, "Idle", null, 0.0D);
    }

    /** Models {@code Phase} state used by the AFMA creator. */
    public enum Phase {
        /** Indicates that no encode work has started. */
        IDLE,
        /** Indicates that source paths and frame inputs are being validated. */
        VALIDATING_SOURCES,
        /** Indicates that source frames are being analyzed and planned. */
        ANALYZING_FRAMES,
        /** Indicates that planned payloads are being packed into the AFMA archive. */
        PACKING_ARCHIVE,
        /** Indicates that archive encoding completed successfully. */
        COMPLETE,
        /** Indicates that the operation terminated with a failure. */
        FAILED,
        /** Indicates that cancellation terminated the operation. */
        CANCELLED
    }

}
