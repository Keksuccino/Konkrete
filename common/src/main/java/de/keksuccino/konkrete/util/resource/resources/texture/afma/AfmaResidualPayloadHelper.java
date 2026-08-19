package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/** Provides {@code AfmaResidualPayloadHelper} transformations and validation used by the AFMA codec. */
public final class AfmaResidualPayloadHelper {

    /** Channel count for alpha only samples. */
    public static final int ALPHA_ONLY_CHANNELS = 1;
    /** Channel count for rgb samples. */
    public static final int RGB_CHANNELS = 3;
    /** Channel count for rgba samples. */
    public static final int RGBA_CHANNELS = 4;
    /** Upper bound for residual full codec evaluation bytes enforced by the AFMA codec. */
    protected static final int RESIDUAL_FULL_CODEC_EVALUATION_MAX_BYTES = 512;
    /** Upper bound for residual full codec evaluation samples enforced by the AFMA codec. */
    protected static final int RESIDUAL_FULL_CODEC_EVALUATION_MAX_SAMPLES = 64;
    /** Upper bound for residual zigzag abs value enforced by the AFMA codec. */
    protected static final int RESIDUAL_ZIGZAG_MAX_ABS_VALUE = 8;
    /** Upper bound for residual delta abs step enforced by the AFMA codec. */
    protected static final int RESIDUAL_DELTA_MAX_ABS_STEP = 6;
    /** Lower bound for residual delta sample count accepted by the AFMA codec. */
    protected static final int RESIDUAL_DELTA_MIN_SAMPLE_COUNT = 12;
    /** Lower bound for residual zigzag small value ratio accepted by the AFMA codec. */
    protected static final double RESIDUAL_MIN_ZIGZAG_SMALL_VALUE_RATIO = 0.40D;
    /** Lower bound for residual zigzag zero ratio accepted by the AFMA codec. */
    protected static final double RESIDUAL_MIN_ZIGZAG_ZERO_RATIO = 0.20D;
    /** Lower bound for residual delta smooth pair ratio accepted by the AFMA codec. */
    protected static final double RESIDUAL_MIN_DELTA_SMOOTH_PAIR_RATIO = 0.60D;
    /** Lower bound for residual delta small value ratio accepted by the AFMA codec. */
    protected static final double RESIDUAL_MIN_DELTA_SMALL_VALUE_RATIO = 0.35D;

    private AfmaResidualPayloadHelper() {
    }

    /** Calculates the channel count value for the AFMA codec. */
    public static int channelCount(boolean includeAlpha) {
        return includeAlpha ? RGBA_CHANNELS : RGB_CHANNELS;
    }

    /** Returns whether valid channel count. */
    public static boolean isValidChannelCount(int channels) {
        return channels == RGB_CHANNELS || channels == RGBA_CHANNELS;
    }

    /** Returns whether valid stream channel count. */
    public static boolean isValidStreamChannelCount(int channels) {
        return channels == ALPHA_ONLY_CHANNELS || isValidChannelCount(channels);
    }

    /** Calculates the expected dense residual bytes value for the AFMA codec. */
    public static int expectedDenseResidualBytes(int width, int height, int channels) {
        if ((width <= 0) || (height <= 0) || !isValidChannelCount(channels)) {
            return 0;
        }
        long totalPixels = (long) width * (long) height;
        long totalBytes = totalPixels * channels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    /** Calculates the expected dense residual bytes value for the AFMA codec. */
    public static int expectedDenseResidualBytes(int width, int height, @NotNull AfmaResidualPayload residualPayload) {
        if ((width <= 0) || (height <= 0)) {
            return 0;
        }
        return expectedResidualPayloadBytes((long) width * height, residualPayload.getChannels(),
                residualPayload.getAlphaMode(), residualPayload.getAlphaChangedPixelCount());
    }

    /** Calculates the expected sparse residual bytes value for the AFMA codec. */
    public static int expectedSparseResidualBytes(int changedPixelCount, int channels) {
        if ((changedPixelCount < 0) || !isValidChannelCount(channels)) {
            return 0;
        }
        long totalBytes = (long) changedPixelCount * channels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    /** Calculates the expected sparse residual bytes value for the AFMA codec. */
    public static int expectedSparseResidualBytes(int changedPixelCount, @NotNull AfmaSparsePayload sparsePayload) {
        return expectedResidualPayloadBytes(changedPixelCount, sparsePayload.getChannels(),
                sparsePayload.getAlphaMode(), sparsePayload.getAlphaChangedPixelCount());
    }

    /** Calculates the expected residual payload bytes value for the AFMA codec. */
    public static int expectedResidualPayloadBytes(long sampleCount, int channels,
                                                   @NotNull AfmaAlphaResidualMode alphaMode,
                                                   int alphaChangedPixelCount) {
        if ((sampleCount < 0L) || (sampleCount > Integer.MAX_VALUE) || !isValidChannelCount(channels)) {
            return 0;
        }

        long totalBytes = (long) expectedPrimaryStreamBytes((int) sampleCount, channels, alphaMode)
                + expectedAlphaMaskBytes((int) sampleCount, alphaMode)
                + expectedAlphaStreamBytes(alphaMode, alphaChangedPixelCount);
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    /** Calculates the expected sparse mask bytes value for the AFMA codec. */
    public static int expectedSparseMaskBytes(int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            return 0;
        }
        return expectedSparseBitsetBytes((long) width * height);
    }

    /** Calculates the expected sparse bitset bytes value for the AFMA codec. */
    public static int expectedSparseBitsetBytes(long bitCount) {
        if (bitCount <= 0L || bitCount > Integer.MAX_VALUE) {
            return 0;
        }
        long totalBytes = (bitCount + 7L) / 8L;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    /** Calculates the expected primary stream bytes value for the AFMA codec. */
    public static int expectedPrimaryStreamBytes(int sampleCount, int channels, @NotNull AfmaAlphaResidualMode alphaMode) {
        if ((sampleCount < 0) || !isValidChannelCount(channels)) {
            return 0;
        }
        int primaryChannels = (alphaMode == AfmaAlphaResidualMode.FULL) ? channels : RGB_CHANNELS;
        if (!isValidStreamChannelCount(primaryChannels)) {
            return 0;
        }
        long totalBytes = (long) sampleCount * primaryChannels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    /** Calculates the expected alpha mask bytes value for the AFMA codec. */
    public static int expectedAlphaMaskBytes(int sampleCount, @NotNull AfmaAlphaResidualMode alphaMode) {
        return (alphaMode == AfmaAlphaResidualMode.SPARSE) ? expectedSparseBitsetBytes(sampleCount) : 0;
    }

    /** Calculates the expected alpha stream bytes value for the AFMA codec. */
    public static int expectedAlphaStreamBytes(@NotNull AfmaAlphaResidualMode alphaMode, int alphaChangedPixelCount) {
        if (alphaMode != AfmaAlphaResidualMode.SPARSE) {
            return 0;
        }
        if (alphaChangedPixelCount < 0) {
            return 0;
        }
        return alphaChangedPixelCount;
    }

    /** Validates the dense payload before AFMA codec processing. */
    public static void validateDensePayload(@NotNull byte[] payloadBytes, int offset, int length,
                                            int width, int height, @NotNull AfmaResidualPayload residualPayload) {
        residualPayload.validate("AFMA residual payload descriptor");
        validateResidualPayload(payloadBytes, offset, length, (long) width * height,
                residualPayload.getChannels(), residualPayload.getAlphaMode(), residualPayload.getAlphaChangedPixelCount());
    }

    /** Validates the sparse payload before AFMA codec processing. */
    public static void validateSparsePayload(@NotNull byte[] payloadBytes, int offset, int length,
                                             int changedPixelCount, @NotNull AfmaSparsePayload sparsePayload) {
        sparsePayload.validateMetadata("AFMA sparse payload descriptor");
        validateResidualPayload(payloadBytes, offset, length, changedPixelCount,
                sparsePayload.getChannels(), sparsePayload.getAlphaMode(), sparsePayload.getAlphaChangedPixelCount());
    }

    /** Validates the residual payload before AFMA codec processing. */
    public static void validateResidualPayload(@NotNull byte[] payloadBytes, int offset, int length, long sampleCount,
                                               int channels, @NotNull AfmaAlphaResidualMode alphaMode,
                                               int alphaChangedPixelCount) {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(alphaMode);
        if ((alphaMode == AfmaAlphaResidualMode.SPARSE) && ((alphaChangedPixelCount <= 0) || (alphaChangedPixelCount > sampleCount))) {
            throw new IllegalStateException("AFMA sparse alpha metadata does not match the frame descriptor");
        }
        int expectedBytes = expectedResidualPayloadBytes(sampleCount, channels, alphaMode, alphaChangedPixelCount);
        if ((expectedBytes <= 0) || (length != expectedBytes) || (offset < 0)
                || (((long) offset + length) > payloadBytes.length)) {
            throw new IllegalStateException("AFMA residual payload size does not match the frame descriptor");
        }
    }

    /** Validates the sparse alpha mask population before AFMA codec processing. */
    public static void validateSparseAlphaMaskPopulation(@NotNull byte[] payloadBytes, int payloadOffset, int sampleCount,
                                                         int channels, @NotNull AfmaAlphaResidualMode alphaMode,
                                                         int alphaChangedPixelCount) {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(alphaMode);
        if (alphaMode != AfmaAlphaResidualMode.SPARSE) {
            return;
        }

        int alphaMaskOffset = resolveAlphaMaskOffset(payloadOffset, sampleCount, channels, alphaMode);
        int alphaMaskLength = resolveAlphaMaskLength(sampleCount, alphaMode);
        if ((alphaMaskLength <= 0) || (alphaMaskOffset < 0) || (((long) alphaMaskOffset + alphaMaskLength) > payloadBytes.length)) {
            throw new IllegalStateException("AFMA sparse alpha mask slice is invalid");
        }

        int populatedBits = countMaskBits(payloadBytes, alphaMaskOffset, sampleCount);
        if (populatedBits != alphaChangedPixelCount) {
            throw new IllegalStateException("AFMA sparse alpha mask bit population does not match its descriptor");
        }
    }

    /** Sets the mask bit used by subsequent AFMA codec operations. */
    public static void setMaskBit(@NotNull byte[] maskBytes, int pixelIndex) {
        maskBytes[pixelIndex >>> 3] |= (byte) (1 << (7 - (pixelIndex & 7)));
    }

    /** Returns whether mask bit set. */
    public static boolean isMaskBitSet(@NotNull byte[] maskBytes, int pixelIndex) {
        return (maskBytes[pixelIndex >>> 3] & (1 << (7 - (pixelIndex & 7)))) != 0;
    }

    /** Counts the mask bits in the AFMA codec. */
    public static int countMaskBits(@NotNull byte[] maskBytes, int offset, int bitCount) {
        Objects.requireNonNull(maskBytes);
        if ((offset < 0) || (bitCount < 0) || (((long) offset + expectedSparseBitsetBytes(bitCount)) > maskBytes.length)) {
            throw new IllegalArgumentException("AFMA sparse mask slice is invalid");
        }

        int populatedBits = 0;
        for (int bitIndex = 0; bitIndex < bitCount; bitIndex++) {
            if ((maskBytes[offset + (bitIndex >>> 3)] & (1 << (7 - (bitIndex & 7)))) != 0) {
                populatedBits++;
            }
        }
        return populatedBits;
    }

    /** Writes the residual to the AFMA codec output. */
    public static int writeResidual(@NotNull byte[] target, int offset, int predictedColor, int currentColor, boolean includeAlpha) {
        target[offset++] = (byte) ((((currentColor >> 16) & 0xFF) - ((predictedColor >> 16) & 0xFF)) & 0xFF);
        target[offset++] = (byte) ((((currentColor >> 8) & 0xFF) - ((predictedColor >> 8) & 0xFF)) & 0xFF);
        target[offset++] = (byte) (((currentColor & 0xFF) - (predictedColor & 0xFF)) & 0xFF);
        if (includeAlpha) {
            target[offset++] = (byte) ((((currentColor >>> 24) & 0xFF) - ((predictedColor >>> 24) & 0xFF)) & 0xFF);
        }
        return offset;
    }

    /** Applies the residual to argb to the AFMA codec. */
    public static int applyResidualToArgb(int predictedColor, @NotNull byte[] residualBytes, int offset, int channels) {
        int alpha = (predictedColor >>> 24) & 0xFF;
        int red = (predictedColor >> 16) & 0xFF;
        int green = (predictedColor >> 8) & 0xFF;
        int blue = predictedColor & 0xFF;

        red = (red + residualBytes[offset++]) & 0xFF;
        green = (green + residualBytes[offset++]) & 0xFF;
        blue = (blue + residualBytes[offset++]) & 0xFF;
        if (channels == RGBA_CHANNELS) {
            alpha = (alpha + residualBytes[offset]) & 0xFF;
        }

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /** Encodes the best residual payload for the AFMA codec. */
    @NotNull
    public static EncodedResidualPayload encodeBestResidualPayload(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                                   int sampleCount, boolean includeAlpha) {
        return encodeBestResidualPayload(predictedColors, currentColors, sampleCount, includeAlpha, null);
    }

    /** Encodes the best residual payload for the AFMA codec. */
    @NotNull
    public static EncodedResidualPayload encodeBestResidualPayload(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                                   int sampleCount, boolean includeAlpha,
                                                                   @Nullable ResidualEncodeWorkspace workspace) {
        Objects.requireNonNull(predictedColors);
        Objects.requireNonNull(currentColors);
        if ((sampleCount <= 0) || (sampleCount > predictedColors.length) || (sampleCount > currentColors.length)) {
            throw new IllegalArgumentException("AFMA residual sample count is invalid");
        }

        int channels = includeAlpha ? RGBA_CHANNELS : RGB_CHANNELS;
        int alphaChangedPixelCount = includeAlpha ? countAlphaChanges(predictedColors, currentColors, sampleCount) : 0;
        EncodedResidualPayload bestCandidate = encodeBestResidualPayloadForAlphaMode(predictedColors, currentColors, sampleCount, channels,
                includeAlpha ? AfmaAlphaResidualMode.FULL : AfmaAlphaResidualMode.NONE, alphaChangedPixelCount, workspace);
        if (includeAlpha && (alphaChangedPixelCount > 0) && (alphaChangedPixelCount < sampleCount)) {
            EncodedResidualPayload sparseAlphaCandidate = encodeBestResidualPayloadForAlphaMode(predictedColors, currentColors, sampleCount, channels,
                    AfmaAlphaResidualMode.SPARSE, alphaChangedPixelCount, workspace);
            if (sparseAlphaCandidate.isBetterThan(bestCandidate)) {
                bestCandidate = sparseAlphaCandidate;
            }
        }
        return Objects.requireNonNull(bestCandidate, "Failed to encode AFMA residual payload");
    }

    /** Encodes the best residual payload for alpha mode for the AFMA codec. */
    @NotNull
    protected static EncodedResidualPayload encodeBestResidualPayloadForAlphaMode(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                                                  int sampleCount, int channels,
                                                                                  @NotNull AfmaAlphaResidualMode alphaMode,
                                                                                  int alphaChangedPixelCount,
                                                                                  @Nullable ResidualEncodeWorkspace workspace) {
        int primaryChannels = (alphaMode == AfmaAlphaResidualMode.FULL) ? channels : RGB_CHANNELS;
        int colorRawLength = sampleCount * primaryChannels;
        byte[] colorRawStream = buildColorResidualStream(predictedColors, currentColors, sampleCount, primaryChannels, workspace);
        int alphaMaskLength = (alphaMode == AfmaAlphaResidualMode.SPARSE)
                ? expectedSparseBitsetBytes(sampleCount)
                : 0;
        byte[] alphaMask = (alphaMaskLength > 0)
                ? buildAlphaChangeMask(predictedColors, currentColors, sampleCount, workspace)
                : null;
        int alphaRawLength = (alphaMode == AfmaAlphaResidualMode.SPARSE) ? alphaChangedPixelCount : 0;
        byte[] alphaRawStream = (alphaRawLength > 0)
                ? buildSparseAlphaResidualStream(predictedColors, currentColors, sampleCount, alphaChangedPixelCount, workspace)
                : null;

        EncodedResidualPayload bestCandidate = null;
        AfmaResidualCodec[] candidateCodecs = selectCodecShortlist(colorRawStream, colorRawLength, sampleCount,
                primaryChannels, alphaRawStream, alphaRawLength, alphaChangedPixelCount);
        for (AfmaResidualCodec codec : candidateCodecs) {
            EncodedResidualPayload candidate = buildCodecCandidate(colorRawStream, colorRawLength, alphaMask, alphaMaskLength,
                    alphaRawStream, alphaRawLength, sampleCount, channels, alphaMode, alphaChangedPixelCount, codec, workspace);
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate;
            }
        }
        return Objects.requireNonNull(bestCandidate, "Failed to encode AFMA residual payload");
    }

    /** Selects the codec shortlist for the AFMA codec. */
    @NotNull
    protected static AfmaResidualCodec[] selectCodecShortlist(@NotNull byte[] colorRawStream, int colorRawLength,
                                                              int sampleCount, int channels,
                                                              @Nullable byte[] alphaRawStream, int alphaRawLength,
                                                              int alphaChangedPixelCount) {
        // Large residual payloads dominate planner time, so keep the stable baselines and only
        // fan out into heavier transforms when the raw stream shows evidence that they can help.
        int totalBytes = colorRawLength + alphaRawLength;
        if ((totalBytes <= RESIDUAL_FULL_CODEC_EVALUATION_MAX_BYTES) || (sampleCount <= RESIDUAL_FULL_CODEC_EVALUATION_MAX_SAMPLES)) {
            return AfmaResidualCodec.values();
        }

        ResidualStreamStats colorStats = analyzeResidualStream(colorRawStream, colorRawLength, sampleCount, channels);
        ResidualStreamStats alphaStats = analyzeResidualStream(alphaRawStream, alphaRawLength, alphaChangedPixelCount, ALPHA_ONLY_CHANNELS);
        int totalValueCount = colorStats.valueCount() + alphaStats.valueCount();
        int totalPairCount = colorStats.pairCount() + alphaStats.pairCount();
        double smallValueRatio = resolveRatio(colorStats.smallValueCount() + alphaStats.smallValueCount(), totalValueCount);
        double zeroValueRatio = resolveRatio(colorStats.zeroValueCount() + alphaStats.zeroValueCount(), totalValueCount);
        double smoothPairRatio = resolveRatio(colorStats.smoothPairCount() + alphaStats.smoothPairCount(), totalPairCount);

        ArrayList<AfmaResidualCodec> codecs = new ArrayList<>(4);
        codecs.add(AfmaResidualCodec.INTERLEAVED);
        codecs.add(AfmaResidualCodec.PLANAR);
        if ((smallValueRatio >= RESIDUAL_MIN_ZIGZAG_SMALL_VALUE_RATIO) || (zeroValueRatio >= RESIDUAL_MIN_ZIGZAG_ZERO_RATIO)) {
            codecs.add(AfmaResidualCodec.PLANAR_ZIGZAG);
        }
        if (((sampleCount >= RESIDUAL_DELTA_MIN_SAMPLE_COUNT) || (alphaChangedPixelCount >= RESIDUAL_DELTA_MIN_SAMPLE_COUNT))
                && (smallValueRatio >= RESIDUAL_MIN_DELTA_SMALL_VALUE_RATIO)
                && (smoothPairRatio >= RESIDUAL_MIN_DELTA_SMOOTH_PAIR_RATIO)) {
            codecs.add(AfmaResidualCodec.PLANAR_ZIGZAG_DELTA);
        }
        return codecs.toArray(new AfmaResidualCodec[0]);
    }

    /** Analyzes the residual stream for the AFMA codec. */
    @NotNull
    protected static ResidualStreamStats analyzeResidualStream(@Nullable byte[] rawBytes, int rawLength, int sampleCount, int channels) {
        if ((rawBytes == null) || (sampleCount <= 0) || (channels <= 0) || (rawLength != (sampleCount * channels)) || (rawBytes.length < rawLength)) {
            return ResidualStreamStats.EMPTY;
        }

        int zeroValueCount = 0;
        int smallValueCount = 0;
        int smoothPairCount = 0;
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            int interleavedOffset = sampleIndex * channels;
            for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                int byteIndex = interleavedOffset + channelIndex;
                int value = rawBytes[byteIndex];
                if (value == 0) {
                    zeroValueCount++;
                }
                if (Math.abs(value) <= RESIDUAL_ZIGZAG_MAX_ABS_VALUE) {
                    smallValueCount++;
                }
                if ((sampleIndex > 0) && (Math.abs(value - rawBytes[byteIndex - channels]) <= RESIDUAL_DELTA_MAX_ABS_STEP)) {
                    smoothPairCount++;
                }
            }
        }
        return new ResidualStreamStats(rawLength, Math.max(0, sampleCount - 1) * channels,
                zeroValueCount, smallValueCount, smoothPairCount);
    }

    /** Resolves the ratio for the AFMA codec. */
    protected static double resolveRatio(int matchedCount, int totalCount) {
        return (totalCount > 0) ? ((double) matchedCount / (double) totalCount) : 0D;
    }

    /** Builds the codec candidate for the AFMA codec. */
    @NotNull
    protected static EncodedResidualPayload buildCodecCandidate(@NotNull byte[] colorRawStream, int colorRawLength,
                                                                @Nullable byte[] alphaMask, int alphaMaskLength,
                                                                @Nullable byte[] alphaRawStream, int alphaRawLength,
                                                                int sampleCount, int channels,
                                                                @NotNull AfmaAlphaResidualMode alphaMode,
                                                                int alphaChangedPixelCount,
                                                                @NotNull AfmaResidualCodec codec,
                                                                @Nullable ResidualEncodeWorkspace workspace) {
        int primaryChannels = (alphaMode == AfmaAlphaResidualMode.FULL) ? channels : RGB_CHANNELS;
        byte[] primaryBytes = transformResidualStream(colorRawStream, colorRawLength, sampleCount,
                primaryChannels, codec, workspace, true);
        byte[] secondaryBytes = (alphaRawStream != null)
                ? transformResidualStream(alphaRawStream, alphaRawLength, alphaChangedPixelCount,
                        ALPHA_ONLY_CHANNELS, codec, workspace, false)
                : null;
        byte[] stablePrimaryBytes = copyPayloadSegment(primaryBytes, colorRawLength);
        byte[] stableAlphaMask = copyPayloadSegment(alphaMask, alphaMaskLength);
        byte[] stableSecondaryBytes = copyPayloadSegment(secondaryBytes, alphaRawLength);
        AfmaStoredPayload.Writer payloadWriter = out -> writeMergedStreams(out, stablePrimaryBytes, stableAlphaMask, stableSecondaryBytes);
        return new EncodedResidualPayload(
                summarizeMergedStreams(payloadWriter),
                payloadWriter,
                channels,
                codec,
                alphaMode,
                alphaMode == AfmaAlphaResidualMode.SPARSE ? alphaChangedPixelCount : 0,
                codec.getComplexityScore() + (alphaMode == AfmaAlphaResidualMode.SPARSE ? 1 : 0)
        );
    }

    /** Builds the color residual stream for the AFMA codec. */
    @NotNull
    protected static byte[] buildColorResidualStream(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                     int sampleCount, int channels,
                                                     @Nullable ResidualEncodeWorkspace workspace) {
        int expectedBytes = sampleCount * channels;
        byte[] rawBytes = (workspace != null) ? workspace.colorRawStream(expectedBytes) : new byte[expectedBytes];
        int offset = 0;
        for (int i = 0; i < sampleCount; i++) {
            int predictedColor = predictedColors[i];
            int currentColor = currentColors[i];
            rawBytes[offset++] = (byte) ((((currentColor >> 16) & 0xFF) - ((predictedColor >> 16) & 0xFF)) & 0xFF);
            rawBytes[offset++] = (byte) ((((currentColor >> 8) & 0xFF) - ((predictedColor >> 8) & 0xFF)) & 0xFF);
            rawBytes[offset++] = (byte) (((currentColor & 0xFF) - (predictedColor & 0xFF)) & 0xFF);
            if (channels == RGBA_CHANNELS) {
                rawBytes[offset++] = (byte) ((((currentColor >>> 24) & 0xFF) - ((predictedColor >>> 24) & 0xFF)) & 0xFF);
            }
        }
        return rawBytes;
    }

    /** Builds the alpha change mask for the AFMA codec. */
    @NotNull
    protected static byte[] buildAlphaChangeMask(@NotNull int[] predictedColors, @NotNull int[] currentColors, int sampleCount,
                                                 @Nullable ResidualEncodeWorkspace workspace) {
        int expectedBytes = expectedSparseBitsetBytes(sampleCount);
        byte[] maskBytes = (workspace != null) ? workspace.alphaMask(expectedBytes) : new byte[expectedBytes];
        Arrays.fill(maskBytes, 0, expectedBytes, (byte) 0);
        for (int i = 0; i < sampleCount; i++) {
            if (((predictedColors[i] ^ currentColors[i]) & 0xFF000000) != 0) {
                setMaskBit(maskBytes, i);
            }
        }
        return maskBytes;
    }

    /** Builds the sparse alpha residual stream for the AFMA codec. */
    @NotNull
    protected static byte[] buildSparseAlphaResidualStream(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                           int sampleCount, int alphaChangedPixelCount,
                                                           @Nullable ResidualEncodeWorkspace workspace) {
        byte[] rawBytes = (workspace != null) ? workspace.alphaRawStream(alphaChangedPixelCount) : new byte[alphaChangedPixelCount];
        int offset = 0;
        for (int i = 0; i < sampleCount; i++) {
            int predictedColor = predictedColors[i];
            int currentColor = currentColors[i];
            if (((predictedColor ^ currentColor) & 0xFF000000) == 0) {
                continue;
            }
            rawBytes[offset++] = (byte) ((((currentColor >>> 24) & 0xFF) - ((predictedColor >>> 24) & 0xFF)) & 0xFF);
        }
        return rawBytes;
    }

    /** Transforms the residual stream for the AFMA codec. */
    @NotNull
    protected static byte[] transformResidualStream(@NotNull byte[] rawBytes, int rawLength, int sampleCount, int channels,
                                                    @NotNull AfmaResidualCodec codec,
                                                    @Nullable ResidualEncodeWorkspace workspace,
                                                    boolean primaryStream) {
        if (codec == AfmaResidualCodec.INTERLEAVED) {
            return rawBytes;
        }

        if ((sampleCount < 0) || !isValidStreamChannelCount(channels)
                || (rawLength != (sampleCount * channels)) || (rawBytes.length < rawLength)) {
            throw new IllegalArgumentException("AFMA residual stream dimensions are invalid");
        }

        byte[] transformedBytes = (workspace != null)
                ? workspace.transformStream(rawLength, primaryStream)
                : new byte[rawLength];
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            int interleavedOffset = sampleIndex * channels;
            for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                int transformedOffset = (channelIndex * sampleCount) + sampleIndex;
                byte value = rawBytes[interleavedOffset + channelIndex];
                transformedBytes[transformedOffset] = switch (codec) {
                    case PLANAR -> value;
                    case PLANAR_ZIGZAG, PLANAR_ZIGZAG_DELTA -> (byte) zigzagEncode(value);
                    case INTERLEAVED -> throw new IllegalStateException("INTERLEAVED residual streams should not be transformed");
                };
            }
        }

        if (codec == AfmaResidualCodec.PLANAR_ZIGZAG_DELTA) {
            for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                int planeOffset = channelIndex * sampleCount;
                int previousValue = 0;
                for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
                    int currentValue = transformedBytes[planeOffset + sampleIndex] & 0xFF;
                    transformedBytes[planeOffset + sampleIndex] = (byte) ((currentValue - previousValue) & 0xFF);
                    previousValue = currentValue;
                }
            }
        }

        return transformedBytes;
    }

    /** Copies the payload segment for the AFMA codec. */
    @Nullable
    protected static byte[] copyPayloadSegment(@Nullable byte[] payloadSegment, int length) {
        if ((payloadSegment == null) || (length <= 0)) {
            return null;
        }
        if (payloadSegment.length < length) {
            throw new IllegalArgumentException("AFMA residual payload segment is shorter than expected");
        }
        return Arrays.copyOf(payloadSegment, length);
    }

    /** Writes the merged streams to the AFMA codec output. */
    protected static void writeMergedStreams(@NotNull OutputStream out,
                                             @NotNull byte[] primaryStream,
                                             @Nullable byte[] alphaMask,
                                             @Nullable byte[] alphaStream) throws IOException {
        out.write(primaryStream);
        if (alphaMask != null) {
            out.write(alphaMask);
        }
        if (alphaStream != null) {
            out.write(alphaStream);
        }
    }

    /** Summarizes the merged streams for the AFMA codec. */
    @NotNull
    protected static AfmaStoredPayload.PayloadSummary summarizeMergedStreams(@NotNull AfmaStoredPayload.Writer payloadWriter) {
        try {
            return AfmaStoredPayload.summarize(payloadWriter);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to summarize AFMA residual payload", ex);
        }
    }

    /** Counts the alpha changes in the AFMA codec. */
    protected static int countAlphaChanges(@NotNull int[] predictedColors, @NotNull int[] currentColors, int sampleCount) {
        int alphaChangedPixelCount = 0;
        for (int i = 0; i < sampleCount; i++) {
            if (((predictedColors[i] ^ currentColors[i]) & 0xFF000000) != 0) {
                alphaChangedPixelCount++;
            }
        }
        return alphaChangedPixelCount;
    }

    /** Encodes the signed AFMA codec value with zigzag encoding. */
    protected static int zigzagEncode(int value) {
        return ((value << 1) ^ (value >> 31)) & 0xFF;
    }

    /** Decodes the zigzag-encoded AFMA codec value. */
    protected static int zigzagDecode(int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    /** Resolves the primary stream length for the AFMA codec. */
    public static int resolvePrimaryStreamLength(int sampleCount, int channels, @NotNull AfmaAlphaResidualMode alphaMode) {
        return expectedPrimaryStreamBytes(sampleCount, channels, alphaMode);
    }

    /** Resolves the alpha mask offset for the AFMA codec. */
    public static int resolveAlphaMaskOffset(int payloadOffset, int sampleCount, int channels, @NotNull AfmaAlphaResidualMode alphaMode) {
        return payloadOffset + resolvePrimaryStreamLength(sampleCount, channels, alphaMode);
    }

    /** Resolves the alpha mask length for the AFMA codec. */
    public static int resolveAlphaMaskLength(int sampleCount, @NotNull AfmaAlphaResidualMode alphaMode) {
        return expectedAlphaMaskBytes(sampleCount, alphaMode);
    }

    /** Resolves the alpha stream offset for the AFMA codec. */
    public static int resolveAlphaStreamOffset(int payloadOffset, int sampleCount, int channels, @NotNull AfmaAlphaResidualMode alphaMode) {
        return resolveAlphaMaskOffset(payloadOffset, sampleCount, channels, alphaMode)
                + resolveAlphaMaskLength(sampleCount, alphaMode);
    }

    /** Opens the reader for the AFMA codec. */
    @NotNull
    public static ResidualSampleReader openReader(@NotNull byte[] payloadBytes, int offset, int length,
                                                  int sampleCount, int channels, @NotNull AfmaResidualCodec codec) {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(codec);
        if (!isValidStreamChannelCount(channels)) {
            throw new IllegalArgumentException("AFMA residual stream channel count is invalid: " + channels);
        }
        int expectedBytes = sampleCount * channels;
        if ((expectedBytes < 0) || (length != expectedBytes) || (offset < 0) || (((long) offset + length) > payloadBytes.length)) {
            throw new IllegalArgumentException("AFMA residual stream slice is invalid");
        }
        return new ResidualSampleReader(payloadBytes, offset, sampleCount, channels, codec);
    }

    /** Carries {@code EncodedResidualPayload} data between validated stages of the AFMA codec. */
    public record EncodedResidualPayload(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                         @NotNull AfmaStoredPayload.Writer payloadWriter,
                                         int channels,
                                         @NotNull AfmaResidualCodec codec,
                                         @NotNull AfmaAlphaResidualMode alphaMode,
                                         int alphaChangedPixelCount,
                                         int complexityScore) {

        /** Returns whether better than. */
        public boolean isBetterThan(@NotNull EncodedResidualPayload other) {
            if (this.payloadSummary.estimatedArchiveBytes() != other.payloadSummary.estimatedArchiveBytes()) {
                return this.payloadSummary.estimatedArchiveBytes() < other.payloadSummary.estimatedArchiveBytes();
            }
            if (this.complexityScore != other.complexityScore) {
                return this.complexityScore < other.complexityScore;
            }
            return this.payloadSummary.length() < other.payloadSummary.length();
        }

        /** Converts the residual metadata from the AFMA codec representation. */
        @NotNull
        public AfmaResidualPayload toResidualMetadata() {
            return new AfmaResidualPayload(this.channels, this.codec, this.alphaMode, this.alphaChangedPixelCount);
        }

        /** Converts the sparse metadata from the AFMA codec representation. */
        @NotNull
        public AfmaSparsePayload toSparseMetadata(@NotNull String pixelsPath, int changedPixelCount,
                                                  @NotNull AfmaSparseLayoutCodec layoutCodec) {
            return new AfmaSparsePayload(pixelsPath, changedPixelCount, this.channels, layoutCodec, this.codec, this.alphaMode, this.alphaChangedPixelCount);
        }

    }

    /** Reports {@code ResidualStreamStats} measurements produced by the AFMA codec. */
    protected record ResidualStreamStats(int valueCount, int pairCount, int zeroValueCount, int smallValueCount, int smoothPairCount) {

        /** Shared immutable empty value used by the AFMA codec. */
        protected static final ResidualStreamStats EMPTY = new ResidualStreamStats(0, 0, 0, 0, 0);
    }

    /** Retains or reuses {@code ResidualEncodeWorkspace} state while the AFMA codec is active. */
    public static final class ResidualEncodeWorkspace {

        private byte[] colorRawStream = new byte[0];
        private byte[] alphaMask = new byte[0];
        private byte[] alphaRawStream = new byte[0];
        private byte[] primaryTransformStream = new byte[0];
        private byte[] secondaryTransformStream = new byte[0];

        /** Returns the color raw stream produced by the AFMA codec. */
        @NotNull
        protected byte[] colorRawStream(int minLength) {
            if (this.colorRawStream.length < minLength) {
                this.colorRawStream = new byte[minLength];
            }
            return this.colorRawStream;
        }

        /** Returns the alpha mask produced by the AFMA codec. */
        @NotNull
        protected byte[] alphaMask(int minLength) {
            if (this.alphaMask.length < minLength) {
                this.alphaMask = new byte[minLength];
            }
            return this.alphaMask;
        }

        /** Returns the alpha raw stream produced by the AFMA codec. */
        @NotNull
        protected byte[] alphaRawStream(int minLength) {
            if (this.alphaRawStream.length < minLength) {
                this.alphaRawStream = new byte[minLength];
            }
            return this.alphaRawStream;
        }

        /** Transforms the stream for the AFMA codec. */
        @NotNull
        protected byte[] transformStream(int minLength, boolean primaryStream) {
            byte[] transformStream = primaryStream ? this.primaryTransformStream : this.secondaryTransformStream;
            if (transformStream.length < minLength) {
                transformStream = new byte[minLength];
                if (primaryStream) {
                    this.primaryTransformStream = transformStream;
                } else {
                    this.secondaryTransformStream = transformStream;
                }
            }
            return transformStream;
        }

    }

    /** Reads {@code ResidualSampleReader} input for the AFMA codec. */
    public static final class ResidualSampleReader {

        private final byte[] payloadBytes;
        private final int channels;
        private final AfmaResidualCodec codec;
        private final int[] planeOffsets;
        private final int[] planeAccumulators;
        private int interleavedOffset;

        /** Initializes a new {@code ResidualSampleReader} for AFMA codec use. */
        protected ResidualSampleReader(@NotNull byte[] payloadBytes, int offset, int sampleCount, int channels, @NotNull AfmaResidualCodec codec) {
            this.payloadBytes = payloadBytes;
            this.interleavedOffset = offset;
            this.channels = channels;
            this.codec = codec;
            this.planeOffsets = new int[channels];
            this.planeAccumulators = new int[channels];
            if (codec != AfmaResidualCodec.INTERLEAVED) {
                for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                    this.planeOffsets[channelIndex] = offset + (channelIndex * sampleCount);
                }
            }
        }

        /** Reads the next rgb into abgr from the AFMA codec input. */
        public int readNextRgbIntoAbgr(int predictedAbgr) {
            int red = (((predictedAbgr) & 0xFF) + this.nextDelta(0)) & 0xFF;
            int green = (((predictedAbgr >> 8) & 0xFF) + this.nextDelta(1)) & 0xFF;
            int blue = (((predictedAbgr >> 16) & 0xFF) + this.nextDelta(2)) & 0xFF;
            return (predictedAbgr & 0xFF000000) | (blue << 16) | (green << 8) | red;
        }

        /** Reads the next rgba into abgr from the AFMA codec input. */
        public int readNextRgbaIntoAbgr(int predictedAbgr) {
            int red = (((predictedAbgr) & 0xFF) + this.nextDelta(0)) & 0xFF;
            int green = (((predictedAbgr >> 8) & 0xFF) + this.nextDelta(1)) & 0xFF;
            int blue = (((predictedAbgr >> 16) & 0xFF) + this.nextDelta(2)) & 0xFF;
            int alpha = (((predictedAbgr >>> 24) & 0xFF) + this.nextDelta(3)) & 0xFF;
            return (alpha << 24) | (blue << 16) | (green << 8) | red;
        }

        /** Reads the next alpha into abgr from the AFMA codec input. */
        public int readNextAlphaIntoAbgr(int predictedAbgr) {
            int alpha = (((predictedAbgr >>> 24) & 0xFF) + this.nextDelta(0)) & 0xFF;
            return (alpha << 24) | (predictedAbgr & 0x00FFFFFF);
        }

        /** Advances to the delta value in the AFMA codec. */
        protected int nextDelta(int channelIndex) {
            if (this.codec == AfmaResidualCodec.INTERLEAVED) {
                return this.payloadBytes[this.interleavedOffset++];
            }

            int encodedValue = this.payloadBytes[this.planeOffsets[channelIndex]++] & 0xFF;
            if (this.codec == AfmaResidualCodec.PLANAR_ZIGZAG_DELTA) {
                encodedValue = (this.planeAccumulators[channelIndex] + encodedValue) & 0xFF;
                this.planeAccumulators[channelIndex] = encodedValue;
            }
            return switch (this.codec) {
                case PLANAR -> (byte) encodedValue;
                case PLANAR_ZIGZAG, PLANAR_ZIGZAG_DELTA -> zigzagDecode(encodedValue);
                case INTERLEAVED -> throw new IllegalStateException("AFMA interleaved residual readers should not use planar decoding");
            };
        }

    }

}
