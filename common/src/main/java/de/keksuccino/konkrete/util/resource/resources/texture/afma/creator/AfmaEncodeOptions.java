package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** Configures {@code AfmaEncodeOptions} limits and tradeoffs for the AFMA creator. */
public class AfmaEncodeOptions {

    /** Default near lossless max channel delta used by the AFMA creator. */
    public static final int DEFAULT_NEAR_LOSSLESS_MAX_CHANNEL_DELTA = 2;
    /** Default max copy search distance used by the AFMA creator. */
    public static final int DEFAULT_MAX_COPY_SEARCH_DISTANCE = 512;
    /** Default max candidate axis offsets used by the AFMA creator. */
    public static final int DEFAULT_MAX_CANDIDATE_AXIS_OFFSETS = 5;

    /** Current loop count measured or selected by this AFMA creator instance. */
    protected int loopCount = 0;
    /** Current frame time ms measured or selected by this AFMA creator instance. */
    protected long frameTimeMs = 41L;
    /** Current intro frame time ms measured or selected by this AFMA creator instance. */
    protected long introFrameTimeMs = 41L;
    /** Holds the customFrameTimes collection used by this AFMA creator instance. */
    @NotNull
    protected Map<Integer, Long> customFrameTimes = new LinkedHashMap<>();
    /** Holds the customIntroFrameTimes collection used by this AFMA creator instance. */
    @NotNull
    protected Map<Integer, Long> customIntroFrameTimes = new LinkedHashMap<>();
    /** Current keyframe interval state for this AFMA creator instance. */
    protected int keyframeInterval = AfmaMetadata.DEFAULT_KEYFRAME_INTERVAL;
    /** Whether rect copy enabled currently applies to this AFMA creator instance. */
    protected boolean rectCopyEnabled = true;
    /** Whether duplicate frame elision currently applies to this AFMA creator instance. */
    protected boolean duplicateFrameElision = true;
    /** Current max copy search distance state for this AFMA creator instance. */
    protected int maxCopySearchDistance = DEFAULT_MAX_COPY_SEARCH_DISTANCE;
    /** Current max candidate axis offsets state for this AFMA creator instance. */
    protected int maxCandidateAxisOffsets = DEFAULT_MAX_CANDIDATE_AXIS_OFFSETS;
    /** Current max delta area ratio without strong savings state for this AFMA creator instance. */
    protected double maxDeltaAreaRatioWithoutStrongSavings = 0.94D;
    /** Current max copy patch area ratio without strong savings state for this AFMA creator instance. */
    protected double maxCopyPatchAreaRatioWithoutStrongSavings = 0.90D;
    /** Current min complex candidate savings bytes measured or selected by this AFMA creator instance. */
    protected long minComplexCandidateSavingsBytes = 24L * 1024L;
    /** Current min strong complex candidate savings bytes measured or selected by this AFMA creator instance. */
    protected long minStrongComplexCandidateSavingsBytes = 96L * 1024L;
    /** Current min complex candidate savings ratio state for this AFMA creator instance. */
    protected double minComplexCandidateSavingsRatio = 0.015D;
    /** Current min strong complex candidate savings ratio state for this AFMA creator instance. */
    protected double minStrongComplexCandidateSavingsRatio = 0.06D;
    /** Current near lossless max channel delta state for this AFMA creator instance. */
    protected int nearLosslessMaxChannelDelta = 0;
    /** Whether adaptive keyframe placement currently applies to this AFMA creator instance. */
    protected boolean adaptiveKeyframePlacement = false;
    /** Current adaptive max keyframe interval state for this AFMA creator instance. */
    protected int adaptiveMaxKeyframeInterval = AfmaMetadata.DEFAULT_KEYFRAME_INTERVAL;
    /** Current adaptive continuation min savings bytes measured or selected by this AFMA creator instance. */
    protected long adaptiveContinuationMinSavingsBytes = 512L;
    /** Current adaptive continuation min savings ratio state for this AFMA creator instance. */
    protected double adaptiveContinuationMinSavingsRatio = 0.005D;
    /** Current perceptual bin intra max visible color delta state for this AFMA creator instance. */
    protected int perceptualBinIntraMaxVisibleColorDelta = 0;
    /** Current perceptual bin intra max alpha delta state for this AFMA creator instance. */
    protected int perceptualBinIntraMaxAlphaDelta = 0;
    /** Current perceptual bin intra max average error state for this AFMA creator instance. */
    protected double perceptualBinIntraMaxAverageError = 0D;
    /** Current planner search window frames state for this AFMA creator instance. */
    protected int plannerSearchWindowFrames = 12;
    /** Current planner beam width state for this AFMA creator instance. */
    protected int plannerBeamWidth = 8;
    /** Current planner decode cost penalty bytes measured or selected by this AFMA creator instance. */
    protected long plannerDecodeCostPenaltyBytes = 160L;
    /** Current planner complexity penalty bytes measured or selected by this AFMA creator instance. */
    protected long plannerComplexityPenaltyBytes = 16L;
    /** Current planner average drift penalty bytes measured or selected by this AFMA creator instance. */
    protected double plannerAverageDriftPenaltyBytes = 48D;
    /** Current planner visible color drift penalty bytes measured or selected by this AFMA creator instance. */
    protected long plannerVisibleColorDriftPenaltyBytes = 12L;
    /** Current planner alpha drift penalty bytes measured or selected by this AFMA creator instance. */
    protected long plannerAlphaDriftPenaltyBytes = 12L;
    /** Current planner lossy continuation penalty bytes measured or selected by this AFMA creator instance. */
    protected long plannerLossyContinuationPenaltyBytes = 96L;
    /** Current planner keyframe distance penalty bytes measured or selected by this AFMA creator instance. */
    protected long plannerKeyframeDistancePenaltyBytes = 160L;
    /** Whether full frame reference plan enabled currently applies to this AFMA creator instance. */
    protected boolean fullFrameReferencePlanEnabled = false;
    /** Current planner max cumulative average error state for this AFMA creator instance. */
    protected double plannerMaxCumulativeAverageError = 0D;
    /** Current planner max cumulative visible color delta state for this AFMA creator instance. */
    protected int plannerMaxCumulativeVisibleColorDelta = 0;
    /** Current planner max cumulative alpha delta state for this AFMA creator instance. */
    protected int plannerMaxCumulativeAlphaDelta = 0;
    /** Current planner max consecutive lossy frames state for this AFMA creator instance. */
    protected int plannerMaxConsecutiveLossyFrames = 0;

    /** Returns the loop count used by this AFMA creator instance. */
    public int getLoopCount() {
        return this.loopCount;
    }

    /** Sets the loop count used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setLoopCount(int loopCount) {
        this.loopCount = loopCount;
        return this;
    }

    /** Returns the frame time ms used by this AFMA creator instance. */
    public long getFrameTimeMs() {
        return this.frameTimeMs;
    }

    /** Sets the frame time ms used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setFrameTimeMs(long frameTimeMs) {
        this.frameTimeMs = frameTimeMs;
        return this;
    }

    /** Returns the intro frame time ms used by this AFMA creator instance. */
    public long getIntroFrameTimeMs() {
        return this.introFrameTimeMs;
    }

    /** Sets the intro frame time ms used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setIntroFrameTimeMs(long introFrameTimeMs) {
        this.introFrameTimeMs = introFrameTimeMs;
        return this;
    }

    /** Returns the custom frame times used by this AFMA creator instance. */
    @NotNull
    public Map<Integer, Long> getCustomFrameTimes() {
        return this.customFrameTimes;
    }

    /** Sets the custom frame times used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setCustomFrameTimes(@Nullable Map<Integer, Long> customFrameTimes) {
        this.customFrameTimes = (customFrameTimes != null) ? new LinkedHashMap<>(customFrameTimes) : new LinkedHashMap<>();
        return this;
    }

    /** Returns the custom intro frame times used by this AFMA creator instance. */
    @NotNull
    public Map<Integer, Long> getCustomIntroFrameTimes() {
        return this.customIntroFrameTimes;
    }

    /** Sets the custom intro frame times used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setCustomIntroFrameTimes(@Nullable Map<Integer, Long> customIntroFrameTimes) {
        this.customIntroFrameTimes = (customIntroFrameTimes != null) ? new LinkedHashMap<>(customIntroFrameTimes) : new LinkedHashMap<>();
        return this;
    }

    /** Returns the keyframe interval used by this AFMA creator instance. */
    public int getKeyframeInterval() {
        return this.keyframeInterval;
    }

    /** Sets the keyframe interval used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setKeyframeInterval(int keyframeInterval) {
        this.keyframeInterval = keyframeInterval;
        if (this.adaptiveMaxKeyframeInterval < keyframeInterval) {
            this.adaptiveMaxKeyframeInterval = keyframeInterval;
        }
        return this;
    }

    /** Returns whether rect copy enabled. */
    public boolean isRectCopyEnabled() {
        return this.rectCopyEnabled;
    }

    /** Sets the rect copy enabled used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setRectCopyEnabled(boolean rectCopyEnabled) {
        this.rectCopyEnabled = rectCopyEnabled;
        return this;
    }

    /** Returns whether duplicate frame elision. */
    public boolean isDuplicateFrameElision() {
        return this.duplicateFrameElision;
    }

    /** Sets the duplicate frame elision used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setDuplicateFrameElision(boolean duplicateFrameElision) {
        this.duplicateFrameElision = duplicateFrameElision;
        return this;
    }

    /** Returns the max copy search distance used by this AFMA creator instance. */
    public int getMaxCopySearchDistance() {
        return this.maxCopySearchDistance;
    }

    /** Sets the max copy search distance used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setMaxCopySearchDistance(int maxCopySearchDistance) {
        this.maxCopySearchDistance = maxCopySearchDistance;
        return this;
    }

    /** Returns the max candidate axis offsets used by this AFMA creator instance. */
    public int getMaxCandidateAxisOffsets() {
        return this.maxCandidateAxisOffsets;
    }

    /** Sets the max candidate axis offsets used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setMaxCandidateAxisOffsets(int maxCandidateAxisOffsets) {
        this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
        return this;
    }

    /** Returns the max delta area ratio without strong savings used by this AFMA creator instance. */
    public double getMaxDeltaAreaRatioWithoutStrongSavings() {
        return this.maxDeltaAreaRatioWithoutStrongSavings;
    }

    /** Sets the max delta area ratio without strong savings used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setMaxDeltaAreaRatioWithoutStrongSavings(double maxDeltaAreaRatioWithoutStrongSavings) {
        this.maxDeltaAreaRatioWithoutStrongSavings = maxDeltaAreaRatioWithoutStrongSavings;
        return this;
    }

    /** Returns the max copy patch area ratio without strong savings used by this AFMA creator instance. */
    public double getMaxCopyPatchAreaRatioWithoutStrongSavings() {
        return this.maxCopyPatchAreaRatioWithoutStrongSavings;
    }

    /** Sets the max copy patch area ratio without strong savings used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setMaxCopyPatchAreaRatioWithoutStrongSavings(double maxCopyPatchAreaRatioWithoutStrongSavings) {
        this.maxCopyPatchAreaRatioWithoutStrongSavings = maxCopyPatchAreaRatioWithoutStrongSavings;
        return this;
    }

    /** Returns the min complex candidate savings bytes used by this AFMA creator instance. */
    public long getMinComplexCandidateSavingsBytes() {
        return this.minComplexCandidateSavingsBytes;
    }

    /** Sets the min complex candidate savings bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setMinComplexCandidateSavingsBytes(long minComplexCandidateSavingsBytes) {
        this.minComplexCandidateSavingsBytes = minComplexCandidateSavingsBytes;
        return this;
    }

    /** Returns the min strong complex candidate savings bytes used by this AFMA creator instance. */
    public long getMinStrongComplexCandidateSavingsBytes() {
        return this.minStrongComplexCandidateSavingsBytes;
    }

    /** Sets the min strong complex candidate savings bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setMinStrongComplexCandidateSavingsBytes(long minStrongComplexCandidateSavingsBytes) {
        this.minStrongComplexCandidateSavingsBytes = minStrongComplexCandidateSavingsBytes;
        return this;
    }

    /** Returns the min complex candidate savings ratio used by this AFMA creator instance. */
    public double getMinComplexCandidateSavingsRatio() {
        return this.minComplexCandidateSavingsRatio;
    }

    /** Sets the min complex candidate savings ratio used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setMinComplexCandidateSavingsRatio(double minComplexCandidateSavingsRatio) {
        this.minComplexCandidateSavingsRatio = minComplexCandidateSavingsRatio;
        return this;
    }

    /** Returns the min strong complex candidate savings ratio used by this AFMA creator instance. */
    public double getMinStrongComplexCandidateSavingsRatio() {
        return this.minStrongComplexCandidateSavingsRatio;
    }

    /** Sets the min strong complex candidate savings ratio used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setMinStrongComplexCandidateSavingsRatio(double minStrongComplexCandidateSavingsRatio) {
        this.minStrongComplexCandidateSavingsRatio = minStrongComplexCandidateSavingsRatio;
        return this;
    }

    /** Returns the near lossless max channel delta used by this AFMA creator instance. */
    public int getNearLosslessMaxChannelDelta() {
        return this.nearLosslessMaxChannelDelta;
    }

    /** Returns whether near lossless enabled. */
    public boolean isNearLosslessEnabled() {
        return this.nearLosslessMaxChannelDelta > 0;
    }

    /** Sets the near lossless max channel delta used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setNearLosslessMaxChannelDelta(int nearLosslessMaxChannelDelta) {
        this.nearLosslessMaxChannelDelta = nearLosslessMaxChannelDelta;
        return this;
    }

    /** Returns whether adaptive keyframe placement enabled. */
    public boolean isAdaptiveKeyframePlacementEnabled() {
        return this.adaptiveKeyframePlacement;
    }

    /** Sets the adaptive keyframe placement used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setAdaptiveKeyframePlacement(boolean adaptiveKeyframePlacement) {
        this.adaptiveKeyframePlacement = adaptiveKeyframePlacement;
        return this;
    }

    /** Returns the adaptive max keyframe interval used by this AFMA creator instance. */
    public int getAdaptiveMaxKeyframeInterval() {
        return this.adaptiveMaxKeyframeInterval;
    }

    /** Sets the adaptive max keyframe interval used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setAdaptiveMaxKeyframeInterval(int adaptiveMaxKeyframeInterval) {
        this.adaptiveMaxKeyframeInterval = adaptiveMaxKeyframeInterval;
        return this;
    }

    /** Returns the adaptive continuation min savings bytes used by this AFMA creator instance. */
    public long getAdaptiveContinuationMinSavingsBytes() {
        return this.adaptiveContinuationMinSavingsBytes;
    }

    /** Sets the adaptive continuation min savings bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setAdaptiveContinuationMinSavingsBytes(long adaptiveContinuationMinSavingsBytes) {
        this.adaptiveContinuationMinSavingsBytes = adaptiveContinuationMinSavingsBytes;
        return this;
    }

    /** Returns the adaptive continuation min savings ratio used by this AFMA creator instance. */
    public double getAdaptiveContinuationMinSavingsRatio() {
        return this.adaptiveContinuationMinSavingsRatio;
    }

    /** Sets the adaptive continuation min savings ratio used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setAdaptiveContinuationMinSavingsRatio(double adaptiveContinuationMinSavingsRatio) {
        this.adaptiveContinuationMinSavingsRatio = adaptiveContinuationMinSavingsRatio;
        return this;
    }

    /** Returns the perceptual bin intra max visible color delta used by this AFMA creator instance. */
    public int getPerceptualBinIntraMaxVisibleColorDelta() {
        return this.perceptualBinIntraMaxVisibleColorDelta;
    }

    /** Sets the perceptual bin intra max visible color delta used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPerceptualBinIntraMaxVisibleColorDelta(int perceptualBinIntraMaxVisibleColorDelta) {
        this.perceptualBinIntraMaxVisibleColorDelta = perceptualBinIntraMaxVisibleColorDelta;
        return this;
    }

    /** Returns the perceptual bin intra max alpha delta used by this AFMA creator instance. */
    public int getPerceptualBinIntraMaxAlphaDelta() {
        return this.perceptualBinIntraMaxAlphaDelta;
    }

    /** Sets the perceptual bin intra max alpha delta used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPerceptualBinIntraMaxAlphaDelta(int perceptualBinIntraMaxAlphaDelta) {
        this.perceptualBinIntraMaxAlphaDelta = perceptualBinIntraMaxAlphaDelta;
        return this;
    }

    /** Returns the perceptual bin intra max average error used by this AFMA creator instance. */
    public double getPerceptualBinIntraMaxAverageError() {
        return this.perceptualBinIntraMaxAverageError;
    }

    /** Returns whether perceptual bin intra enabled. */
    public boolean isPerceptualBinIntraEnabled() {
        return this.perceptualBinIntraMaxAverageError > 0D
                && this.perceptualBinIntraMaxVisibleColorDelta > 0
                && this.perceptualBinIntraMaxAlphaDelta >= 0;
    }

    /** Sets the perceptual bin intra max average error used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPerceptualBinIntraMaxAverageError(double perceptualBinIntraMaxAverageError) {
        this.perceptualBinIntraMaxAverageError = perceptualBinIntraMaxAverageError;
        return this;
    }

    /** Returns the planner search window frames used by this AFMA creator instance. */
    public int getPlannerSearchWindowFrames() {
        return this.plannerSearchWindowFrames;
    }

    /** Sets the planner search window frames used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerSearchWindowFrames(int plannerSearchWindowFrames) {
        this.plannerSearchWindowFrames = plannerSearchWindowFrames;
        return this;
    }

    /** Returns the planner beam width used by this AFMA creator instance. */
    public int getPlannerBeamWidth() {
        return this.plannerBeamWidth;
    }

    /** Sets the planner beam width used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerBeamWidth(int plannerBeamWidth) {
        this.plannerBeamWidth = plannerBeamWidth;
        return this;
    }

    /** Returns the planner decode cost penalty bytes used by this AFMA creator instance. */
    public long getPlannerDecodeCostPenaltyBytes() {
        return this.plannerDecodeCostPenaltyBytes;
    }

    /** Sets the planner decode cost penalty bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerDecodeCostPenaltyBytes(long plannerDecodeCostPenaltyBytes) {
        this.plannerDecodeCostPenaltyBytes = plannerDecodeCostPenaltyBytes;
        return this;
    }

    /** Returns the planner complexity penalty bytes used by this AFMA creator instance. */
    public long getPlannerComplexityPenaltyBytes() {
        return this.plannerComplexityPenaltyBytes;
    }

    /** Sets the planner complexity penalty bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerComplexityPenaltyBytes(long plannerComplexityPenaltyBytes) {
        this.plannerComplexityPenaltyBytes = plannerComplexityPenaltyBytes;
        return this;
    }

    /** Returns the planner average drift penalty bytes used by this AFMA creator instance. */
    public double getPlannerAverageDriftPenaltyBytes() {
        return this.plannerAverageDriftPenaltyBytes;
    }

    /** Sets the planner average drift penalty bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerAverageDriftPenaltyBytes(double plannerAverageDriftPenaltyBytes) {
        this.plannerAverageDriftPenaltyBytes = plannerAverageDriftPenaltyBytes;
        return this;
    }

    /** Returns the planner visible color drift penalty bytes used by this AFMA creator instance. */
    public long getPlannerVisibleColorDriftPenaltyBytes() {
        return this.plannerVisibleColorDriftPenaltyBytes;
    }

    /** Sets the planner visible color drift penalty bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerVisibleColorDriftPenaltyBytes(long plannerVisibleColorDriftPenaltyBytes) {
        this.plannerVisibleColorDriftPenaltyBytes = plannerVisibleColorDriftPenaltyBytes;
        return this;
    }

    /** Returns the planner alpha drift penalty bytes used by this AFMA creator instance. */
    public long getPlannerAlphaDriftPenaltyBytes() {
        return this.plannerAlphaDriftPenaltyBytes;
    }

    /** Sets the planner alpha drift penalty bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerAlphaDriftPenaltyBytes(long plannerAlphaDriftPenaltyBytes) {
        this.plannerAlphaDriftPenaltyBytes = plannerAlphaDriftPenaltyBytes;
        return this;
    }

    /** Returns the planner lossy continuation penalty bytes used by this AFMA creator instance. */
    public long getPlannerLossyContinuationPenaltyBytes() {
        return this.plannerLossyContinuationPenaltyBytes;
    }

    /** Sets the planner lossy continuation penalty bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerLossyContinuationPenaltyBytes(long plannerLossyContinuationPenaltyBytes) {
        this.plannerLossyContinuationPenaltyBytes = plannerLossyContinuationPenaltyBytes;
        return this;
    }

    /** Returns the planner keyframe distance penalty bytes used by this AFMA creator instance. */
    public long getPlannerKeyframeDistancePenaltyBytes() {
        return this.plannerKeyframeDistancePenaltyBytes;
    }

    /** Sets the planner keyframe distance penalty bytes used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerKeyframeDistancePenaltyBytes(long plannerKeyframeDistancePenaltyBytes) {
        this.plannerKeyframeDistancePenaltyBytes = plannerKeyframeDistancePenaltyBytes;
        return this;
    }

    /** Returns whether full frame reference plan enabled. */
    public boolean isFullFrameReferencePlanEnabled() {
        return this.fullFrameReferencePlanEnabled;
    }

    /** Sets the full frame reference plan enabled used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setFullFrameReferencePlanEnabled(boolean fullFrameReferencePlanEnabled) {
        this.fullFrameReferencePlanEnabled = fullFrameReferencePlanEnabled;
        return this;
    }

    /** Returns the planner max cumulative average error used by this AFMA creator instance. */
    public double getPlannerMaxCumulativeAverageError() {
        return this.plannerMaxCumulativeAverageError;
    }

    /** Sets the planner max cumulative average error used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerMaxCumulativeAverageError(double plannerMaxCumulativeAverageError) {
        this.plannerMaxCumulativeAverageError = plannerMaxCumulativeAverageError;
        return this;
    }

    /** Returns the planner max cumulative visible color delta used by this AFMA creator instance. */
    public int getPlannerMaxCumulativeVisibleColorDelta() {
        return this.plannerMaxCumulativeVisibleColorDelta;
    }

    /** Sets the planner max cumulative visible color delta used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerMaxCumulativeVisibleColorDelta(int plannerMaxCumulativeVisibleColorDelta) {
        this.plannerMaxCumulativeVisibleColorDelta = plannerMaxCumulativeVisibleColorDelta;
        return this;
    }

    /** Returns the planner max cumulative alpha delta used by this AFMA creator instance. */
    public int getPlannerMaxCumulativeAlphaDelta() {
        return this.plannerMaxCumulativeAlphaDelta;
    }

    /** Sets the planner max cumulative alpha delta used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerMaxCumulativeAlphaDelta(int plannerMaxCumulativeAlphaDelta) {
        this.plannerMaxCumulativeAlphaDelta = plannerMaxCumulativeAlphaDelta;
        return this;
    }

    /** Returns the planner max consecutive lossy frames used by this AFMA creator instance. */
    public int getPlannerMaxConsecutiveLossyFrames() {
        return this.plannerMaxConsecutiveLossyFrames;
    }

    /** Sets the planner max consecutive lossy frames used by subsequent AFMA creator operations. */
    public AfmaEncodeOptions setPlannerMaxConsecutiveLossyFrames(int plannerMaxConsecutiveLossyFrames) {
        this.plannerMaxConsecutiveLossyFrames = plannerMaxConsecutiveLossyFrames;
        return this;
    }

    /** Validates frame counts, timing, search bounds, and custom frame indexes before planning. */
    public void validateForCounts(int mainFrameCount, int introFrameCount) {
        ResourceRuntime.getSafetyLimits().validateFrameCount((long) mainFrameCount + introFrameCount, "AFMA source");
        if (mainFrameCount <= 0 && introFrameCount <= 0) {
            throw new IllegalArgumentException("AFMA encoding requires at least one main or intro frame");
        }
        if (this.frameTimeMs <= 0L || this.introFrameTimeMs <= 0L) {
            throw new IllegalArgumentException("AFMA frame times must be greater than 0");
        }
        if (this.keyframeInterval <= 0) {
            throw new IllegalArgumentException("AFMA keyframe interval must be greater than 0");
        }
        if (this.adaptiveMaxKeyframeInterval <= 0 || this.adaptiveMaxKeyframeInterval < this.keyframeInterval) {
            throw new IllegalArgumentException("AFMA adaptive keyframe interval must be greater than or equal to the preferred keyframe interval");
        }
        if (this.maxCopySearchDistance < 0) {
            throw new IllegalArgumentException("AFMA copy search distance cannot be negative");
        }
        if (this.maxCandidateAxisOffsets <= 0) {
            throw new IllegalArgumentException("AFMA candidate axis count must be greater than 0");
        }
        if (this.maxDeltaAreaRatioWithoutStrongSavings <= 0D || this.maxDeltaAreaRatioWithoutStrongSavings > 1D) {
            throw new IllegalArgumentException("AFMA delta area ratio must stay within (0, 1]");
        }
        if (this.maxCopyPatchAreaRatioWithoutStrongSavings <= 0D || this.maxCopyPatchAreaRatioWithoutStrongSavings > 1D) {
            throw new IllegalArgumentException("AFMA copy patch area ratio must stay within (0, 1]");
        }
        if (this.minComplexCandidateSavingsBytes < 0L || this.minStrongComplexCandidateSavingsBytes < 0L) {
            throw new IllegalArgumentException("AFMA candidate savings thresholds cannot be negative");
        }
        if (this.minComplexCandidateSavingsRatio < 0D || this.minStrongComplexCandidateSavingsRatio < 0D) {
            throw new IllegalArgumentException("AFMA candidate savings ratios cannot be negative");
        }
        if (this.nearLosslessMaxChannelDelta < 0 || this.nearLosslessMaxChannelDelta > 255) {
            throw new IllegalArgumentException("AFMA near-lossless channel delta must stay within [0, 255]");
        }
        if (this.adaptiveContinuationMinSavingsBytes < 0L || this.adaptiveContinuationMinSavingsRatio < 0D) {
            throw new IllegalArgumentException("AFMA adaptive GOP continuation thresholds cannot be negative");
        }
        if (this.perceptualBinIntraMaxVisibleColorDelta < 0 || this.perceptualBinIntraMaxVisibleColorDelta > 255
                || this.perceptualBinIntraMaxAlphaDelta < 0 || this.perceptualBinIntraMaxAlphaDelta > 255) {
            throw new IllegalArgumentException("AFMA perceptual BIN_INTRA deltas must stay within [0, 255]");
        }
        if (this.perceptualBinIntraMaxAverageError < 0D) {
            throw new IllegalArgumentException("AFMA perceptual BIN_INTRA average error cannot be negative");
        }
        if (this.plannerSearchWindowFrames <= 0) {
            throw new IllegalArgumentException("AFMA planner search window must be greater than 0");
        }
        if (this.plannerBeamWidth <= 0) {
            throw new IllegalArgumentException("AFMA planner beam width must be greater than 0");
        }
        if (this.plannerDecodeCostPenaltyBytes < 0L
                || this.plannerComplexityPenaltyBytes < 0L
                || this.plannerVisibleColorDriftPenaltyBytes < 0L
                || this.plannerAlphaDriftPenaltyBytes < 0L
                || this.plannerLossyContinuationPenaltyBytes < 0L
                || this.plannerKeyframeDistancePenaltyBytes < 0L) {
            throw new IllegalArgumentException("AFMA planner penalty values cannot be negative");
        }
        if (this.plannerAverageDriftPenaltyBytes < 0D) {
            throw new IllegalArgumentException("AFMA planner drift penalty cannot be negative");
        }
        if (this.plannerMaxCumulativeAverageError < 0D
                || this.plannerMaxCumulativeVisibleColorDelta < 0
                || this.plannerMaxCumulativeAlphaDelta < 0
                || this.plannerMaxConsecutiveLossyFrames < 0) {
            throw new IllegalArgumentException("AFMA planner cumulative drift limits cannot be negative");
        }

        validateCustomFrameTimes(this.customFrameTimes, mainFrameCount, "main");
        validateCustomFrameTimes(this.customIntroFrameTimes, introFrameCount, "intro");
    }

    /** Validates the custom frame times before AFMA creator processing. */
    protected static void validateCustomFrameTimes(@NotNull Map<Integer, Long> frameTimes, int frameCount, @NotNull String sequenceName) {
        for (Map.Entry<Integer, Long> entry : frameTimes.entrySet()) {
            Integer frameIndex = entry.getKey();
            Long delay = entry.getValue();
            if (frameIndex == null || frameIndex < 0 || frameIndex >= frameCount) {
                throw new IllegalArgumentException("Invalid custom " + sequenceName + " frame index: " + frameIndex);
            }
            if (delay == null || delay <= 0L) {
                throw new IllegalArgumentException("Invalid custom " + sequenceName + " frame delay at index " + frameIndex);
            }
        }
    }

}
