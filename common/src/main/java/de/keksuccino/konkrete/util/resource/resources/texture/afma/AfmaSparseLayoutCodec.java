package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/** Models {@code AfmaSparseLayoutCodec} state used by the AFMA codec. */
public enum AfmaSparseLayoutCodec {

    /** Selects the {@code BITMASK} serialized AFMA representation. */
    BITMASK(0, 0),
    /** Selects the {@code ROW_SPANS} serialized AFMA representation. */
    ROW_SPANS(1, 1),
    /** Selects the {@code TILE_MASK} serialized AFMA representation. */
    TILE_MASK(2, 2),
    /** Selects the {@code COORD_LIST} serialized AFMA representation. */
    COORD_LIST(3, 2);

    private final int id;
    private final int complexityScore;

    AfmaSparseLayoutCodec(int id, int complexityScore) {
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
    public static AfmaSparseLayoutCodec byId(int id) throws IOException {
        for (AfmaSparseLayoutCodec codec : values()) {
            if (codec.id == id) {
                return codec;
            }
        }
        throw new IOException("Unknown AFMA sparse layout codec id: " + id);
    }

}
