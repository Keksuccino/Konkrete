package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import com.google.gson.annotations.SerializedName;

/** Selects the {@code AfmaFrameOperationType} representation or behavior used by the AFMA codec. */
public enum AfmaFrameOperationType {

    /** Selects the {@code FULL} serialized AFMA representation. */
    @SerializedName("full")
    FULL,
    /** Selects the {@code DELTA_RECT} serialized AFMA representation. */
    @SerializedName("delta_rect")
    DELTA_RECT,
    /** Selects the {@code RESIDUAL_DELTA_RECT} serialized AFMA representation. */
    @SerializedName("residual_delta_rect")
    RESIDUAL_DELTA_RECT,
    /** Selects the {@code SPARSE_DELTA_RECT} serialized AFMA representation. */
    @SerializedName("sparse_delta_rect")
    SPARSE_DELTA_RECT,
    /** Selects the {@code SAME} serialized AFMA representation. */
    @SerializedName("same")
    SAME,
    /** Selects the {@code COPY_RECT_PATCH} serialized AFMA representation. */
    @SerializedName("copy_rect_patch")
    COPY_RECT_PATCH,
    /** Selects the {@code COPY_RECT_RESIDUAL_PATCH} serialized AFMA representation. */
    @SerializedName("copy_rect_residual_patch")
    COPY_RECT_RESIDUAL_PATCH,
    /** Selects the {@code COPY_RECT_SPARSE_PATCH} serialized AFMA representation. */
    @SerializedName("copy_rect_sparse_patch")
    COPY_RECT_SPARSE_PATCH,
    /** Selects the {@code MULTI_COPY_PATCH} serialized AFMA representation. */
    @SerializedName("multi_copy_patch")
    MULTI_COPY_PATCH,
    /** Selects the {@code MULTI_COPY_RESIDUAL_PATCH} serialized AFMA representation. */
    @SerializedName("multi_copy_residual_patch")
    MULTI_COPY_RESIDUAL_PATCH,
    /** Selects the {@code MULTI_COPY_SPARSE_PATCH} serialized AFMA representation. */
    @SerializedName("multi_copy_sparse_patch")
    MULTI_COPY_SPARSE_PATCH,
    /** Selects the {@code BLOCK_INTER} serialized AFMA representation. */
    @SerializedName("block_inter")
    BLOCK_INTER

}
