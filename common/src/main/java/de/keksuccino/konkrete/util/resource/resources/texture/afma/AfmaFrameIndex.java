package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Describes serialized {@code AfmaFrameIndex} structure consumed by the AFMA codec. */
public class AfmaFrameIndex {

    /** Holds the frames collection used by this AFMA codec instance. */
    @Nullable
    protected List<AfmaFrameDescriptor> frames;
    /** Holds the intro frames collection used by this AFMA codec instance. */
    @Nullable
    protected List<AfmaFrameDescriptor> intro_frames;

    /** Initializes a new {@code AfmaFrameIndex} for AFMA codec use. */
    public AfmaFrameIndex() {
    }

    /** Initializes a new {@code AfmaFrameIndex} for AFMA codec use. */
    public AfmaFrameIndex(@NotNull List<AfmaFrameDescriptor> frames, @Nullable List<AfmaFrameDescriptor> introFrames) {
        this.frames = frames.isEmpty() ? null : new ArrayList<>(frames);
        this.intro_frames = ((introFrames != null) && !introFrames.isEmpty()) ? new ArrayList<>(introFrames) : null;
    }

    /** Returns the frames used by this AFMA codec instance. */
    @NotNull
    public List<AfmaFrameDescriptor> getFrames() {
        return (this.frames != null) ? this.frames : List.of();
    }

    /** Returns the intro frames used by this AFMA codec instance. */
    @NotNull
    public List<AfmaFrameDescriptor> getIntroFrames() {
        return (this.intro_frames != null) ? this.intro_frames : List.of();
    }

}
