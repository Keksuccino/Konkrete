package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Carries {@code AfmaSparsePayload} data between validated stages of the AFMA codec. */
public class AfmaSparsePayload {

    /** Holds the pixels path value used by this AFMA codec instance. */
    @Nullable
    protected String pixels_path;
    /** Holds the pixel count value used by this AFMA codec instance. */
    protected int pixel_count;
    /** Current channels state for this AFMA codec instance. */
    protected int channels;
    /** Holds the layout codec value used by this AFMA codec instance. */
    protected AfmaSparseLayoutCodec layout_codec;
    /** Holds the residual codec value used by this AFMA codec instance. */
    protected AfmaResidualCodec residual_codec;
    /** Holds the alpha mode value used by this AFMA codec instance. */
    protected AfmaAlphaResidualMode alpha_mode;
    /** Holds the alpha changed pixel count value used by this AFMA codec instance. */
    protected int alpha_changed_pixel_count;

    /** Initializes a new {@code AfmaSparsePayload} for AFMA codec use. */
    public AfmaSparsePayload() {
    }

    /** Initializes a new {@code AfmaSparsePayload} for AFMA codec use. */
    public AfmaSparsePayload(@Nullable String pixelsPath, int pixelCount, int channels) {
        this(pixelsPath, pixelCount, channels, AfmaSparseLayoutCodec.BITMASK, AfmaResidualCodec.INTERLEAVED,
                (channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) ? AfmaAlphaResidualMode.FULL : AfmaAlphaResidualMode.NONE, 0);
    }

    /** Initializes a new {@code AfmaSparsePayload} for AFMA codec use. */
    public AfmaSparsePayload(@Nullable String pixelsPath, int pixelCount, int channels,
                             @NotNull AfmaSparseLayoutCodec layoutCodec, @NotNull AfmaResidualCodec residualCodec,
                             @NotNull AfmaAlphaResidualMode alphaMode, int alphaChangedPixelCount) {
        this.pixels_path = pixelsPath;
        this.pixel_count = pixelCount;
        this.channels = channels;
        this.layout_codec = layoutCodec;
        this.residual_codec = residualCodec;
        this.alpha_mode = alphaMode;
        this.alpha_changed_pixel_count = alphaChangedPixelCount;
    }

    /** Returns the pixels path, or {@code null} when it is not available. */
    @Nullable
    public String getPixelsPath() {
        return this.pixels_path;
    }

    /** Returns the changed pixel count used by this AFMA codec instance. */
    public int getChangedPixelCount() {
        return this.pixel_count;
    }

    /** Returns the channels used by this AFMA codec instance. */
    public int getChannels() {
        return this.channels;
    }

    /** Returns the layout codec used by this AFMA codec instance. */
    @NotNull
    public AfmaSparseLayoutCodec getLayoutCodec() {
        return (this.layout_codec != null) ? this.layout_codec : AfmaSparseLayoutCodec.BITMASK;
    }

    /** Returns the residual codec used by this AFMA codec instance. */
    @NotNull
    public AfmaResidualCodec getResidualCodec() {
        return (this.residual_codec != null) ? this.residual_codec : AfmaResidualCodec.INTERLEAVED;
    }

    /** Returns the alpha mode used by this AFMA codec instance. */
    @NotNull
    public AfmaAlphaResidualMode getAlphaMode() {
        if (this.alpha_mode != null) {
            return this.alpha_mode;
        }
        return (this.channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) ? AfmaAlphaResidualMode.FULL : AfmaAlphaResidualMode.NONE;
    }

    /** Returns the alpha changed pixel count used by this AFMA codec instance. */
    public int getAlphaChangedPixelCount() {
        return this.alpha_changed_pixel_count;
    }

    /** Validates the current state before AFMA codec processing. */
    public void validate(@NotNull String context) {
        this.validate(context, true);
    }

    /** Validates the metadata before AFMA codec processing. */
    public void validateMetadata(@NotNull String context) {
        this.validate(context, false);
    }

    /** Validates the current state before AFMA codec processing. */
    protected void validate(@NotNull String context, boolean requirePixelsPath) {
        if (requirePixelsPath && ((this.pixels_path == null) || this.pixels_path.isBlank())) {
            throw new IllegalArgumentException(context + " is missing its sparse residual payload path");
        }
        if (this.pixel_count <= 0) {
            throw new IllegalArgumentException(context + " has an invalid sparse pixel count: " + this.pixel_count);
        }
        if (!AfmaResidualPayloadHelper.isValidChannelCount(this.channels)) {
            throw new IllegalArgumentException(context + " has an invalid sparse residual channel count: " + this.channels);
        }
        if (this.layout_codec == null) {
            throw new IllegalArgumentException(context + " is missing its sparse layout codec");
        }
        if (this.residual_codec == null) {
            throw new IllegalArgumentException(context + " is missing its sparse residual codec");
        }
        if (this.alpha_mode == null) {
            throw new IllegalArgumentException(context + " is missing its sparse alpha residual mode");
        }
        if ((this.channels == AfmaResidualPayloadHelper.RGB_CHANNELS) && (this.alpha_mode != AfmaAlphaResidualMode.NONE)) {
            throw new IllegalArgumentException(context + " cannot use alpha residual metadata with RGB-only sparse payloads");
        }
        if ((this.channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) && (this.alpha_mode == AfmaAlphaResidualMode.NONE)) {
            throw new IllegalArgumentException(context + " is missing alpha residual data despite using RGBA sparse channels");
        }
        if ((this.alpha_mode != AfmaAlphaResidualMode.SPARSE) && (this.alpha_changed_pixel_count != 0)) {
            throw new IllegalArgumentException(context + " has an unexpected sparse alpha-change count");
        }
        if ((this.alpha_mode == AfmaAlphaResidualMode.SPARSE) && (this.alpha_changed_pixel_count <= 0)) {
            throw new IllegalArgumentException(context + " has an invalid sparse alpha-change count: " + this.alpha_changed_pixel_count);
        }
    }

    /** Creates a copy with the pixels path changed for the AFMA codec. */
    @NotNull
    public AfmaSparsePayload withPixelsPath(@NotNull String pixelsPath) {
        return new AfmaSparsePayload(pixelsPath, this.getChangedPixelCount(), this.getChannels(),
                this.getLayoutCodec(), this.getResidualCodec(), this.getAlphaMode(), this.getAlphaChangedPixelCount());
    }

}
