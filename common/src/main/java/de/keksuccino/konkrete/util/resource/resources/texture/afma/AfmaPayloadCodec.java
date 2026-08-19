package de.keksuccino.konkrete.util.resource.resources.texture.afma;

/** Models {@code AfmaPayloadCodec} state used by the AFMA codec. */
public enum AfmaPayloadCodec {
    /** Selects the {@code NONE} serialized AFMA representation. */
    NONE,
    /** Selects the {@code BIN_INTRA} serialized AFMA representation. */
    BIN_INTRA,
    /** Selects the {@code RAW_RESIDUAL} serialized AFMA representation. */
    RAW_RESIDUAL,
    /** Selects the {@code RAW_SPARSE_LAYOUT} serialized AFMA representation. */
    RAW_SPARSE_LAYOUT,
    /** Selects the {@code RAW_SPARSE_RESIDUAL} serialized AFMA representation. */
    RAW_SPARSE_RESIDUAL,
    /** Selects the {@code INTER_FRAME} serialized AFMA representation. */
    INTER_FRAME,
    /** Selects the {@code TILE_INTRA} serialized AFMA representation. */
    TILE_INTRA
}
