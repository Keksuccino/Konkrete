package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;

/** Carries {@code AfmaSourceSequence} data between validated stages of the AFMA creator. */
public class AfmaSourceSequence {

    /** Holds the frames collection used by this AFMA creator instance. */
    @NotNull
    protected final List<File> frames;

    /** Initializes a new {@code AfmaSourceSequence} for AFMA creator use. */
    public AfmaSourceSequence(@NotNull List<File> frames) {
        Objects.requireNonNull(frames);
        this.frames = List.copyOf(frames);
    }

    /** Builds the files for the AFMA creator. */
    @NotNull
    public static AfmaSourceSequence ofFiles(@NotNull List<File> frames) {
        return new AfmaSourceSequence(frames);
    }

    /** Creates the empty AFMA creator variant. */
    @NotNull
    public static AfmaSourceSequence empty() {
        return new AfmaSourceSequence(List.of());
    }

    /** Returns whether empty. */
    public boolean isEmpty() {
        return this.frames.isEmpty();
    }

    /** Returns the size used by the AFMA creator. */
    public int size() {
        return this.frames.size();
    }

    /** Returns the frame, or {@code null} when it is not available. */
    @Nullable
    public File getFrame(int index) {
        if (index < 0 || index >= this.frames.size()) return null;
        return this.frames.get(index);
    }

    /** Returns the frames used by this AFMA creator instance. */
    @NotNull
    public List<File> getFrames() {
        return this.frames;
    }

}
