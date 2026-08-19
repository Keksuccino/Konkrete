package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Reports {@code AfmaFramePairAnalysis} measurements produced by the AFMA creator. */
public final class AfmaFramePairAnalysis {

    /** Scoring or hashing coefficient for tile hash offset basis in the AFMA creator. */
    protected static final long TILE_HASH_OFFSET_BASIS_KONKRETE = 0xCBF29CE484222325L;
    /** Scoring or hashing coefficient for tile hash prime in the AFMA creator. */
    protected static final long TILE_HASH_PRIME_KONKRETE = 0x100000001B3L;
    /** Upper bound for cached region diff analyses enforced by the AFMA creator. */
    protected static final int MAX_CACHED_REGION_DIFF_ANALYSES_KONKRETE = 4;
    /** Upper bound for cached changed tile grids enforced by the AFMA creator. */
    protected static final int MAX_CACHED_CHANGED_TILE_GRIDS_KONKRETE = 4;

    /** Owned previous frame state for this AFMA creator instance. */
    @NotNull
    protected final AfmaPixelFrame previousFrame;
    /** Owned next frame state for this AFMA creator instance. */
    @NotNull
    protected final AfmaPixelFrame nextFrame;
    /** Decoded canvas width in pixels for this AFMA creator instance. */
    protected final int width;
    /** Decoded canvas height in pixels for this AFMA creator instance. */
    protected final int height;
    /** Holds the previousPixels collection used by this AFMA creator instance. */
    @NotNull
    protected final int[] previousPixels;
    /** Holds the nextPixels collection used by this AFMA creator instance. */
    @NotNull
    protected final int[] nextPixels;
    /** Current shared analysis index measured or selected by this AFMA creator instance. */
    @NotNull
    protected final SharedAnalysisIndex sharedAnalysisIndex;
    /** Holds the dirtyBoundsAfterCopyByKey collection used by this AFMA creator instance. */
    @Nullable
    protected Map<CopyRectKey, DirtyBoundsAfterCopyResult> dirtyBoundsAfterCopyByKey;
    /** Holds the motionSearchAnalysesByKey collection used by this AFMA creator instance. */
    @Nullable
    protected Map<MotionSearchKey, AfmaRectCopyDetector.MotionSearchAnalysis> motionSearchAnalysesByKey;
    /** Holds the regionDiffAnalysesByKey collection used by this AFMA creator instance. */
    @Nullable
    protected Map<RegionKey, RegionDiffAnalysis> regionDiffAnalysesByKey;
    /** Holds the copyAdjustedRegionDiffAnalysesByKey collection used by this AFMA creator instance. */
    @Nullable
    protected Map<CopyAdjustedRegionKey, RegionDiffAnalysis> copyAdjustedRegionDiffAnalysesByKey;
    /** Holds the tileGridSummariesByTileSize collection used by this AFMA creator instance. */
    @NotNull
    protected Map<Integer, TileGridSummary> tileGridSummariesByTileSize = new LinkedHashMap<>();
    /** Builds the lru cache for the AFMA creator. */
    @NotNull
    protected Map<Integer, ChangedTileGrid> changedTileGridsByTileSize = createLruCache(MAX_CACHED_CHANGED_TILE_GRIDS_KONKRETE);

    /** Initializes a new {@code AfmaFramePairAnalysis} for AFMA creator use. */
    public AfmaFramePairAnalysis(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame nextFrame) {
        previousFrame = Objects.requireNonNull(previousFrame);
        nextFrame = Objects.requireNonNull(nextFrame);
        AfmaPixelFrameHelper.ensureSameSize(previousFrame, nextFrame);
        this.previousFrame = previousFrame;
        this.nextFrame = nextFrame;
        this.width = previousFrame.getWidth();
        this.height = previousFrame.getHeight();
        this.previousPixels = previousFrame.getPixelsUnsafe();
        this.nextPixels = nextFrame.getPixelsUnsafe();
        this.sharedAnalysisIndex = this.buildSharedAnalysisIndex();
    }

    /** Returns the previous frame produced by the AFMA creator. */
    @NotNull
    public AfmaPixelFrame previousFrame() {
        return this.previousFrame;
    }

    /** Advances to the frame value in the AFMA creator. */
    @NotNull
    public AfmaPixelFrame nextFrame() {
        return this.nextFrame;
    }

    /** Returns whether identical. */
    public boolean isIdentical() {
        return this.sharedAnalysisIndex.identical();
    }

    /** Compares the difference bounds data in the AFMA creator. */
    public @Nullable AfmaRect differenceBounds() {
        return this.sharedAnalysisIndex.differenceBounds();
    }

    /** Returns the changed pixel count produced by the AFMA creator. */
    public int changedPixelCount() {
        return this.sharedAnalysisIndex.changedPixelCount();
    }

    /** Returns the region diff analysis produced by the AFMA creator. */
    @Nullable
    public RegionDiffAnalysis regionDiffAnalysis(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.requireContainedRegion(x, y, width, height);
        if (clippedRegion == null) {
            return null;
        }

        RegionKey cacheKey = new RegionKey(clippedRegion.x(), clippedRegion.y(), clippedRegion.width(), clippedRegion.height());
        if (this.regionDiffAnalysesByKey == null) {
            this.regionDiffAnalysesByKey = createLruCache(MAX_CACHED_REGION_DIFF_ANALYSES_KONKRETE);
        } else {
            RegionDiffAnalysis cachedAnalysis = this.regionDiffAnalysesByKey.get(cacheKey);
            if (cachedAnalysis != null) {
                return cachedAnalysis;
            }
        }

        RegionDiffAnalysis regionAnalysis = this.buildRegionDiffAnalysis(cacheKey);
        this.regionDiffAnalysesByKey.put(cacheKey, regionAnalysis);
        return regionAnalysis;
    }

    /** Copies the adjusted region diff analysis for the AFMA creator. */
    @Nullable
    public RegionDiffAnalysis copyAdjustedRegionDiffAnalysis(@NotNull AfmaCopyRect copyRect, int x, int y, int width, int height) {
        Objects.requireNonNull(copyRect);
        ClippedRegion clippedRegion = this.requireContainedRegion(x, y, width, height);
        if (clippedRegion == null) {
            return null;
        }

        CopyAdjustedRegionKey cacheKey = CopyAdjustedRegionKey.of(copyRect, clippedRegion);
        if (this.copyAdjustedRegionDiffAnalysesByKey == null) {
            this.copyAdjustedRegionDiffAnalysesByKey = createLruCache(MAX_CACHED_REGION_DIFF_ANALYSES_KONKRETE);
        } else {
            RegionDiffAnalysis cachedAnalysis = this.copyAdjustedRegionDiffAnalysesByKey.get(cacheKey);
            if (cachedAnalysis != null) {
                return cachedAnalysis;
            }
        }

        RegionDiffAnalysis regionAnalysis = this.buildCopyAdjustedRegionDiffAnalysis(cacheKey);
        this.copyAdjustedRegionDiffAnalysesByKey.put(cacheKey, regionAnalysis);
        return regionAnalysis;
    }

    /** Resolves the dirty bounds after copy for the AFMA creator. */
    public @Nullable AfmaRect findDirtyBoundsAfterCopy(@NotNull AfmaCopyRect copyRect) {
        return this.analyzeDirtyAfterCopy(copyRect).bounds();
    }

    /** Counts the dirty pixels in the AFMA creator. */
    public int countDirtyPixels(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return 0;
        }
        return sumIntPrefix(this.sharedAnalysisIndex.dirtyPrefixSum(), clippedRegion, this.width + 1);
    }

    /** Returns whether alpha difference. */
    public boolean hasAlphaDifference(int x, int y, int width, int height) {
        return this.regionErrorMetrics(x, y, width, height).alphaErrorSum() > 0L;
    }

    /** Returns whether next region has non opaque pixels. */
    public boolean nextRegionHasNonOpaquePixels(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return false;
        }
        return sumIntPrefix(this.sharedAnalysisIndex.nextNonOpaquePrefixSum(), clippedRegion, this.width + 1) > 0;
    }

    /** Returns the region error metrics produced by the AFMA creator. */
    @NotNull
    public RegionErrorMetrics regionErrorMetrics(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return new RegionErrorMetrics(0, 0L, 0L, false);
        }
        return new RegionErrorMetrics(
                sumIntPrefix(this.sharedAnalysisIndex.dirtyPrefixSum(), clippedRegion, this.width + 1),
                sumLongPrefix(this.sharedAnalysisIndex.colorErrorPrefixSum(), clippedRegion, this.width + 1),
                sumLongPrefix(this.sharedAnalysisIndex.alphaErrorPrefixSum(), clippedRegion, this.width + 1),
                sumIntPrefix(this.sharedAnalysisIndex.nextNonOpaquePrefixSum(), clippedRegion, this.width + 1) > 0
        );
    }

    /** Intersects the difference bounds bounds for the AFMA creator. */
    public @Nullable AfmaRect intersectDifferenceBounds(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return null;
        }
        AfmaRect differenceBounds = this.sharedAnalysisIndex.differenceBounds();
        if (differenceBounds == null) {
            return null;
        }

        int minX = Math.max(clippedRegion.x(), differenceBounds.x());
        int minY = Math.max(clippedRegion.y(), differenceBounds.y());
        int maxX = Math.min(clippedRegion.right(), differenceBounds.x() + differenceBounds.width());
        int maxY = Math.min(clippedRegion.bottom(), differenceBounds.y() + differenceBounds.height());
        if ((maxX <= minX) || (maxY <= minY)) {
            return null;
        }
        return new AfmaRect(minX, minY, maxX - minX, maxY - minY);
    }

    @NotNull
    DirtyBoundsAfterCopyResult analyzeDirtyAfterCopy(@NotNull AfmaCopyRect copyRect) {
        CopyRectKey cacheKey = CopyRectKey.of(copyRect);
        if (this.dirtyBoundsAfterCopyByKey == null) {
            this.dirtyBoundsAfterCopyByKey = new LinkedHashMap<>();
        } else {
            DirtyBoundsAfterCopyResult cachedResult = this.dirtyBoundsAfterCopyByKey.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        DirtyBoundsAfterCopyResult dirtyAfterCopy = this.scanDirtyBoundsAfterCopy(copyRect);
        this.dirtyBoundsAfterCopyByKey.put(cacheKey, dirtyAfterCopy);
        return dirtyAfterCopy;
    }

    @Nullable
    AfmaRectCopyDetector.MotionSearchAnalysis getMotionSearchAnalysis(int maxSearchDistance, int maxCandidateAxisOffsets) {
        if (this.motionSearchAnalysesByKey == null) {
            return null;
        }
        return this.motionSearchAnalysesByKey.get(new MotionSearchKey(maxSearchDistance, maxCandidateAxisOffsets));
    }

    void cacheMotionSearchAnalysis(int maxSearchDistance, int maxCandidateAxisOffsets,
                                   @NotNull AfmaRectCopyDetector.MotionSearchAnalysis motionSearchAnalysis) {
        if (this.motionSearchAnalysesByKey == null) {
            this.motionSearchAnalysesByKey = new LinkedHashMap<>();
        }
        this.motionSearchAnalysesByKey.put(new MotionSearchKey(maxSearchDistance, maxCandidateAxisOffsets), motionSearchAnalysis);
    }

    /** Returns the perceptual drift metrics produced by the AFMA creator. */
    @NotNull
    public PerceptualDriftMetrics perceptualDriftMetrics() {
        return this.sharedAnalysisIndex.perceptualDriftMetrics();
    }

    /** Returns the perceptual drift extrema outside region produced by the AFMA creator. */
    @NotNull
    public PerceptualDriftExtrema perceptualDriftExtremaOutsideRegion(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            PerceptualDriftMetrics driftMetrics = this.sharedAnalysisIndex.perceptualDriftMetrics();
            return new PerceptualDriftExtrema(driftMetrics.maxVisibleColorDelta(), driftMetrics.maxAlphaDelta());
        }
        if ((clippedRegion.x() == 0) && (clippedRegion.y() == 0)
                && (clippedRegion.width() == this.width) && (clippedRegion.height() == this.height)) {
            return new PerceptualDriftExtrema(0, 0);
        }

        int maxVisibleColorDelta = 0;
        int maxAlphaDelta = 0;
        byte[] visibleColorRowMax = this.sharedAnalysisIndex.visibleColorMaxByRow();
        byte[] alphaRowMax = this.sharedAnalysisIndex.alphaMaxByRow();
        byte[] visibleColorPrefixMax = this.sharedAnalysisIndex.visibleColorPrefixMaxByPixel();
        byte[] alphaPrefixMax = this.sharedAnalysisIndex.alphaPrefixMaxByPixel();
        byte[] visibleColorSuffixMax = this.sharedAnalysisIndex.visibleColorSuffixMaxByPixel();
        byte[] alphaSuffixMax = this.sharedAnalysisIndex.alphaSuffixMaxByPixel();
        int clippedTop = clippedRegion.y();
        int clippedBottom = clippedRegion.bottom();
        int clippedLeft = clippedRegion.x();
        int clippedRight = clippedRegion.right();

        for (int row = 0; row < this.height; row++) {
            if ((row < clippedTop) || (row >= clippedBottom)) {
                maxVisibleColorDelta = Math.max(maxVisibleColorDelta, unsignedByte(visibleColorRowMax[row]));
                maxAlphaDelta = Math.max(maxAlphaDelta, unsignedByte(alphaRowMax[row]));
                continue;
            }

            int rowOffset = row * this.width;
            if (clippedLeft > 0) {
                maxVisibleColorDelta = Math.max(maxVisibleColorDelta, unsignedByte(visibleColorPrefixMax[rowOffset + clippedLeft - 1]));
                maxAlphaDelta = Math.max(maxAlphaDelta, unsignedByte(alphaPrefixMax[rowOffset + clippedLeft - 1]));
            }
            if (clippedRight < this.width) {
                maxVisibleColorDelta = Math.max(maxVisibleColorDelta, unsignedByte(visibleColorSuffixMax[rowOffset + clippedRight]));
                maxAlphaDelta = Math.max(maxAlphaDelta, unsignedByte(alphaSuffixMax[rowOffset + clippedRight]));
            }
        }

        return new PerceptualDriftExtrema(maxVisibleColorDelta, maxAlphaDelta);
    }

    /** Calculates the sample match ratio value for the AFMA creator. */
    public double sampleMatchRatio(int srcX, int srcY, int dstX, int dstY, int sampleWidth, int sampleHeight) {
        int stepX = Math.max(1, sampleWidth / 48);
        int stepY = Math.max(1, sampleHeight / 16);
        int matches = 0;
        int samples = 0;

        for (int y = 0; y < sampleHeight; y += stepY) {
            int previousRowOffset = ((srcY + y) * this.width) + srcX;
            int nextRowOffset = ((dstY + y) * this.width) + dstX;
            for (int x = 0; x < sampleWidth; x += stepX) {
                samples++;
                if (this.previousPixels[previousRowOffset + x] == this.nextPixels[nextRowOffset + x]) {
                    matches++;
                }
            }
        }

        return (samples > 0) ? ((double) matches / samples) : 0D;
    }

    /** Calculates the sample hash match ratio value for the AFMA creator. */
    public double sampleHashMatchRatio(int srcX, int srcY, int dstX, int dstY, int sampleWidth, int sampleHeight,
                                       int targetColumns, int targetRows) {
        if ((sampleWidth <= 0) || (sampleHeight <= 0)) {
            return 0D;
        }

        int sampleColumns = Math.min(sampleWidth, Math.max(1, targetColumns));
        int sampleRows = Math.min(sampleHeight, Math.max(1, targetRows));
        int matches = 0;
        int samples = 0;
        for (int sampleRow = 0; sampleRow < sampleRows; sampleRow++) {
            int cellStartY = (sampleRow * sampleHeight) / sampleRows;
            int cellEndY = ((sampleRow + 1) * sampleHeight) / sampleRows;
            int probeHeight = Math.max(1, Math.min(2, cellEndY - cellStartY));
            int probeY = cellStartY + Math.max(0, ((cellEndY - cellStartY) - probeHeight) / 2);
            for (int sampleColumn = 0; sampleColumn < sampleColumns; sampleColumn++) {
                int cellStartX = (sampleColumn * sampleWidth) / sampleColumns;
                int cellEndX = ((sampleColumn + 1) * sampleWidth) / sampleColumns;
                int probeWidth = Math.max(1, Math.min(2, cellEndX - cellStartX));
                int probeX = cellStartX + Math.max(0, ((cellEndX - cellStartX) - probeWidth) / 2);
                samples++;
                int previousHash = hashSampleProbe(this.previousPixels, this.width, srcX + probeX, srcY + probeY, probeWidth, probeHeight);
                int nextHash = hashSampleProbe(this.nextPixels, this.width, dstX + probeX, dstY + probeY, probeWidth, probeHeight);
                if (previousHash == nextHash) {
                    matches++;
                }
            }
        }

        return (samples > 0) ? ((double) matches / samples) : 0D;
    }

    /** Returns the changed tile grid produced by the AFMA creator. */
    @NotNull
    public ChangedTileGrid changedTileGrid(int tileSize) {
        if (tileSize <= 0) {
            throw new IllegalArgumentException("AFMA tile size must be greater than zero");
        }
        ChangedTileGrid cachedGrid = this.changedTileGridsByTileSize.get(tileSize);
        if (cachedGrid != null) {
            return cachedGrid;
        }

        ChangedTileGrid changedTileGrid = this.buildChangedTileGrid(tileSize);
        this.changedTileGridsByTileSize.put(tileSize, changedTileGrid);
        return changedTileGrid;
    }

    /** Returns the tile grid summary produced by the AFMA creator. */
    @NotNull
    public TileGridSummary tileGridSummary(int tileSize) {
        if (tileSize <= 0) {
            throw new IllegalArgumentException("AFMA tile size must be greater than zero");
        }
        TileGridSummary cachedSummary = this.tileGridSummariesByTileSize.get(tileSize);
        if (cachedSummary != null) {
            return cachedSummary;
        }

        TileGridSummary tileGridSummary = this.buildTileGridSummary(tileSize);
        this.tileGridSummariesByTileSize.put(tileSize, tileGridSummary);
        return tileGridSummary;
    }

    /** Builds the shared analysis index for the AFMA creator. */
    @NotNull
    protected SharedAnalysisIndex buildSharedAnalysisIndex() {
        int prefixWidth = this.width + 1;
        int prefixSize = prefixWidth * (this.height + 1);
        int[] differenceRowFirstDirtyX = new int[this.height];
        int[] differenceRowLastDirtyX = new int[this.height];
        Arrays.fill(differenceRowFirstDirtyX, -1);
        Arrays.fill(differenceRowLastDirtyX, -1);
        int[] dirtyPrefixSum = new int[prefixSize];
        int[] nextNonOpaquePrefixSum = new int[prefixSize];
        long[] colorErrorPrefixSum = new long[prefixSize];
        long[] alphaErrorPrefixSum = new long[prefixSize];
        byte[] visibleColorMaxByRow = new byte[this.height];
        byte[] alphaMaxByRow = new byte[this.height];
        byte[] visibleColorPrefixMaxByPixel = new byte[this.previousPixels.length];
        byte[] alphaPrefixMaxByPixel = new byte[this.previousPixels.length];
        byte[] visibleColorSuffixMaxByPixel = new byte[this.previousPixels.length];
        byte[] alphaSuffixMaxByPixel = new byte[this.previousPixels.length];
        byte[] visibleColorDeltasByRow = new byte[this.width];
        byte[] alphaDeltasByRow = new byte[this.width];
        int minX = this.width;
        int minY = this.height;
        int maxX = -1;
        int maxY = -1;
        int changedPixelCount = 0;
        double totalPerceptualError = 0D;
        int maxVisibleColorDelta = 0;
        int maxAlphaDelta = 0;
        for (int y = 0; y < this.height; y++) {
            int rowOffset = y * this.width;
            int rowVisibleColorMax = 0;
            int rowAlphaMax = 0;
            for (int x = 0; x < this.width; x++) {
                int pixelIndex = rowOffset + x;
                int previousColor = this.previousPixels[pixelIndex];
                int nextColor = this.nextPixels[pixelIndex];
                boolean dirty = previousColor != nextColor;
                if (dirty) {
                    changedPixelCount++;
                    if (differenceRowFirstDirtyX[y] < 0) {
                        differenceRowFirstDirtyX[y] = x;
                    }
                    differenceRowLastDirtyX[y] = x;
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }

                int previousAlpha = (previousColor >>> 24) & 0xFF;
                int nextAlpha = (nextColor >>> 24) & 0xFF;
                int alphaDelta = Math.abs(previousAlpha - nextAlpha);
                alphaDeltasByRow[x] = (byte) alphaDelta;
                if (alphaDelta > rowAlphaMax) {
                    rowAlphaMax = alphaDelta;
                }
                if (alphaDelta > maxAlphaDelta) {
                    maxAlphaDelta = alphaDelta;
                }
                alphaPrefixMaxByPixel[pixelIndex] = (byte) rowAlphaMax;

                int redDelta = channelDifference(previousColor >> 16, nextColor >> 16);
                int greenDelta = channelDifference(previousColor >> 8, nextColor >> 8);
                int blueDelta = channelDifference(previousColor, nextColor);
                int visibleColorDelta = 0;
                int visibilityAlpha = Math.max(previousAlpha, nextAlpha);
                if (visibilityAlpha > 0) {
                    visibleColorDelta = Math.max(redDelta, Math.max(greenDelta, blueDelta));
                    if (visibleColorDelta > maxVisibleColorDelta) {
                        maxVisibleColorDelta = visibleColorDelta;
                    }
                    totalPerceptualError += (alphaDelta * 2.0D) + ((redDelta + greenDelta + blueDelta) * (visibilityAlpha / 255.0D));
                }
                visibleColorDeltasByRow[x] = (byte) visibleColorDelta;
                if (visibleColorDelta > rowVisibleColorMax) {
                    rowVisibleColorMax = visibleColorDelta;
                }
                visibleColorPrefixMaxByPixel[pixelIndex] = (byte) rowVisibleColorMax;

                int prefixArrayIndex = prefixIndex(x + 1, y + 1, prefixWidth);
                int aboveIndex = prefixIndex(x + 1, y, prefixWidth);
                int leftIndex = prefixIndex(x, y + 1, prefixWidth);
                int upperLeftIndex = prefixIndex(x, y, prefixWidth);
                dirtyPrefixSum[prefixArrayIndex] = dirtyPrefixSum[aboveIndex] + dirtyPrefixSum[leftIndex] - dirtyPrefixSum[upperLeftIndex] + (dirty ? 1 : 0);
                nextNonOpaquePrefixSum[prefixArrayIndex] = nextNonOpaquePrefixSum[aboveIndex] + nextNonOpaquePrefixSum[leftIndex]
                        - nextNonOpaquePrefixSum[upperLeftIndex] + ((nextAlpha != 0xFF) ? 1 : 0);
                colorErrorPrefixSum[prefixArrayIndex] = colorErrorPrefixSum[aboveIndex] + colorErrorPrefixSum[leftIndex]
                        - colorErrorPrefixSum[upperLeftIndex] + redDelta + greenDelta + blueDelta;
                alphaErrorPrefixSum[prefixArrayIndex] = alphaErrorPrefixSum[aboveIndex] + alphaErrorPrefixSum[leftIndex]
                        - alphaErrorPrefixSum[upperLeftIndex] + alphaDelta;
            }
            visibleColorMaxByRow[y] = (byte) rowVisibleColorMax;
            alphaMaxByRow[y] = (byte) rowAlphaMax;

            int rowVisibleColorSuffixMax = 0;
            int rowAlphaSuffixMax = 0;
            for (int x = this.width - 1; x >= 0; x--) {
                rowVisibleColorSuffixMax = Math.max(rowVisibleColorSuffixMax, unsignedByte(visibleColorDeltasByRow[x]));
                rowAlphaSuffixMax = Math.max(rowAlphaSuffixMax, unsignedByte(alphaDeltasByRow[x]));
                int pixelIndex = rowOffset + x;
                visibleColorSuffixMaxByPixel[pixelIndex] = (byte) rowVisibleColorSuffixMax;
                alphaSuffixMaxByPixel[pixelIndex] = (byte) rowAlphaSuffixMax;
            }
        }

        boolean identical = (maxX < minX) || (maxY < minY);
        AfmaRect differenceBounds = identical ? null : new AfmaRect(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
        return new SharedAnalysisIndex(
                identical,
                changedPixelCount,
                differenceBounds,
                differenceRowFirstDirtyX,
                differenceRowLastDirtyX,
                dirtyPrefixSum,
                nextNonOpaquePrefixSum,
                colorErrorPrefixSum,
                alphaErrorPrefixSum,
                visibleColorMaxByRow,
                alphaMaxByRow,
                visibleColorPrefixMaxByPixel,
                alphaPrefixMaxByPixel,
                visibleColorSuffixMaxByPixel,
                alphaSuffixMaxByPixel,
                new PerceptualDriftMetrics(
                        totalPerceptualError,
                        totalPerceptualError / Math.max(1, this.previousPixels.length),
                        maxVisibleColorDelta,
                        maxAlphaDelta
                )
        );
    }

    /** Builds the region diff analysis for the AFMA creator. */
    @NotNull
    protected RegionDiffAnalysis buildRegionDiffAnalysis(@NotNull RegionKey regionKey) {
        int regionWidth = regionKey.width();
        int regionHeight = regionKey.height();
        int pixelCount = regionWidth * regionHeight;
        int[] predictedColors = new int[pixelCount];
        int[] currentColors = new int[pixelCount];
        BitSet changedPixelMask = new BitSet(pixelCount);
        int changedPixelCount = 0;
        boolean includeAlpha = false;
        int pixelOffset = 0;
        int[] rowFirstDirtyX = this.sharedAnalysisIndex.differenceRowFirstDirtyX();
        int[] rowLastDirtyX = this.sharedAnalysisIndex.differenceRowLastDirtyX();

        for (int localY = 0; localY < regionHeight; localY++) {
            int pixelY = regionKey.y() + localY;
            int rowOffset = (pixelY * this.width) + regionKey.x();
            System.arraycopy(this.previousPixels, rowOffset, predictedColors, pixelOffset, regionWidth);
            System.arraycopy(this.nextPixels, rowOffset, currentColors, pixelOffset, regionWidth);

            int rowFirst = rowFirstDirtyX[pixelY];
            int rowLast = rowLastDirtyX[pixelY];
            if ((rowFirst >= 0) && (rowLast >= regionKey.x()) && (rowFirst < (regionKey.x() + regionWidth))) {
                int dirtyStart = Math.max(0, rowFirst - regionKey.x());
                int dirtyEnd = Math.min(regionWidth, (rowLast - regionKey.x()) + 1);
                for (int localX = dirtyStart; localX < dirtyEnd; localX++) {
                    int pixelIndex = pixelOffset + localX;
                    int predictedColor = predictedColors[pixelIndex];
                    int currentColor = currentColors[pixelIndex];
                    if (predictedColor == currentColor) {
                        continue;
                    }

                    changedPixelMask.set(pixelIndex);
                    changedPixelCount++;
                    if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                        includeAlpha = true;
                    }
                }
            }
            pixelOffset += regionWidth;
        }

        return new RegionDiffAnalysis(regionWidth, regionHeight, changedPixelCount, includeAlpha,
                predictedColors, currentColors, changedPixelMask);
    }

    /** Builds the copy adjusted region diff analysis for the AFMA creator. */
    @NotNull
    protected RegionDiffAnalysis buildCopyAdjustedRegionDiffAnalysis(@NotNull CopyAdjustedRegionKey regionKey) {
        int regionWidth = regionKey.width();
        int regionHeight = regionKey.height();
        int pixelCount = regionWidth * regionHeight;
        int[] predictedColors = new int[pixelCount];
        int[] currentColors = new int[pixelCount];
        BitSet changedPixelMask = new BitSet(pixelCount);
        int changedPixelCount = 0;
        boolean includeAlpha = false;
        int pixelOffset = 0;
        int startX = regionKey.x();
        int startY = regionKey.y();
        int dstLeft = regionKey.dstX();
        int dstTop = regionKey.dstY();
        int dstRight = dstLeft + regionKey.copyWidth();
        int dstBottom = dstTop + regionKey.copyHeight();

        for (int localY = 0; localY < regionHeight; localY++) {
            int pixelY = startY + localY;
            int rowOffset = (pixelY * this.width) + startX;
            System.arraycopy(this.previousPixels, rowOffset, predictedColors, pixelOffset, regionWidth);
            System.arraycopy(this.nextPixels, rowOffset, currentColors, pixelOffset, regionWidth);

            if ((pixelY >= dstTop) && (pixelY < dstBottom)) {
                int overlapStartX = Math.max(startX, dstLeft);
                int overlapEndX = Math.min(startX + regionWidth, dstRight);
                if (overlapEndX > overlapStartX) {
                    int sourceRowOffset = ((regionKey.srcY() + (pixelY - dstTop)) * this.width) + regionKey.srcX() + (overlapStartX - dstLeft);
                    int targetOffset = pixelOffset + (overlapStartX - startX);
                    System.arraycopy(this.previousPixels, sourceRowOffset, predictedColors, targetOffset, overlapEndX - overlapStartX);
                }
            }

            for (int localX = 0; localX < regionWidth; localX++) {
                int pixelIndex = pixelOffset + localX;
                int predictedColor = predictedColors[pixelIndex];
                int currentColor = currentColors[pixelIndex];
                if (predictedColor == currentColor) {
                    continue;
                }

                changedPixelMask.set(pixelIndex);
                changedPixelCount++;
                if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                    includeAlpha = true;
                }
            }
            pixelOffset += regionWidth;
        }

        return new RegionDiffAnalysis(regionWidth, regionHeight, changedPixelCount, includeAlpha,
                predictedColors, currentColors, changedPixelMask);
    }

    /** Iterates over the dirty bounds after copy in the AFMA creator data. */
    protected @NotNull DirtyBoundsAfterCopyResult scanDirtyBoundsAfterCopy(@NotNull AfmaCopyRect copyRect) {
        int dstLeft = copyRect.getDstX();
        int dstTop = copyRect.getDstY();
        int dstRight = dstLeft + copyRect.getWidth();
        int dstBottom = dstTop + copyRect.getHeight();
        int srcLeft = copyRect.getSrcX();
        int srcTop = copyRect.getSrcY();
        int minX = this.width;
        int minY = this.height;
        int maxX = -1;
        int maxY = -1;
        int initialDirtyPixelsInsideCopy = this.countDirtyPixels(dstLeft, dstTop, copyRect.getWidth(), copyRect.getHeight());
        int dirtyPixelsInsideCopyAfterCopy = 0;

        // A copy only changes dirty membership inside its destination rectangle.
        for (int localY = 0; localY < copyRect.getHeight(); localY++) {
            int pixelY = dstTop + localY;
            int dstRowOffset = (pixelY * this.width) + dstLeft;
            int srcRowOffset = ((srcTop + localY) * this.width) + srcLeft;
            for (int localX = 0; localX < copyRect.getWidth(); localX++) {
                int pixelIndex = dstRowOffset + localX;
                if (this.previousPixels[srcRowOffset + localX] == this.nextPixels[pixelIndex]) {
                    continue;
                }

                dirtyPixelsInsideCopyAfterCopy++;
                int pixelX = dstLeft + localX;
                if (pixelX < minX) minX = pixelX;
                if (pixelY < minY) minY = pixelY;
                if (pixelX > maxX) maxX = pixelX;
                if (pixelY > maxY) maxY = pixelY;
            }
        }

        int dirtyPixelCount = this.sharedAnalysisIndex.changedPixelCount() - initialDirtyPixelsInsideCopy + dirtyPixelsInsideCopyAfterCopy;
        AfmaRect initialDirtyBounds = this.sharedAnalysisIndex.differenceBounds();
        if (initialDirtyBounds != null) {
            int[] differenceRowFirstDirtyX = this.sharedAnalysisIndex.differenceRowFirstDirtyX();
            int[] differenceRowLastDirtyX = this.sharedAnalysisIndex.differenceRowLastDirtyX();
            int scanStartY = initialDirtyBounds.y();
            int scanEndY = initialDirtyBounds.y() + initialDirtyBounds.height();
            for (int y = scanStartY; y < scanEndY; y++) {
                int rowFirstDirtyX = differenceRowFirstDirtyX[y];
                if (rowFirstDirtyX < 0) {
                    continue;
                }

                int rowLastDirtyX = differenceRowLastDirtyX[y];
                int rowMinX = rowFirstDirtyX;
                int rowMaxX = rowLastDirtyX;
                if ((y >= dstTop) && (y < dstBottom)) {
                    boolean hasLeftDirty = rowFirstDirtyX < dstLeft;
                    boolean hasRightDirty = rowLastDirtyX >= dstRight;
                    if (!hasLeftDirty && !hasRightDirty) {
                        continue;
                    }
                    rowMinX = hasLeftDirty ? rowFirstDirtyX : rowLastDirtyX;
                    rowMaxX = hasRightDirty ? rowLastDirtyX : rowFirstDirtyX;
                }

                if (rowMinX < minX) minX = rowMinX;
                if (y < minY) minY = y;
                if (rowMaxX > maxX) maxX = rowMaxX;
                if (y > maxY) maxY = y;
            }
        }

        if (dirtyPixelCount <= 0) {
            return new DirtyBoundsAfterCopyResult(null, 0);
        }
        if ((maxX < minX) || (maxY < minY)) {
            return new DirtyBoundsAfterCopyResult(null, dirtyPixelCount);
        }
        return new DirtyBoundsAfterCopyResult(
                new AfmaRect(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1),
                dirtyPixelCount
        );
    }

    /** Requires the contained region before AFMA creator processing. */
    @Nullable
    protected ClippedRegion requireContainedRegion(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return null;
        }
        if ((clippedRegion.x() != x) || (clippedRegion.y() != y)
                || (clippedRegion.width() != width) || (clippedRegion.height() != height)) {
            return null;
        }
        if (((long) width * (long) height) > Integer.MAX_VALUE) {
            return null;
        }
        return clippedRegion;
    }

    /** Builds the changed tile grid for the AFMA creator. */
    @NotNull
    protected ChangedTileGrid buildChangedTileGrid(int tileSize) {
        int tileCountX = (this.width + tileSize - 1) / tileSize;
        int tileCountY = (this.height + tileSize - 1) / tileSize;
        boolean[] changedTilesByIndex = new boolean[tileCountX * tileCountY];
        int changedTileCount = 0;
        int prefixWidth = this.width + 1;
        int tileIndex = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            int startY = tileY * tileSize;
            int tileHeight = Math.min(tileSize, this.height - startY);
            for (int tileX = 0; tileX < tileCountX; tileX++, tileIndex++) {
                int startX = tileX * tileSize;
                int tileWidth = Math.min(tileSize, this.width - startX);
                boolean tileChanged = sumIntPrefix(
                        this.sharedAnalysisIndex.dirtyPrefixSum(),
                        new ClippedRegion(startX, startY, tileWidth, tileHeight),
                        prefixWidth
                ) > 0;
                changedTilesByIndex[tileIndex] = tileChanged;
                if (tileChanged) {
                    changedTileCount++;
                }
            }
        }
        return new ChangedTileGrid(tileSize, tileCountX, tileCountY, changedTileCount, changedTilesByIndex);
    }

    /** Calculates the channel difference value for the AFMA creator. */
    protected static int channelDifference(int first, int second) {
        return Math.abs((first & 0xFF) - (second & 0xFF));
    }

    /** Builds the lru cache for the AFMA creator. */
    @NotNull
    protected static <K, V> Map<K, V> createLruCache(int maxEntries) {
        return new LinkedHashMap<>(Math.max(2, maxEntries), 0.75F, true) {
            /** Removes the eldest entry from this AFMA creator component. */
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return this.size() > maxEntries;
            }
        };
    }

    /** Returns the unsigned byte produced by the AFMA creator. */
    protected static int unsignedByte(byte value) {
        return value & 0xFF;
    }

    /** Computes the stable pixel hash for one rectangular sampling probe. */
    protected static int hashSampleProbe(@NotNull int[] pixels, int frameWidth, int x, int y, int width, int height) {
        int hash = 1;
        for (int localY = 0; localY < height; localY++) {
            int rowOffset = ((y + localY) * frameWidth) + x;
            for (int localX = 0; localX < width; localX++) {
                hash = (31 * hash) + pixels[rowOffset + localX];
            }
        }
        return hash;
    }

    /** Returns the prefix index produced by the AFMA creator. */
    protected static int prefixIndex(int x, int y, int prefixWidth) {
        return (y * prefixWidth) + x;
    }

    /** Calculates the sum int prefix value for the AFMA creator. */
    protected static int sumIntPrefix(@NotNull int[] prefixSum, @NotNull ClippedRegion region, int prefixWidth) {
        int left = region.x();
        int top = region.y();
        int right = region.right();
        int bottom = region.bottom();
        return prefixSum[prefixIndex(right, bottom, prefixWidth)]
                - prefixSum[prefixIndex(left, bottom, prefixWidth)]
                - prefixSum[prefixIndex(right, top, prefixWidth)]
                + prefixSum[prefixIndex(left, top, prefixWidth)];
    }

    /** Calculates the sum long prefix value for the AFMA creator. */
    protected static long sumLongPrefix(@NotNull long[] prefixSum, @NotNull ClippedRegion region, int prefixWidth) {
        int left = region.x();
        int top = region.y();
        int right = region.right();
        int bottom = region.bottom();
        return prefixSum[prefixIndex(right, bottom, prefixWidth)]
                - prefixSum[prefixIndex(left, bottom, prefixWidth)]
                - prefixSum[prefixIndex(right, top, prefixWidth)]
                + prefixSum[prefixIndex(left, top, prefixWidth)];
    }

    /** Returns the clip region produced by the AFMA creator. */
    @Nullable
    protected ClippedRegion clipRegion(int x, int y, int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            return null;
        }

        int minX = Math.max(0, x);
        int minY = Math.max(0, y);
        int maxX = Math.min(this.width, x + width);
        int maxY = Math.min(this.height, y + height);
        if ((maxX <= minX) || (maxY <= minY)) {
            return null;
        }
        return new ClippedRegion(minX, minY, maxX - minX, maxY - minY);
    }

    /** Builds the tile grid summary for the AFMA creator. */
    @NotNull
    protected TileGridSummary buildTileGridSummary(int tileSize) {
        int tileCountX = (this.width + tileSize - 1) / tileSize;
        int tileCountY = (this.height + tileSize - 1) / tileSize;
        TileStats[] tileStatsByIndex = new TileStats[tileCountX * tileCountY];

        int tileIndex = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            int startY = tileY * tileSize;
            int tileHeight = Math.min(tileSize, this.height - startY);
            for (int tileX = 0; tileX < tileCountX; tileX++, tileIndex++) {
                int startX = tileX * tileSize;
                int tileWidth = Math.min(tileSize, this.width - startX);
                long previousHash = TILE_HASH_OFFSET_BASIS_KONKRETE;
                long nextHash = TILE_HASH_OFFSET_BASIS_KONKRETE;
                int changedPixelCount = 0;
                long colorErrorSum = 0L;
                long alphaErrorSum = 0L;
                int maxVisibleColorDelta = 0;
                int maxAlphaDelta = 0;
                boolean nextHasNonOpaquePixels = false;

                for (int localY = 0; localY < tileHeight; localY++) {
                    int rowOffset = ((startY + localY) * this.width) + startX;
                    for (int localX = 0; localX < tileWidth; localX++) {
                        int pixelIndex = rowOffset + localX;
                        int previousColor = this.previousPixels[pixelIndex];
                        int nextColor = this.nextPixels[pixelIndex];
                        previousHash = mixTileHash(previousHash, previousColor);
                        nextHash = mixTileHash(nextHash, nextColor);
                        if (previousColor != nextColor) {
                            changedPixelCount++;
                        }

                        int previousAlpha = (previousColor >>> 24) & 0xFF;
                        int nextAlpha = (nextColor >>> 24) & 0xFF;
                        int alphaDelta = Math.abs(previousAlpha - nextAlpha);
                        alphaErrorSum += alphaDelta;
                        if (alphaDelta > maxAlphaDelta) {
                            maxAlphaDelta = alphaDelta;
                        }
                        if (nextAlpha != 0xFF) {
                            nextHasNonOpaquePixels = true;
                        }

                        int redDelta = channelDifference(previousColor >> 16, nextColor >> 16);
                        int greenDelta = channelDifference(previousColor >> 8, nextColor >> 8);
                        int blueDelta = channelDifference(previousColor, nextColor);
                        colorErrorSum += redDelta + greenDelta + blueDelta;
                        if (Math.max(previousAlpha, nextAlpha) > 0) {
                            int visibleColorDelta = Math.max(redDelta, Math.max(greenDelta, blueDelta));
                            if (visibleColorDelta > maxVisibleColorDelta) {
                                maxVisibleColorDelta = visibleColorDelta;
                            }
                        }
                    }
                }

                tileStatsByIndex[tileIndex] = new TileStats(
                        previousHash,
                        nextHash,
                        changedPixelCount,
                        colorErrorSum,
                        alphaErrorSum,
                        maxVisibleColorDelta,
                        maxAlphaDelta,
                        nextHasNonOpaquePixels
                );
            }
        }

        return new TileGridSummary(tileSize, tileCountX, tileCountY, tileStatsByIndex);
    }

    /** Returns the mix tile hash produced by the AFMA creator. */
    protected static long mixTileHash(long hash, int color) {
        hash ^= Integer.toUnsignedLong(color);
        hash *= TILE_HASH_PRIME_KONKRETE;
        hash ^= (hash >>> 32);
        return hash;
    }

    /** Reports {@code PerceptualDriftMetrics} measurements produced by the AFMA creator. */
    public record PerceptualDriftMetrics(double totalError, double averageError, int maxVisibleColorDelta, int maxAlphaDelta) {
    }

    /** Reports {@code PerceptualDriftExtrema} measurements produced by the AFMA creator. */
    public record PerceptualDriftExtrema(int maxVisibleColorDelta, int maxAlphaDelta) {
    }

    /** Reports {@code RegionErrorMetrics} measurements produced by the AFMA creator. */
    public record RegionErrorMetrics(int changedPixelCount, long colorErrorSum, long alphaErrorSum,
                                     boolean nextHasNonOpaquePixels) {
    }

    /** Provides a value-based {@code RegionKey} cache key for the AFMA creator. */
    protected record RegionKey(int x, int y, int width, int height) {
    }

    /** Provides a value-based {@code CopyRectKey} cache key for the AFMA creator. */
    protected record CopyRectKey(int srcX, int srcY, int dstX, int dstY, int width, int height) {

        /** Builds a resource value for the AFMA creator. */
        @NotNull
        public static CopyRectKey of(@NotNull AfmaCopyRect copyRect) {
            return new CopyRectKey(
                    copyRect.getSrcX(),
                    copyRect.getSrcY(),
                    copyRect.getDstX(),
                    copyRect.getDstY(),
                    copyRect.getWidth(),
                    copyRect.getHeight()
            );
        }

    }

    /** Provides a value-based {@code CopyAdjustedRegionKey} cache key for the AFMA creator. */
    protected record CopyAdjustedRegionKey(@NotNull CopyRectKey copyRectKey, int x, int y, int width, int height) {

        /** Builds a resource value for the AFMA creator. */
        @NotNull
        public static CopyAdjustedRegionKey of(@NotNull AfmaCopyRect copyRect, @NotNull ClippedRegion clippedRegion) {
            return new CopyAdjustedRegionKey(CopyRectKey.of(copyRect),
                    clippedRegion.x(), clippedRegion.y(), clippedRegion.width(), clippedRegion.height());
        }

        /** Returns the src x produced by the AFMA creator. */
        public int srcX() {
            return this.copyRectKey.srcX();
        }

        /** Returns the src y produced by the AFMA creator. */
        public int srcY() {
            return this.copyRectKey.srcY();
        }

        /** Returns the dst x produced by the AFMA creator. */
        public int dstX() {
            return this.copyRectKey.dstX();
        }

        /** Returns the dst y produced by the AFMA creator. */
        public int dstY() {
            return this.copyRectKey.dstY();
        }

        /** Copies the width for the AFMA creator. */
        public int copyWidth() {
            return this.copyRectKey.width();
        }

        /** Copies the height for the AFMA creator. */
        public int copyHeight() {
            return this.copyRectKey.height();
        }

    }

    /** Provides a value-based {@code MotionSearchKey} cache key for the AFMA creator. */
    protected record MotionSearchKey(int maxSearchDistance, int maxCandidateAxisOffsets) {
    }

    /** Reports {@code DirtyBoundsAfterCopyResult} measurements produced by the AFMA creator. */
    protected record DirtyBoundsAfterCopyResult(@Nullable AfmaRect bounds, int dirtyPixelCount) {
    }

    /** Carries {@code ClippedRegion} data between validated stages of the AFMA creator. */
    protected record ClippedRegion(int x, int y, int width, int height) {

        /** Creates the right variant for the AFMA creator. */
        public int right() {
            return this.x + this.width;
        }

        /** Returns the exclusive bottom edge of this clipped region. */
        public int bottom() {
            return this.y + this.height;
        }

    }

    /** Describes serialized {@code SharedAnalysisIndex} structure consumed by the AFMA creator. */
    protected record SharedAnalysisIndex(boolean identical, int changedPixelCount,
                                         @Nullable AfmaRect differenceBounds,
                                         @NotNull int[] differenceRowFirstDirtyX,
                                         @NotNull int[] differenceRowLastDirtyX,
                                         @NotNull int[] dirtyPrefixSum,
                                         @NotNull int[] nextNonOpaquePrefixSum,
                                         @NotNull long[] colorErrorPrefixSum,
                                         @NotNull long[] alphaErrorPrefixSum,
                                         @NotNull byte[] visibleColorMaxByRow,
                                         @NotNull byte[] alphaMaxByRow,
                                         @NotNull byte[] visibleColorPrefixMaxByPixel,
                                         @NotNull byte[] alphaPrefixMaxByPixel,
                                         @NotNull byte[] visibleColorSuffixMaxByPixel,
                                         @NotNull byte[] alphaSuffixMaxByPixel,
                                         @NotNull PerceptualDriftMetrics perceptualDriftMetrics) {
    }

    /** Models {@code TileGridSummary} state used by the AFMA creator. */
    public static final class TileGridSummary {

        /** Current tile size measured or selected by this AFMA creator instance. */
        protected final int tileSize;
        /** Current tile count x state for this AFMA creator instance. */
        protected final int tileCountX;
        /** Current tile count y state for this AFMA creator instance. */
        protected final int tileCountY;
        /** Holds the tileStatsByIndex collection used by this AFMA creator instance. */
        @NotNull
        protected final TileStats[] tileStatsByIndex;

        /** Initializes a new {@code TileGridSummary} for AFMA creator use. */
        protected TileGridSummary(int tileSize, int tileCountX, int tileCountY, @NotNull TileStats[] tileStatsByIndex) {
            this.tileSize = tileSize;
            this.tileCountX = tileCountX;
            this.tileCountY = tileCountY;
            this.tileStatsByIndex = tileStatsByIndex;
        }

        /** Calculates the tile size value for the AFMA creator. */
        public int tileSize() {
            return this.tileSize;
        }

        /** Calculates the tile count x value for the AFMA creator. */
        public int tileCountX() {
            return this.tileCountX;
        }

        /** Calculates the tile count y value for the AFMA creator. */
        public int tileCountY() {
            return this.tileCountY;
        }

        /** Returns the tile stats produced by the AFMA creator. */
        @NotNull
        public TileStats tileStats(int tileX, int tileY) {
            if ((tileX < 0) || (tileY < 0) || (tileX >= this.tileCountX) || (tileY >= this.tileCountY)) {
                throw new IndexOutOfBoundsException("AFMA tile coordinates out of bounds");
            }
            return this.tileStatsByIndex[(tileY * this.tileCountX) + tileX];
        }

    }

    /** Carries {@code ChangedTileGrid} data between validated stages of the AFMA creator. */
    public static final class ChangedTileGrid {

        /** Current tile size measured or selected by this AFMA creator instance. */
        protected final int tileSize;
        /** Current tile count x state for this AFMA creator instance. */
        protected final int tileCountX;
        /** Current tile count y state for this AFMA creator instance. */
        protected final int tileCountY;
        /** Current changed tile count measured or selected by this AFMA creator instance. */
        protected final int changedTileCount;
        /** Whether changed tiles by index currently applies to this AFMA creator instance. */
        @NotNull
        protected final boolean[] changedTilesByIndex;

        /** Initializes a new {@code ChangedTileGrid} for AFMA creator use. */
        protected ChangedTileGrid(int tileSize, int tileCountX, int tileCountY, int changedTileCount,
                                  @NotNull boolean[] changedTilesByIndex) {
            this.tileSize = tileSize;
            this.tileCountX = tileCountX;
            this.tileCountY = tileCountY;
            this.changedTileCount = changedTileCount;
            this.changedTilesByIndex = changedTilesByIndex;
        }

        /** Calculates the tile size value for the AFMA creator. */
        public int tileSize() {
            return this.tileSize;
        }

        /** Calculates the tile count x value for the AFMA creator. */
        public int tileCountX() {
            return this.tileCountX;
        }

        /** Calculates the tile count y value for the AFMA creator. */
        public int tileCountY() {
            return this.tileCountY;
        }

        /** Returns the changed tile count produced by the AFMA creator. */
        public int changedTileCount() {
            return this.changedTileCount;
        }

        /** Returns whether tile changed. */
        public boolean tileChanged(int tileX, int tileY) {
            if ((tileX < 0) || (tileY < 0) || (tileX >= this.tileCountX) || (tileY >= this.tileCountY)) {
                throw new IndexOutOfBoundsException("AFMA tile coordinates out of bounds");
            }
            return this.changedTilesByIndex[(tileY * this.tileCountX) + tileX];
        }

    }

    /** Reports {@code RegionDiffAnalysis} measurements produced by the AFMA creator. */
    public static final class RegionDiffAnalysis {

        /** Decoded canvas width in pixels for this AFMA creator instance. */
        protected final int width;
        /** Decoded canvas height in pixels for this AFMA creator instance. */
        protected final int height;
        /** Current changed pixel count measured or selected by this AFMA creator instance. */
        protected final int changedPixelCount;
        /** Whether include alpha currently applies to this AFMA creator instance. */
        protected final boolean includeAlpha;
        /** Holds the predictedColors collection used by this AFMA creator instance. */
        @NotNull
        protected final int[] predictedColors;
        /** Holds the currentColors collection used by this AFMA creator instance. */
        @NotNull
        protected final int[] currentColors;
        /** Current changed pixel mask state for this AFMA creator instance. */
        @NotNull
        protected final BitSet changedPixelMask;

        /** Initializes a new {@code RegionDiffAnalysis} for AFMA creator use. */
        protected RegionDiffAnalysis(int width, int height, int changedPixelCount, boolean includeAlpha,
                                     @NotNull int[] predictedColors, @NotNull int[] currentColors,
                                     @NotNull BitSet changedPixelMask) {
            this.width = width;
            this.height = height;
            this.changedPixelCount = changedPixelCount;
            this.includeAlpha = includeAlpha;
            this.predictedColors = predictedColors;
            this.currentColors = currentColors;
            this.changedPixelMask = changedPixelMask;
        }

        /** Returns the width used by the AFMA creator. */
        public int width() {
            return this.width;
        }

        /** Returns the height used by the AFMA creator. */
        public int height() {
            return this.height;
        }

        /** Calculates the pixel count value for the AFMA creator. */
        public int pixelCount() {
            return this.width * this.height;
        }

        /** Returns the changed pixel count produced by the AFMA creator. */
        public int changedPixelCount() {
            return this.changedPixelCount;
        }

        /** Returns whether include alpha. */
        public boolean includeAlpha() {
            return this.includeAlpha;
        }

        /** Calculates the predicted colors value for the AFMA creator. */
        @NotNull
        public int[] predictedColors() {
            return this.predictedColors;
        }

        /** Returns the current colors produced by the AFMA creator. */
        @NotNull
        public int[] currentColors() {
            return this.currentColors;
        }

        /** Copies the changed pixels to for the AFMA creator. */
        public void copyChangedPixelsTo(@NotNull int[] changedIndices, @NotNull int[] predictedColors, @NotNull int[] currentColors) {
            if ((changedIndices.length < this.changedPixelCount)
                    || (predictedColors.length < this.changedPixelCount)
                    || (currentColors.length < this.changedPixelCount)) {
                throw new IllegalArgumentException("AFMA sparse residual buffers are smaller than the cached changed pixel analysis");
            }

            int changedOffset = 0;
            for (int changedPixelIndex = this.changedPixelMask.nextSetBit(0);
                 (changedPixelIndex >= 0) && (changedOffset < this.changedPixelCount);
                 changedPixelIndex = this.changedPixelMask.nextSetBit(changedPixelIndex + 1)) {
                changedIndices[changedOffset] = changedPixelIndex;
                predictedColors[changedOffset] = this.predictedColors[changedPixelIndex];
                currentColors[changedOffset] = this.currentColors[changedPixelIndex];
                changedOffset++;
            }
            if (changedOffset != this.changedPixelCount) {
                throw new IllegalStateException("AFMA changed pixel cache drifted out of sync with its recorded changed pixel count");
            }
        }

    }

    /** Reports {@code TileStats} measurements produced by the AFMA creator. */
    public record TileStats(long previousHash, long nextHash, int changedPixelCount, long colorErrorSum,
                            long alphaErrorSum, int maxVisibleColorDelta, int maxAlphaDelta,
                            boolean nextHasNonOpaquePixels) {

        /** Returns whether identical. */
        public boolean isIdentical() {
            return this.changedPixelCount <= 0;
        }

    }

}
