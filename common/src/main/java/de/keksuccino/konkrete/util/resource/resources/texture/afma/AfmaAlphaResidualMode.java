package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/** Selects the {@code AfmaAlphaResidualMode} representation or behavior used by the AFMA codec. */
public enum AfmaAlphaResidualMode {

    /** Selects the {@code NONE} serialized AFMA representation. */
    NONE(0),
    /** Selects the {@code FULL} serialized AFMA representation. */
    FULL(1),
    /** Selects the {@code SPARSE} serialized AFMA representation. */
    SPARSE(2);

    private final int id;

    AfmaAlphaResidualMode(int id) {
        this.id = id;
    }

    /** Returns the id used by this AFMA codec instance. */
    public int getId() {
        return this.id;
    }

    /** Resolves the id to the matching AFMA codec value. */
    @NotNull
    public static AfmaAlphaResidualMode byId(int id) throws IOException {
        for (AfmaAlphaResidualMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        throw new IOException("Unknown AFMA alpha residual mode id: " + id);
    }

}
