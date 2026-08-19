package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

/** Carries {@code AfmaResidualPayload} data between validated stages of the AFMA codec. */
public class AfmaResidualPayload {

    /** Current channels state for this AFMA codec instance. */
    protected int channels;
    /** Current codec state for this AFMA codec instance. */
    protected AfmaResidualCodec codec;
    /** Holds the alpha mode value used by this AFMA codec instance. */
    protected AfmaAlphaResidualMode alpha_mode;
    /** Holds the alpha changed pixel count value used by this AFMA codec instance. */
    protected int alpha_changed_pixel_count;

    /** Initializes a new {@code AfmaResidualPayload} for AFMA codec use. */
    public AfmaResidualPayload() {
    }

    /** Initializes a new {@code AfmaResidualPayload} for AFMA codec use. */
    public AfmaResidualPayload(int channels) {
        this(channels, AfmaResidualCodec.INTERLEAVED, (channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) ? AfmaAlphaResidualMode.FULL : AfmaAlphaResidualMode.NONE, 0);
    }

    /** Initializes a new {@code AfmaResidualPayload} for AFMA codec use. */
    public AfmaResidualPayload(int channels, AfmaResidualCodec codec, AfmaAlphaResidualMode alphaMode, int alphaChangedPixelCount) {
        this.channels = channels;
        this.codec = codec;
        this.alpha_mode = alphaMode;
        this.alpha_changed_pixel_count = alphaChangedPixelCount;
    }

    /** Returns the channels used by this AFMA codec instance. */
    public int getChannels() {
        return this.channels;
    }

    /** Returns the codec used by this AFMA codec instance. */
    @NotNull
    public AfmaResidualCodec getCodec() {
        return (this.codec != null) ? this.codec : AfmaResidualCodec.INTERLEAVED;
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
        if (!AfmaResidualPayloadHelper.isValidChannelCount(this.channels)) {
            throw new IllegalArgumentException(context + " has an invalid residual channel count: " + this.channels);
        }
        if (this.codec == null) {
            throw new IllegalArgumentException(context + " is missing its residual codec");
        }
        if (this.alpha_mode == null) {
            throw new IllegalArgumentException(context + " is missing its alpha residual mode");
        }
        if ((this.channels == AfmaResidualPayloadHelper.RGB_CHANNELS) && (this.alpha_mode != AfmaAlphaResidualMode.NONE)) {
            throw new IllegalArgumentException(context + " cannot store alpha residual metadata with RGB-only channels");
        }
        if ((this.channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) && (this.alpha_mode == AfmaAlphaResidualMode.NONE)) {
            throw new IllegalArgumentException(context + " is missing alpha residual data despite using RGBA channels");
        }
        if ((this.alpha_mode != AfmaAlphaResidualMode.SPARSE) && (this.alpha_changed_pixel_count != 0)) {
            throw new IllegalArgumentException(context + " has an unexpected sparse alpha-change count");
        }
        if ((this.alpha_mode == AfmaAlphaResidualMode.SPARSE) && (this.alpha_changed_pixel_count <= 0)) {
            throw new IllegalArgumentException(context + " has an invalid sparse alpha-change count: " + this.alpha_changed_pixel_count);
        }
    }

}
