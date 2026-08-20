package de.keksuccino.konkrete.util.rendering.glsl;

import org.jetbrains.annotations.NotNull;

/** Pure description of the A-to-D immediate-swap feedback ordering used by the GPU runtime. */
public final class GlslPassGraph {

    private GlslPassGraph() {
    }

    /** Computes buffer version from the supplied inputs. */
    @NotNull
    public static BufferVersion resolveBufferVersion(int renderingPassIndex, int referencedBufferIndex, boolean imagePass, @NotNull boolean[] activeBufferPasses) {
        if (referencedBufferIndex < 0 || referencedBufferIndex >= activeBufferPasses.length || !activeBufferPasses[referencedBufferIndex]) {
            return BufferVersion.FALLBACK;
        }
        if (imagePass || referencedBufferIndex < renderingPassIndex) {
            return BufferVersion.CURRENT_FRAME;
        }
        return BufferVersion.PREVIOUS_FRAME;
    }

    /** Identifies one version of buffer data. */
    public enum BufferVersion {

        /** Reads the previous frame feedback-buffer version. */
        PREVIOUS_FRAME,
        /** Reads the current frame feedback-buffer version. */
        CURRENT_FRAME,
        /** Reads the fallback feedback-buffer version. */
        FALLBACK

    }

}
