package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/** Models {@code AfmaResidualCodec} state used by the AFMA codec. */
public enum AfmaResidualCodec {

    /** Selects the {@code INTERLEAVED} serialized AFMA representation. */
    INTERLEAVED(0, 0),
    /** Selects the {@code PLANAR} serialized AFMA representation. */
    PLANAR(1, 1),
    /** Selects the {@code PLANAR_ZIGZAG} serialized AFMA representation. */
    PLANAR_ZIGZAG(2, 2),
    /** Selects the {@code PLANAR_ZIGZAG_DELTA} serialized AFMA representation. */
    PLANAR_ZIGZAG_DELTA(3, 3);

    private final int id;
    private final int complexityScore;

    AfmaResidualCodec(int id, int complexityScore) {
        this.id = id;
        this.complexityScore = complexityScore;
    }

    /** Returns the id used by this AFMA codec instance. */
    public int getId() {
        return this.id;
    }

    /** Returns the complexity score used by this AFMA codec instance. */
    public int getComplexityScore() {
        return this.complexityScore;
    }

    /** Resolves the id to the matching AFMA codec value. */
    @NotNull
    public static AfmaResidualCodec byId(int id) throws IOException {
        for (AfmaResidualCodec codec : values()) {
            if (codec.id == id) {
                return codec;
            }
        }
        throw new IOException("Unknown AFMA residual codec id: " + id);
    }

}
