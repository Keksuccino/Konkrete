package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaMultiCopy;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Models {@code AfmaRectCopyDetector} state used by the AFMA creator. */
public class AfmaRectCopyDetector {

    /** Lower bound for candidate score accepted by the AFMA creator. */
    protected static final double MIN_CANDIDATE_SCORE = 0.20D;
    /** Decision threshold for small offset probe in the AFMA creator. */
    protected static final int SMALL_OFFSET_PROBE = 4;
    /** Search or sampling budget for neighbor offset radius in the AFMA creator. */
    protected static final int NEIGHBOR_OFFSET_RADIUS = 2;
    /** Lower bound for single copy motion vectors accepted by the AFMA creator. */
    protected static final int MIN_SINGLE_COPY_MOTION_VECTORS = 12;
    /** Upper bound for single copy motion vectors enforced by the AFMA creator. */
    protected static final int MAX_SINGLE_COPY_MOTION_VECTORS = 40;
    /** Search or sampling budget for motion hash stage one columns in the AFMA creator. */
    protected static final int MOTION_HASH_STAGE_ONE_COLUMNS = 6;
    /** Search or sampling budget for motion hash stage one rows in the AFMA creator. */
    protected static final int MOTION_HASH_STAGE_ONE_ROWS = 4;
    /** Search or sampling budget for motion hash stage two columns in the AFMA creator. */
    protected static final int MOTION_HASH_STAGE_TWO_COLUMNS = 12;
    /** Search or sampling budget for motion hash stage two rows in the AFMA creator. */
    protected static final int MOTION_HASH_STAGE_TWO_ROWS = 8;
    /** Scoring or hashing coefficient for motion hash frontier multiplier in the AFMA creator. */
    protected static final int MOTION_HASH_FRONTIER_MULTIPLIER = 6;
    /** Scoring or hashing coefficient for motion hash second stage multiplier in the AFMA creator. */
    protected static final int MOTION_HASH_SECOND_STAGE_MULTIPLIER = 3;
    /** Search or sampling budget for motion neighbor refinement seeds in the AFMA creator. */
    protected static final int MOTION_NEIGHBOR_REFINEMENT_SEEDS = 4;
    /** Search or sampling budget for motion neighbor refinement radius in the AFMA creator. */
    protected static final int MOTION_NEIGHBOR_REFINEMENT_RADIUS = 2;
    /** Upper bound for multi copy rects enforced by the AFMA creator. */
    protected static final int MAX_MULTI_COPY_RECTS = 4;
    /** Upper bound for multi copy motion vectors enforced by the AFMA creator. */
    protected static final int MAX_MULTI_COPY_MOTION_VECTORS = 12;
    /** Lower bound for multi copy rect area accepted by the AFMA creator. */
    protected static final int MIN_MULTI_COPY_RECT_AREA = 64;
    /** Lower bound for multi copy dirty pixels accepted by the AFMA creator. */
    protected static final int MIN_MULTI_COPY_DIRTY_PIXELS = 48;
    /** Lower bound for multi copy dirty density accepted by the AFMA creator. */
    protected static final double MIN_MULTI_COPY_DIRTY_DENSITY = 0.35D;
    /** Upper bound for multi copy dirty density without single evidence enforced by the AFMA creator. */
    protected static final double MAX_MULTI_COPY_DIRTY_DENSITY_WITHOUT_SINGLE_EVIDENCE = 0.80D;
    /** Lower bound for multi copy remaining dirty ratio after single accepted by the AFMA creator. */
    protected static final double MIN_MULTI_COPY_REMAINING_DIRTY_RATIO_AFTER_SINGLE = 0.20D;
    /** Lower bound for multi copy remaining patch ratio after single accepted by the AFMA creator. */
    protected static final double MIN_MULTI_COPY_REMAINING_PATCH_RATIO_AFTER_SINGLE = 0.25D;

    /** Current max search distance state for this AFMA creator instance. */
    protected final int maxSearchDistance;
    /** Current max candidate axis offsets state for this AFMA creator instance. */
    protected final int maxCandidateAxisOffsets;

    /** Initializes a new {@code AfmaRectCopyDetector} for AFMA creator use. */
    public AfmaRectCopyDetector(int maxSearchDistance, int maxCandidateAxisOffsets) {
        this.maxSearchDistance = maxSearchDistance;
        this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
    }

    /** Detects resource state in the AFMA creator input. */
    @Nullable
    public Detection detect(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        return this.detect(new AfmaFramePairAnalysis(previous, next));
    }

    /** Detects resource state in the AFMA creator input. */
    @Nullable
    public Detection detect(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        if (pairAnalysis.isIdentical()) {
            return null;
        }
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        List<MotionVector> motionVectors = this.collectTopMotionVectors(pairAnalysis, this.computeMaxSingleCopyMotionVectors(pairAnalysis));
        if (motionVectors.isEmpty()) {
            return null;
        }

        int width = previous.getWidth();
        int height = previous.getHeight();

        Detection bestDetection = null;
        for (MotionVector motionVector : motionVectors) {
            int dx = motionVector.dx();
            int dy = motionVector.dy();
            int overlapWidth = width - Math.abs(dx);
            int overlapHeight = height - Math.abs(dy);
            if (overlapWidth <= 0 || overlapHeight <= 0) {
                continue;
            }

            AfmaCopyRect copyRect = new AfmaCopyRect(
                    Math.max(0, -dx),
                    Math.max(0, -dy),
                    Math.max(0, dx),
                    Math.max(0, dy),
                    overlapWidth,
                    overlapHeight
            );

            var dirtyAfterCopy = pairAnalysis.analyzeDirtyAfterCopy(copyRect);
            AfmaRect patchBounds = dirtyAfterCopy.bounds();
            long patchArea = (patchBounds != null) ? patchBounds.area() : 0L;
            long copyArea = copyRect.getArea();
            long usefulness = copyArea - patchArea;
            if (usefulness <= 0L) {
                continue;
            }

            Detection detection = new Detection(copyRect, patchBounds, usefulness, dirtyAfterCopy.dirtyPixelCount());
            if ((bestDetection == null) || (detection.usefulness() > bestDetection.usefulness())
                    || ((detection.usefulness() == bestDetection.usefulness()) && (patchArea < bestDetection.patchArea()))) {
                bestDetection = detection;
            }
        }

        return bestDetection;
    }

    /** Detects the multi in the AFMA creator input. */
    @Nullable
    public MultiDetection detectMulti(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        return this.detectMulti(new AfmaFramePairAnalysis(previous, next));
    }

    /** Detects the multi in the AFMA creator input. */
    @Nullable
    public MultiDetection detectMulti(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        return this.detectMulti(pairAnalysis, null);
    }

    /** Detects the multi in the AFMA creator input. */
    @Nullable
    public MultiDetection detectMulti(@NotNull AfmaFramePairAnalysis pairAnalysis, @Nullable Detection singleDetection) {
        if (pairAnalysis.isIdentical()) {
            return null;
        }
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        AfmaPixelFrame next = pairAnalysis.nextFrame();

        AfmaRect initialDirtyBounds = pairAnalysis.differenceBounds();
        if ((initialDirtyBounds == null) || !this.shouldAttemptMultiCopy(pairAnalysis, singleDetection, initialDirtyBounds)) {
            return null;
        }

        long initialDirtyPixelCount = pairAnalysis.changedPixelCount();
        int width = previous.getWidth();
        int height = previous.getHeight();
        int[] predictedPixels = previous.copyPixels();
        ArrayList<AfmaCopyRect> copyRects = new ArrayList<>();
        MultiCopyScratchWorkspace multiCopyScratchWorkspace = new MultiCopyScratchWorkspace((width / 2) + 2);
        long totalDirtyCoverage = 0L;
        AfmaFramePairAnalysis motionSearchPairAnalysis = pairAnalysis;

        for (int copyIndex = 0; copyIndex < MAX_MULTI_COPY_RECTS; copyIndex++) {
            CopyRectCandidate nextCandidate = this.findBestMultiCopyRect(
                    motionSearchPairAnalysis,
                    predictedPixels,
                    width,
                    height,
                    next,
                    multiCopyScratchWorkspace
            );
            if (nextCandidate == null) {
                break;
            }

            AfmaPixelFrameHelper.applyCopyRect(predictedPixels, width, nextCandidate.copyRect());
            copyRects.add(nextCandidate.copyRect());
            totalDirtyCoverage += nextCandidate.dirtyPixels();
            if ((initialDirtyPixelCount - totalDirtyCoverage) < MIN_MULTI_COPY_DIRTY_PIXELS) {
                break;
            }
            motionSearchPairAnalysis = null;
        }

        if (copyRects.size() < 2) {
            return null;
        }

        AfmaPixelFrame predictedFrame = new AfmaPixelFrame(width, height, predictedPixels);
        AfmaFramePairAnalysis predictedPairAnalysis = new AfmaFramePairAnalysis(predictedFrame, next);
        AfmaRect patchBounds = predictedPairAnalysis.differenceBounds();
        int remainingDirtyPixelCount = predictedPairAnalysis.changedPixelCount();
        long remainingPatchArea = (patchBounds != null) ? patchBounds.area() : 0L;
        long patchReduction = initialDirtyBounds.area() - remainingPatchArea;
        if (patchReduction <= 0L) {
            return null;
        }

        return new MultiDetection(new AfmaMultiCopy(copyRects), patchBounds, patchReduction + totalDirtyCoverage, remainingDirtyPixelCount);
    }

    /** Returns whether attempt multi copy. */
    protected boolean shouldAttemptMultiCopy(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                             @Nullable Detection singleDetection, @NotNull AfmaRect initialDirtyBounds) {
        long initialDirtyPixelCount = pairAnalysis.changedPixelCount();
        if (initialDirtyPixelCount < ((long) MIN_MULTI_COPY_DIRTY_PIXELS * 2L)) {
            return false;
        }

        long dirtyBoundsArea = initialDirtyBounds.area();
        if (dirtyBoundsArea <= 0L) {
            return false;
        }

        if (singleDetection == null) {
            return ((double) initialDirtyPixelCount / (double) dirtyBoundsArea) <= MAX_MULTI_COPY_DIRTY_DENSITY_WITHOUT_SINGLE_EVIDENCE;
        }

        if (singleDetection.patchArea() <= 0L) {
            return false;
        }

        // Skip multi-copy when the best single-copy already collapses almost all remaining work.
        double remainingDirtyRatio = (double) singleDetection.remainingDirtyPixelCount() / (double) initialDirtyPixelCount;
        double remainingPatchRatio = (double) singleDetection.patchArea() / (double) dirtyBoundsArea;
        return (remainingDirtyRatio >= MIN_MULTI_COPY_REMAINING_DIRTY_RATIO_AFTER_SINGLE)
                || (remainingPatchRatio >= MIN_MULTI_COPY_REMAINING_PATCH_RATIO_AFTER_SINGLE);
    }

    /** Collects the motion vectors for the AFMA creator. */
    @NotNull
    public List<MotionVector> collectMotionVectors(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, boolean includeZeroVector) {
        return this.collectMotionVectors(new AfmaFramePairAnalysis(previous, next), includeZeroVector);
    }

    /** Collects the motion vectors for the AFMA creator. */
    @NotNull
    public List<MotionVector> collectMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis, boolean includeZeroVector) {
        MotionSearchAnalysis motionSearchAnalysis = this.getMotionSearchAnalysis(pairAnalysis);
        this.ensureAxisCandidates(pairAnalysis, motionSearchAnalysis);
        this.ensureRankedMotionVectors(pairAnalysis, motionSearchAnalysis);
        return includeZeroVector ? motionSearchAnalysis.rankedMotionVectors() : motionSearchAnalysis.rankedMotionVectorsWithoutZero();
    }

    /** Collects the top motion vectors for the AFMA creator. */
    @NotNull
    protected List<MotionVector> collectTopMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis, int maxNonZeroVectors) {
        if (maxNonZeroVectors <= 0) {
            return List.of();
        }

        MotionSearchAnalysis motionSearchAnalysis = this.getMotionSearchAnalysis(pairAnalysis);
        this.ensureTopRankedMotionVectors(pairAnalysis, motionSearchAnalysis, maxNonZeroVectors);
        return motionSearchAnalysis.topRankedMotionVectorsWithoutZero(maxNonZeroVectors);
    }

    /** Collects the bounded motion candidates for the AFMA creator. */
    @NotNull
    public List<AfmaTileMotionSearch.MotionCandidate> collectBoundedMotionCandidates(@NotNull AfmaPixelFrame previous,
                                                                                     @NotNull AfmaPixelFrame next,
                                                                                     int maxNonZeroVectors) {
        return this.collectBoundedMotionCandidates(new AfmaFramePairAnalysis(previous, next), maxNonZeroVectors);
    }

    /** Collects the bounded motion candidates for the AFMA creator. */
    @NotNull
    public List<AfmaTileMotionSearch.MotionCandidate> collectBoundedMotionCandidates(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                                     int maxNonZeroVectors) {
        if (pairAnalysis.isIdentical() || (maxNonZeroVectors <= 0)) {
            return List.of();
        }
        return AfmaTileMotionSearch.collectFrameCandidates(pairAnalysis, this.maxSearchDistance, maxNonZeroVectors);
    }

    /** Returns the motion search analysis used by this AFMA creator instance. */
    @NotNull
    protected MotionSearchAnalysis getMotionSearchAnalysis(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        // Cache motion search bookkeeping even when the expensive axis scan has not run yet.
        MotionSearchAnalysis cachedAnalysis = pairAnalysis.getMotionSearchAnalysis(this.maxSearchDistance, this.maxCandidateAxisOffsets);
        if (cachedAnalysis != null) {
            return cachedAnalysis;
        }

        MotionSearchAnalysis analysis = new MotionSearchAnalysis();
        pairAnalysis.cacheMotionSearchAnalysis(this.maxSearchDistance, this.maxCandidateAxisOffsets, analysis);
        return analysis;
    }

    /** Ensures the axis candidates is valid for the AFMA creator. */
    protected void ensureAxisCandidates(@NotNull AfmaFramePairAnalysis pairAnalysis, @NotNull MotionSearchAnalysis motionSearchAnalysis) {
        if (motionSearchAnalysis.hasAxisCandidates()) {
            return;
        }

        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int maxDx = Math.min(previous.getWidth() - 1, this.maxSearchDistance);
        int maxDy = Math.min(previous.getHeight() - 1, this.maxSearchDistance);
        motionSearchAnalysis.cacheAxisCandidates(
                this.collectAxisCandidates(pairAnalysis, true, maxDx),
                this.collectAxisCandidates(pairAnalysis, false, maxDy)
        );
    }

    /** Calculates the max single copy motion vectors for the AFMA creator. */
    protected int computeMaxSingleCopyMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        AfmaRect dirtyBounds = pairAnalysis.differenceBounds();
        if (dirtyBounds == null) {
            return MIN_SINGLE_COPY_MOTION_VECTORS;
        }

        int changedPixelCount = pairAnalysis.changedPixelCount();
        long dirtyArea = dirtyBounds.area();
        int budget = MIN_SINGLE_COPY_MOTION_VECTORS
                + Math.max(changedPixelCount / 2048, (int) (dirtyArea / 4096L));
        return Math.max(MIN_SINGLE_COPY_MOTION_VECTORS, Math.min(MAX_SINGLE_COPY_MOTION_VECTORS, budget));
    }

    /** Collects the candidate motion vectors for the AFMA creator. */
    @NotNull
    protected List<MotionVector> collectCandidateMotionVectors(@NotNull MotionSearchAnalysis motionSearchAnalysis,
                                                               int width, int height) {
        List<MotionVector> cachedCandidateVectors = motionSearchAnalysis.candidateMotionVectors();
        if (cachedCandidateVectors != null) {
            return cachedCandidateVectors;
        }

        ArrayList<MotionVector> candidateVectors = new ArrayList<>(
                motionSearchAnalysis.candidateDx().size() * motionSearchAnalysis.candidateDy().size()
        );
        for (int dx : motionSearchAnalysis.candidateDx()) {
            for (int dy : motionSearchAnalysis.candidateDy()) {
                if ((dx == 0) && (dy == 0)) {
                    continue;
                }
                if (this.isValidMotionVector(dx, dy, width, height)) {
                    candidateVectors.add(new MotionVector(dx, dy));
                }
            }
        }
        List<MotionVector> cachedVectors = List.copyOf(candidateVectors);
        motionSearchAnalysis.cacheCandidateMotionVectors(cachedVectors);
        return cachedVectors;
    }

    /** Ensures the ranked motion vectors is valid for the AFMA creator. */
    protected void ensureRankedMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis, @NotNull MotionSearchAnalysis motionSearchAnalysis) {
        if (motionSearchAnalysis.hasRankedMotionVectors()) {
            return;
        }
        this.ensureAxisCandidates(pairAnalysis, motionSearchAnalysis);

        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int width = previous.getWidth();
        int height = previous.getHeight();
        ArrayList<ScoredMotionVector> scoredVectors = new ArrayList<>(
                (motionSearchAnalysis.candidateDx().size() * motionSearchAnalysis.candidateDy().size()) + 1
        );
        scoredVectors.add(new ScoredMotionVector(new MotionVector(0, 0),
                this.getOrComputeMotionVectorScore(pairAnalysis, motionSearchAnalysis, 0, 0)));
        for (int dx : motionSearchAnalysis.candidateDx()) {
            for (int dy : motionSearchAnalysis.candidateDy()) {
                if ((dx == 0) && (dy == 0)) {
                    continue;
                }

                int overlapWidth = width - Math.abs(dx);
                int overlapHeight = height - Math.abs(dy);
                if (overlapWidth <= 0 || overlapHeight <= 0) {
                    continue;
                }

                scoredVectors.add(new ScoredMotionVector(
                        new MotionVector(dx, dy),
                        this.getOrComputeMotionVectorScore(pairAnalysis, motionSearchAnalysis, dx, dy)
                ));
            }
        }

        scoredVectors.sort(Comparator.comparingDouble(ScoredMotionVector::score).reversed());
        ArrayList<MotionVector> rankedVectors = new ArrayList<>(scoredVectors.size());
        ArrayList<MotionVector> rankedVectorsWithoutZero = new ArrayList<>(Math.max(0, scoredVectors.size() - 1));
        for (ScoredMotionVector scoredVector : scoredVectors) {
            MotionVector motionVector = scoredVector.vector();
            rankedVectors.add(motionVector);
            if ((motionVector.dx() != 0) || (motionVector.dy() != 0)) {
                rankedVectorsWithoutZero.add(motionVector);
            }
        }

        motionSearchAnalysis.cacheRankedMotionVectors(List.copyOf(rankedVectors), List.copyOf(rankedVectorsWithoutZero));
    }

    /** Ensures the top ranked motion vectors is valid for the AFMA creator. */
    protected void ensureTopRankedMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                @NotNull MotionSearchAnalysis motionSearchAnalysis, int maxNonZeroVectors) {
        if (motionSearchAnalysis.hasTopRankedMotionVectorsWithoutZero(maxNonZeroVectors)) {
            return;
        }

        List<AfmaTileMotionSearch.MotionCandidate> boundedCandidates = this.collectBoundedMotionCandidates(pairAnalysis, maxNonZeroVectors);
        if (!boundedCandidates.isEmpty()) {
            ArrayList<MotionVector> boundedVectors = new ArrayList<>(boundedCandidates.size());
            for (AfmaTileMotionSearch.MotionCandidate boundedCandidate : boundedCandidates) {
                boundedVectors.add(boundedCandidate.vector());
            }
            motionSearchAnalysis.cacheTopRankedMotionVectorsWithoutZero(
                    List.copyOf(boundedVectors),
                    boundedVectors.size() < maxNonZeroVectors,
                    maxNonZeroVectors
            );
            return;
        }

        this.ensureAxisCandidates(pairAnalysis, motionSearchAnalysis);

        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int width = previous.getWidth();
        int height = previous.getHeight();
        List<MotionVector> candidateVectors = this.collectCandidateMotionVectors(motionSearchAnalysis, width, height);
        if (candidateVectors.isEmpty()) {
            motionSearchAnalysis.cacheTopRankedMotionVectorsWithoutZero(List.of(), true, maxNonZeroVectors);
            return;
        }

        if (candidateVectors.size() <= maxNonZeroVectors) {
            motionSearchAnalysis.cacheTopRankedMotionVectorsWithoutZero(
                    this.rankMotionVectorsByScore(pairAnalysis, motionSearchAnalysis, candidateVectors, maxNonZeroVectors),
                    true,
                    maxNonZeroVectors
            );
            return;
        }

        int firstStageLimit = Math.min(
                candidateVectors.size(),
                Math.max(maxNonZeroVectors, maxNonZeroVectors * MOTION_HASH_FRONTIER_MULTIPLIER)
        );
        List<MotionVector> firstStageFrontier = this.shortlistMotionVectorsByHash(
                pairAnalysis,
                motionSearchAnalysis,
                candidateVectors,
                firstStageLimit,
                MOTION_HASH_STAGE_ONE_COLUMNS,
                MOTION_HASH_STAGE_ONE_ROWS
        );
        List<MotionVector> expandedFirstStageFrontier = this.expandMotionVectorNeighborhood(
                firstStageFrontier,
                width,
                height,
                MOTION_NEIGHBOR_REFINEMENT_SEEDS,
                MOTION_NEIGHBOR_REFINEMENT_RADIUS
        );
        int secondStageLimit = Math.min(
                expandedFirstStageFrontier.size(),
                Math.max(maxNonZeroVectors, maxNonZeroVectors * MOTION_HASH_SECOND_STAGE_MULTIPLIER)
        );
        List<MotionVector> secondStageFrontier = this.shortlistMotionVectorsByHash(
                pairAnalysis,
                motionSearchAnalysis,
                expandedFirstStageFrontier,
                secondStageLimit,
                MOTION_HASH_STAGE_TWO_COLUMNS,
                MOTION_HASH_STAGE_TWO_ROWS
        );
        List<MotionVector> finalFrontier = this.expandMotionVectorNeighborhood(
                secondStageFrontier,
                width,
                height,
                MOTION_NEIGHBOR_REFINEMENT_SEEDS,
                1
        );
        motionSearchAnalysis.cacheTopRankedMotionVectorsWithoutZero(
                this.rankMotionVectorsByScore(pairAnalysis, motionSearchAnalysis, finalFrontier, maxNonZeroVectors),
                false,
                maxNonZeroVectors
        );
    }

    /** Selects the motion vectors by hash for the AFMA creator. */
    @NotNull
    protected List<MotionVector> shortlistMotionVectorsByHash(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                              @NotNull MotionSearchAnalysis motionSearchAnalysis,
                                                              @NotNull List<MotionVector> motionVectors, int maxVectors,
                                                              int sampleColumns, int sampleRows) {
        ArrayList<ScoredMotionVector> rankedVectors = new ArrayList<>(Math.min(maxVectors, motionVectors.size()));
        for (MotionVector motionVector : motionVectors) {
            this.insertRankedMotionVector(
                    rankedVectors,
                    maxVectors,
                    motionVector.dx(),
                    motionVector.dy(),
                    this.getOrComputeMotionVectorHashScore(
                            pairAnalysis,
                            motionSearchAnalysis,
                            motionVector.dx(),
                            motionVector.dy(),
                            sampleColumns,
                            sampleRows
                    )
            );
        }
        return this.extractMotionVectors(rankedVectors);
    }

    /** Ranks the motion vectors by score for the AFMA creator. */
    @NotNull
    protected List<MotionVector> rankMotionVectorsByScore(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                          @NotNull MotionSearchAnalysis motionSearchAnalysis,
                                                          @NotNull List<MotionVector> motionVectors, int maxVectors) {
        ArrayList<ScoredMotionVector> rankedVectors = new ArrayList<>(Math.min(maxVectors, motionVectors.size()));
        for (MotionVector motionVector : motionVectors) {
            this.insertRankedMotionVector(
                    rankedVectors,
                    maxVectors,
                    motionVector.dx(),
                    motionVector.dy(),
                    this.getOrComputeMotionVectorScore(pairAnalysis, motionSearchAnalysis, motionVector.dx(), motionVector.dy())
            );
        }
        return this.extractMotionVectors(rankedVectors);
    }

    /** Returns the or compute motion vector score used by this AFMA creator instance. */
    protected double getOrComputeMotionVectorScore(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                   @NotNull MotionSearchAnalysis motionSearchAnalysis,
                                                   int dx, int dy) {
        long motionVectorKey = packMotionVector(dx, dy);
        Double cachedScore = motionSearchAnalysis.cachedMotionVectorScore(motionVectorKey);
        if (cachedScore != null) {
            return cachedScore;
        }

        double score = this.scoreMotionVector(pairAnalysis, dx, dy);
        motionSearchAnalysis.cacheMotionVectorScore(motionVectorKey, score);
        return score;
    }

    /** Returns the or compute motion vector hash score used by this AFMA creator instance. */
    protected double getOrComputeMotionVectorHashScore(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                       @NotNull MotionSearchAnalysis motionSearchAnalysis,
                                                       int dx, int dy, int sampleColumns, int sampleRows) {
        long motionVectorKey = packMotionVector(dx, dy);
        Double cachedScore = motionSearchAnalysis.cachedMotionVectorHashScore(sampleColumns, sampleRows, motionVectorKey);
        if (cachedScore != null) {
            return cachedScore;
        }

        double score = this.scoreMotionVectorHash(pairAnalysis, dx, dy, sampleColumns, sampleRows);
        motionSearchAnalysis.cacheMotionVectorHashScore(sampleColumns, sampleRows, motionVectorKey, score);
        return score;
    }

    /** Extracts the motion vectors from the AFMA creator data. */
    @NotNull
    protected List<MotionVector> extractMotionVectors(@NotNull List<ScoredMotionVector> rankedVectors) {
        ArrayList<MotionVector> motionVectors = new ArrayList<>(rankedVectors.size());
        for (ScoredMotionVector rankedVector : rankedVectors) {
            motionVectors.add(rankedVector.vector());
        }
        return List.copyOf(motionVectors);
    }

    /** Collects the axis candidates for the AFMA creator. */
    @NotNull
    protected List<Integer> collectAxisCandidates(@NotNull AfmaFramePairAnalysis pairAnalysis, boolean horizontal, int maxOffset) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        Set<Integer> offsets = new LinkedHashSet<>();
        offsets.add(0);
        if (maxOffset <= 0) {
            return List.copyOf(offsets);
        }
        for (int offset = 1; offset <= Math.min(SMALL_OFFSET_PROBE, maxOffset); offset++) {
            offsets.add(offset);
            offsets.add(-offset);
        }

        if (this.maxCandidateAxisOffsets <= 0) {
            AxisCandidate bestCandidate = null;
            for (int offset = -maxOffset; offset <= maxOffset; offset++) {
                if (offset == 0) {
                    continue;
                }

                double score = this.scoreOffset(pairAnalysis, horizontal, offset);
                if ((score > MIN_CANDIDATE_SCORE) && ((bestCandidate == null) || (score > bestCandidate.score()))) {
                    bestCandidate = new AxisCandidate(offset, score);
                }
            }
            if (bestCandidate != null) {
                this.addAxisCandidateOffsets(offsets, bestCandidate.offset(), maxOffset);
            }
            return List.copyOf(offsets);
        }

        ArrayList<AxisCandidate> rankedCandidates = new ArrayList<>(this.maxCandidateAxisOffsets);
        for (int offset = -maxOffset; offset <= maxOffset; offset++) {
            if (offset == 0) {
                continue;
            }

            this.insertRankedAxisCandidate(
                    rankedCandidates,
                    this.maxCandidateAxisOffsets,
                    offset,
                    this.scoreOffset(pairAnalysis, horizontal, offset)
            );
        }

        for (AxisCandidate candidate : rankedCandidates) {
            this.addAxisCandidateOffsets(offsets, candidate.offset(), maxOffset);
        }

        return List.copyOf(offsets);
    }

    /** Scores the offset for AFMA creator candidate selection. */
    protected double scoreOffset(@NotNull AfmaFramePairAnalysis pairAnalysis, boolean horizontal, int offset) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int width = previous.getWidth();
        int height = previous.getHeight();
        int overlapWidth = horizontal ? width - Math.abs(offset) : width;
        int overlapHeight = horizontal ? height : height - Math.abs(offset);
        if (overlapWidth <= 0 || overlapHeight <= 0) return 0D;

        int srcX = horizontal ? Math.max(0, -offset) : 0;
        int dstX = horizontal ? Math.max(0, offset) : 0;
        int srcY = horizontal ? 0 : Math.max(0, -offset);
        int dstY = horizontal ? 0 : Math.max(0, offset);
        AfmaRect scoringBounds = pairAnalysis.intersectDifferenceBounds(dstX, dstY, overlapWidth, overlapHeight);
        if (scoringBounds == null) {
            return pairAnalysis.isIdentical() ? 1D : 0D;
        }

        int sampleSrcX = srcX + (scoringBounds.x() - dstX);
        int sampleSrcY = srcY + (scoringBounds.y() - dstY);
        int sampleWidth = scoringBounds.width();
        int sampleHeight = scoringBounds.height();

        double fullScore = this.sampleMatchRatio(pairAnalysis, sampleSrcX, sampleSrcY,
                scoringBounds.x(), scoringBounds.y(), sampleWidth, sampleHeight);
        if (horizontal) {
            int bandHeight = Math.max(1, sampleHeight / 2);
            int bandY = sampleSrcY + Math.max(0, (sampleHeight - bandHeight) / 2);
            int dstBandY = scoringBounds.y() + Math.max(0, (sampleHeight - bandHeight) / 2);
            return Math.max(fullScore, this.sampleMatchRatio(pairAnalysis,
                    sampleSrcX, bandY, scoringBounds.x(), dstBandY, sampleWidth, bandHeight));
        }

        int bandWidth = Math.max(1, sampleWidth / 2);
        int bandX = sampleSrcX + Math.max(0, (sampleWidth - bandWidth) / 2);
        int dstBandX = scoringBounds.x() + Math.max(0, (sampleWidth - bandWidth) / 2);
        return Math.max(fullScore, this.sampleMatchRatio(pairAnalysis,
                bandX, sampleSrcY, dstBandX, scoringBounds.y(), bandWidth, sampleHeight));
    }

    /** Calculates the sample match ratio value for the AFMA creator. */
    protected double sampleMatchRatio(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                      int srcX, int srcY, int dstX, int dstY, int sampleWidth, int sampleHeight) {
        return pairAnalysis.sampleMatchRatio(srcX, srcY, dstX, dstY, sampleWidth, sampleHeight);
    }

    /** Scores the motion vector hash for AFMA creator candidate selection. */
    protected double scoreMotionVectorHash(@NotNull AfmaFramePairAnalysis pairAnalysis, int dx, int dy,
                                           int sampleColumns, int sampleRows) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int overlapWidth = previous.getWidth() - Math.abs(dx);
        int overlapHeight = previous.getHeight() - Math.abs(dy);
        if (overlapWidth <= 0 || overlapHeight <= 0) {
            return 0D;
        }

        int srcX = Math.max(0, -dx);
        int srcY = Math.max(0, -dy);
        int dstX = Math.max(0, dx);
        int dstY = Math.max(0, dy);
        AfmaRect scoringBounds = pairAnalysis.intersectDifferenceBounds(dstX, dstY, overlapWidth, overlapHeight);
        if (scoringBounds == null) {
            return pairAnalysis.isIdentical() ? 1D : 0D;
        }

        double hashScore = pairAnalysis.sampleHashMatchRatio(
                srcX + (scoringBounds.x() - dstX),
                srcY + (scoringBounds.y() - dstY),
                scoringBounds.x(),
                scoringBounds.y(),
                scoringBounds.width(),
                scoringBounds.height(),
                sampleColumns,
                sampleRows
        );
        return hashScore - ((Math.abs(dx) + Math.abs(dy)) * 1.0E-6D);
    }

    /** Scores the motion vector for AFMA creator candidate selection. */
    protected double scoreMotionVector(@NotNull AfmaFramePairAnalysis pairAnalysis, int dx, int dy) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int overlapWidth = previous.getWidth() - Math.abs(dx);
        int overlapHeight = previous.getHeight() - Math.abs(dy);
        if (overlapWidth <= 0 || overlapHeight <= 0) {
            return 0D;
        }

        int srcX = Math.max(0, -dx);
        int srcY = Math.max(0, -dy);
        int dstX = Math.max(0, dx);
        int dstY = Math.max(0, dy);
        AfmaRect scoringBounds = pairAnalysis.intersectDifferenceBounds(dstX, dstY, overlapWidth, overlapHeight);
        if (scoringBounds == null) {
            return pairAnalysis.isIdentical() ? 1D : 0D;
        }
        return this.sampleMatchRatio(
                pairAnalysis,
                srcX + (scoringBounds.x() - dstX),
                srcY + (scoringBounds.y() - dstY),
                scoringBounds.x(),
                scoringBounds.y(),
                scoringBounds.width(),
                scoringBounds.height()
        );
    }

    /** Returns whether valid motion vector. */
    protected boolean isValidMotionVector(int dx, int dy, int width, int height) {
        return (width - Math.abs(dx)) > 0
                && (height - Math.abs(dy)) > 0;
    }

    /** Returns the expand motion vector neighborhood produced by the AFMA creator. */
    @NotNull
    protected List<MotionVector> expandMotionVectorNeighborhood(@NotNull List<MotionVector> motionVectors,
                                                                int width, int height, int maxSeeds, int radius) {
        if (motionVectors.isEmpty() || (maxSeeds <= 0) || (radius <= 0)) {
            return motionVectors;
        }

        int maxDx = Math.min(width - 1, this.maxSearchDistance);
        int maxDy = Math.min(height - 1, this.maxSearchDistance);
        LinkedHashSet<Long> motionVectorKeys = new LinkedHashSet<>(motionVectors.size() * ((radius * 2) + 1));
        ArrayList<MotionVector> expandedVectors = new ArrayList<>(motionVectors.size());
        for (MotionVector motionVector : motionVectors) {
            this.addExpandedMotionVector(expandedVectors, motionVectorKeys, motionVector.dx(), motionVector.dy(), width, height, maxDx, maxDy);
        }

        int seedCount = Math.min(maxSeeds, motionVectors.size());
        for (int seedIndex = 0; seedIndex < seedCount; seedIndex++) {
            MotionVector seed = motionVectors.get(seedIndex);
            for (int candidateDy = seed.dy() - radius; candidateDy <= seed.dy() + radius; candidateDy++) {
                for (int candidateDx = seed.dx() - radius; candidateDx <= seed.dx() + radius; candidateDx++) {
                    this.addExpandedMotionVector(
                            expandedVectors,
                            motionVectorKeys,
                            candidateDx,
                            candidateDy,
                            width,
                            height,
                            maxDx,
                            maxDy
                    );
                }
            }
        }
        return List.copyOf(expandedVectors);
    }

    /** Registers the expanded motion vector with this AFMA creator component. */
    protected void addExpandedMotionVector(@NotNull List<MotionVector> expandedVectors, @NotNull Set<Long> motionVectorKeys,
                                           int dx, int dy, int width, int height, int maxDx, int maxDy) {
        if ((dx == 0) && (dy == 0)) {
            return;
        }
        if ((Math.abs(dx) > maxDx) || (Math.abs(dy) > maxDy) || !this.isValidMotionVector(dx, dy, width, height)) {
            return;
        }
        long motionVectorKey = packMotionVector(dx, dy);
        if (motionVectorKeys.add(motionVectorKey)) {
            expandedVectors.add(new MotionVector(dx, dy));
        }
    }

    /** Resolves the best multi copy rect for the AFMA creator. */
    @Nullable
    protected CopyRectCandidate findBestMultiCopyRect(@Nullable AfmaFramePairAnalysis pairAnalysis,
                                                      @NotNull int[] predictedPixels, int width, int height, @NotNull AfmaPixelFrame next,
                                                      @NotNull MultiCopyScratchWorkspace multiCopyScratchWorkspace) {
        List<MotionVector> motionVectors;
        if (pairAnalysis != null) {
            motionVectors = this.collectTopMotionVectors(pairAnalysis, MAX_MULTI_COPY_MOTION_VECTORS);
        } else {
            AfmaPixelFrame predictedFrame = new AfmaPixelFrame(width, height, predictedPixels);
            motionVectors = this.collectTopMotionVectors(new AfmaFramePairAnalysis(predictedFrame, next), MAX_MULTI_COPY_MOTION_VECTORS);
        }
        if (motionVectors.isEmpty()) {
            return null;
        }

        int[] nextPixels = next.getPixelsUnsafe();
        CopyRectCandidate bestCandidate = null;
        for (int motionIndex = 0; motionIndex < motionVectors.size(); motionIndex++) {
            MotionVector motionVector = motionVectors.get(motionIndex);
            CopyRectCandidate candidate = this.findBestMultiCopyRectForMotion(
                    predictedPixels,
                    nextPixels,
                    width,
                    height,
                    motionVector.dx(),
                    motionVector.dy(),
                    multiCopyScratchWorkspace
            );
            if ((candidate != null) && ((bestCandidate == null) || candidate.isBetterThan(bestCandidate))) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    /** Resolves the best multi copy rect for motion for the AFMA creator. */
    @Nullable
    protected CopyRectCandidate findBestMultiCopyRectForMotion(@NotNull int[] predictedPixels, @NotNull int[] nextPixels,
                                                               int width, int height, int dx, int dy,
                                                               @NotNull MultiCopyScratchWorkspace multiCopyScratchWorkspace) {
        int overlapWidth = width - Math.abs(dx);
        int overlapHeight = height - Math.abs(dy);
        if ((overlapWidth <= 0) || (overlapHeight <= 0)) {
            return null;
        }

        int srcBaseX = Math.max(0, -dx);
        int srcBaseY = Math.max(0, -dy);
        int dstBaseX = Math.max(0, dx);
        int dstBaseY = Math.max(0, dy);
        int[] prevStartX = multiCopyScratchWorkspace.runStartXBufferA();
        int[] prevEndX = multiCopyScratchWorkspace.runEndXBufferA();
        int[] prevStartY = multiCopyScratchWorkspace.runStartYBufferA();
        int[] prevHeight = multiCopyScratchWorkspace.runHeightBufferA();
        long[] prevDirty = multiCopyScratchWorkspace.runDirtyBufferA();
        int prevCount = 0;
        int[] currStartX = multiCopyScratchWorkspace.runStartXBufferB();
        int[] currEndX = multiCopyScratchWorkspace.runEndXBufferB();
        int[] currStartY = multiCopyScratchWorkspace.runStartYBufferB();
        int[] currHeight = multiCopyScratchWorkspace.runHeightBufferB();
        long[] currDirty = multiCopyScratchWorkspace.runDirtyBufferB();
        CopyRectCandidate bestCandidate = null;

        for (int localY = 0; localY < overlapHeight; localY++) {
            int srcRowOffset = ((srcBaseY + localY) * width) + srcBaseX;
            int dstRowOffset = ((dstBaseY + localY) * width) + dstBaseX;
            int currCount = 0;
            int runStartX = -1;
            int runDirtyPixels = 0;

            for (int localX = 0; localX < overlapWidth; localX++) {
                boolean motionMatch = predictedPixels[srcRowOffset + localX] == nextPixels[dstRowOffset + localX];
                if (!motionMatch) {
                    if (runStartX >= 0) {
                        currStartX[currCount] = runStartX;
                        currEndX[currCount] = localX;
                        currStartY[currCount] = localY;
                        currHeight[currCount] = 1;
                        currDirty[currCount] = runDirtyPixels;
                        currCount++;
                        runStartX = -1;
                        runDirtyPixels = 0;
                    }
                    continue;
                }

                if (runStartX < 0) {
                    runStartX = localX;
                    runDirtyPixels = 0;
                }
                if (predictedPixels[dstRowOffset + localX] != nextPixels[dstRowOffset + localX]) {
                    runDirtyPixels++;
                }
            }
            if (runStartX >= 0) {
                currStartX[currCount] = runStartX;
                currEndX[currCount] = overlapWidth;
                currStartY[currCount] = localY;
                currHeight[currCount] = 1;
                currDirty[currCount] = runDirtyPixels;
                currCount++;
            }

            for (int previousIndex = 0; previousIndex < prevCount; previousIndex++) {
                boolean matched = false;
                for (int currentIndex = 0; currentIndex < currCount; currentIndex++) {
                    if ((prevStartX[previousIndex] == currStartX[currentIndex])
                            && (prevEndX[previousIndex] == currEndX[currentIndex])) {
                        currStartY[currentIndex] = prevStartY[previousIndex];
                        currHeight[currentIndex] = prevHeight[previousIndex] + 1;
                        currDirty[currentIndex] = prevDirty[previousIndex] + currDirty[currentIndex];
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    bestCandidate = this.selectBetterMultiCopyCandidate(
                            bestCandidate,
                            prevStartX[previousIndex],
                            prevStartY[previousIndex],
                            prevEndX[previousIndex] - prevStartX[previousIndex],
                            prevHeight[previousIndex],
                            prevDirty[previousIndex],
                            srcBaseX,
                            srcBaseY,
                            dstBaseX,
                            dstBaseY,
                            dx,
                            dy
                    );
                }
            }

            int[] tempStartX = prevStartX;
            prevStartX = currStartX;
            currStartX = tempStartX;
            int[] tempEndX = prevEndX;
            prevEndX = currEndX;
            currEndX = tempEndX;
            int[] tempStartY = prevStartY;
            prevStartY = currStartY;
            currStartY = tempStartY;
            int[] tempHeight = prevHeight;
            prevHeight = currHeight;
            currHeight = tempHeight;
            long[] tempDirty = prevDirty;
            prevDirty = currDirty;
            currDirty = tempDirty;
            prevCount = currCount;
        }

        for (int previousIndex = 0; previousIndex < prevCount; previousIndex++) {
            bestCandidate = this.selectBetterMultiCopyCandidate(
                    bestCandidate,
                    prevStartX[previousIndex],
                    prevStartY[previousIndex],
                    prevEndX[previousIndex] - prevStartX[previousIndex],
                    prevHeight[previousIndex],
                    prevDirty[previousIndex],
                    srcBaseX,
                    srcBaseY,
                    dstBaseX,
                    dstBaseY,
                    dx,
                    dy
            );
        }
        return bestCandidate;
    }

    /** Inserts the ranked motion vector into the ordered AFMA creator state. */
    protected void insertRankedMotionVector(@NotNull List<ScoredMotionVector> rankedVectors, int maxVectors,
                                            int dx, int dy, double score) {
        int insertIndex = rankedVectors.size();
        while ((insertIndex > 0) && (score > rankedVectors.get(insertIndex - 1).score())) {
            insertIndex--;
        }
        if ((insertIndex >= maxVectors) && (rankedVectors.size() >= maxVectors)) {
            return;
        }

        rankedVectors.add(insertIndex, new ScoredMotionVector(new MotionVector(dx, dy), score));
        if (rankedVectors.size() > maxVectors) {
            rankedVectors.remove(rankedVectors.size() - 1);
        }
    }

    /** Inserts the ranked axis candidate into the ordered AFMA creator state. */
    protected void insertRankedAxisCandidate(@NotNull List<AxisCandidate> rankedCandidates, int maxCandidates,
                                             int offset, double score) {
        int insertIndex = rankedCandidates.size();
        while ((insertIndex > 0) && (score > rankedCandidates.get(insertIndex - 1).score())) {
            insertIndex--;
        }
        if ((insertIndex >= maxCandidates) && (rankedCandidates.size() >= maxCandidates)) {
            return;
        }

        rankedCandidates.add(insertIndex, new AxisCandidate(offset, score));
        if (rankedCandidates.size() > maxCandidates) {
            rankedCandidates.remove(rankedCandidates.size() - 1);
        }
    }

    /** Registers the axis candidate offsets with this AFMA creator component. */
    protected void addAxisCandidateOffsets(@NotNull Set<Integer> offsets, int offset, int maxOffset) {
        offsets.add(offset);
        for (int neighborOffset = 1; neighborOffset <= NEIGHBOR_OFFSET_RADIUS; neighborOffset++) {
            int negativeNeighbor = offset - neighborOffset;
            if (negativeNeighbor >= -maxOffset) {
                offsets.add(negativeNeighbor);
            }
            int positiveNeighbor = offset + neighborOffset;
            if (positiveNeighbor <= maxOffset) {
                offsets.add(positiveNeighbor);
            }
        }
    }

    /** Packs the motion vector into the AFMA creator representation. */
    protected static long packMotionVector(int dx, int dy) {
        return (((long) dx) << 32) ^ (dy & 0xFFFFFFFFL);
    }

    /** Selects the better multi copy candidate for the AFMA creator. */
    @Nullable
    protected CopyRectCandidate selectBetterMultiCopyCandidate(@Nullable CopyRectCandidate bestCandidate,
                                                               int startX, int startY, int width, int height, long dirtyPixels,
                                                               int srcBaseX, int srcBaseY, int dstBaseX, int dstBaseY,
                                                               int dx, int dy) {
        long rectArea = (long) width * height;
        if ((rectArea < MIN_MULTI_COPY_RECT_AREA) || (dirtyPixels < MIN_MULTI_COPY_DIRTY_PIXELS)) {
            return bestCandidate;
        }
        if (((double) dirtyPixels / (double) rectArea) < MIN_MULTI_COPY_DIRTY_DENSITY) {
            return bestCandidate;
        }

        AfmaCopyRect copyRect = new AfmaCopyRect(
                srcBaseX + startX,
                srcBaseY + startY,
                dstBaseX + startX,
                dstBaseY + startY,
                width,
                height
        );
        CopyRectCandidate candidate = new CopyRectCandidate(copyRect, dirtyPixels, rectArea, dx, dy);
        if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
            return candidate;
        }
        return bestCandidate;
    }

    /** Reports {@code MotionSearchAnalysis} measurements produced by the AFMA creator. */
    protected static final class MotionSearchAnalysis {

        @Nullable
        private List<Integer> candidateDx;
        @Nullable
        private List<Integer> candidateDy;
        @Nullable
        private List<MotionVector> candidateMotionVectors;
        @Nullable
        private List<MotionVector> rankedMotionVectors;
        @Nullable
        private List<MotionVector> rankedMotionVectorsWithoutZero;
        @Nullable
        private List<MotionVector> topRankedMotionVectorsWithoutZero;
        private boolean topRankedMotionVectorsWithoutZeroComplete;
        private int topRankedMotionVectorsWithoutZeroBudget;
        @Nullable
        private Map<Long, Double> motionVectorScoresByKey;
        @Nullable
        private Map<HashSamplingKey, Map<Long, Double>> motionVectorHashScoresBySampling;

        /** Initializes a new {@code MotionSearchAnalysis} for AFMA creator use. */
        protected MotionSearchAnalysis() {
        }

        /** Returns whether axis candidates. */
        public boolean hasAxisCandidates() {
            return (this.candidateDx != null) && (this.candidateDy != null);
        }

        /** Updates the cache axis candidates state in the AFMA creator. */
        public void cacheAxisCandidates(@NotNull List<Integer> candidateDx, @NotNull List<Integer> candidateDy) {
            this.candidateDx = List.copyOf(candidateDx);
            this.candidateDy = List.copyOf(candidateDy);
        }

        /** Returns the cached horizontal candidate offsets, failing if collection has not run. */
        @NotNull
        public List<Integer> candidateDx() {
            if (this.candidateDx == null) {
                throw new IllegalStateException("AFMA horizontal motion offsets have not been collected yet");
            }
            return this.candidateDx;
        }

        /** Returns the cached vertical candidate offsets, failing if collection has not run. */
        @NotNull
        public List<Integer> candidateDy() {
            if (this.candidateDy == null) {
                throw new IllegalStateException("AFMA vertical motion offsets have not been collected yet");
            }
            return this.candidateDy;
        }

        /** Returns cached motion-vector candidates, or {@code null} before they are computed. */
        public @Nullable List<MotionVector> candidateMotionVectors() {
            return this.candidateMotionVectors;
        }

        /** Updates the cache candidate motion vectors state in the AFMA creator. */
        public void cacheCandidateMotionVectors(@NotNull List<MotionVector> candidateMotionVectors) {
            this.candidateMotionVectors = candidateMotionVectors;
        }

        /** Returns whether ranked motion vectors. */
        public boolean hasRankedMotionVectors() {
            return (this.rankedMotionVectors != null) && (this.rankedMotionVectorsWithoutZero != null);
        }

        /** Updates the cache ranked motion vectors state in the AFMA creator. */
        public void cacheRankedMotionVectors(@NotNull List<MotionVector> rankedMotionVectors,
                                             @NotNull List<MotionVector> rankedMotionVectorsWithoutZero) {
            this.rankedMotionVectors = rankedMotionVectors;
            this.rankedMotionVectorsWithoutZero = rankedMotionVectorsWithoutZero;
            this.topRankedMotionVectorsWithoutZero = rankedMotionVectorsWithoutZero;
            this.topRankedMotionVectorsWithoutZeroComplete = true;
            this.topRankedMotionVectorsWithoutZeroBudget = rankedMotionVectorsWithoutZero.size();
        }

        /** Returns whether top ranked motion vectors without zero. */
        public boolean hasTopRankedMotionVectorsWithoutZero(int requestedCount) {
            return (this.topRankedMotionVectorsWithoutZero != null)
                    && (this.topRankedMotionVectorsWithoutZeroComplete
                    || (this.topRankedMotionVectorsWithoutZeroBudget >= requestedCount));
        }

        /** Updates the cache top ranked motion vectors without zero state in the AFMA creator. */
        public void cacheTopRankedMotionVectorsWithoutZero(@NotNull List<MotionVector> topRankedMotionVectorsWithoutZero,
                                                           boolean complete, int budget) {
            this.topRankedMotionVectorsWithoutZero = topRankedMotionVectorsWithoutZero;
            this.topRankedMotionVectorsWithoutZeroComplete = complete;
            this.topRankedMotionVectorsWithoutZeroBudget = Math.max(topRankedMotionVectorsWithoutZero.size(), budget);
        }

        /** Ranks the ranked motion vectors for the AFMA creator. */
        @NotNull
        public List<MotionVector> rankedMotionVectors() {
            if (this.rankedMotionVectors == null) {
                throw new IllegalStateException("AFMA motion vectors have not been ranked yet");
            }
            return this.rankedMotionVectors;
        }

        /** Ranks the ranked motion vectors without zero for the AFMA creator. */
        @NotNull
        public List<MotionVector> rankedMotionVectorsWithoutZero() {
            if (this.rankedMotionVectorsWithoutZero == null) {
                throw new IllegalStateException("AFMA non-zero motion vectors have not been ranked yet");
            }
            return this.rankedMotionVectorsWithoutZero;
        }

        /** Returns the top ranked motion vectors without zero produced by the AFMA creator. */
        @NotNull
        public List<MotionVector> topRankedMotionVectorsWithoutZero(int maxCount) {
            if (this.topRankedMotionVectorsWithoutZero == null) {
                throw new IllegalStateException("AFMA top motion vectors have not been ranked yet");
            }
            if (this.topRankedMotionVectorsWithoutZero.size() <= maxCount) {
                if (!this.topRankedMotionVectorsWithoutZeroComplete
                        && (this.topRankedMotionVectorsWithoutZeroBudget < maxCount)) {
                    throw new IllegalStateException("AFMA top motion vectors have not been ranked deeply enough");
                }
                return this.topRankedMotionVectorsWithoutZero;
            }
            return this.topRankedMotionVectorsWithoutZero.subList(0, maxCount);
        }

        /** Returns the cached motion vector score produced by the AFMA creator. */
        public @Nullable Double cachedMotionVectorScore(long motionVectorKey) {
            return (this.motionVectorScoresByKey != null) ? this.motionVectorScoresByKey.get(motionVectorKey) : null;
        }

        /** Updates the cache motion vector score state in the AFMA creator. */
        public void cacheMotionVectorScore(long motionVectorKey, double score) {
            if (this.motionVectorScoresByKey == null) {
                this.motionVectorScoresByKey = new HashMap<>();
            }
            this.motionVectorScoresByKey.put(motionVectorKey, score);
        }

        /** Returns the cached motion vector hash score produced by the AFMA creator. */
        public @Nullable Double cachedMotionVectorHashScore(int sampleColumns, int sampleRows, long motionVectorKey) {
            if (this.motionVectorHashScoresBySampling == null) {
                return null;
            }
            Map<Long, Double> scoresByVector = this.motionVectorHashScoresBySampling.get(new HashSamplingKey(sampleColumns, sampleRows));
            return (scoresByVector != null) ? scoresByVector.get(motionVectorKey) : null;
        }

        /** Updates the cache motion vector hash score state in the AFMA creator. */
        public void cacheMotionVectorHashScore(int sampleColumns, int sampleRows, long motionVectorKey, double score) {
            if (this.motionVectorHashScoresBySampling == null) {
                this.motionVectorHashScoresBySampling = new HashMap<>();
            }
            this.motionVectorHashScoresBySampling
                    .computeIfAbsent(new HashSamplingKey(sampleColumns, sampleRows), ignored -> new HashMap<>())
                    .put(motionVectorKey, score);
        }

    }

    /** Provides a value-based {@code HashSamplingKey} cache key for the AFMA creator. */
    protected record HashSamplingKey(int sampleColumns, int sampleRows) {
    }

    /** Carries {@code AxisCandidate} data between validated stages of the AFMA creator. */
    protected record AxisCandidate(int offset, double score) {
    }

    /** Models {@code ScoredMotionVector} state used by the AFMA creator. */
    protected record ScoredMotionVector(@NotNull MotionVector vector, double score) {
    }

    /** Retains or reuses {@code MultiCopyScratchWorkspace} state while the AFMA creator is active. */
    protected record MultiCopyScratchWorkspace(@NotNull int[] runStartXBufferA, @NotNull int[] runEndXBufferA,
                                               @NotNull int[] runStartYBufferA, @NotNull int[] runHeightBufferA,
                                               @NotNull long[] runDirtyBufferA, @NotNull int[] runStartXBufferB,
                                               @NotNull int[] runEndXBufferB, @NotNull int[] runStartYBufferB,
                                               @NotNull int[] runHeightBufferB, @NotNull long[] runDirtyBufferB) {

        /** Initializes a new {@code MultiCopyScratchWorkspace} for AFMA creator use. */
        protected MultiCopyScratchWorkspace(int maxRuns) {
            this(
                    new int[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new long[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new long[maxRuns]
            );
        }

    }

    /** Models {@code Detection} state used by the AFMA creator. */
    public record Detection(@NotNull AfmaCopyRect copyRect, @Nullable AfmaRect patchBounds, long usefulness,
                            int remainingDirtyPixelCount) {
        /** Calculates the patch area value for the AFMA creator. */
        public long patchArea() {
            return (this.patchBounds != null) ? this.patchBounds.area() : 0L;
        }
    }

    /** Models {@code MotionVector} state used by the AFMA creator. */
    public record MotionVector(int dx, int dy) {
    }

    /** Models {@code MultiDetection} state used by the AFMA creator. */
    public record MultiDetection(@NotNull AfmaMultiCopy multiCopy, @Nullable AfmaRect patchBounds, long usefulness,
                                 int remainingDirtyPixelCount) {
        /** Calculates the patch area value for the AFMA creator. */
        public long patchArea() {
            return (this.patchBounds != null) ? this.patchBounds.area() : 0L;
        }
    }

    /** Carries {@code CopyRectCandidate} data between validated stages of the AFMA creator. */
    protected record CopyRectCandidate(@NotNull AfmaCopyRect copyRect, long dirtyPixels, long area, int dx, int dy) {
        /** Returns whether better than. */
        public boolean isBetterThan(@NotNull CopyRectCandidate other) {
            if (this.dirtyPixels != other.dirtyPixels) {
                return this.dirtyPixels > other.dirtyPixels;
            }
            if (this.area != other.area) {
                return this.area > other.area;
            }

            int motionMagnitude = Math.abs(this.dx) + Math.abs(this.dy);
            int otherMotionMagnitude = Math.abs(other.dx) + Math.abs(other.dy);
            if (motionMagnitude != otherMotionMagnitude) {
                return motionMagnitude < otherMotionMagnitude;
            }

            if (this.copyRect.getDstY() != other.copyRect.getDstY()) {
                return this.copyRect.getDstY() < other.copyRect.getDstY();
            }
            return this.copyRect.getDstX() < other.copyRect.getDstX();
        }
    }

}
