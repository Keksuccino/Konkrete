package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Models {@code AfmaTileMotionSearch} state used by the AFMA creator. */
public final class AfmaTileMotionSearch {

    /** Search or sampling budget for coarse hash columns in the AFMA creator. */
    protected static final int COARSE_HASH_COLUMNS_KONKRETE = 6;
    /** Search or sampling budget for coarse hash rows in the AFMA creator. */
    protected static final int COARSE_HASH_ROWS_KONKRETE = 4;
    /** Search or sampling budget for fine hash columns in the AFMA creator. */
    protected static final int FINE_HASH_COLUMNS_KONKRETE = 12;
    /** Search or sampling budget for fine hash rows in the AFMA creator. */
    protected static final int FINE_HASH_ROWS_KONKRETE = 8;
    /** Search or sampling budget for approx sad columns in the AFMA creator. */
    protected static final int APPROX_SAD_COLUMNS_KONKRETE = 12;
    /** Search or sampling budget for approx sad rows in the AFMA creator. */
    protected static final int APPROX_SAD_ROWS_KONKRETE = 8;
    /** Scoring or hashing coefficient for hash frontier multiplier in the AFMA creator. */
    protected static final int HASH_FRONTIER_MULTIPLIER_KONKRETE = 6;
    /** Scoring or hashing coefficient for approx sad frontier multiplier in the AFMA creator. */
    protected static final int APPROX_SAD_FRONTIER_MULTIPLIER_KONKRETE = 3;
    /** Upper bound for refinement seeds enforced by the AFMA creator. */
    protected static final int MAX_REFINEMENT_SEEDS_KONKRETE = 4;
    /** Search or sampling budget for neighbor refinement radius in the AFMA creator. */
    protected static final int NEIGHBOR_REFINEMENT_RADIUS_KONKRETE = 2;
    /** Search or sampling budget for initial grid target samples per axis in the AFMA creator. */
    protected static final int INITIAL_GRID_TARGET_SAMPLES_PER_AXIS_KONKRETE = 8;

    private AfmaTileMotionSearch() {
    }

    /** Collects the frame candidates for the AFMA creator. */
    @NotNull
    public static List<MotionCandidate> collectFrameCandidates(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                               int maxSearchDistance, int maxCandidates) {
        Objects.requireNonNull(pairAnalysis);
        AfmaRect dirtyBounds = pairAnalysis.differenceBounds();
        if ((dirtyBounds == null) || (maxSearchDistance <= 0) || (maxCandidates <= 0)) {
            return List.of();
        }

        return collectRegionCandidates(pairAnalysis,
                dirtyBounds.x(),
                dirtyBounds.y(),
                dirtyBounds.width(),
                dirtyBounds.height(),
                maxSearchDistance,
                maxCandidates,
                List.of());
    }

    /** Collects the region candidates for the AFMA creator. */
    @NotNull
    public static List<MotionCandidate> collectRegionCandidates(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                int dstX, int dstY, int width, int height,
                                                                int maxSearchDistance, int maxCandidates) {
        return collectRegionCandidates(pairAnalysis, dstX, dstY, width, height, maxSearchDistance, maxCandidates, List.of());
    }

    /** Collects the region candidates for the AFMA creator. */
    @NotNull
    public static List<MotionCandidate> collectRegionCandidates(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                int dstX, int dstY, int width, int height,
                                                                int maxSearchDistance, int maxCandidates,
                                                                @NotNull List<AfmaRectCopyDetector.MotionVector> seedVectors) {
        Objects.requireNonNull(pairAnalysis);
        Objects.requireNonNull(seedVectors);
        if ((maxSearchDistance <= 0) || (maxCandidates <= 0)
                || !AfmaPixelFrameHelper.isRegionInBounds(pairAnalysis.nextFrame(), dstX, dstY, width, height)) {
            return List.of();
        }

        int initialStep = computeInitialSearchStep(maxSearchDistance);
        int hashFrontierLimit = Math.max(maxCandidates, maxCandidates * HASH_FRONTIER_MULTIPLIER_KONKRETE);
        ArrayList<MotionCandidate> hashFrontier = new ArrayList<>(Math.min(hashFrontierLimit, 16));

        for (AfmaRectCopyDetector.MotionVector seedVector : seedVectors) {
            scoreAndInsertByHash(hashFrontier, hashFrontierLimit, pairAnalysis,
                    dstX, dstY, width, height, seedVector.dx(), seedVector.dy(),
                    FINE_HASH_COLUMNS_KONKRETE, FINE_HASH_ROWS_KONKRETE);
        }

        scoreInitialGrid(hashFrontier, hashFrontierLimit, pairAnalysis,
                dstX, dstY, width, height, maxSearchDistance, initialStep);

        if (hashFrontier.isEmpty()) {
            return List.of();
        }

        for (int step = Math.max(1, initialStep / 2); step >= 1; step /= 2) {
            hashFrontier = refineHashFrontier(hashFrontier, hashFrontierLimit, pairAnalysis,
                    dstX, dstY, width, height, step);
            if (step == 1) {
                break;
            }
        }

        return shortlistByApproxSad(hashFrontier, pairAnalysis.previousFrame(), pairAnalysis.nextFrame(),
                dstX, dstY, width, height, maxCandidates);
    }

    /** Scores the initial grid for AFMA creator candidate selection. */
    protected static void scoreInitialGrid(@NotNull List<MotionCandidate> hashFrontier, int hashFrontierLimit,
                                           @NotNull AfmaFramePairAnalysis pairAnalysis,
                                           int dstX, int dstY, int width, int height,
                                           int maxSearchDistance, int initialStep) {
        for (int candidateDy = -maxSearchDistance; candidateDy <= maxSearchDistance; candidateDy += initialStep) {
            for (int candidateDx = -maxSearchDistance; candidateDx <= maxSearchDistance; candidateDx += initialStep) {
                if ((candidateDx == 0) && (candidateDy == 0)) {
                    continue;
                }
                scoreAndInsertByHash(hashFrontier, hashFrontierLimit, pairAnalysis,
                        dstX, dstY, width, height, candidateDx, candidateDy,
                        COARSE_HASH_COLUMNS_KONKRETE, COARSE_HASH_ROWS_KONKRETE);
            }
        }
    }

    /** Refines the hash frontier for the AFMA creator. */
    @NotNull
    protected static ArrayList<MotionCandidate> refineHashFrontier(@NotNull List<MotionCandidate> hashFrontier,
                                                                   int hashFrontierLimit,
                                                                   @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                   int dstX, int dstY, int width, int height,
                                                                   int step) {
        ArrayList<MotionCandidate> refinedFrontier = new ArrayList<>(hashFrontier.size());
        for (MotionCandidate candidate : hashFrontier) {
            insertRankedByHash(refinedFrontier, hashFrontierLimit, candidate);
        }

        int seedCount = Math.min(MAX_REFINEMENT_SEEDS_KONKRETE, hashFrontier.size());
        for (int seedIndex = 0; seedIndex < seedCount; seedIndex++) {
            MotionCandidate seedCandidate = hashFrontier.get(seedIndex);
            int centerDx = seedCandidate.vector().dx();
            int centerDy = seedCandidate.vector().dy();
            for (int candidateDy = centerDy - (step * NEIGHBOR_REFINEMENT_RADIUS_KONKRETE);
                 candidateDy <= centerDy + (step * NEIGHBOR_REFINEMENT_RADIUS_KONKRETE);
                 candidateDy += step) {
                for (int candidateDx = centerDx - (step * NEIGHBOR_REFINEMENT_RADIUS_KONKRETE);
                     candidateDx <= centerDx + (step * NEIGHBOR_REFINEMENT_RADIUS_KONKRETE);
                     candidateDx += step) {
                    if ((candidateDx == centerDx) && (candidateDy == centerDy)) {
                        continue;
                    }
                    scoreAndInsertByHash(refinedFrontier, hashFrontierLimit, pairAnalysis,
                            dstX, dstY, width, height, candidateDx, candidateDy,
                            FINE_HASH_COLUMNS_KONKRETE, FINE_HASH_ROWS_KONKRETE);
                }
            }
        }
        return refinedFrontier;
    }

    /** Selects the by approx sad for the AFMA creator. */
    @NotNull
    protected static List<MotionCandidate> shortlistByApproxSad(@NotNull List<MotionCandidate> hashFrontier,
                                                                @NotNull AfmaPixelFrame previousFrame,
                                                                @NotNull AfmaPixelFrame nextFrame,
                                                                int dstX, int dstY, int width, int height,
                                                                int maxCandidates) {
        int shortlistLimit = Math.min(
                hashFrontier.size(),
                Math.max(maxCandidates, maxCandidates * APPROX_SAD_FRONTIER_MULTIPLIER_KONKRETE)
        );
        ArrayList<MotionCandidate> rankedCandidates = new ArrayList<>(Math.min(shortlistLimit, hashFrontier.size()));
        for (MotionCandidate candidate : hashFrontier) {
            long sadCutoff = (rankedCandidates.size() >= shortlistLimit)
                    ? rankedCandidates.get(rankedCandidates.size() - 1).approximateSad()
                    : Long.MAX_VALUE;
            long approximateSad = computeApproxSad(
                    previousFrame,
                    nextFrame,
                    dstX,
                    dstY,
                    dstX + candidate.vector().dx(),
                    dstY + candidate.vector().dy(),
                    width,
                    height,
                    sadCutoff
            );
            if (approximateSad == Long.MAX_VALUE) {
                continue;
            }

            insertRankedBySad(rankedCandidates, shortlistLimit,
                    new MotionCandidate(candidate.vector(), candidate.hashScore(), approximateSad));
        }

        if (rankedCandidates.size() <= maxCandidates) {
            return List.copyOf(rankedCandidates);
        }
        return List.copyOf(rankedCandidates.subList(0, maxCandidates));
    }

    /** Scores one hashed motion candidate and inserts it into the bounded ranking. */
    protected static void scoreAndInsertByHash(@NotNull List<MotionCandidate> rankedCandidates, int maxCandidates,
                                               @NotNull AfmaFramePairAnalysis pairAnalysis,
                                               int dstX, int dstY, int width, int height,
                                               int dx, int dy, int sampleColumns, int sampleRows) {
        if ((dx == 0) && (dy == 0)) {
            return;
        }

        double hashScore = scoreRegionHash(pairAnalysis, dstX, dstY, width, height, dx, dy, sampleColumns, sampleRows);
        if (hashScore <= 0D) {
            return;
        }
        insertRankedByHash(rankedCandidates, maxCandidates,
                new MotionCandidate(new AfmaRectCopyDetector.MotionVector(dx, dy), hashScore, Long.MAX_VALUE));
    }

    /** Inserts the ranked by hash into the ordered AFMA creator state. */
    protected static void insertRankedByHash(@NotNull List<MotionCandidate> rankedCandidates, int maxCandidates,
                                             @NotNull MotionCandidate candidate) {
        int existingIndex = findCandidateIndex(rankedCandidates, candidate.vector().dx(), candidate.vector().dy());
        if (existingIndex >= 0) {
            MotionCandidate existingCandidate = rankedCandidates.get(existingIndex);
            if (compareHashCandidate(candidate, existingCandidate) >= 0) {
                return;
            }
            rankedCandidates.remove(existingIndex);
        }

        int insertIndex = rankedCandidates.size();
        while ((insertIndex > 0) && (compareHashCandidate(candidate, rankedCandidates.get(insertIndex - 1)) < 0)) {
            insertIndex--;
        }
        if ((insertIndex >= maxCandidates) && (rankedCandidates.size() >= maxCandidates)) {
            return;
        }

        rankedCandidates.add(insertIndex, candidate);
        if (rankedCandidates.size() > maxCandidates) {
            rankedCandidates.remove(rankedCandidates.size() - 1);
        }
    }

    /** Inserts the ranked by sad into the ordered AFMA creator state. */
    protected static void insertRankedBySad(@NotNull List<MotionCandidate> rankedCandidates, int maxCandidates,
                                            @NotNull MotionCandidate candidate) {
        int existingIndex = findCandidateIndex(rankedCandidates, candidate.vector().dx(), candidate.vector().dy());
        if (existingIndex >= 0) {
            MotionCandidate existingCandidate = rankedCandidates.get(existingIndex);
            if (compareSadCandidate(candidate, existingCandidate) >= 0) {
                return;
            }
            rankedCandidates.remove(existingIndex);
        }

        int insertIndex = rankedCandidates.size();
        while ((insertIndex > 0) && (compareSadCandidate(candidate, rankedCandidates.get(insertIndex - 1)) < 0)) {
            insertIndex--;
        }
        if ((insertIndex >= maxCandidates) && (rankedCandidates.size() >= maxCandidates)) {
            return;
        }

        rankedCandidates.add(insertIndex, candidate);
        if (rankedCandidates.size() > maxCandidates) {
            rankedCandidates.remove(rankedCandidates.size() - 1);
        }
    }

    /** Compares the hash candidate values for the AFMA creator. */
    protected static int compareHashCandidate(@NotNull MotionCandidate first, @NotNull MotionCandidate second) {
        int hashCompare = Double.compare(second.hashScore(), first.hashScore());
        if (hashCompare != 0) {
            return hashCompare;
        }
        return compareMotionVectors(first.vector(), second.vector());
    }

    /** Compares the sad candidate values for the AFMA creator. */
    protected static int compareSadCandidate(@NotNull MotionCandidate first, @NotNull MotionCandidate second) {
        int sadCompare = Long.compare(first.approximateSad(), second.approximateSad());
        if (sadCompare != 0) {
            return sadCompare;
        }
        int hashCompare = Double.compare(second.hashScore(), first.hashScore());
        if (hashCompare != 0) {
            return hashCompare;
        }
        return compareMotionVectors(first.vector(), second.vector());
    }

    /** Compares the motion vectors values for the AFMA creator. */
    protected static int compareMotionVectors(@NotNull AfmaRectCopyDetector.MotionVector first,
                                              @NotNull AfmaRectCopyDetector.MotionVector second) {
        int firstMagnitude = Math.abs(first.dx()) + Math.abs(first.dy());
        int secondMagnitude = Math.abs(second.dx()) + Math.abs(second.dy());
        if (firstMagnitude != secondMagnitude) {
            return Integer.compare(firstMagnitude, secondMagnitude);
        }
        int dyCompare = Integer.compare(first.dy(), second.dy());
        if (dyCompare != 0) {
            return dyCompare;
        }
        return Integer.compare(first.dx(), second.dx());
    }

    /** Resolves the candidate index for the AFMA creator. */
    protected static int findCandidateIndex(@NotNull List<MotionCandidate> rankedCandidates, int dx, int dy) {
        for (int candidateIndex = 0; candidateIndex < rankedCandidates.size(); candidateIndex++) {
            MotionCandidate rankedCandidate = rankedCandidates.get(candidateIndex);
            if ((rankedCandidate.vector().dx() == dx) && (rankedCandidate.vector().dy() == dy)) {
                return candidateIndex;
            }
        }
        return -1;
    }

    /** Calculates the initial search step for the AFMA creator. */
    protected static int computeInitialSearchStep(int maxSearchDistance) {
        int targetStep = Math.max(1, maxSearchDistance / INITIAL_GRID_TARGET_SAMPLES_PER_AXIS_KONKRETE);
        return Math.max(1, Integer.highestOneBit(targetStep));
    }

    /** Returns whether motion region in bounds. */
    public static boolean isMotionRegionInBounds(@NotNull AfmaPixelFrame frame, int x, int y, int width, int height) {
        return AfmaPixelFrameHelper.isRegionInBounds(frame, x, y, width, height);
    }

    /** Scores the region hash for AFMA creator candidate selection. */
    public static double scoreRegionHash(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                         int dstX, int dstY, int width, int height,
                                         int dx, int dy, int sampleColumns, int sampleRows) {
        Objects.requireNonNull(pairAnalysis);
        int srcX = dstX + dx;
        int srcY = dstY + dy;
        if (!isMotionRegionInBounds(pairAnalysis.previousFrame(), srcX, srcY, width, height)) {
            return 0D;
        }

        double hashScore = pairAnalysis.sampleHashMatchRatio(
                srcX,
                srcY,
                dstX,
                dstY,
                width,
                height,
                sampleColumns,
                sampleRows
        );
        return hashScore - ((Math.abs(dx) + Math.abs(dy)) * 1.0E-6D);
    }

    /** Calculates the approx sad for the AFMA creator. */
    public static long computeApproxSad(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                        int dstX, int dstY, int srcX, int srcY, int width, int height, long sadCutoff) {
        Objects.requireNonNull(previousFrame);
        Objects.requireNonNull(currentFrame);
        if (!isMotionRegionInBounds(previousFrame, srcX, srcY, width, height)
                || !AfmaPixelFrameHelper.isRegionInBounds(currentFrame, dstX, dstY, width, height)) {
            return Long.MAX_VALUE;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int sampleColumns = Math.min(width, APPROX_SAD_COLUMNS_KONKRETE);
        int sampleRows = Math.min(height, APPROX_SAD_ROWS_KONKRETE);
        long sad = 0L;
        for (int sampleRow = 0; sampleRow < sampleRows; sampleRow++) {
            int cellStartY = (sampleRow * height) / sampleRows;
            int cellEndY = ((sampleRow + 1) * height) / sampleRows;
            int cellHeight = Math.max(1, cellEndY - cellStartY);
            int probeY = cellStartY + Math.max(0, (cellHeight - 1) / 2);
            for (int sampleColumn = 0; sampleColumn < sampleColumns; sampleColumn++) {
                int cellStartX = (sampleColumn * width) / sampleColumns;
                int cellEndX = ((sampleColumn + 1) * width) / sampleColumns;
                int cellWidth = Math.max(1, cellEndX - cellStartX);
                int probeX = cellStartX + Math.max(0, (cellWidth - 1) / 2);
                int previousColor = previousPixels[((srcY + probeY) * frameWidth) + srcX + probeX];
                int currentColor = currentPixels[((dstY + probeY) * frameWidth) + dstX + probeX];
                long weightedArea = (long) cellWidth * cellHeight;
                sad += weightedArea * Math.abs(((previousColor >> 16) & 0xFF) - ((currentColor >> 16) & 0xFF));
                sad += weightedArea * Math.abs(((previousColor >> 8) & 0xFF) - ((currentColor >> 8) & 0xFF));
                sad += weightedArea * Math.abs((previousColor & 0xFF) - (currentColor & 0xFF));
                sad += weightedArea * Math.abs(((previousColor >>> 24) & 0xFF) - ((currentColor >>> 24) & 0xFF));
                if (sad >= sadCutoff) {
                    return sad;
                }
            }
        }
        return sad;
    }

    /** Calculates the sad for the AFMA creator. */
    public static long computeSad(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                  int dstX, int dstY, int srcX, int srcY, int width, int height) {
        Objects.requireNonNull(previousFrame);
        Objects.requireNonNull(currentFrame);
        if (!isMotionRegionInBounds(previousFrame, srcX, srcY, width, height)
                || !AfmaPixelFrameHelper.isRegionInBounds(currentFrame, dstX, dstY, width, height)) {
            return Long.MAX_VALUE;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        long sad = 0L;
        for (int localY = 0; localY < height; localY++) {
            int previousRowOffset = ((srcY + localY) * frameWidth) + srcX;
            int currentRowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int previousColor = previousPixels[previousRowOffset + localX];
                int currentColor = currentPixels[currentRowOffset + localX];
                sad += Math.abs(((previousColor >> 16) & 0xFF) - ((currentColor >> 16) & 0xFF));
                sad += Math.abs(((previousColor >> 8) & 0xFF) - ((currentColor >> 8) & 0xFF));
                sad += Math.abs((previousColor & 0xFF) - (currentColor & 0xFF));
                sad += Math.abs(((previousColor >>> 24) & 0xFF) - ((currentColor >>> 24) & 0xFF));
            }
        }
        return sad;
    }

    /** Carries {@code MotionCandidate} data between validated stages of the AFMA creator. */
    public record MotionCandidate(@NotNull AfmaRectCopyDetector.MotionVector vector,
                                  double hashScore,
                                  long approximateSad) {
    }

}
