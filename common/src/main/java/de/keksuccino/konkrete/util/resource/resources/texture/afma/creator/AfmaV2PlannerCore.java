package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.CloseableUtils;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaAlphaResidualMode;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaBinIntraPayloadHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaBlockInter;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaBlockInterPayloadHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaContainerV2;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaMultiCopy;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaPatchRegion;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaPayloadArchiveLayout;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaPayloadMetricsHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaRect;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaResidualCodec;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaResidualPayload;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaResidualPayloadHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaSparseLayoutCodec;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaSparsePayload;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaSparsePayloadHelper;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaStoredPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

final class AfmaV2PlannerCore {

    /** Lower bound for sparse delta changed pixels accepted by the AFMA creator. */
    protected static final int MIN_SPARSE_DELTA_CHANGED_PIXELS = 1;
    /** Upper bound for sparse delta changed density enforced by the AFMA creator. */
    protected static final double MAX_SPARSE_DELTA_CHANGED_DENSITY = 0.75D;
    /** Configured block inter tile size used by the AFMA creator. */
    protected static final int BLOCK_INTER_TILE_SIZE = 16;
    /** Lower bound for parallel block inter tiles accepted by the AFMA creator. */
    protected static final int MIN_PARALLEL_BLOCK_INTER_TILES = 24;
    /** Upper bound for block inter motion vectors enforced by the AFMA creator. */
    protected static final int MAX_BLOCK_INTER_MOTION_VECTORS = 5;
    /** Lower bound for block inter region area accepted by the AFMA creator. */
    protected static final long BLOCK_INTER_MIN_REGION_AREA = (long) BLOCK_INTER_TILE_SIZE * BLOCK_INTER_TILE_SIZE * 3L;
    /** Decision threshold for the block inter required savings ratio. */
    protected static final double BLOCK_INTER_REQUIRED_SAVINGS_RATIO = 0.96D;
    /** Decision threshold for the strong full frame delta skip changed ratio. */
    protected static final double STRONG_FULL_FRAME_DELTA_SKIP_CHANGED_RATIO = 0.35D;
    /** Lower bound for strong delta skip motion savings bytes accepted by the AFMA creator. */
    protected static final long STRONG_DELTA_SKIP_MOTION_MIN_SAVINGS_BYTES = 128L * 1024L;
    /** Lower bound for strong delta skip motion savings ratio accepted by the AFMA creator. */
    protected static final double STRONG_DELTA_SKIP_MOTION_MIN_SAVINGS_RATIO = 0.14D;
    /** Lower bound for strong delta skip full savings bytes accepted by the AFMA creator. */
    protected static final long STRONG_DELTA_SKIP_FULL_MIN_SAVINGS_BYTES = 192L * 1024L;
    /** Lower bound for strong delta skip full savings ratio accepted by the AFMA creator. */
    protected static final double STRONG_DELTA_SKIP_FULL_MIN_SAVINGS_RATIO = 0.18D;
    /** Upper bound for strong delta skip full changed ratio enforced by the AFMA creator. */
    protected static final double STRONG_DELTA_SKIP_FULL_MAX_CHANGED_RATIO = 0.68D;
    /** Scoring or hashing coefficient for perceptual full estimate scale in the AFMA creator. */
    protected static final double PERCEPTUAL_FULL_ESTIMATE_SCALE = 0.45D;
    /** Scoring or hashing coefficient for lossless full estimate scale in the AFMA creator. */
    protected static final double LOSSLESS_FULL_ESTIMATE_SCALE = 0.60D;
    /** Byte budget for family switch margin in the AFMA creator. */
    protected static final long FAMILY_SWITCH_MARGIN_BYTES = 48L;
    /** Decision threshold for the family switch margin ratio. */
    protected static final double FAMILY_SWITCH_MARGIN_RATIO = 0.05D;
    /** Search or sampling budget for full reference probe windows per sequence in the AFMA creator. */
    protected static final int FULL_REFERENCE_PROBE_WINDOWS_PER_SEQUENCE = 3;
    /** Search or sampling budget for full reference probe frames per window in the AFMA creator. */
    protected static final int FULL_REFERENCE_PROBE_FRAMES_PER_WINDOW = 3;
    /** Lower bound for full reference probe mixed wins accepted by the AFMA creator. */
    protected static final int FULL_REFERENCE_PROBE_MIN_MIXED_WINS = 2;
    /** Byte budget for full reference probe required savings in the AFMA creator. */
    protected static final long FULL_REFERENCE_PROBE_REQUIRED_SAVINGS_BYTES = 48L * 1024L;
    /** Decision threshold for the full reference probe required savings ratio. */
    protected static final double FULL_REFERENCE_PROBE_REQUIRED_SAVINGS_RATIO = 0.06D;
    /** Search or sampling budget for full reference mixed probe interval frames in the AFMA creator. */
    protected static final int FULL_REFERENCE_MIXED_PROBE_INTERVAL_FRAMES = 4;
    /** Search or sampling budget for full reference mixed probe end frames in the AFMA creator. */
    protected static final int FULL_REFERENCE_MIXED_PROBE_END_FRAMES = 16;
    /** Lower bound for full reference mixed probe bounds ratio accepted by the AFMA creator. */
    protected static final double FULL_REFERENCE_MIXED_PROBE_MIN_BOUNDS_RATIO = 0.94D;
    /** Upper bound for full reference mixed probe changed ratio enforced by the AFMA creator. */
    protected static final double FULL_REFERENCE_MIXED_PROBE_MAX_CHANGED_RATIO = 0.30D;
    /** Decision threshold for the full reference mixed probe strong changed ratio. */
    protected static final double FULL_REFERENCE_MIXED_PROBE_STRONG_CHANGED_RATIO = 0.18D;
    /** Configured quick reference probe pair count used by the AFMA creator. */
    protected static final int QUICK_REFERENCE_PROBE_PAIR_COUNT = 4;
    /** Upper bound for quick reference mixed average changed ratio enforced by the AFMA creator. */
    protected static final double QUICK_REFERENCE_MIXED_MAX_AVERAGE_CHANGED_RATIO = 0.18D;
    /** Lower bound for quick reference mixed average bounds ratio accepted by the AFMA creator. */
    protected static final double QUICK_REFERENCE_MIXED_MIN_AVERAGE_BOUNDS_RATIO = 0.94D;
    /** Lower bound for quick reference full average changed ratio accepted by the AFMA creator. */
    protected static final double QUICK_REFERENCE_FULL_MIN_AVERAGE_CHANGED_RATIO = 0.25D;
    /** Lower bound for quick reference full average bounds ratio accepted by the AFMA creator. */
    protected static final double QUICK_REFERENCE_FULL_MIN_AVERAGE_BOUNDS_RATIO = 0.75D;
    /** Upper bound for quick reference full average changed ratio enforced by the AFMA creator. */
    protected static final double QUICK_REFERENCE_FULL_MAX_AVERAGE_CHANGED_RATIO = 0.06D;
    /** Upper bound for quick reference full average bounds ratio enforced by the AFMA creator. */
    protected static final double QUICK_REFERENCE_FULL_MAX_AVERAGE_BOUNDS_RATIO = 0.30D;
    /** Search or sampling budget for motion family warmup attempts in the AFMA creator. */
    protected static final int MOTION_FAMILY_WARMUP_ATTEMPTS = 6;
    /** Search or sampling budget for motion family probe attempts in the AFMA creator. */
    protected static final int MOTION_FAMILY_PROBE_ATTEMPTS = 1;
    /** Search or sampling budget for motion family probe interval frames in the AFMA creator. */
    protected static final int MOTION_FAMILY_PROBE_INTERVAL_FRAMES = 24;
    /** Byte budget for packed archive rescoring clear savings in the AFMA creator. */
    protected static final long PACKED_ARCHIVE_RESCORING_CLEAR_SAVINGS_BYTES = 128L * 1024L;
    /** Decision threshold for the packed archive rescoring clear savings ratio. */
    protected static final double PACKED_ARCHIVE_RESCORING_CLEAR_SAVINGS_RATIO = 0.10D;
    /** Upper bound for packed archive rescoring changed ratio enforced by the AFMA creator. */
    protected static final double PACKED_ARCHIVE_RESCORING_MAX_CHANGED_RATIO = 0.25D;

    /** Current frame normalizer state for this AFMA creator instance. */
    @NotNull
    protected final AfmaFrameNormalizer frameNormalizer;
    /** Current residual planner workspace state for this AFMA creator instance. */
    @NotNull
    protected final ThreadLocal<ResidualPlannerWorkspace> residualPlannerWorkspace = ThreadLocal.withInitial(ResidualPlannerWorkspace::new);

    AfmaV2PlannerCore(@NotNull AfmaFrameNormalizer frameNormalizer) {
        this.frameNormalizer = Objects.requireNonNull(frameNormalizer);
    }

    /** Plans resource state for the AFMA creator. */
    @NotNull
    public AfmaEncodePlan plan(@NotNull AfmaSourceSequence mainSequence, @Nullable AfmaSourceSequence introSequence,
                               @NotNull AfmaEncodeOptions options,
                               @Nullable BooleanSupplier cancellationRequested,
                               @Nullable AfmaEncodePlanner.ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(mainSequence);
        Objects.requireNonNull(options);

        AfmaSourceSequence intro = (introSequence != null) ? introSequence : AfmaSourceSequence.empty();
        options.validateForCounts(mainSequence.size(), intro.size());
        if (options.isFullFrameReferencePlanEnabled()) {
            reportProgress(progressListener, "Profiling smallest-file mode...", 0.04D);
            boolean useFullIntro = this.shouldUseFullFrameReferencePlanForSequence(intro, true, options, cancellationRequested);
            reportProgress(progressListener, "Profiling smallest-file mode...", 0.06D);
            boolean useFullMain = this.shouldUseFullFrameReferencePlanForSequence(mainSequence, false, options, cancellationRequested);
            reportProgress(progressListener, "Profiling smallest-file mode...", 0.08D);
            if (useFullIntro || useFullMain) {
                if (useFullIntro && useFullMain) {
                    return this.buildFullFrameReferencePlan(mainSequence, intro, options, cancellationRequested, progressListener);
                }
                return this.buildAdaptiveReferencePlan(mainSequence, intro, useFullMain, useFullIntro, options, cancellationRequested, progressListener);
            }
        }
        return this.buildMixedPlan(mainSequence, intro, options, cancellationRequested, progressListener);
    }

    /** Builds the executor for the AFMA creator. */
    @Nullable
    protected ExecutorService createExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        if (processors <= 1) {
            return null;
        }
        int threads = Math.max(1, processors - 1);
        AtomicInteger counter = new AtomicInteger(0);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "Konkrete-AfmaV2Planner-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(threads, threadFactory);
    }

    /** Returns whether use full frame reference plan for sequence. */
    protected boolean shouldUseFullFrameReferencePlanForSequence(@NotNull AfmaSourceSequence sequence,
                                                                 boolean introSequence,
                                                                 @NotNull AfmaEncodeOptions options,
                                                                 @Nullable BooleanSupplier cancellationRequested) throws IOException {
        if (sequence.size() <= 1) {
            return true;
        }

        QuickReferenceDecision quickDecision = this.tryQuickReferencePlanDecision(sequence, cancellationRequested);
        if (quickDecision != QuickReferenceDecision.UNKNOWN) {
            return quickDecision == QuickReferenceDecision.FULL_REFERENCE;
        }

        ExecutorService executor = this.createExecutor();
        AfmaFastPixelBufferPool pixelBufferPool = new AfmaFastPixelBufferPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        try {
            checkCancelled(cancellationRequested);
            AfmaRectCopyDetector copyDetector = new AfmaRectCopyDetector(options.getMaxCopySearchDistance(), options.getMaxCandidateAxisOffsets());
            int sampledFrames = 0;
            int mixedWins = 0;
            long selectedBytes = 0L;
            long fullReferenceBytes = 0L;
            for (Integer windowStartIndex : this.buildFullReferenceProbeWindowStartIndexes(sequence.size())) {
                int sampleFrameCount = Math.min(FULL_REFERENCE_PROBE_FRAMES_PER_WINDOW, sequence.size() - windowStartIndex);
                if (sampleFrameCount <= 0) {
                    continue;
                }
                ReferencePlanProbeResult probe = this.probeFullReferenceWindow(
                        sequence,
                        introSequence,
                        windowStartIndex,
                        sampleFrameCount,
                        options,
                        copyDetector,
                        pixelBufferPool,
                        executor,
                        cancellationRequested
                );
                sampledFrames += probe.sampledFrames();
                mixedWins += probe.mixedWins();
                selectedBytes += probe.selectedArchiveBytes();
                fullReferenceBytes += probe.fullReferenceArchiveBytes();
            }
            if (sampledFrames <= 1) {
                return true;
            }

            long requiredSavings = Math.max(
                    FULL_REFERENCE_PROBE_REQUIRED_SAVINGS_BYTES,
                    (long) Math.ceil(fullReferenceBytes * FULL_REFERENCE_PROBE_REQUIRED_SAVINGS_RATIO)
            );
            boolean mixedShowsRepeatedWins = mixedWins >= Math.max(FULL_REFERENCE_PROBE_MIN_MIXED_WINS, sampledFrames / 4);
            boolean mixedShowsMeaningfulSavings = (fullReferenceBytes - selectedBytes) >= requiredSavings;
            return !mixedShowsRepeatedWins && !mixedShowsMeaningfulSavings;
        } finally {
            pixelBufferPool.clear();
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    /** Attempts the quick reference plan decision without committing partial AFMA creator state. */
    @NotNull
    protected QuickReferenceDecision tryQuickReferencePlanDecision(@NotNull AfmaSourceSequence sequence,
                                                                   @Nullable BooleanSupplier cancellationRequested) throws IOException {
        if (sequence.size() <= 1) {
            return QuickReferenceDecision.FULL_REFERENCE;
        }

        List<Integer> probeIndexes = this.buildQuickReferenceProbeIndexes(sequence.size());
        if (probeIndexes.isEmpty()) {
            return QuickReferenceDecision.UNKNOWN;
        }

        AfmaFastPixelBufferPool pixelBufferPool = new AfmaFastPixelBufferPool(Math.min(QUICK_REFERENCE_PROBE_PAIR_COUNT + 1, Math.max(2, Runtime.getRuntime().availableProcessors())));
        double changedRatioSum = 0D;
        double boundsRatioSum = 0D;
        int measuredPairs = 0;
        try {
            for (int currentFrameIndex : probeIndexes) {
                checkCancelled(cancellationRequested);
                File previousFile = Objects.requireNonNull(sequence.getFrame(currentFrameIndex - 1));
                File currentFile = Objects.requireNonNull(sequence.getFrame(currentFrameIndex));
                AfmaPixelFrame previousFrame = this.frameNormalizer.loadFrame(previousFile, pixelBufferPool);
                AfmaPixelFrame currentFrame = this.frameNormalizer.loadFrame(currentFile, pixelBufferPool);
                try {
                    if ((previousFrame.getWidth() != currentFrame.getWidth()) || (previousFrame.getHeight() != currentFrame.getHeight())) {
                        return QuickReferenceDecision.UNKNOWN;
                    }

                    AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(previousFrame, currentFrame);
                    long frameArea = (long) currentFrame.getWidth() * (long) currentFrame.getHeight();
                    if (frameArea <= 0L) {
                        continue;
                    }

                    changedRatioSum += (double) pairAnalysis.changedPixelCount() / (double) frameArea;
                    AfmaRect differenceBounds = pairAnalysis.differenceBounds();
                    boundsRatioSum += (differenceBounds != null) ? ((double) differenceBounds.area() / (double) frameArea) : 0D;
                    measuredPairs++;
                } finally {
                    CloseableUtils.closeQuietly(previousFrame);
                    CloseableUtils.closeQuietly(currentFrame);
                }
            }
        } finally {
            pixelBufferPool.clear();
        }

        if (measuredPairs <= 0) {
            return QuickReferenceDecision.UNKNOWN;
        }

        double averageChangedRatio = changedRatioSum / (double) measuredPairs;
        double averageBoundsRatio = boundsRatioSum / (double) measuredPairs;
        if ((averageBoundsRatio >= QUICK_REFERENCE_MIXED_MIN_AVERAGE_BOUNDS_RATIO)
                && (averageChangedRatio <= QUICK_REFERENCE_MIXED_MAX_AVERAGE_CHANGED_RATIO)) {
            return QuickReferenceDecision.MIXED;
        }
        if ((averageChangedRatio >= QUICK_REFERENCE_FULL_MIN_AVERAGE_CHANGED_RATIO)
                && (averageBoundsRatio >= QUICK_REFERENCE_FULL_MIN_AVERAGE_BOUNDS_RATIO)) {
            return QuickReferenceDecision.FULL_REFERENCE;
        }
        if ((averageChangedRatio <= QUICK_REFERENCE_FULL_MAX_AVERAGE_CHANGED_RATIO)
                && (averageBoundsRatio <= QUICK_REFERENCE_FULL_MAX_AVERAGE_BOUNDS_RATIO)) {
            return QuickReferenceDecision.FULL_REFERENCE;
        }
        return QuickReferenceDecision.UNKNOWN;
    }

    /** Builds the quick reference probe indexes for the AFMA creator. */
    @NotNull
    protected List<Integer> buildQuickReferenceProbeIndexes(int sequenceSize) {
        if (sequenceSize <= 1) {
            return List.of();
        }

        int maxIndex = sequenceSize - 1;
        int pairCount = Math.min(QUICK_REFERENCE_PROBE_PAIR_COUNT, maxIndex);
        LinkedHashSet<Integer> indexes = new LinkedHashSet<>(pairCount);
        indexes.add(1);
        indexes.add(maxIndex);
        if (pairCount > 2) {
            for (int probeIndex = 1; probeIndex < (pairCount - 1); probeIndex++) {
                int index = (int) Math.round((double) maxIndex * ((double) probeIndex / (double) (pairCount - 1)));
                indexes.add(Math.max(1, Math.min(maxIndex, index)));
            }
        }
        ArrayList<Integer> orderedIndexes = new ArrayList<>(indexes);
        Collections.sort(orderedIndexes);
        return List.copyOf(orderedIndexes);
    }

    /** Returns whether probe mixed candidate in reference sequence. */
    protected boolean shouldProbeMixedCandidateInReferenceSequence(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                   int frameIndex,
                                                                   int sequenceSize) {
        AfmaRect differenceBounds = pairAnalysis.differenceBounds();
        if (differenceBounds == null) {
            return false;
        }

        long frameArea = (long) pairAnalysis.width * (long) pairAnalysis.height;
        if (frameArea <= 0L) {
            return false;
        }

        double boundsRatio = (double) differenceBounds.area() / (double) frameArea;
        if (boundsRatio < FULL_REFERENCE_MIXED_PROBE_MIN_BOUNDS_RATIO) {
            return false;
        }

        double changedRatio = (double) pairAnalysis.changedPixelCount() / (double) frameArea;
        if (changedRatio > FULL_REFERENCE_MIXED_PROBE_MAX_CHANGED_RATIO) {
            return false;
        }

        if (changedRatio <= FULL_REFERENCE_MIXED_PROBE_STRONG_CHANGED_RATIO) {
            return true;
        }
        if (frameIndex >= Math.max(1, sequenceSize - FULL_REFERENCE_MIXED_PROBE_END_FRAMES)) {
            return true;
        }
        return (frameIndex % FULL_REFERENCE_MIXED_PROBE_INTERVAL_FRAMES) == 0;
    }

    /** Returns whether switch reference sequence to mixed. */
    protected boolean shouldSwitchReferenceSequenceToMixed(@NotNull FrameCandidate selectedCandidate) {
        return (selectedCandidate.kind() != CandidateKind.FULL) && (selectedCandidate.kind() != CandidateKind.SAME);
    }

    /** Builds the full reference probe window start indexes for the AFMA creator. */
    @NotNull
    protected List<Integer> buildFullReferenceProbeWindowStartIndexes(int sequenceSize) {
        if (sequenceSize <= FULL_REFERENCE_PROBE_FRAMES_PER_WINDOW) {
            return List.of(0);
        }

        int maxStartIndex = Math.max(0, sequenceSize - FULL_REFERENCE_PROBE_FRAMES_PER_WINDOW);
        int requestedWindowCount = Math.min(
                FULL_REFERENCE_PROBE_WINDOWS_PER_SEQUENCE,
                Math.max(1, sequenceSize - FULL_REFERENCE_PROBE_FRAMES_PER_WINDOW + 1)
        );
        LinkedHashSet<Integer> startIndexes = new LinkedHashSet<>();
        startIndexes.add(0);
        if (requestedWindowCount > 1) {
            startIndexes.add(maxStartIndex);
        }
        for (int windowIndex = 1; (windowIndex < (requestedWindowCount - 1)) && (maxStartIndex > 0); windowIndex++) {
            int startIndex = (int) Math.round((double) maxStartIndex * ((double) windowIndex / (double) (requestedWindowCount - 1)));
            startIndexes.add(Math.max(0, Math.min(maxStartIndex, startIndex)));
        }
        ArrayList<Integer> sortedStartIndexes = new ArrayList<>(startIndexes);
        Collections.sort(sortedStartIndexes);
        return List.copyOf(sortedStartIndexes);
    }

    /** Probes the full reference window without committing AFMA creator state. */
    @NotNull
    protected ReferencePlanProbeResult probeFullReferenceWindow(@NotNull AfmaSourceSequence sequence,
                                                                boolean introSequence,
                                                                int startFrameIndex,
                                                                int sampleFrameCount,
                                                                @NotNull AfmaEncodeOptions options,
                                                                @NotNull AfmaRectCopyDetector copyDetector,
                                                                @NotNull AfmaFastPixelBufferPool pixelBufferPool,
                                                                @Nullable ExecutorService executor,
                                                                @Nullable BooleanSupplier cancellationRequested) throws IOException {
        if (sequence.isEmpty() || sampleFrameCount <= 0) {
            return ReferencePlanProbeResult.empty();
        }

        PayloadInterner payloadInterner = new PayloadInterner();
        ArrayList<AfmaFrameDescriptor> descriptors = new ArrayList<>();
        AfmaPixelFrame previousEncodedFrame = null;
        QualityBudgetState qualityBudgetState = QualityBudgetState.lossless();
        int framesSinceKeyframe = 0;
        int sampledFrames = 0;
        int mixedWins = 0;
        long selectedArchiveBytes = 0L;
        long fullReferenceArchiveBytes = 0L;
        Integer expectedWidth = null;
        Integer expectedHeight = null;
        MotionFamilyUsageTracker motionFamilyUsageTracker = new MotionFamilyUsageTracker();
        try {
            int firstSampleFrameIndex = Math.max(0, Math.min(sequence.size() - 1, startFrameIndex));
            int limit = Math.min(sequence.size(), firstSampleFrameIndex + Math.max(0, sampleFrameCount));
            if ((firstSampleFrameIndex > 0) && (firstSampleFrameIndex < sequence.size())) {
                AfmaPixelFrame warmupSourceFrame = this.frameNormalizer.loadFrame(
                        Objects.requireNonNull(sequence.getFrame(firstSampleFrameIndex - 1)),
                        pixelBufferPool
                );
                AfmaPixelFrame warmupOutputFrame = warmupSourceFrame;
                boolean keepWarmupSourceFrame = false;
                try {
                    expectedWidth = warmupSourceFrame.getWidth();
                    expectedHeight = warmupSourceFrame.getHeight();
                    FrameCandidate warmupFullCandidate = this.createMeasuredFullCandidate(
                            warmupSourceFrame,
                            warmupSourceFrame,
                            introSequence,
                            firstSampleFrameIndex - 1,
                            options,
                            options.isPerceptualBinIntraEnabled()
                    );
                    AfmaFrameDescriptor warmupDescriptor = payloadInterner.intern(warmupFullCandidate);
                    descriptors.add(warmupDescriptor);
                    qualityBudgetState = qualityBudgetState.advance(warmupFullCandidate);
                    previousEncodedFrame = warmupFullCandidate.outputFrame();
                    warmupOutputFrame = previousEncodedFrame;
                    keepWarmupSourceFrame = previousEncodedFrame == warmupSourceFrame;
                } finally {
                    if (!keepWarmupSourceFrame && (warmupSourceFrame != warmupOutputFrame)) {
                        CloseableUtils.closeQuietly(warmupSourceFrame);
                    }
                }
            }

            for (int frameIndex = firstSampleFrameIndex; frameIndex < limit; frameIndex++) {
                checkCancelled(cancellationRequested);
                AfmaPixelFrame sourceFrame = this.frameNormalizer.loadFrame(Objects.requireNonNull(sequence.getFrame(frameIndex)), pixelBufferPool);
                AfmaPixelFrame workingFrame = sourceFrame;
                AfmaPixelFrame nextPreviousEncodedFrame = previousEncodedFrame;
                boolean keepSourceFrame = false;
                boolean keepWorkingFrame = false;
                try {
                    if (expectedWidth == null) {
                        expectedWidth = sourceFrame.getWidth();
                        expectedHeight = sourceFrame.getHeight();
                    } else if ((sourceFrame.getWidth() != expectedWidth) || (sourceFrame.getHeight() != expectedHeight)) {
                        throw new IOException("AFMA v2 probe frame dimensions do not match the sequence dimensions");
                    }

                    boolean bootstrapFrame = previousEncodedFrame == null;
                    if (!bootstrapFrame && options.isNearLosslessEnabled()) {
                        workingFrame = this.applyNearLosslessTemporalMerge(Objects.requireNonNull(previousEncodedFrame), sourceFrame, options.getNearLosslessMaxChannelDelta());
                    }

                    boolean allowPerceptual = bootstrapFrame
                            ? options.isPerceptualBinIntraEnabled()
                            : this.shouldAllowPerceptualContinuation(framesSinceKeyframe, options);
                    FrameCandidate fullCandidate = this.createMeasuredFullCandidate(
                            sourceFrame,
                            workingFrame,
                            introSequence,
                            frameIndex,
                            options,
                            allowPerceptual
                    );
                    fullReferenceArchiveBytes += fullCandidate.totalArchiveBytes(payloadInterner);

                    FrameDecision decision = bootstrapFrame
                            ? new FrameDecision(fullCandidate, fullCandidate.outputFrame())
                            : this.planFrame(
                            previousEncodedFrame,
                            sourceFrame,
                            workingFrame,
                            introSequence,
                            descriptors,
                            List.of(),
                            frameIndex,
                            framesSinceKeyframe,
                            qualityBudgetState,
                            options,
                            copyDetector,
                            payloadInterner,
                            motionFamilyUsageTracker,
                            executor,
                            cancellationRequested
                    );
                    FrameCandidate selectedCandidate = decision.candidate();
                    selectedArchiveBytes += selectedCandidate.totalArchiveBytes(payloadInterner);
                    if ((selectedCandidate.kind() != CandidateKind.FULL) && (selectedCandidate.kind() != CandidateKind.SAME)) {
                        mixedWins++;
                    }

                    qualityBudgetState = qualityBudgetState.advance(selectedCandidate);
                    if ((selectedCandidate.kind() != CandidateKind.SAME) || !options.isDuplicateFrameElision()) {
                        AfmaFrameDescriptor finalizedDescriptor = payloadInterner.intern(selectedCandidate);
                        descriptors.add(finalizedDescriptor);
                        if (finalizedDescriptor.isKeyframe()) {
                            framesSinceKeyframe = 0;
                        } else {
                            framesSinceKeyframe++;
                        }
                    }

                    nextPreviousEncodedFrame = decision.outputFrame();
                    keepSourceFrame = nextPreviousEncodedFrame == sourceFrame;
                    keepWorkingFrame = nextPreviousEncodedFrame == workingFrame;
                    sampledFrames++;
                } finally {
                    if (!keepWorkingFrame && (workingFrame != sourceFrame) && (workingFrame != nextPreviousEncodedFrame)) {
                        CloseableUtils.closeQuietly(workingFrame);
                    }
                    if (!keepSourceFrame && (sourceFrame != nextPreviousEncodedFrame)) {
                        CloseableUtils.closeQuietly(sourceFrame);
                    }
                    if (previousEncodedFrame != nextPreviousEncodedFrame) {
                        CloseableUtils.closeQuietly(previousEncodedFrame);
                    }
                    previousEncodedFrame = nextPreviousEncodedFrame;
                }
            }
            return new ReferencePlanProbeResult(List.copyOf(descriptors), sampledFrames, mixedWins, selectedArchiveBytes, fullReferenceArchiveBytes);
        } finally {
            CloseableUtils.closeQuietly(previousEncodedFrame);
            this.closeInternedPayloads(payloadInterner);
        }
    }

    /** Builds the adaptive reference plan for the AFMA creator. */
    @NotNull
    protected AfmaEncodePlan buildAdaptiveReferencePlan(@NotNull AfmaSourceSequence mainSequence,
                                                        @NotNull AfmaSourceSequence introSequence,
                                                        boolean useFullMainPlan,
                                                        boolean useFullIntroPlan,
                                                        @NotNull AfmaEncodeOptions options,
                                                        @Nullable BooleanSupplier cancellationRequested,
                                                        @Nullable AfmaEncodePlanner.ProgressListener progressListener) throws IOException {
        ExecutorService executor = this.createExecutor();
        AfmaFastPixelBufferPool pixelBufferPool = new AfmaFastPixelBufferPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        try {
            checkCancelled(cancellationRequested);
            AfmaSourceSequence dimensionSource = !mainSequence.isEmpty() ? mainSequence : introSequence;
            LoadedDimensionFrame loadedDimension = this.loadDimensionFrame(dimensionSource, pixelBufferPool, cancellationRequested, progressListener);
            Dimension dimension = loadedDimension.dimension();
            AfmaPixelFrame preloadedMainFrame = (dimensionSource == mainSequence) ? loadedDimension.frame() : null;
            AfmaPixelFrame preloadedIntroFrame = (dimensionSource == introSequence) ? loadedDimension.frame() : null;
            int totalFrameCount = Math.max(1, mainSequence.size() + introSequence.size());
            PayloadInterner payloadInterner = new PayloadInterner();
            AfmaRectCopyDetector copyDetector = new AfmaRectCopyDetector(options.getMaxCopySearchDistance(), options.getMaxCandidateAxisOffsets());
            try {
                PlannedSequence plannedIntro = useFullIntroPlan
                        ? this.planFullFrameSequence(
                        introSequence,
                        true,
                        List.of(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        0,
                        totalFrameCount,
                        preloadedIntroFrame
                )
                        : this.planSequence(
                        introSequence,
                        true,
                        List.of(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        0,
                        totalFrameCount,
                        preloadedIntroFrame
                );
                preloadedIntroFrame = null;
                PlannedSequence plannedMain = useFullMainPlan
                        ? this.planFullFrameSequence(
                        mainSequence,
                        false,
                        plannedIntro.frames(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        introSequence.size(),
                        totalFrameCount,
                        preloadedMainFrame
                )
                        : this.planSequence(
                        mainSequence,
                        false,
                        plannedIntro.frames(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        introSequence.size(),
                        totalFrameCount,
                        preloadedMainFrame
                );
                preloadedMainFrame = null;
                return this.buildPlan(dimension, options, plannedMain, plannedIntro, payloadInterner.payloads());
            } finally {
                CloseableUtils.closeQuietly(preloadedIntroFrame);
                CloseableUtils.closeQuietly(preloadedMainFrame);
            }
        } finally {
            this.residualPlannerWorkspace.remove();
            pixelBufferPool.clear();
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    /** Builds the mixed plan for the AFMA creator. */
    @NotNull
    protected AfmaEncodePlan buildMixedPlan(@NotNull AfmaSourceSequence mainSequence,
                                            @NotNull AfmaSourceSequence introSequence,
                                            @NotNull AfmaEncodeOptions options,
                                            @Nullable BooleanSupplier cancellationRequested,
                                            @Nullable AfmaEncodePlanner.ProgressListener progressListener) throws IOException {
        ExecutorService executor = this.createExecutor();
        AfmaFastPixelBufferPool pixelBufferPool = new AfmaFastPixelBufferPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        try {
            checkCancelled(cancellationRequested);
            AfmaSourceSequence dimensionSource = !mainSequence.isEmpty() ? mainSequence : introSequence;
            LoadedDimensionFrame loadedDimension = this.loadDimensionFrame(dimensionSource, pixelBufferPool, cancellationRequested, progressListener);
            Dimension dimension = loadedDimension.dimension();
            AfmaPixelFrame preloadedMainFrame = (dimensionSource == mainSequence) ? loadedDimension.frame() : null;
            AfmaPixelFrame preloadedIntroFrame = (dimensionSource == introSequence) ? loadedDimension.frame() : null;
            int totalFrameCount = Math.max(1, mainSequence.size() + introSequence.size());
            PayloadInterner payloadInterner = new PayloadInterner();
            AfmaRectCopyDetector copyDetector = new AfmaRectCopyDetector(options.getMaxCopySearchDistance(), options.getMaxCandidateAxisOffsets());
            try {
                PlannedSequence plannedIntro = this.planSequence(
                        introSequence,
                        true,
                        List.of(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        0,
                        totalFrameCount,
                        preloadedIntroFrame
                );
                preloadedIntroFrame = null;
                PlannedSequence plannedMain = this.planSequence(
                        mainSequence,
                        false,
                        plannedIntro.frames(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        introSequence.size(),
                        totalFrameCount,
                        preloadedMainFrame
                );
                preloadedMainFrame = null;
                return this.buildPlan(dimension, options, plannedMain, plannedIntro, payloadInterner.payloads());
            } finally {
                CloseableUtils.closeQuietly(preloadedIntroFrame);
                CloseableUtils.closeQuietly(preloadedMainFrame);
            }
        } finally {
            this.residualPlannerWorkspace.remove();
            pixelBufferPool.clear();
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    /** Builds the full frame reference plan for the AFMA creator. */
    @NotNull
    protected AfmaEncodePlan buildFullFrameReferencePlan(@NotNull AfmaSourceSequence mainSequence,
                                                         @NotNull AfmaSourceSequence introSequence,
                                                         @NotNull AfmaEncodeOptions options,
                                                         @Nullable BooleanSupplier cancellationRequested,
                                                         @Nullable AfmaEncodePlanner.ProgressListener progressListener) throws IOException {
        ExecutorService executor = this.createExecutor();
        AfmaFastPixelBufferPool pixelBufferPool = new AfmaFastPixelBufferPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        try {
            checkCancelled(cancellationRequested);
            AfmaSourceSequence dimensionSource = !mainSequence.isEmpty() ? mainSequence : introSequence;
            LoadedDimensionFrame loadedDimension = this.loadDimensionFrame(dimensionSource, pixelBufferPool, cancellationRequested, progressListener);
            Dimension dimension = loadedDimension.dimension();
            AfmaPixelFrame preloadedMainFrame = (dimensionSource == mainSequence) ? loadedDimension.frame() : null;
            AfmaPixelFrame preloadedIntroFrame = (dimensionSource == introSequence) ? loadedDimension.frame() : null;
            int totalFrameCount = Math.max(1, mainSequence.size() + introSequence.size());
            PayloadInterner payloadInterner = new PayloadInterner();
            AfmaRectCopyDetector copyDetector = new AfmaRectCopyDetector(options.getMaxCopySearchDistance(), options.getMaxCandidateAxisOffsets());
            try {
                PlannedSequence plannedIntro = this.planFullFrameSequence(
                        introSequence,
                        true,
                        List.of(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        0,
                        totalFrameCount,
                        preloadedIntroFrame
                );
                preloadedIntroFrame = null;
                PlannedSequence plannedMain = this.planFullFrameSequence(
                        mainSequence,
                        false,
                        plannedIntro.frames(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        introSequence.size(),
                        totalFrameCount,
                        preloadedMainFrame
                );
                preloadedMainFrame = null;
                return this.buildPlan(dimension, options, plannedMain, plannedIntro, payloadInterner.payloads());
            } finally {
                CloseableUtils.closeQuietly(preloadedIntroFrame);
                CloseableUtils.closeQuietly(preloadedMainFrame);
            }
        } finally {
            this.residualPlannerWorkspace.remove();
            pixelBufferPool.clear();
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    /** Plans the full frame sequence for the AFMA creator. */
    @NotNull
    protected PlannedSequence planFullFrameSequence(@NotNull AfmaSourceSequence sequence,
                                                    boolean introSequence,
                                                    @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                                    @NotNull Dimension dimension,
                                                    @NotNull AfmaEncodeOptions options,
                                                    @NotNull AfmaRectCopyDetector copyDetector,
                                                    @NotNull PayloadInterner payloadInterner,
                                                    @NotNull AfmaFastPixelBufferPool pixelBufferPool,
                                                    @Nullable ExecutorService executor,
                                                    @Nullable BooleanSupplier cancellationRequested,
                                                    @Nullable AfmaEncodePlanner.ProgressListener progressListener,
                                                    int startOffset, int totalFrameCount,
                                                    @Nullable AfmaPixelFrame firstFrameOverride) throws IOException {
        List<PlannedTimedFrame> plannedFrames = new ArrayList<>();
        List<AfmaFrameDescriptor> plannedDescriptors = new ArrayList<>();
        if (sequence.isEmpty()) {
            return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence));
        }

        AsyncFrameLoader frameLoader = new AsyncFrameLoader(sequence, dimension, firstFrameOverride, pixelBufferPool, executor, cancellationRequested);
        AfmaPixelFrame previousEncodedFrame = null;
        QualityBudgetState qualityBudgetState = QualityBudgetState.lossless();
        int framesSinceKeyframe = 0;
        boolean mixedPlanningEnabled = false;
        MotionFamilyUsageTracker motionFamilyUsageTracker = new MotionFamilyUsageTracker();
        try {
            for (int frameIndex = 0; frameIndex < sequence.size(); frameIndex++) {
                checkCancelled(cancellationRequested);
                AfmaPixelFrame sourceFrame = frameLoader.takeFrame(frameIndex);
                AfmaPixelFrame workingFrame = sourceFrame;
                AfmaPixelFrame nextPreviousEncodedFrame = previousEncodedFrame;
                boolean keepSourceFrame = false;
                boolean keepWorkingFrame = false;
                try {
                    long frameDelayMs = this.resolveSourceFrameDelay(options, introSequence, frameIndex);
                    reportPlanningFrameProgress(
                            progressListener,
                            "Planning reference-biased",
                            introSequence,
                            frameIndex + 1,
                            sequence.size(),
                            startOffset + frameIndex + 1D,
                            totalFrameCount
                    );

                    boolean allowPerceptual = options.isPerceptualBinIntraEnabled();
                    if ((previousEncodedFrame != null) && options.isNearLosslessEnabled() && allowPerceptual) {
                        workingFrame = this.applyNearLosslessTemporalMerge(previousEncodedFrame, sourceFrame, options.getNearLosslessMaxChannelDelta());
                    }

                    AfmaFramePairAnalysis pairAnalysis = (previousEncodedFrame != null)
                            ? new AfmaFramePairAnalysis(previousEncodedFrame, workingFrame)
                            : null;
                    if ((pairAnalysis != null) && options.isDuplicateFrameElision() && pairAnalysis.isIdentical()) {
                        this.extendPlannedFrameDelay(plannedFrames, frameDelayMs);
                        continue;
                    }

                    FrameDecision frameDecision;
                    if (mixedPlanningEnabled) {
                        frameDecision = this.planFrame(
                                previousEncodedFrame,
                                sourceFrame,
                                workingFrame,
                                introSequence,
                                plannedDescriptors,
                                companionSequenceFrames,
                                frameIndex,
                                framesSinceKeyframe,
                                qualityBudgetState,
                                options,
                                copyDetector,
                                payloadInterner,
                                motionFamilyUsageTracker,
                                executor,
                                cancellationRequested
                        );
                    } else if ((pairAnalysis != null) && this.shouldProbeMixedCandidateInReferenceSequence(pairAnalysis, frameIndex, sequence.size())) {
                        frameDecision = this.planFrame(
                                previousEncodedFrame,
                                sourceFrame,
                                workingFrame,
                                introSequence,
                                plannedDescriptors,
                                companionSequenceFrames,
                                frameIndex,
                                framesSinceKeyframe,
                                qualityBudgetState,
                                options,
                                copyDetector,
                                payloadInterner,
                                motionFamilyUsageTracker,
                                executor,
                                cancellationRequested
                        );
                        mixedPlanningEnabled = this.shouldSwitchReferenceSequenceToMixed(frameDecision.candidate());
                    } else {
                        FrameCandidate fullCandidate = this.createMeasuredFullCandidate(
                                sourceFrame,
                                workingFrame,
                                introSequence,
                                frameIndex,
                                options,
                                allowPerceptual
                        );
                        frameDecision = this.finalizeFrameDecision(fullCandidate, motionFamilyUsageTracker, frameIndex, false, fullCandidate);
                    }
                    nextPreviousEncodedFrame = frameDecision.outputFrame();
                    keepSourceFrame = nextPreviousEncodedFrame == sourceFrame;
                    keepWorkingFrame = nextPreviousEncodedFrame == workingFrame;
                    qualityBudgetState = qualityBudgetState.advance(frameDecision.candidate());
                    if ((frameDecision.candidate().kind() == CandidateKind.SAME) && options.isDuplicateFrameElision()) {
                        this.extendPlannedFrameDelay(plannedFrames, frameDelayMs);
                    } else {
                        AfmaFrameDescriptor finalizedDescriptor = payloadInterner.intern(frameDecision.candidate());
                        plannedFrames.add(new PlannedTimedFrame(finalizedDescriptor, frameDelayMs));
                        plannedDescriptors.add(finalizedDescriptor);
                        if (finalizedDescriptor.isKeyframe()) {
                            framesSinceKeyframe = 0;
                        } else {
                            framesSinceKeyframe++;
                        }
                    }
                } finally {
                    if (!keepSourceFrame && (sourceFrame != nextPreviousEncodedFrame)) {
                        CloseableUtils.closeQuietly(sourceFrame);
                    }
                    if (!keepWorkingFrame && (workingFrame != sourceFrame) && (workingFrame != nextPreviousEncodedFrame)) {
                        CloseableUtils.closeQuietly(workingFrame);
                    }
                    if (previousEncodedFrame != nextPreviousEncodedFrame) {
                        CloseableUtils.closeQuietly(previousEncodedFrame);
                    }
                    previousEncodedFrame = nextPreviousEncodedFrame;
                }
            }
            return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence));
        } finally {
            CloseableUtils.closeQuietly(previousEncodedFrame);
            frameLoader.close();
        }
    }

    /** Builds the plan for the AFMA creator. */
    protected AfmaEncodePlan buildPlan(@NotNull Dimension dimension,
                                       @NotNull AfmaEncodeOptions options,
                                       @NotNull PlannedSequence plannedMain,
                                       @NotNull PlannedSequence plannedIntro,
                                       @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads) {
        long mainFrameTime = plannedMain.defaultDelayMs();
        long introFrameTime = plannedIntro.defaultDelayMs();
        if (plannedMain.frames().isEmpty() && !plannedIntro.frames().isEmpty()) {
            mainFrameTime = introFrameTime;
        } else if (plannedIntro.frames().isEmpty()) {
            introFrameTime = mainFrameTime;
        }

        AfmaMetadata metadata = AfmaMetadata.create(
                dimension.width(),
                dimension.height(),
                options.getLoopCount(),
                mainFrameTime,
                introFrameTime,
                plannedMain.customFrameTimes(),
                plannedIntro.customFrameTimes(),
                options.isAdaptiveKeyframePlacementEnabled() ? options.getAdaptiveMaxKeyframeInterval() : options.getKeyframeInterval(),
                options.isRectCopyEnabled(),
                options.isDuplicateFrameElision()
        );
        return new AfmaEncodePlan(
                metadata,
                new AfmaFrameIndex(plannedMain.frames(), plannedIntro.frames()),
                payloads
        );
    }

    /** Plans the sequence for the AFMA creator. */
    @NotNull
    protected PlannedSequence planSequence(@NotNull AfmaSourceSequence sequence, boolean introSequence,
                                           @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                           @NotNull Dimension dimension,
                                           @NotNull AfmaEncodeOptions options, @NotNull AfmaRectCopyDetector copyDetector,
                                           @NotNull PayloadInterner payloadInterner, @NotNull AfmaFastPixelBufferPool pixelBufferPool,
                                           @Nullable ExecutorService executor,
                                           @Nullable BooleanSupplier cancellationRequested,
                                           @Nullable AfmaEncodePlanner.ProgressListener progressListener,
                                           int startOffset, int totalFrameCount,
                                           @Nullable AfmaPixelFrame firstFrameOverride) throws IOException {
        List<PlannedTimedFrame> plannedFrames = new ArrayList<>();
        List<AfmaFrameDescriptor> plannedDescriptors = new ArrayList<>();
        if (sequence.isEmpty()) {
            return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence));
        }

        AsyncFrameLoader frameLoader = new AsyncFrameLoader(sequence, dimension, firstFrameOverride, pixelBufferPool, executor, cancellationRequested);
        AfmaPixelFrame previousEncodedFrame = null;
        QualityBudgetState qualityBudgetState = QualityBudgetState.lossless();
        int framesSinceKeyframe = 0;
        MotionFamilyUsageTracker motionFamilyUsageTracker = new MotionFamilyUsageTracker();
        try {
            for (int frameIndex = 0; frameIndex < sequence.size(); frameIndex++) {
                checkCancelled(cancellationRequested);
                AfmaPixelFrame sourceFrame = frameLoader.takeFrame(frameIndex);
                AfmaPixelFrame workingFrame = sourceFrame;
                AfmaPixelFrame nextPreviousEncodedFrame = previousEncodedFrame;
                boolean keepSourceFrame = false;
                boolean keepWorkingFrame = false;
                try {
                    long frameDelayMs = this.resolveSourceFrameDelay(options, introSequence, frameIndex);
                    reportPlanningFrameProgress(
                            progressListener,
                            "Planning",
                            introSequence,
                            frameIndex + 1,
                            sequence.size(),
                            startOffset + frameIndex + 1D,
                            totalFrameCount
                    );

                    boolean bootstrapFrame = plannedFrames.isEmpty();
                    if (!bootstrapFrame && options.isNearLosslessEnabled()) {
                        workingFrame = this.applyNearLosslessTemporalMerge(Objects.requireNonNull(previousEncodedFrame), sourceFrame, options.getNearLosslessMaxChannelDelta());
                    }

                    FrameDecision frameDecision = this.planFrame(
                            previousEncodedFrame,
                            sourceFrame,
                            workingFrame,
                            introSequence,
                            plannedDescriptors,
                            companionSequenceFrames,
                            frameIndex,
                            framesSinceKeyframe,
                            qualityBudgetState,
                            options,
                            copyDetector,
                            payloadInterner,
                            motionFamilyUsageTracker,
                            executor,
                            cancellationRequested
                    );
                    nextPreviousEncodedFrame = frameDecision.outputFrame();
                    keepSourceFrame = nextPreviousEncodedFrame == sourceFrame;
                    keepWorkingFrame = nextPreviousEncodedFrame == workingFrame;

                    qualityBudgetState = qualityBudgetState.advance(frameDecision.candidate());
                    if ((frameDecision.candidate().kind() == CandidateKind.SAME) && options.isDuplicateFrameElision()) {
                        this.extendPlannedFrameDelay(plannedFrames, frameDelayMs);
                    } else {
                        AfmaFrameDescriptor finalizedDescriptor = payloadInterner.intern(frameDecision.candidate());
                        plannedFrames.add(new PlannedTimedFrame(finalizedDescriptor, frameDelayMs));
                        plannedDescriptors.add(finalizedDescriptor);
                        if (finalizedDescriptor.isKeyframe()) {
                            framesSinceKeyframe = 0;
                        } else {
                            framesSinceKeyframe++;
                        }
                    }

                    AfmaPixelFrame oldPreviousEncodedFrame = previousEncodedFrame;
                    previousEncodedFrame = nextPreviousEncodedFrame;
                    if ((oldPreviousEncodedFrame != null) && (oldPreviousEncodedFrame != previousEncodedFrame)) {
                        CloseableUtils.closeQuietly(oldPreviousEncodedFrame);
                    }
                } finally {
                    if (!keepWorkingFrame && (workingFrame != sourceFrame)) {
                        CloseableUtils.closeQuietly(workingFrame);
                    }
                    if (!keepSourceFrame) {
                        CloseableUtils.closeQuietly(sourceFrame);
                    }
                }
            }
        } finally {
            frameLoader.close();
            CloseableUtils.closeQuietly(previousEncodedFrame);
        }

        return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence));
    }

    /** Plans the frame for the AFMA creator. */
    @NotNull
    protected FrameDecision planFrame(@Nullable AfmaPixelFrame previousFrame,
                                      @NotNull AfmaPixelFrame sourceFrame,
                                      @NotNull AfmaPixelFrame workingFrame,
                                      boolean introSequence,
                                      @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                      @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                      int frameIndex, int framesSinceKeyframe,
                                      @NotNull QualityBudgetState qualityBudgetState,
                                      @NotNull AfmaEncodeOptions options,
                                      @NotNull AfmaRectCopyDetector copyDetector,
                                      @NotNull PayloadInterner payloadInterner,
                                      @Nullable MotionFamilyUsageTracker motionFamilyUsageTracker,
                                      @Nullable ExecutorService executor,
                                      @Nullable BooleanSupplier cancellationRequested) throws IOException {
        Objects.requireNonNull(sourceFrame);
        Objects.requireNonNull(workingFrame);
        boolean allowPerceptual = (previousFrame == null)
                ? options.isPerceptualBinIntraEnabled()
                : this.shouldAllowPerceptualContinuation(framesSinceKeyframe, options);
        if (previousFrame == null) {
            FrameCandidate fullCandidate = this.createMeasuredFullCandidate(
                    sourceFrame,
                    workingFrame,
                    introSequence,
                    frameIndex,
                    options,
                    allowPerceptual
            );
            return this.finalizeFrameDecision(fullCandidate, motionFamilyUsageTracker, frameIndex, false, fullCandidate);
        }

        checkCancelled(cancellationRequested);
        AfmaPixelFrame candidateFrame = workingFrame;
        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(previousFrame, candidateFrame);
        AfmaRect deltaBounds = pairAnalysis.differenceBounds();
        if (deltaBounds == null) {
            FrameCandidate sameCandidate = this.withQualityMetrics(new FrameCandidate(
                    CandidateKind.SAME,
                    AfmaFrameDescriptor.same(),
                    null,
                    null,
                    null,
                    null,
                    previousFrame,
                    0,
                    CandidateQualityMetrics.losslessMetrics()
            ), sourceFrame);
            if (this.isCandidateAllowed(sameCandidate, qualityBudgetState, options)) {
                return this.finalizeFrameDecision(sameCandidate, motionFamilyUsageTracker, frameIndex, false, sameCandidate);
            }
            candidateFrame = sourceFrame;
            pairAnalysis = new AfmaFramePairAnalysis(previousFrame, sourceFrame);
            deltaBounds = pairAnalysis.differenceBounds();
            if (deltaBounds == null) {
                FrameCandidate fullCandidate = this.createMeasuredFullCandidate(
                        sourceFrame,
                        sourceFrame,
                        introSequence,
                        frameIndex,
                        options,
                        allowPerceptual
                );
                return this.finalizeFrameDecision(fullCandidate, motionFamilyUsageTracker, frameIndex, false, fullCandidate);
            }
        }

        FrameCandidate bestCandidate = null;
        FrameCandidate deltaCandidate = this.withQualityMetrics(this.createBestDeltaFamilyCandidate(
                previousFrame,
                candidateFrame,
                pairAnalysis,
                introSequence,
                frameIndex,
                deltaBounds,
                options,
                allowPerceptual
        ), sourceFrame);
        bestCandidate = this.pickBetterCandidate(bestCandidate, deltaCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        FrameCandidate exactContinuationCandidate = this.withQualityMetrics(
                this.createExactContinuationCandidate(
                        previousFrame,
                        sourceFrame,
                        candidateFrame,
                        introSequence,
                        frameIndex,
                        deltaCandidate,
                        options
                ),
                sourceFrame
        );
        bestCandidate = this.pickBetterCandidate(bestCandidate, exactContinuationCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        int hardInterval = options.isAdaptiveKeyframePlacementEnabled()
                ? options.getAdaptiveMaxKeyframeInterval()
                : options.getKeyframeInterval();
        boolean hardKeyframe = (framesSinceKeyframe + 1) >= hardInterval;
        boolean preferredKeyframe = (framesSinceKeyframe + 1) >= options.getKeyframeInterval();
        EstimatedFullCandidateMetrics estimatedFullCandidate = this.estimateFullCandidateMetrics(
                candidateFrame,
                introSequence,
                frameIndex,
                options,
                allowPerceptual
        );
        long deltaCandidateArchiveBytes = (deltaCandidate != null) ? deltaCandidate.totalArchiveBytes(payloadInterner) : Long.MAX_VALUE;

        boolean motionFamiliesEnabled = (motionFamilyUsageTracker == null) || motionFamilyUsageTracker.shouldEvaluate(frameIndex);
        boolean skipMotionFamilies = !motionFamiliesEnabled
                || this.shouldSkipMotionFamiliesForStrongDelta(deltaCandidate, deltaCandidateArchiveBytes, estimatedFullCandidate, pairAnalysis, sourceFrame);
        boolean motionFamiliesAttempted = motionFamiliesEnabled && !skipMotionFamilies;
        CopyEvaluation copyEvaluation = (options.isRectCopyEnabled() && motionFamiliesAttempted)
                ? this.evaluateCopyDetections(previousFrame, candidateFrame, pairAnalysis, copyDetector)
                : CopyEvaluation.EMPTY;
        FrameCandidate copyCandidate = (options.isRectCopyEnabled() && motionFamiliesAttempted)
                ? this.withQualityMetrics(this.createBestCopyFamilyCandidate(
                previousFrame,
                candidateFrame,
                pairAnalysis,
                introSequence,
                frameIndex,
                options,
                allowPerceptual,
                copyEvaluation
        ), sourceFrame)
                : null;
        bestCandidate = this.pickBetterCandidate(bestCandidate, copyCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);

        long blockInterReferenceBytes = (bestCandidate != null)
                ? bestCandidate.totalArchiveBytes(payloadInterner)
                : 0L;
        FrameCandidate blockInterCandidate = motionFamiliesAttempted
                ? this.withQualityMetrics(this.createBlockInterFamilyCandidate(
                previousFrame,
                candidateFrame,
                pairAnalysis,
                introSequence,
                frameIndex,
                deltaBounds,
                copyDetector,
                copyEvaluation,
                blockInterReferenceBytes,
                executor,
                cancellationRequested
        ), sourceFrame)
                : null;
        bestCandidate = this.pickBetterCandidate(bestCandidate, blockInterCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        long bestCandidateArchiveBytes = (bestCandidate != null) ? bestCandidate.totalArchiveBytes(payloadInterner) : Long.MAX_VALUE;
        FrameCandidate fullCandidate = this.shouldSkipExactFullCandidate(
                bestCandidate,
                bestCandidateArchiveBytes,
                estimatedFullCandidate,
                pairAnalysis,
                sourceFrame,
                hardKeyframe,
                preferredKeyframe
        ) ? null : this.createMeasuredFullCandidate(
                sourceFrame,
                candidateFrame,
                introSequence,
                frameIndex,
                options,
                allowPerceptual
        );
        PackedArchiveCandidateMetrics packedArchiveMetrics = this.resolveArchiveCandidateMetrics(
                Arrays.asList(deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate),
                payloadInterner,
                pairAnalysis,
                introSequence,
                currentSequenceFrames,
                companionSequenceFrames,
                qualityBudgetState,
                options
        );
        Map<FrameCandidate, Long> packedArchiveBytesByCandidate = packedArchiveMetrics.totalBytesByCandidate();
        Map<FrameCandidate, Long> packedArchiveAddedBytesByCandidate = packedArchiveMetrics.addedBytesByCandidate();
        deltaCandidate = this.filterWeakCandidateAgainstFull(
                deltaCandidate,
                fullCandidate,
                packedArchiveAddedBytesByCandidate,
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                options
        );
        exactContinuationCandidate = this.filterWeakCandidateAgainstFull(
                exactContinuationCandidate,
                fullCandidate,
                packedArchiveAddedBytesByCandidate,
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                options
        );
        copyCandidate = this.filterWeakCandidateAgainstFull(
                copyCandidate,
                fullCandidate,
                packedArchiveAddedBytesByCandidate,
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                options
        );
        blockInterCandidate = this.filterWeakCandidateAgainstFull(
                blockInterCandidate,
                fullCandidate,
                packedArchiveAddedBytesByCandidate,
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                options
        );
        bestCandidate = null;
        bestCandidate = this.pickBetterCandidate(bestCandidate, deltaCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        bestCandidate = this.pickBetterCandidate(bestCandidate, exactContinuationCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        bestCandidate = this.pickBetterCandidate(bestCandidate, copyCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        bestCandidate = this.pickBetterCandidate(bestCandidate, blockInterCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);

        if (hardKeyframe) {
            FrameCandidate selected = Objects.requireNonNull(fullCandidate, "AFMA v2 hard-keyframe candidate was NULL");
            return this.finalizeFrameDecision(selected, motionFamilyUsageTracker, frameIndex, motionFamiliesAttempted, deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate);
        }

        if (bestCandidate == null) {
            FrameCandidate selected = Objects.requireNonNull(fullCandidate, "AFMA v2 fallback full candidate was NULL");
            return this.finalizeFrameDecision(selected, motionFamilyUsageTracker, frameIndex, motionFamiliesAttempted, deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate);
        }

        if (fullCandidate != null) {
            if (!options.isAdaptiveKeyframePlacementEnabled() && preferredKeyframe) {
                return this.finalizeFrameDecision(fullCandidate, motionFamilyUsageTracker, frameIndex, motionFamiliesAttempted, deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate);
            }
            if (options.isAdaptiveKeyframePlacementEnabled() && preferredKeyframe) {
                long continuationSavings = fullCandidate.totalArchiveBytes(payloadInterner) - bestCandidate.totalArchiveBytes(payloadInterner);
                long requiredSavings = this.resolveAdaptiveContinuationSavings(fullCandidate.totalArchiveBytes(payloadInterner), options);
                if (continuationSavings < requiredSavings) {
                    return this.finalizeFrameDecision(fullCandidate, motionFamilyUsageTracker, frameIndex, motionFamiliesAttempted, deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate);
                }
            }
            if (this.pickBetterCandidate(bestCandidate, fullCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe) == fullCandidate) {
                return this.finalizeFrameDecision(fullCandidate, motionFamilyUsageTracker, frameIndex, motionFamiliesAttempted, deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate);
            }
        }

        FrameCandidate packedArchiveWinner = this.pickBestPackedArchiveCandidate(
                Arrays.asList(deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate),
                packedArchiveBytesByCandidate,
                payloadInterner,
                qualityBudgetState,
                options,
                framesSinceKeyframe
        );
        if (packedArchiveWinner != null) {
            return this.finalizeFrameDecision(packedArchiveWinner, motionFamilyUsageTracker, frameIndex, motionFamiliesAttempted, deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate);
        }

        return this.finalizeFrameDecision(bestCandidate, motionFamilyUsageTracker, frameIndex, motionFamiliesAttempted, deltaCandidate, exactContinuationCandidate, copyCandidate, blockInterCandidate, fullCandidate);
    }

    /** Returns whether skip motion families for strong delta. */
    protected boolean shouldSkipMotionFamiliesForStrongDelta(@Nullable FrameCandidate deltaCandidate,
                                                             long deltaCandidateArchiveBytes,
                                                             @Nullable EstimatedFullCandidateMetrics estimatedFullCandidate,
                                                             @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                             @NotNull AfmaPixelFrame currentFrame) {
        if (this.shouldSkipMotionFamiliesForStrongFullFrameDelta(deltaCandidate, pairAnalysis, currentFrame)) {
            return true;
        }
        if ((deltaCandidate == null)
                || ((deltaCandidate.kind() != CandidateKind.DELTA_SPARSE) && (deltaCandidate.kind() != CandidateKind.DELTA_RESIDUAL))
                || (estimatedFullCandidate == null)
                || (deltaCandidateArchiveBytes == Long.MAX_VALUE)) {
            return false;
        }
        long estimatedFullArchiveBytes = estimatedFullCandidate.estimatedArchiveBytes();
        if ((estimatedFullArchiveBytes <= 0L) || (deltaCandidateArchiveBytes >= estimatedFullArchiveBytes)) {
            return false;
        }
        long requiredSavings = Math.max(
                STRONG_DELTA_SKIP_MOTION_MIN_SAVINGS_BYTES,
                (long) Math.ceil(estimatedFullArchiveBytes * STRONG_DELTA_SKIP_MOTION_MIN_SAVINGS_RATIO)
        );
        return (estimatedFullArchiveBytes - deltaCandidateArchiveBytes) >= requiredSavings;
    }

    /** Returns whether skip motion families for strong full frame delta. */
    protected boolean shouldSkipMotionFamiliesForStrongFullFrameDelta(@Nullable FrameCandidate deltaCandidate,
                                                                      @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                      @NotNull AfmaPixelFrame currentFrame) {
        if ((deltaCandidate == null) || (deltaCandidate.kind() != CandidateKind.DELTA_SPARSE)) {
            return false;
        }
        AfmaRect differenceBounds = pairAnalysis.differenceBounds();
        if (differenceBounds == null) {
            return false;
        }
        long frameArea = (long) currentFrame.getWidth() * (long) currentFrame.getHeight();
        if ((frameArea <= 0L) || (differenceBounds.area() < frameArea)) {
            return false;
        }
        return ((double) pairAnalysis.changedPixelCount() / (double) frameArea) <= STRONG_FULL_FRAME_DELTA_SKIP_CHANGED_RATIO;
    }

    /** Returns whether skip exact full candidate. */
    protected boolean shouldSkipExactFullCandidate(@Nullable FrameCandidate bestCandidate,
                                                   long bestCandidateArchiveBytes,
                                                   @Nullable EstimatedFullCandidateMetrics estimatedFullCandidate,
                                                   @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                   @NotNull AfmaPixelFrame currentFrame,
                                                   boolean hardKeyframe,
                                                   boolean preferredKeyframe) {
        if (hardKeyframe || preferredKeyframe || (bestCandidate == null) || (estimatedFullCandidate == null)) {
            return false;
        }
        if ((bestCandidate.kind() == CandidateKind.FULL) || (bestCandidate.kind() == CandidateKind.SAME) || !bestCandidate.qualityMetrics().lossless()) {
            return false;
        }

        long estimatedFullArchiveBytes = estimatedFullCandidate.estimatedArchiveBytes();
        if ((estimatedFullArchiveBytes <= 0L)
                || (bestCandidateArchiveBytes == Long.MAX_VALUE)
                || (bestCandidateArchiveBytes >= estimatedFullArchiveBytes)) {
            return false;
        }

        long frameArea = (long) currentFrame.getWidth() * (long) currentFrame.getHeight();
        if (frameArea <= 0L) {
            return false;
        }
        double changedRatio = (double) pairAnalysis.changedPixelCount() / (double) frameArea;
        if ((changedRatio > STRONG_DELTA_SKIP_FULL_MAX_CHANGED_RATIO) && (bestCandidate.kind() != CandidateKind.DELTA_SPARSE)) {
            return false;
        }

        long requiredSavings = Math.max(
                STRONG_DELTA_SKIP_FULL_MIN_SAVINGS_BYTES,
                (long) Math.ceil(estimatedFullArchiveBytes * STRONG_DELTA_SKIP_FULL_MIN_SAVINGS_RATIO)
        );
        return (estimatedFullArchiveBytes - bestCandidateArchiveBytes) >= requiredSavings;
    }

    /** Resolves the adaptive continuation savings for the AFMA creator. */
    protected long resolveAdaptiveContinuationSavings(long fullBytes, @NotNull AfmaEncodeOptions options) {
        long ratioSavings = Math.round(Math.max(0D, fullBytes) * Math.max(0D, options.getAdaptiveContinuationMinSavingsRatio()));
        return Math.max(options.getAdaptiveContinuationMinSavingsBytes(), ratioSavings);
    }

    /** Selects the better candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate pickBetterCandidate(@Nullable FrameCandidate first, @Nullable FrameCandidate second,
                                                 @NotNull PayloadInterner payloadInterner,
                                                 @NotNull QualityBudgetState qualityBudgetState,
                                                 @NotNull AfmaEncodeOptions options,
                                                 int framesSinceKeyframe) {
        if (!this.isCandidateAllowed(first, qualityBudgetState, options)) {
            first = null;
        }
        if (!this.isCandidateAllowed(second, qualityBudgetState, options)) {
            second = null;
        }
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        double firstScore = this.scoreCandidate(first, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        double secondScore = this.scoreCandidate(second, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        if (Double.compare(firstScore, secondScore) != 0) {
            return (firstScore < secondScore) ? first : second;
        }
        long firstBytes = first.totalArchiveBytes(payloadInterner);
        long secondBytes = second.totalArchiveBytes(payloadInterner);
        if (firstBytes != secondBytes) {
            return (firstBytes < secondBytes) ? first : second;
        }
        if (first.decodeComplexity() != second.decodeComplexity()) {
            return (first.decodeComplexity() < second.decodeComplexity()) ? first : second;
        }
        return (first.kind().stabilityRank() <= second.kind().stabilityRank()) ? first : second;
    }

    /** Returns whether candidate allowed. */
    protected boolean isCandidateAllowed(@Nullable FrameCandidate candidate,
                                         @NotNull QualityBudgetState qualityBudgetState,
                                         @NotNull AfmaEncodeOptions options) {
        if (candidate == null) {
            return false;
        }

        QualityBudgetState projectedState = qualityBudgetState.advance(candidate);
        if ((options.getPlannerMaxConsecutiveLossyFrames() > 0)
                && (projectedState.consecutiveLossyFrames() > options.getPlannerMaxConsecutiveLossyFrames())) {
            return false;
        }
        if ((options.getPlannerMaxCumulativeAverageError() > 0D)
                && (projectedState.cumulativeAverageError() > options.getPlannerMaxCumulativeAverageError())) {
            return false;
        }
        if ((options.getPlannerMaxCumulativeVisibleColorDelta() > 0)
                && (projectedState.cumulativeVisibleColorDelta() > options.getPlannerMaxCumulativeVisibleColorDelta())) {
            return false;
        }
        return (options.getPlannerMaxCumulativeAlphaDelta() <= 0)
                || (projectedState.cumulativeAlphaDelta() <= options.getPlannerMaxCumulativeAlphaDelta());
    }

    /** Scores the candidate for AFMA creator candidate selection. */
    protected double scoreCandidate(@NotNull FrameCandidate candidate,
                                    @NotNull PayloadInterner payloadInterner,
                                    @NotNull QualityBudgetState qualityBudgetState,
                                    @NotNull AfmaEncodeOptions options,
                                    int framesSinceKeyframe) {
        return this.scoreCandidate(candidate, candidate.totalArchiveBytes(payloadInterner), qualityBudgetState, options, framesSinceKeyframe);
    }

    /** Scores the candidate for AFMA creator candidate selection. */
    protected double scoreCandidate(@NotNull FrameCandidate candidate,
                                    long archiveBytes,
                                    @NotNull QualityBudgetState qualityBudgetState,
                                    @NotNull AfmaEncodeOptions options,
                                    int framesSinceKeyframe) {
        QualityBudgetState projectedState = qualityBudgetState.advance(candidate);
        double score = archiveBytes;
        score += (double) candidate.decodeComplexity() * (double) options.getPlannerDecodeCostPenaltyBytes();
        score += (double) candidate.kind().stabilityRank() * (double) options.getPlannerComplexityPenaltyBytes();
        score += projectedState.cumulativeAverageError() * options.getPlannerAverageDriftPenaltyBytes();
        score += (double) projectedState.cumulativeVisibleColorDelta() * (double) options.getPlannerVisibleColorDriftPenaltyBytes();
        score += (double) projectedState.cumulativeAlphaDelta() * (double) options.getPlannerAlphaDriftPenaltyBytes();
        score += (double) projectedState.consecutiveLossyFrames() * (double) options.getPlannerLossyContinuationPenaltyBytes();
        if (!candidate.descriptor().isKeyframe()) {
            score += (double) (framesSinceKeyframe + 1) * (double) options.getPlannerKeyframeDistancePenaltyBytes();
        }
        return score;
    }

    /** Selects the best packed archive candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate pickBestPackedArchiveCandidate(@NotNull List<FrameCandidate> candidates,
                                                            @NotNull Map<FrameCandidate, Long> packedArchiveBytesByCandidate,
                                                            @NotNull PayloadInterner payloadInterner,
                                                            @NotNull QualityBudgetState qualityBudgetState,
                                                            @NotNull AfmaEncodeOptions options,
                                                            int framesSinceKeyframe) throws IOException {
        FrameCandidate bestCandidate = null;
        long bestPackedArchiveBytes = Long.MAX_VALUE;
        double bestPackedScore = Double.POSITIVE_INFINITY;
        for (FrameCandidate candidate : candidates) {
            if (!this.isCandidateAllowed(candidate, qualityBudgetState, options)) {
                continue;
            }

            long packedArchiveBytes = packedArchiveBytesByCandidate.getOrDefault(
                    candidate,
                    candidate.totalArchiveBytes(payloadInterner)
            );
            double packedScore = this.scoreCandidate(candidate, packedArchiveBytes, qualityBudgetState, options, framesSinceKeyframe);
            if (bestCandidate == null
                    || (Double.compare(packedScore, bestPackedScore) < 0)
                    || ((Double.compare(packedScore, bestPackedScore) == 0) && (packedArchiveBytes < bestPackedArchiveBytes))
                    || ((Double.compare(packedScore, bestPackedScore) == 0) && (packedArchiveBytes == bestPackedArchiveBytes)
                    && (candidate.decodeComplexity() < bestCandidate.decodeComplexity()))
                    || ((Double.compare(packedScore, bestPackedScore) == 0) && (packedArchiveBytes == bestPackedArchiveBytes)
                    && (candidate.decodeComplexity() == bestCandidate.decodeComplexity())
                    && (candidate.kind().stabilityRank() < bestCandidate.kind().stabilityRank()))) {
                bestCandidate = candidate;
                bestPackedArchiveBytes = packedArchiveBytes;
                bestPackedScore = packedScore;
            }
        }
        return bestCandidate;
    }

    /** Resolves the archive candidate metrics for the AFMA creator. */
    @NotNull
    protected PackedArchiveCandidateMetrics resolveArchiveCandidateMetrics(@NotNull List<FrameCandidate> candidates,
                                                                           @NotNull PayloadInterner payloadInterner,
                                                                           @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                           boolean introSequence,
                                                                           @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                                                           @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                                                           @NotNull QualityBudgetState qualityBudgetState,
                                                                           @NotNull AfmaEncodeOptions options) throws IOException {
        if (this.shouldUseApproximateArchiveCandidateMetrics(candidates, payloadInterner, pairAnalysis, currentSequenceFrames, companionSequenceFrames, qualityBudgetState, options)) {
            return this.buildLocalArchiveCandidateMetrics(candidates, payloadInterner, qualityBudgetState, options);
        }
        return this.estimatePackedArchiveBytesByCandidate(
                candidates,
                payloadInterner,
                introSequence,
                currentSequenceFrames,
                companionSequenceFrames,
                qualityBudgetState,
                options
        );
    }

    /** Returns whether use approximate archive candidate metrics. */
    protected boolean shouldUseApproximateArchiveCandidateMetrics(@NotNull List<FrameCandidate> candidates,
                                                                  @NotNull PayloadInterner payloadInterner,
                                                                  @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                  @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                                                  @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                                                  @NotNull QualityBudgetState qualityBudgetState,
                                                                  @NotNull AfmaEncodeOptions options) {
        int sequenceFrameCount = currentSequenceFrames.size() + companionSequenceFrames.size();
        if (sequenceFrameCount < 24) {
            return false;
        }

        FrameCandidate bestCandidate = null;
        FrameCandidate secondBestCandidate = null;
        FrameCandidate fullCandidate = null;
        long bestBytes = Long.MAX_VALUE;
        long secondBestBytes = Long.MAX_VALUE;
        long fullBytes = Long.MAX_VALUE;
        for (FrameCandidate candidate : candidates) {
            if ((candidate == null) || !this.isCandidateAllowed(candidate, qualityBudgetState, options)) {
                continue;
            }

            long candidateBytes = candidate.totalArchiveBytes(payloadInterner);
            if (candidate.kind() == CandidateKind.FULL) {
                fullCandidate = candidate;
                fullBytes = candidateBytes;
            }
            if (candidateBytes < bestBytes) {
                secondBestCandidate = bestCandidate;
                secondBestBytes = bestBytes;
                bestCandidate = candidate;
                bestBytes = candidateBytes;
            } else if (candidateBytes < secondBestBytes) {
                secondBestCandidate = candidate;
                secondBestBytes = candidateBytes;
            }
        }

        if ((bestCandidate == null) || (fullCandidate == null)) {
            return true;
        }
        if ((bestCandidate.kind() == CandidateKind.FULL) || (bestCandidate.kind() == CandidateKind.SAME)) {
            return true;
        }
        if ((bestCandidate.kind() != CandidateKind.DELTA_SPARSE) && (bestCandidate.kind() != CandidateKind.DELTA_RESIDUAL)) {
            return false;
        }
        if (bestBytes >= fullBytes) {
            return false;
        }

        long frameArea = (long) pairAnalysis.width * (long) pairAnalysis.height;
        if (frameArea <= 0L) {
            return false;
        }
        double changedRatio = (double) pairAnalysis.changedPixelCount() / (double) frameArea;
        if (changedRatio > PACKED_ARCHIVE_RESCORING_MAX_CHANGED_RATIO) {
            return false;
        }

        long clearMargin = Math.max(
                PACKED_ARCHIVE_RESCORING_CLEAR_SAVINGS_BYTES,
                (long) Math.ceil(fullBytes * PACKED_ARCHIVE_RESCORING_CLEAR_SAVINGS_RATIO)
        );
        if ((fullBytes - bestBytes) < clearMargin) {
            return false;
        }
        if ((secondBestCandidate != null) && ((secondBestBytes - bestBytes) < (clearMargin / 2L))) {
            return false;
        }
        return true;
    }

    /** Builds the local archive candidate metrics for the AFMA creator. */
    @NotNull
    protected PackedArchiveCandidateMetrics buildLocalArchiveCandidateMetrics(@NotNull List<FrameCandidate> candidates,
                                                                              @NotNull PayloadInterner payloadInterner,
                                                                              @NotNull QualityBudgetState qualityBudgetState,
                                                                              @NotNull AfmaEncodeOptions options) {
        IdentityHashMap<FrameCandidate, Long> localArchiveBytesByCandidate = new IdentityHashMap<>();
        IdentityHashMap<FrameCandidate, Long> localArchiveAddedBytesByCandidate = new IdentityHashMap<>();
        for (FrameCandidate candidate : candidates) {
            if ((candidate == null) || !this.isCandidateAllowed(candidate, qualityBudgetState, options)) {
                continue;
            }

            long localArchiveBytes = candidate.totalArchiveBytes(payloadInterner);
            localArchiveBytesByCandidate.put(candidate, localArchiveBytes);
            localArchiveAddedBytesByCandidate.put(candidate, localArchiveBytes);
        }
        return new PackedArchiveCandidateMetrics(localArchiveBytesByCandidate, localArchiveAddedBytesByCandidate);
    }

    /** Calculates the packed archive bytes by candidate for the AFMA creator. */
    @NotNull
    protected PackedArchiveCandidateMetrics estimatePackedArchiveBytesByCandidate(@NotNull List<FrameCandidate> candidates,
                                                                                  @NotNull PayloadInterner payloadInterner,
                                                                                  boolean introSequence,
                                                                                  @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                                                                  @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                                                                  @NotNull QualityBudgetState qualityBudgetState,
                                                                                  @NotNull AfmaEncodeOptions options) throws IOException {
        IdentityHashMap<FrameCandidate, Long> packedArchiveBytesByCandidate = new IdentityHashMap<>();
        IdentityHashMap<FrameCandidate, Long> packedArchiveAddedBytesByCandidate = new IdentityHashMap<>();
        long basePackedArchiveBytes = payloadInterner.estimatePackedArchiveBytes(
                introSequence,
                currentSequenceFrames,
                companionSequenceFrames,
                options.getLoopCount()
        );
        for (FrameCandidate candidate : candidates) {
            if ((candidate == null) || !this.isCandidateAllowed(candidate, qualityBudgetState, options)) {
                continue;
            }
            long packedArchiveBytes = payloadInterner.estimatePackedCandidateArchiveBytes(
                    candidate,
                    introSequence,
                    currentSequenceFrames,
                    companionSequenceFrames,
                    options.getLoopCount()
            );
            packedArchiveBytesByCandidate.put(candidate, packedArchiveBytes);
            packedArchiveAddedBytesByCandidate.put(candidate, Math.max(0L, packedArchiveBytes - basePackedArchiveBytes));
        }
        return new PackedArchiveCandidateMetrics(packedArchiveBytesByCandidate, packedArchiveAddedBytesByCandidate);
    }

    /** Returns the filter weak candidate against full produced by the AFMA creator. */
    @Nullable
    protected FrameCandidate filterWeakCandidateAgainstFull(@Nullable FrameCandidate candidate,
                                                            @Nullable FrameCandidate fullCandidate,
                                                            @NotNull Map<FrameCandidate, Long> packedArchiveAddedBytesByCandidate,
                                                            int frameWidth, int frameHeight,
                                                            @NotNull AfmaEncodeOptions options) {
        if ((candidate == null) || (fullCandidate == null) || (candidate == fullCandidate)) {
            return candidate;
        }
        if (!this.shouldKeepCandidateAgainstFull(candidate, fullCandidate, packedArchiveAddedBytesByCandidate, frameWidth, frameHeight, options)) {
            return null;
        }
        return candidate;
    }

    /** Returns whether keep candidate against full. */
    protected boolean shouldKeepCandidateAgainstFull(@NotNull FrameCandidate candidate,
                                                     @NotNull FrameCandidate fullCandidate,
                                                     @NotNull Map<FrameCandidate, Long> packedArchiveAddedBytesByCandidate,
                                                     int frameWidth, int frameHeight,
                                                     @NotNull AfmaEncodeOptions options) {
        if ((candidate.kind() == CandidateKind.FULL) || (candidate.kind() == CandidateKind.SAME)) {
            return true;
        }

        Long candidateArchiveAddedBytes = packedArchiveAddedBytesByCandidate.get(candidate);
        Long fullArchiveAddedBytes = packedArchiveAddedBytesByCandidate.get(fullCandidate);
        if ((candidateArchiveAddedBytes == null) || (fullArchiveAddedBytes == null)) {
            return true;
        }

        double maxAreaRatioWithoutStrongSavings = this.resolveMaxAreaRatioWithoutStrongSavings(candidate.kind(), options);
        long patchArea = this.resolveCandidatePatchArea(candidate);
        return switch (candidate.kind()) {
            case DELTA_BIN_INTRA, COPY_BIN_INTRA, MULTI_COPY_BIN_INTRA, BLOCK_INTER ->
                    this.shouldKeepComplexCandidateByArchiveBytes(
                            candidateArchiveAddedBytes,
                            fullArchiveAddedBytes,
                            patchArea,
                            frameWidth,
                            frameHeight,
                            maxAreaRatioWithoutStrongSavings,
                            options
                    );
            case DELTA_RESIDUAL, COPY_RESIDUAL, MULTI_COPY_RESIDUAL ->
                    this.shouldKeepResidualCandidateByArchiveBytes(
                            candidateArchiveAddedBytes,
                            fullArchiveAddedBytes,
                            patchArea,
                            frameWidth,
                            frameHeight,
                            maxAreaRatioWithoutStrongSavings,
                            options
                    );
            case DELTA_SPARSE, COPY_SPARSE, MULTI_COPY_SPARSE ->
                    this.shouldKeepSparseCandidateByArchiveBytes(
                            candidateArchiveAddedBytes,
                            fullArchiveAddedBytes,
                            patchArea,
                            frameWidth,
                            frameHeight,
                            maxAreaRatioWithoutStrongSavings,
                            options
                    );
            default -> true;
        };
    }

    /** Resolves the max area ratio without strong savings for the AFMA creator. */
    protected double resolveMaxAreaRatioWithoutStrongSavings(@NotNull CandidateKind kind, @NotNull AfmaEncodeOptions options) {
        return switch (kind) {
            case DELTA_BIN_INTRA, DELTA_RESIDUAL, DELTA_SPARSE, BLOCK_INTER -> options.getMaxDeltaAreaRatioWithoutStrongSavings();
            case COPY_BIN_INTRA, COPY_RESIDUAL, COPY_SPARSE,
                 MULTI_COPY_BIN_INTRA, MULTI_COPY_RESIDUAL, MULTI_COPY_SPARSE -> options.getMaxCopyPatchAreaRatioWithoutStrongSavings();
            case SAME, FULL -> 1D;
        };
    }

    /** Resolves the candidate patch area for the AFMA creator. */
    protected long resolveCandidatePatchArea(@NotNull FrameCandidate candidate) {
        AfmaFrameDescriptor descriptor = candidate.descriptor();
        AfmaPatchRegion patchRegion = descriptor.getPatch();
        if (patchRegion != null) {
            return (long) patchRegion.getWidth() * (long) patchRegion.getHeight();
        }
        return (long) descriptor.getWidth() * (long) descriptor.getHeight();
    }

    /** Returns whether keep complex candidate by archive bytes. */
    protected boolean shouldKeepComplexCandidateByArchiveBytes(long candidateArchiveBytes, long fullArchiveBytes,
                                                               long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                               @NotNull AfmaEncodeOptions options) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea >= frameArea) {
            return false;
        }

        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }
        if (patchArea <= 0L) {
            return true;
        }

        return byteSavings >= this.computeRequiredCandidateSavings(
                fullArchiveBytes,
                patchArea,
                frameArea,
                maxAreaRatioWithoutStrongSavings,
                options
        );
    }

    /** Returns whether keep residual candidate by archive bytes. */
    protected boolean shouldKeepResidualCandidateByArchiveBytes(long candidateArchiveBytes, long fullArchiveBytes,
                                                                long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                                @NotNull AfmaEncodeOptions options) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea <= 0L) {
            return false;
        }

        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }

        long boundedPatchArea = Math.min(patchArea, frameArea);
        return byteSavings >= this.computeRequiredCandidateSavings(
                fullArchiveBytes,
                boundedPatchArea,
                frameArea,
                maxAreaRatioWithoutStrongSavings,
                options
        );
    }

    /** Returns whether keep sparse candidate by archive bytes. */
    protected boolean shouldKeepSparseCandidateByArchiveBytes(long candidateArchiveBytes, long fullArchiveBytes,
                                                              long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                              @NotNull AfmaEncodeOptions options) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea <= 0L) {
            return false;
        }

        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }

        long boundedPatchArea = Math.min(patchArea, frameArea);
        return byteSavings >= this.computeRequiredCandidateSavings(
                fullArchiveBytes,
                boundedPatchArea,
                frameArea,
                maxAreaRatioWithoutStrongSavings,
                options
        );
    }

    /** Calculates the required candidate savings for the AFMA creator. */
    protected long computeRequiredCandidateSavings(long fullArchiveBytes, long boundedPatchArea, long frameArea,
                                                   double maxAreaRatioWithoutStrongSavings, @NotNull AfmaEncodeOptions options) {
        long requiredSavings = this.computeRequiredComplexCandidateSavings(
                fullArchiveBytes,
                options.getMinComplexCandidateSavingsBytes(),
                options.getMinComplexCandidateSavingsRatio()
        );
        double areaRatio = (double) boundedPatchArea / (double) frameArea;
        if (areaRatio > maxAreaRatioWithoutStrongSavings) {
            requiredSavings = Math.max(
                    requiredSavings,
                    this.computeRequiredComplexCandidateSavings(
                            fullArchiveBytes,
                            options.getMinStrongComplexCandidateSavingsBytes(),
                            options.getMinStrongComplexCandidateSavingsRatio()
                    )
            );
        }
        return requiredSavings;
    }

    /** Calculates the required complex candidate savings for the AFMA creator. */
    protected long computeRequiredComplexCandidateSavings(long referenceBytes, long minAbsoluteSavings, double minSavingsRatio) {
        long ratioSavings = (referenceBytes > 0L && minSavingsRatio > 0D)
                ? (long) Math.ceil(referenceBytes * minSavingsRatio)
                : 0L;
        return Math.max(minAbsoluteSavings, ratioSavings);
    }

    /** Converts the frame decision from the AFMA creator representation. */
    @NotNull
    protected FrameDecision toFrameDecision(@NotNull FrameCandidate selectedCandidate, @Nullable FrameCandidate... candidates) {
        this.closeDiscardedCandidatePayloads(selectedCandidate, candidates);
        return new FrameDecision(selectedCandidate, selectedCandidate.outputFrame());
    }

    /** Returns the finalize frame decision produced by the AFMA creator. */
    @NotNull
    protected FrameDecision finalizeFrameDecision(@NotNull FrameCandidate selectedCandidate,
                                                  @Nullable MotionFamilyUsageTracker motionFamilyUsageTracker,
                                                  int frameIndex,
                                                  boolean motionFamiliesAttempted,
                                                  @Nullable FrameCandidate... candidates) {
        if (motionFamilyUsageTracker != null) {
            motionFamilyUsageTracker.recordFrameResult(frameIndex, motionFamiliesAttempted, selectedCandidate.kind());
        }
        return this.toFrameDecision(selectedCandidate, candidates);
    }

    /** Closes the discarded candidate payloads for the AFMA creator. */
    protected void closeDiscardedCandidatePayloads(@NotNull FrameCandidate selectedCandidate, @Nullable FrameCandidate... candidates) {
        Set<FrameCandidate> seenCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
        seenCandidates.add(selectedCandidate);
        for (FrameCandidate candidate : candidates) {
            if ((candidate == null) || !seenCandidates.add(candidate)) {
                continue;
            }
            CloseableUtils.closeQuietly(candidate.primaryPayload());
            CloseableUtils.closeQuietly(candidate.patchPayload());
        }
    }

    /** Closes the interned payloads for the AFMA creator. */
    protected void closeInternedPayloads(@NotNull PayloadInterner payloadInterner) {
        for (AfmaStoredPayload payload : payloadInterner.payloads().values()) {
            CloseableUtils.closeQuietly(payload);
        }
    }

    /** Creates a copy with the quality metrics changed for the AFMA creator. */
    @Nullable
    protected FrameCandidate withQualityMetrics(@Nullable FrameCandidate candidate, @NotNull AfmaPixelFrame sourceFrame) {
        if (candidate == null) {
            return null;
        }
        return candidate.withQualityMetrics(this.measureCandidateQuality(sourceFrame, candidate.outputFrame()));
    }

    /** Measures the candidate quality for the AFMA creator. */
    @NotNull
    protected CandidateQualityMetrics measureCandidateQuality(@NotNull AfmaPixelFrame sourceFrame, @NotNull AfmaPixelFrame outputFrame) {
        if (sourceFrame == outputFrame) {
            return CandidateQualityMetrics.losslessMetrics();
        }

        AfmaFramePairAnalysis driftAnalysis = new AfmaFramePairAnalysis(sourceFrame, outputFrame);
        if (driftAnalysis.isIdentical()) {
            return CandidateQualityMetrics.losslessMetrics();
        }
        AfmaFramePairAnalysis.PerceptualDriftMetrics driftMetrics = driftAnalysis.perceptualDriftMetrics();
        return new CandidateQualityMetrics(
                false,
                driftMetrics.averageError(),
                driftMetrics.maxVisibleColorDelta(),
                driftMetrics.maxAlphaDelta()
        );
    }

    /** Builds the exact full candidate for the AFMA creator. */
    @NotNull
    protected FrameCandidate createExactFullCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                      boolean introSequence, int frameIndex,
                                                      @NotNull AfmaEncodeOptions options) throws IOException {
        return this.createFullCandidate(currentFrame, introSequence, frameIndex, options, false);
    }

    /** Builds the measured full candidate for the AFMA creator. */
    @NotNull
    protected FrameCandidate createMeasuredFullCandidate(@NotNull AfmaPixelFrame sourceFrame,
                                                         @NotNull AfmaPixelFrame encodedFrameInput,
                                                         boolean introSequence, int frameIndex,
                                                         @NotNull AfmaEncodeOptions options,
                                                         boolean allowPerceptual) throws IOException {
        return this.withQualityMetrics(
                this.createFullCandidate(encodedFrameInput, introSequence, frameIndex, options, allowPerceptual),
                sourceFrame
        );
    }

    /** Builds the full candidate for the AFMA creator. */
    @NotNull
    protected FrameCandidate createFullCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                 boolean introSequence, int frameIndex,
                                                 @NotNull AfmaEncodeOptions options,
                                                 boolean allowPerceptual) throws IOException {
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayload(
                currentFrame,
                options,
                allowPerceptual
        );
        return new FrameCandidate(
                CandidateKind.FULL,
                AfmaFrameDescriptor.full(payloadPath),
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                null,
                null,
                encodedPayload.lossless()
                        ? currentFrame
                        : new AfmaPixelFrame(currentFrame.getWidth(), currentFrame.getHeight(), encodedPayload.reconstructedPixels()),
                1,
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the best delta family candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createBestDeltaFamilyCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                            @NotNull AfmaPixelFrame currentFrame,
                                                            @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                            boolean introSequence, int frameIndex,
                                                            @NotNull AfmaRect deltaBounds,
                                                            @NotNull AfmaEncodeOptions options,
                                                            boolean allowPerceptual) throws IOException {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height()
        );
        if (regionAnalysis == null) {
            return null;
        }

        List<StrategyEstimate> rankedStrategies = this.rankDeltaStrategies(regionAnalysis);
        IOException lastException = null;
        for (StrategyEstimate strategy : rankedStrategies) {
            try {
                FrameCandidate candidate = switch (strategy.kind()) {
                    case DELTA_BIN_INTRA -> this.createDeltaBinIntraCandidate(
                            previousFrame,
                            currentFrame,
                            introSequence,
                            frameIndex,
                            deltaBounds,
                            options,
                            allowPerceptual
                    );
                    case DELTA_RESIDUAL -> this.createResidualDeltaCandidate(pairAnalysis, currentFrame, introSequence, frameIndex, deltaBounds);
                    case DELTA_SPARSE -> this.createSparseDeltaCandidate(pairAnalysis, currentFrame, introSequence, frameIndex, deltaBounds);
                    default -> null;
                };
                if (candidate != null) {
                    return candidate;
                }
            } catch (IOException ex) {
                lastException = ex;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return null;
    }

    /** Builds the exact continuation candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createExactContinuationCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                              @NotNull AfmaPixelFrame sourceFrame,
                                                              @NotNull AfmaPixelFrame candidateFrame,
                                                              boolean introSequence,
                                                              int frameIndex,
                                                              @Nullable FrameCandidate mergedDeltaCandidate,
                                                              @NotNull AfmaEncodeOptions options) throws IOException {
        if ((candidateFrame == sourceFrame)
                || (mergedDeltaCandidate == null)
                || mergedDeltaCandidate.qualityMetrics().lossless()
                || ((mergedDeltaCandidate.kind() != CandidateKind.DELTA_SPARSE) && (mergedDeltaCandidate.kind() != CandidateKind.DELTA_RESIDUAL))) {
            return null;
        }

        AfmaFramePairAnalysis exactPairAnalysis = new AfmaFramePairAnalysis(previousFrame, sourceFrame);
        AfmaRect exactDeltaBounds = exactPairAnalysis.differenceBounds();
        if (exactDeltaBounds == null) {
            return new FrameCandidate(
                    CandidateKind.SAME,
                    AfmaFrameDescriptor.same(),
                    null,
                    null,
                    null,
                    null,
                    previousFrame,
                    0,
                    CandidateQualityMetrics.losslessMetrics()
            );
        }

        long frameArea = (long) sourceFrame.getWidth() * (long) sourceFrame.getHeight();
        if (frameArea <= 0L) {
            return null;
        }
        double changedRatio = (double) exactPairAnalysis.changedPixelCount() / (double) frameArea;
        if (changedRatio > MAX_SPARSE_DELTA_CHANGED_DENSITY) {
            return null;
        }

        return this.createBestDeltaFamilyCandidate(
                previousFrame,
                sourceFrame,
                exactPairAnalysis,
                introSequence,
                frameIndex,
                exactDeltaBounds,
                options,
                false
        );
    }

    /** Ranks the delta strategies for the AFMA creator. */
    @NotNull
    protected List<StrategyEstimate> rankDeltaStrategies(@NotNull AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis) {
        ResidualRegionStats residualStats = this.analyzeResidualStats(
                regionAnalysis.predictedColors(),
                regionAnalysis.currentColors(),
                regionAnalysis.pixelCount(),
                regionAnalysis.includeAlpha()
        );
        long binIntraEstimate = this.estimateBinIntraRegionBytes(
                regionAnalysis.width(),
                regionAnalysis.height(),
                regionAnalysis.pixelCount(),
                regionAnalysis.changedPixelCount(),
                regionAnalysis.includeAlpha()
        );
        ArrayList<StrategyEstimate> estimates = new ArrayList<>(3);
        estimates.add(new StrategyEstimate(CandidateKind.DELTA_BIN_INTRA, binIntraEstimate, CandidateKind.DELTA_BIN_INTRA.stabilityRank()));

        long denseResidualEstimate = this.estimateResidualEncodedBytes(
                regionAnalysis.pixelCount(),
                AfmaResidualPayloadHelper.channelCount(regionAnalysis.includeAlpha()),
                residualStats.averageMagnitude()
        );
        estimates.add(new StrategyEstimate(CandidateKind.DELTA_RESIDUAL, denseResidualEstimate, CandidateKind.DELTA_RESIDUAL.stabilityRank()));

        if (this.canAttemptSparse(regionAnalysis.pixelCount(), regionAnalysis.changedPixelCount())) {
            ChangedRegionData changedRegion = this.copyChangedRegion(regionAnalysis);
            long sparseLayoutEstimate = this.estimateBestSparseLayoutBytes(
                    regionAnalysis.width(),
                    regionAnalysis.height(),
                    changedRegion.changedIndices(),
                    changedRegion.changedCount()
            );
            long sparseResidualEstimate = this.estimateResidualEncodedBytes(
                    changedRegion.changedCount(),
                    AfmaResidualPayloadHelper.channelCount(regionAnalysis.includeAlpha()),
                    residualStats.averageChangedMagnitude()
            );
            estimates.add(new StrategyEstimate(
                    CandidateKind.DELTA_SPARSE,
                    sparseLayoutEstimate + sparseResidualEstimate,
                    CandidateKind.DELTA_SPARSE.stabilityRank()
            ));
        }
        estimates.sort(this::compareStrategyEstimates);
        return estimates;
    }

    /** Builds the delta bin intra candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createDeltaBinIntraCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                          @NotNull AfmaPixelFrame currentFrame,
                                                          boolean introSequence, int frameIndex,
                                                          @NotNull AfmaRect deltaBounds,
                                                          @NotNull AfmaEncodeOptions options,
                                                          boolean allowPerceptual) throws IOException {
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                currentFrame,
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height(),
                options,
                allowPerceptual
        );
        AfmaPixelFrame outputFrame = encodedPayload.lossless()
                ? currentFrame
                : this.applyReconstructedPatch(previousFrame, deltaBounds, encodedPayload.reconstructedPixels());
        return new FrameCandidate(
                CandidateKind.DELTA_BIN_INTRA,
                AfmaFrameDescriptor.deltaRect(
                        payloadPath,
                        deltaBounds.x(),
                        deltaBounds.y(),
                        deltaBounds.width(),
                        deltaBounds.height()
                ),
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                null,
                null,
                outputFrame,
                2,
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the residual delta candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createResidualDeltaCandidate(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                          @NotNull AfmaPixelFrame currentFrame,
                                                          boolean introSequence, int frameIndex,
                                                          @NotNull AfmaRect deltaBounds) throws IOException {
        ResidualPayloadData residualPayload = this.buildResidualPayload(pairAnalysis, deltaBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "r");
        return new FrameCandidate(
                CandidateKind.DELTA_RESIDUAL,
                AfmaFrameDescriptor.residualDeltaRect(
                        payloadPath,
                        deltaBounds.x(),
                        deltaBounds.y(),
                        deltaBounds.width(),
                        deltaBounds.height(),
                        residualPayload.metadata()
                ),
                payloadPath,
                this.storePayload(residualPayload.payloadSummary(), residualPayload.payloadWriter()),
                null,
                null,
                currentFrame,
                3 + residualPayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the sparse delta candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createSparseDeltaCandidate(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                        @NotNull AfmaPixelFrame currentFrame,
                                                        boolean introSequence, int frameIndex,
                                                        @NotNull AfmaRect deltaBounds) throws IOException {
        SparseResidualPayloadData sparsePayload = this.buildSparseDeltaPayload(pairAnalysis, deltaBounds);
        if (sparsePayload == null) {
            return null;
        }

        String layoutPath = this.buildRawPayloadPath(introSequence, frameIndex, "m");
        String residualPath = this.buildRawPayloadPath(introSequence, frameIndex, "s");
        return new FrameCandidate(
                CandidateKind.DELTA_SPARSE,
                AfmaFrameDescriptor.sparseDeltaRect(
                        layoutPath,
                        deltaBounds.x(),
                        deltaBounds.y(),
                        deltaBounds.width(),
                        deltaBounds.height(),
                        sparsePayload.toMetadata(residualPath)
                ),
                layoutPath,
                this.storePayload(sparsePayload.layoutPayload()),
                residualPath,
                this.storePayload(sparsePayload.residualPayloadSummary(), sparsePayload.residualPayloadWriter()),
                currentFrame,
                4 + sparsePayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Evaluates the copy detections for AFMA creator candidate selection. */
    @NotNull
    protected CopyEvaluation evaluateCopyDetections(@NotNull AfmaPixelFrame previousFrame,
                                                    @NotNull AfmaPixelFrame currentFrame,
                                                    @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                    @NotNull AfmaRectCopyDetector copyDetector) {
        AfmaRectCopyDetector.Detection singleDetection = copyDetector.detect(pairAnalysis);
        AfmaRectCopyDetector.MultiDetection multiDetection = (singleDetection != null)
                ? copyDetector.detectMulti(pairAnalysis, singleDetection)
                : copyDetector.detectMulti(pairAnalysis);
        return new CopyEvaluation(singleDetection, multiDetection);
    }

    /** Builds the best copy family candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createBestCopyFamilyCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                           @NotNull AfmaPixelFrame currentFrame,
                                                           @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                           boolean introSequence, int frameIndex,
                                                           @NotNull AfmaEncodeOptions options,
                                                           boolean allowPerceptual,
                                                           @NotNull CopyEvaluation copyEvaluation) throws IOException {
        CopyPlan bestPlan = null;
        if (copyEvaluation.singleDetection() != null) {
            bestPlan = this.pickBetterCopyPlan(bestPlan, this.buildSingleCopyPlan(pairAnalysis, copyEvaluation.singleDetection()));
        }
        if (copyEvaluation.multiDetection() != null) {
            bestPlan = this.pickBetterCopyPlan(bestPlan, this.buildMultiCopyPlan(previousFrame, currentFrame, copyEvaluation.multiDetection()));
        }
        if (bestPlan == null) {
            return null;
        }

        IOException lastException = null;
        for (StrategyEstimate strategy : bestPlan.rankedStrategies()) {
            try {
                FrameCandidate candidate;
                if (bestPlan.multiCopy() != null) {
                    candidate = switch (strategy.kind()) {
                        case MULTI_COPY_BIN_INTRA -> this.createMultiCopyBinIntraCandidate(
                                previousFrame,
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.multiDetection()),
                                options,
                                allowPerceptual
                        );
                        case MULTI_COPY_RESIDUAL -> this.createMultiCopyResidualCandidate(
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.multiDetection()),
                                Objects.requireNonNull(bestPlan.referenceFrameAfterCopy())
                        );
                        case MULTI_COPY_SPARSE -> this.createMultiCopySparseCandidate(
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.multiDetection()),
                                Objects.requireNonNull(bestPlan.referenceFrameAfterCopy())
                        );
                        default -> null;
                    };
                } else {
                    candidate = switch (strategy.kind()) {
                        case COPY_BIN_INTRA -> this.createCopyBinIntraCandidate(
                                previousFrame,
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.singleDetection()),
                                options,
                                allowPerceptual
                        );
                        case COPY_RESIDUAL -> this.createCopyResidualCandidate(
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.singleDetection()),
                                pairAnalysis
                        );
                        case COPY_SPARSE -> this.createCopySparseCandidate(
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.singleDetection()),
                                pairAnalysis
                        );
                        default -> null;
                    };
                }
                if (candidate != null) {
                    return candidate;
                }
            } catch (IOException ex) {
                lastException = ex;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        return null;
    }

    /** Builds the single copy plan for the AFMA creator. */
    @Nullable
    protected CopyPlan buildSingleCopyPlan(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                           @NotNull AfmaRectCopyDetector.Detection detection) {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return new CopyPlan(
                    null,
                    detection.copyRect(),
                    null,
                    null,
                    List.of(new StrategyEstimate(CandidateKind.COPY_BIN_INTRA, 0L, CandidateKind.COPY_BIN_INTRA.stabilityRank()))
            );
        }

        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.copyAdjustedRegionDiffAnalysis(
                detection.copyRect(),
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height()
        );
        if (regionAnalysis == null) {
            return null;
        }
        return new CopyPlan(
                null,
                detection.copyRect(),
                null,
                patchBounds,
                this.rankCopyStrategies(regionAnalysis, false)
        );
    }

    /** Builds the multi copy plan for the AFMA creator. */
    @Nullable
    protected CopyPlan buildMultiCopyPlan(@NotNull AfmaPixelFrame previousFrame,
                                          @NotNull AfmaPixelFrame currentFrame,
                                          @NotNull AfmaRectCopyDetector.MultiDetection detection) {
        AfmaRect patchBounds = detection.patchBounds();
        AfmaPixelFrame referenceFrame = this.buildMultiCopyReferenceFrame(previousFrame, detection.multiCopy());
        if (patchBounds == null) {
            return new CopyPlan(
                    referenceFrame,
                    null,
                    detection.multiCopy(),
                    null,
                    List.of(new StrategyEstimate(CandidateKind.MULTI_COPY_BIN_INTRA, 0L, CandidateKind.MULTI_COPY_BIN_INTRA.stabilityRank()))
            );
        }

        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(referenceFrame, currentFrame);
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height()
        );
        if (regionAnalysis == null) {
            return null;
        }
        return new CopyPlan(
                referenceFrame,
                null,
                detection.multiCopy(),
                patchBounds,
                this.rankCopyStrategies(regionAnalysis, true)
        );
    }

    /** Selects the better copy plan for the AFMA creator. */
    @Nullable
    protected CopyPlan pickBetterCopyPlan(@Nullable CopyPlan first, @Nullable CopyPlan second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        StrategyEstimate firstEstimate = first.rankedStrategies().get(0);
        StrategyEstimate secondEstimate = second.rankedStrategies().get(0);
        int compare = this.compareStrategyEstimates(firstEstimate, secondEstimate);
        if (compare != 0) {
            return (compare <= 0) ? first : second;
        }

        long firstPatchArea = (first.patchBounds() != null) ? first.patchBounds().area() : 0L;
        long secondPatchArea = (second.patchBounds() != null) ? second.patchBounds().area() : 0L;
        if (firstPatchArea != secondPatchArea) {
            return (firstPatchArea < secondPatchArea) ? first : second;
        }
        return (first.multiCopy() == null) ? first : second;
    }

    /** Ranks the copy strategies for the AFMA creator. */
    @NotNull
    protected List<StrategyEstimate> rankCopyStrategies(@NotNull AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis,
                                                        boolean multiCopy) {
        ResidualRegionStats residualStats = this.analyzeResidualStats(
                regionAnalysis.predictedColors(),
                regionAnalysis.currentColors(),
                regionAnalysis.pixelCount(),
                regionAnalysis.includeAlpha()
        );
        CandidateKind binIntraKind = multiCopy ? CandidateKind.MULTI_COPY_BIN_INTRA : CandidateKind.COPY_BIN_INTRA;
        CandidateKind residualKind = multiCopy ? CandidateKind.MULTI_COPY_RESIDUAL : CandidateKind.COPY_RESIDUAL;
        CandidateKind sparseKind = multiCopy ? CandidateKind.MULTI_COPY_SPARSE : CandidateKind.COPY_SPARSE;

        ArrayList<StrategyEstimate> estimates = new ArrayList<>(3);
        estimates.add(new StrategyEstimate(
                binIntraKind,
                this.estimateBinIntraRegionBytes(
                        regionAnalysis.width(),
                        regionAnalysis.height(),
                        regionAnalysis.pixelCount(),
                        regionAnalysis.changedPixelCount(),
                        regionAnalysis.includeAlpha()
                ),
                binIntraKind.stabilityRank()
        ));
        estimates.add(new StrategyEstimate(
                residualKind,
                this.estimateResidualEncodedBytes(
                        regionAnalysis.pixelCount(),
                        AfmaResidualPayloadHelper.channelCount(regionAnalysis.includeAlpha()),
                        residualStats.averageMagnitude()
                ),
                residualKind.stabilityRank()
        ));

        if (this.canAttemptSparse(regionAnalysis.pixelCount(), regionAnalysis.changedPixelCount())) {
            ChangedRegionData changedRegion = this.copyChangedRegion(regionAnalysis);
            estimates.add(new StrategyEstimate(
                    sparseKind,
                    this.estimateBestSparseLayoutBytes(regionAnalysis.width(), regionAnalysis.height(), changedRegion.changedIndices(), changedRegion.changedCount())
                            + this.estimateResidualEncodedBytes(
                            changedRegion.changedCount(),
                            AfmaResidualPayloadHelper.channelCount(regionAnalysis.includeAlpha()),
                            residualStats.averageChangedMagnitude()
                    ),
                    sparseKind.stabilityRank()
            ));
        }

        estimates.sort(this::compareStrategyEstimates);
        return estimates;
    }

    /** Builds the copy bin intra candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createCopyBinIntraCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                         @NotNull AfmaPixelFrame currentFrame,
                                                         boolean introSequence, int frameIndex,
                                                         @NotNull AfmaRectCopyDetector.Detection detection,
                                                         @NotNull AfmaEncodeOptions options,
                                                         boolean allowPerceptual) throws IOException {
        AfmaCopyRect copyRect = detection.copyRect();
        AfmaRect patchBounds = detection.patchBounds();
        AfmaPixelFrame referenceFrame = this.buildSingleCopyReferenceFrame(previousFrame, copyRect);
        if (patchBounds == null) {
            return new FrameCandidate(
                    CandidateKind.COPY_BIN_INTRA,
                    AfmaFrameDescriptor.copyRectPatch(copyRect, null),
                    null,
                    null,
                    null,
                    null,
                    currentFrame,
                    3,
                    CandidateQualityMetrics.losslessMetrics()
            );
        }

        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                currentFrame,
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height(),
                options,
                allowPerceptual
        );
        AfmaPatchRegion patchRegion = patchBounds.toPatchRegion(payloadPath);
        AfmaPixelFrame outputFrame = encodedPayload.lossless()
                ? currentFrame
                : this.applyReconstructedPatch(referenceFrame, patchBounds, encodedPayload.reconstructedPixels());
        return new FrameCandidate(
                CandidateKind.COPY_BIN_INTRA,
                AfmaFrameDescriptor.copyRectPatch(copyRect, patchRegion),
                null,
                null,
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                outputFrame,
                3,
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the copy residual candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createCopyResidualCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                         boolean introSequence, int frameIndex,
                                                         @NotNull AfmaRectCopyDetector.Detection detection,
                                                         @NotNull AfmaFramePairAnalysis pairAnalysis) {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return null;
        }

        ResidualPayloadData residualPayload = this.buildCopyResidualPayload(pairAnalysis, detection.copyRect(), patchBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "cr");
        return new FrameCandidate(
                CandidateKind.COPY_RESIDUAL,
                AfmaFrameDescriptor.copyRectResidualPatch(
                        detection.copyRect(),
                        payloadPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        residualPayload.metadata()
                ),
                payloadPath,
                this.storePayload(residualPayload.payloadSummary(), residualPayload.payloadWriter()),
                null,
                null,
                currentFrame,
                4 + residualPayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the copy sparse candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createCopySparseCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                       boolean introSequence, int frameIndex,
                                                       @NotNull AfmaRectCopyDetector.Detection detection,
                                                       @NotNull AfmaFramePairAnalysis pairAnalysis) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return null;
        }

        SparseResidualPayloadData sparsePayload = this.buildCopySparsePayload(pairAnalysis, detection.copyRect(), patchBounds);
        if (sparsePayload == null) {
            return null;
        }

        String layoutPath = this.buildRawPayloadPath(introSequence, frameIndex, "cm");
        String residualPath = this.buildRawPayloadPath(introSequence, frameIndex, "cs");
        return new FrameCandidate(
                CandidateKind.COPY_SPARSE,
                AfmaFrameDescriptor.copyRectSparsePatch(
                        detection.copyRect(),
                        layoutPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        sparsePayload.toMetadata(residualPath)
                ),
                layoutPath,
                this.storePayload(sparsePayload.layoutPayload()),
                residualPath,
                this.storePayload(sparsePayload.residualPayloadSummary(), sparsePayload.residualPayloadWriter()),
                currentFrame,
                5 + sparsePayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the multi copy bin intra candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createMultiCopyBinIntraCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                              @NotNull AfmaPixelFrame currentFrame,
                                                              boolean introSequence, int frameIndex,
                                                              @NotNull AfmaRectCopyDetector.MultiDetection detection,
                                                              @NotNull AfmaEncodeOptions options,
                                                              boolean allowPerceptual) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return new FrameCandidate(
                    CandidateKind.MULTI_COPY_BIN_INTRA,
                    AfmaFrameDescriptor.multiCopyPatch(detection.multiCopy(), null),
                    null,
                    null,
                    null,
                    null,
                    currentFrame,
                    3 + detection.multiCopy().getCopyRectCount(),
                    CandidateQualityMetrics.losslessMetrics()
            );
        }

        AfmaPixelFrame referenceFrame = this.buildMultiCopyReferenceFrame(previousFrame, detection.multiCopy());
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                currentFrame,
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height(),
                options,
                allowPerceptual
        );
        AfmaPatchRegion patchRegion = patchBounds.toPatchRegion(payloadPath);
        AfmaPixelFrame outputFrame = encodedPayload.lossless()
                ? currentFrame
                : this.applyReconstructedPatch(referenceFrame, patchBounds, encodedPayload.reconstructedPixels());
        return new FrameCandidate(
                CandidateKind.MULTI_COPY_BIN_INTRA,
                AfmaFrameDescriptor.multiCopyPatch(detection.multiCopy(), patchRegion),
                null,
                null,
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                outputFrame,
                3 + detection.multiCopy().getCopyRectCount(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the multi copy residual candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createMultiCopyResidualCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                              boolean introSequence, int frameIndex,
                                                              @NotNull AfmaRectCopyDetector.MultiDetection detection,
                                                              @NotNull AfmaPixelFrame referenceFrame) {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return null;
        }

        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(referenceFrame, currentFrame);
        ResidualPayloadData residualPayload = this.buildResidualPayload(pairAnalysis, patchBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "mr");
        return new FrameCandidate(
                CandidateKind.MULTI_COPY_RESIDUAL,
                AfmaFrameDescriptor.multiCopyResidualPatch(
                        detection.multiCopy(),
                        payloadPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        residualPayload.metadata()
                ),
                payloadPath,
                this.storePayload(residualPayload.payloadSummary(), residualPayload.payloadWriter()),
                null,
                null,
                currentFrame,
                4 + detection.multiCopy().getCopyRectCount() + residualPayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the multi copy sparse candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createMultiCopySparseCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                            boolean introSequence, int frameIndex,
                                                            @NotNull AfmaRectCopyDetector.MultiDetection detection,
                                                            @NotNull AfmaPixelFrame referenceFrame) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return null;
        }

        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(referenceFrame, currentFrame);
        SparseResidualPayloadData sparsePayload = this.buildSparseDeltaPayload(pairAnalysis, patchBounds);
        if (sparsePayload == null) {
            return null;
        }

        String layoutPath = this.buildRawPayloadPath(introSequence, frameIndex, "mm");
        String residualPath = this.buildRawPayloadPath(introSequence, frameIndex, "ms");
        return new FrameCandidate(
                CandidateKind.MULTI_COPY_SPARSE,
                AfmaFrameDescriptor.multiCopySparsePatch(
                        detection.multiCopy(),
                        layoutPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        sparsePayload.toMetadata(residualPath)
                ),
                layoutPath,
                this.storePayload(sparsePayload.layoutPayload()),
                residualPath,
                this.storePayload(sparsePayload.residualPayloadSummary(), sparsePayload.residualPayloadWriter()),
                currentFrame,
                5 + detection.multiCopy().getCopyRectCount() + sparsePayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Builds the block inter family candidate for the AFMA creator. */
    @Nullable
    protected FrameCandidate createBlockInterFamilyCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                             @NotNull AfmaPixelFrame currentFrame,
                                                             @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                             boolean introSequence, int frameIndex,
                                                             @NotNull AfmaRect deltaBounds,
                                                             @NotNull AfmaRectCopyDetector copyDetector,
                                                             @NotNull CopyEvaluation copyEvaluation,
                                                             long referenceBytes,
                                                             @Nullable ExecutorService executor,
                                                             @Nullable BooleanSupplier cancellationRequested) throws IOException {
        if (deltaBounds.area() < BLOCK_INTER_MIN_REGION_AREA) {
            return null;
        }

        AfmaRect regionBounds = this.alignBoundsToTileGrid(deltaBounds, BLOCK_INTER_TILE_SIZE, currentFrame.getWidth(), currentFrame.getHeight());
        int tileCountX = AfmaBlockInterPayloadHelper.tileCount(regionBounds.width(), BLOCK_INTER_TILE_SIZE);
        int tileCountY = AfmaBlockInterPayloadHelper.tileCount(regionBounds.height(), BLOCK_INTER_TILE_SIZE);
        int totalTileCount = tileCountX * tileCountY;
        if (totalTileCount < 4) {
            return null;
        }

        List<MotionVector> motionVectors = this.collectBlockInterMotionVectors(copyDetector, pairAnalysis, copyEvaluation);
        if (motionVectors.isEmpty()) {
            return null;
        }

        ApproxBlockInterTile[] tilePlans = this.planApproximateBlockInterTiles(
                previousFrame,
                currentFrame,
                pairAnalysis,
                regionBounds,
                tileCountX,
                tileCountY,
                motionVectors,
                executor,
                cancellationRequested
        );
        if (tilePlans == null) {
            return null;
        }

        long estimatedPayloadBytes = 9L;
        int changedTiles = 0;
        for (ApproxBlockInterTile tilePlan : tilePlans) {
            estimatedPayloadBytes += tilePlan.estimatedBytes();
            if (tilePlan.mode() != AfmaBlockInterPayloadHelper.TileMode.SKIP) {
                changedTiles++;
            }
        }
        if (changedTiles < 2) {
            return null;
        }

        long estimatedTotalBytes = estimatedPayloadBytes
                + this.estimateDescriptorBytes(AfmaFrameDescriptor.blockInter(
                this.buildRawPayloadPath(introSequence, frameIndex, "bi"),
                regionBounds.x(),
                regionBounds.y(),
                regionBounds.width(),
                regionBounds.height(),
                new AfmaBlockInter(BLOCK_INTER_TILE_SIZE)
        ));
        if (estimatedTotalBytes >= Math.round(referenceBytes * BLOCK_INTER_REQUIRED_SAVINGS_RATIO)
                && (estimatedTotalBytes + FAMILY_SWITCH_MARGIN_BYTES) >= referenceBytes) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "bi");
        List<AfmaBlockInterPayloadHelper.TileOperation> tileOperations = this.materializeBlockInterTileOperations(
                tilePlans,
                previousFrame,
                currentFrame,
                pairAnalysis
        );
        AfmaStoredPayload.Writer payloadWriter = out -> AfmaBlockInterPayloadHelper.writePayload(
                out,
                BLOCK_INTER_TILE_SIZE,
                regionBounds.width(),
                regionBounds.height(),
                tileOperations
        );
        return new FrameCandidate(
                CandidateKind.BLOCK_INTER,
                AfmaFrameDescriptor.blockInter(
                        payloadPath,
                        regionBounds.x(),
                        regionBounds.y(),
                        regionBounds.width(),
                        regionBounds.height(),
                        new AfmaBlockInter(BLOCK_INTER_TILE_SIZE)
                ),
                payloadPath,
                this.storePayload(AfmaStoredPayload.summarize(payloadWriter), payloadWriter),
                null,
                null,
                currentFrame,
                7,
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    /** Plans the approximate block inter tiles for the AFMA creator. */
    @Nullable
    protected ApproxBlockInterTile[] planApproximateBlockInterTiles(@NotNull AfmaPixelFrame previousFrame,
                                                                    @NotNull AfmaPixelFrame currentFrame,
                                                                    @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                    @NotNull AfmaRect regionBounds,
                                                                    int tileCountX, int tileCountY,
                                                                    @NotNull List<MotionVector> motionVectors,
                                                                    @Nullable ExecutorService executor,
                                                                    @Nullable BooleanSupplier cancellationRequested) {
        AfmaFramePairAnalysis.TileGridSummary tileGridSummary = pairAnalysis.tileGridSummary(BLOCK_INTER_TILE_SIZE);
        int baseTileX = regionBounds.x() / BLOCK_INTER_TILE_SIZE;
        int baseTileY = regionBounds.y() / BLOCK_INTER_TILE_SIZE;
        int totalTileCount = tileCountX * tileCountY;
        ApproxBlockInterTile[] tilePlans = new ApproxBlockInterTile[totalTileCount];
        if ((executor != null) && (totalTileCount >= MIN_PARALLEL_BLOCK_INTER_TILES) && (Runtime.getRuntime().availableProcessors() > 1)) {
            int workerCount = Math.min(totalTileCount, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
            int tileBatchSize = Math.max(1, (totalTileCount + workerCount - 1) / workerCount);
            ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(workerCount);
            for (int startTileIndex = 0; startTileIndex < totalTileCount; startTileIndex += tileBatchSize) {
                int rangeStart = startTileIndex;
                int rangeEnd = Math.min(totalTileCount, rangeStart + tileBatchSize);
                futures.add(CompletableFuture.runAsync(() -> this.planApproximateBlockInterTileRange(
                        previousFrame,
                        currentFrame,
                        tileGridSummary,
                        regionBounds,
                        tileCountX,
                        tileCountY,
                        baseTileX,
                        baseTileY,
                        rangeStart,
                        rangeEnd,
                        motionVectors,
                        tilePlans,
                        cancellationRequested
                ), executor));
            }
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof CancellationException cancellationException) {
                    throw cancellationException;
                }
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException("Failed to approximate AFMA v2 block_inter tiles", cause);
            }
        } else {
            this.planApproximateBlockInterTileRange(
                    previousFrame,
                    currentFrame,
                    tileGridSummary,
                    regionBounds,
                    tileCountX,
                    tileCountY,
                    baseTileX,
                    baseTileY,
                    0,
                    totalTileCount,
                    motionVectors,
                    tilePlans,
                    cancellationRequested
            );
        }
        return tilePlans;
    }

    /** Plans the approximate block inter tile range for the AFMA creator. */
    protected void planApproximateBlockInterTileRange(@NotNull AfmaPixelFrame previousFrame,
                                                      @NotNull AfmaPixelFrame currentFrame,
                                                      @NotNull AfmaFramePairAnalysis.TileGridSummary tileGridSummary,
                                                      @NotNull AfmaRect regionBounds,
                                                      int tileCountX,
                                                      int tileCountY,
                                                      int baseTileX,
                                                      int baseTileY,
                                                      int startTileIndex,
                                                      int endTileIndex,
                                                      @NotNull List<MotionVector> motionVectors,
                                                      @NotNull ApproxBlockInterTile[] tilePlans,
                                                      @Nullable BooleanSupplier cancellationRequested) {
        for (int tileIndex = startTileIndex; tileIndex < endTileIndex; tileIndex++) {
            checkCancelled(cancellationRequested);
            int tileY = tileIndex / tileCountX;
            int tileX = tileIndex % tileCountX;
            int dstX = regionBounds.x() + (tileX * BLOCK_INTER_TILE_SIZE);
            int dstY = regionBounds.y() + (tileY * BLOCK_INTER_TILE_SIZE);
            int width = AfmaBlockInterPayloadHelper.tileDimension(tileX, tileCountX, BLOCK_INTER_TILE_SIZE, regionBounds.width());
            int height = AfmaBlockInterPayloadHelper.tileDimension(tileY, tileCountY, BLOCK_INTER_TILE_SIZE, regionBounds.height());
            AfmaFramePairAnalysis.TileStats tileStats = tileGridSummary.tileStats(baseTileX + tileX, baseTileY + tileY);
            tilePlans[tileIndex] = this.planApproximateBlockInterTile(previousFrame, currentFrame, tileStats, dstX, dstY, width, height, motionVectors);
        }
    }

    /** Plans the approximate block inter tile for the AFMA creator. */
    @NotNull
    protected ApproxBlockInterTile planApproximateBlockInterTile(@NotNull AfmaPixelFrame previousFrame,
                                                                 @NotNull AfmaPixelFrame currentFrame,
                                                                 @NotNull AfmaFramePairAnalysis.TileStats tileStats,
                                                                 int dstX, int dstY, int width, int height,
                                                                 @NotNull List<MotionVector> motionVectors) {
        int rawChannels = tileStats.nextHasNonOpaquePixels()
                ? AfmaResidualPayloadHelper.RGBA_CHANNELS
                : AfmaResidualPayloadHelper.RGB_CHANNELS;
        ApproxBlockInterTile bestTile = new ApproxBlockInterTile(
                AfmaBlockInterPayloadHelper.TileMode.RAW,
                dstX,
                dstY,
                width,
                height,
                0,
                0,
                rawChannels,
                0,
                0,
                this.estimateBlockInterTileBytes(
                        AfmaBlockInterPayloadHelper.TileMode.RAW,
                        AfmaBlockInterPayloadHelper.expectedRawTileBytes(width, height, rawChannels),
                        0
                )
        );
        if (tileStats.isIdentical()) {
            return new ApproxBlockInterTile(
                    AfmaBlockInterPayloadHelper.TileMode.SKIP,
                    dstX,
                    dstY,
                    width,
                    height,
                    0,
                    0,
                    0,
                    0,
                    0,
                    this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.SKIP, 0, 0)
            );
        }

        for (MotionVector motionVector : motionVectors) {
            int srcX = dstX + motionVector.dx();
            int srcY = dstY + motionVector.dy();
            if (!this.isMotionTileInBounds(previousFrame, srcX, srcY, width, height)) {
                continue;
            }

            MotionTileStats motionStats = this.scanMotionTile(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height);
            if (motionStats.changedPixelCount() <= 0) {
                ApproxBlockInterTile copyTile = new ApproxBlockInterTile(
                        AfmaBlockInterPayloadHelper.TileMode.COPY,
                        dstX,
                        dstY,
                        width,
                        height,
                        motionVector.dx(),
                        motionVector.dy(),
                        0,
                        0,
                        0,
                        this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY, 0, 0)
                );
                if (copyTile.estimatedBytes() < bestTile.estimatedBytes()) {
                    bestTile = copyTile;
                }
                continue;
            }

            int motionChannels = AfmaResidualPayloadHelper.channelCount(motionStats.includeAlpha());
            long denseBytes = this.estimateBlockInterTileBytes(
                    AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                    AfmaBlockInterPayloadHelper.expectedDenseResidualBytes(width, height, motionChannels),
                    0
            );
            ApproxBlockInterTile denseTile = new ApproxBlockInterTile(
                    AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                    dstX,
                    dstY,
                    width,
                    height,
                    motionVector.dx(),
                    motionVector.dy(),
                    motionChannels,
                    motionStats.changedPixelCount(),
                    motionStats.changedPixelCount(),
                    denseBytes
            );
            bestTile = this.pickBetterBlockInterTile(bestTile, denseTile);

            if (this.canAttemptSparse(width * height, motionStats.changedPixelCount())) {
                long sparseBytes = this.estimateBlockInterTileBytes(
                        AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                        this.estimateOptimisticSparseLayoutBytes(width, height, motionStats.changedPixelCount()),
                        this.estimateResidualEncodedBytes(motionStats.changedPixelCount(), motionChannels, 8D)
                );
                ApproxBlockInterTile sparseTile = new ApproxBlockInterTile(
                        AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                        dstX,
                        dstY,
                        width,
                        height,
                        motionVector.dx(),
                        motionVector.dy(),
                        motionChannels,
                        motionStats.changedPixelCount(),
                        motionStats.changedPixelCount(),
                        sparseBytes
                );
                bestTile = this.pickBetterBlockInterTile(bestTile, sparseTile);
            }
        }

        return bestTile;
    }

    /** Selects the better block inter tile for the AFMA creator. */
    @NotNull
    protected ApproxBlockInterTile pickBetterBlockInterTile(@NotNull ApproxBlockInterTile first, @NotNull ApproxBlockInterTile second) {
        if (first.estimatedBytes() != second.estimatedBytes()) {
            return (first.estimatedBytes() < second.estimatedBytes()) ? first : second;
        }
        if (first.mode() != second.mode()) {
            return (first.mode().ordinal() <= second.mode().ordinal()) ? first : second;
        }
        return (first.changedPixelCount() <= second.changedPixelCount()) ? first : second;
    }

    /** Collects the block inter motion vectors for the AFMA creator. */
    @NotNull
    protected List<MotionVector> collectBlockInterMotionVectors(@NotNull AfmaRectCopyDetector copyDetector,
                                                                @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                @NotNull CopyEvaluation copyEvaluation) {
        LinkedHashSet<MotionVector> vectors = new LinkedHashSet<>();
        if (copyEvaluation.singleDetection() != null) {
            AfmaCopyRect copyRect = copyEvaluation.singleDetection().copyRect();
            vectors.add(new MotionVector(copyRect.getSrcX() - copyRect.getDstX(), copyRect.getSrcY() - copyRect.getDstY()));
        }
        for (AfmaRectCopyDetector.MotionVector motionVector : copyDetector.collectMotionVectors(pairAnalysis, false)) {
            vectors.add(new MotionVector(-motionVector.dx(), -motionVector.dy()));
            if (vectors.size() >= MAX_BLOCK_INTER_MOTION_VECTORS) {
                break;
            }
        }
        if (vectors.isEmpty()) {
            return List.of();
        }
        return List.copyOf(vectors);
    }

    /** Materializes the block inter tile operations for the AFMA creator. */
    @NotNull
    protected List<AfmaBlockInterPayloadHelper.TileOperation> materializeBlockInterTileOperations(@NotNull ApproxBlockInterTile[] tilePlans,
                                                                                                  @NotNull AfmaPixelFrame previousFrame,
                                                                                                  @NotNull AfmaPixelFrame currentFrame,
                                                                                                  @NotNull AfmaFramePairAnalysis pairAnalysis) {
        List<AfmaBlockInterPayloadHelper.TileOperation> operations = new ArrayList<>(tilePlans.length);
        for (ApproxBlockInterTile tilePlan : tilePlans) {
            operations.add(this.materializeBlockInterTileOperation(tilePlan, previousFrame, currentFrame, pairAnalysis));
        }
        return operations;
    }

    /** Materializes the block inter tile operation for the AFMA creator. */
    @NotNull
    protected AfmaBlockInterPayloadHelper.TileOperation materializeBlockInterTileOperation(@NotNull ApproxBlockInterTile tilePlan,
                                                                                           @NotNull AfmaPixelFrame previousFrame,
                                                                                           @NotNull AfmaPixelFrame currentFrame,
                                                                                           @NotNull AfmaFramePairAnalysis pairAnalysis) {
        return switch (tilePlan.mode()) {
            case SKIP -> new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.SKIP, 0, 0, 0, 0, null, null, null);
            case COPY -> new AfmaBlockInterPayloadHelper.TileOperation(
                    AfmaBlockInterPayloadHelper.TileMode.COPY,
                    tilePlan.dx(),
                    tilePlan.dy(),
                    0,
                    0,
                    null,
                    null,
                    null
            );
            case COPY_DENSE -> {
                byte[] denseResidual = Objects.requireNonNull(
                        this.buildMotionResidualPayload(
                                previousFrame,
                                currentFrame,
                                tilePlan.dstX(),
                                tilePlan.dstY(),
                                tilePlan.dstX() + tilePlan.dx(),
                                tilePlan.dstY() + tilePlan.dy(),
                                tilePlan.width(),
                                tilePlan.height(),
                                tilePlan.channels() == AfmaResidualPayloadHelper.RGBA_CHANNELS
                        ),
                        "AFMA v2 block_inter dense residual payload was NULL"
                );
                yield new AfmaBlockInterPayloadHelper.TileOperation(
                        AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                        tilePlan.dx(),
                        tilePlan.dy(),
                        tilePlan.channels(),
                        0,
                        denseResidual,
                        null,
                        null
                );
            }
            case COPY_SPARSE -> {
                MotionTileStats tileStats = new MotionTileStats(tilePlan.changedPixelCount(), tilePlan.channels() == AfmaResidualPayloadHelper.RGBA_CHANNELS);
                SparseResidualPayloadData sparsePayload = this.buildMotionSparseResidualPayload(
                        pairAnalysis,
                        previousFrame,
                        currentFrame,
                        tilePlan.dstX(),
                        tilePlan.dstY(),
                        tilePlan.dstX() + tilePlan.dx(),
                        tilePlan.dstY() + tilePlan.dy(),
                        tilePlan.width(),
                        tilePlan.height(),
                        tileStats
                );
                if (sparsePayload != null) {
                    yield new AfmaBlockInterPayloadHelper.TileOperation(
                            AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                            tilePlan.dx(),
                            tilePlan.dy(),
                            sparsePayload.channels(),
                            sparsePayload.changedPixelCount(),
                            sparsePayload.layoutPayload(),
                            sparsePayload.materializeResidualPayload(),
                            sparsePayload.toMetadata(null)
                    );
                }

                byte[] denseResidual = Objects.requireNonNull(
                        this.buildMotionResidualPayload(
                                previousFrame,
                                currentFrame,
                                tilePlan.dstX(),
                                tilePlan.dstY(),
                                tilePlan.dstX() + tilePlan.dx(),
                                tilePlan.dstY() + tilePlan.dy(),
                                tilePlan.width(),
                                tilePlan.height(),
                                tilePlan.channels() == AfmaResidualPayloadHelper.RGBA_CHANNELS
                        ),
                        "AFMA v2 block_inter dense fallback payload was NULL"
                );
                yield new AfmaBlockInterPayloadHelper.TileOperation(
                        AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                        tilePlan.dx(),
                        tilePlan.dy(),
                        tilePlan.channels(),
                        0,
                        denseResidual,
                        null,
                        null
                );
            }
            case RAW -> new AfmaBlockInterPayloadHelper.TileOperation(
                    AfmaBlockInterPayloadHelper.TileMode.RAW,
                    0,
                    0,
                    tilePlan.channels(),
                    0,
                    this.buildRawTileBytes(currentFrame, tilePlan.dstX(), tilePlan.dstY(), tilePlan.width(), tilePlan.height(), tilePlan.channels()),
                    null,
                    null
            );
        };
    }

    /** Builds the single copy reference frame for the AFMA creator. */
    @NotNull
    protected AfmaPixelFrame buildSingleCopyReferenceFrame(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaCopyRect copyRect) {
        int[] copiedPixels = previousFrame.copyPixels();
        AfmaPixelFrameHelper.applyCopyRect(copiedPixels, previousFrame.getWidth(), copyRect);
        return new AfmaPixelFrame(previousFrame.getWidth(), previousFrame.getHeight(), copiedPixels);
    }

    /** Builds the multi copy reference frame for the AFMA creator. */
    @NotNull
    protected AfmaPixelFrame buildMultiCopyReferenceFrame(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaMultiCopy multiCopy) {
        int[] copiedPixels = previousFrame.copyPixels();
        AfmaPixelFrameHelper.applyCopyRects(copiedPixels, previousFrame.getWidth(), multiCopy.getCopyRects());
        return new AfmaPixelFrame(previousFrame.getWidth(), previousFrame.getHeight(), copiedPixels);
    }

    /** Applies the reconstructed patch to the AFMA creator. */
    @NotNull
    protected AfmaPixelFrame applyReconstructedPatch(@NotNull AfmaPixelFrame referenceFrame,
                                                     @NotNull AfmaRect patchBounds,
                                                     @NotNull int[] reconstructedPixels) {
        int[] pixels = referenceFrame.copyPixels();
        int frameWidth = referenceFrame.getWidth();
        int offset = 0;
        for (int localY = 0; localY < patchBounds.height(); localY++) {
            int rowOffset = ((patchBounds.y() + localY) * frameWidth) + patchBounds.x();
            System.arraycopy(reconstructedPixels, offset, pixels, rowOffset, patchBounds.width());
            offset += patchBounds.width();
        }
        return new AfmaPixelFrame(referenceFrame.getWidth(), referenceFrame.getHeight(), pixels);
    }

    /** Returns whether attempt sparse. */
    protected boolean canAttemptSparse(int pixelCount, int changedPixelCount) {
        if (pixelCount <= 0 || changedPixelCount < MIN_SPARSE_DELTA_CHANGED_PIXELS || changedPixelCount >= pixelCount) {
            return false;
        }
        return ((double) changedPixelCount / (double) pixelCount) <= MAX_SPARSE_DELTA_CHANGED_DENSITY;
    }

    /** Compares the strategy estimates values for the AFMA creator. */
    protected int compareStrategyEstimates(@NotNull StrategyEstimate first, @NotNull StrategyEstimate second) {
        long minBytes = Math.min(first.estimatedBytes(), second.estimatedBytes());
        long margin = Math.max(FAMILY_SWITCH_MARGIN_BYTES, Math.round(minBytes * FAMILY_SWITCH_MARGIN_RATIO));
        long delta = first.estimatedBytes() - second.estimatedBytes();
        if (Math.abs(delta) <= margin) {
            return Integer.compare(first.stabilityRank(), second.stabilityRank());
        }
        return Long.compare(first.estimatedBytes(), second.estimatedBytes());
    }

    /** Calculates the bin intra region bytes for the AFMA creator. */
    protected long estimateBinIntraRegionBytes(int width, int height, int pixelCount, int changedPixelCount, boolean includeAlpha) {
        double density = (pixelCount > 0) ? ((double) changedPixelCount / (double) pixelCount) : 1D;
        double bytesPerPixel = includeAlpha ? 2.00D : 1.60D;
        if (pixelCount <= 4096) {
            bytesPerPixel += 0.18D;
        }
        if (density <= 0.25D) {
            bytesPerPixel *= 0.92D;
        }
        if (density <= 0.10D) {
            bytesPerPixel *= 0.88D;
        }
        return Math.max(24L, Math.round(24D + ((long) width * height * bytesPerPixel)));
    }

    /** Calculates the residual encoded bytes for the AFMA creator. */
    protected long estimateResidualEncodedBytes(int sampleCount, int channels, double averageMagnitude) {
        if (sampleCount <= 0 || channels <= 0) {
            return 0L;
        }
        double compressionFactor;
        if (averageMagnitude <= 4D) {
            compressionFactor = 0.34D;
        } else if (averageMagnitude <= 10D) {
            compressionFactor = 0.48D;
        } else if (averageMagnitude <= 20D) {
            compressionFactor = 0.62D;
        } else {
            compressionFactor = 0.78D;
        }
        return Math.max(8L, Math.round(8D + ((long) sampleCount * channels * compressionFactor)));
    }

    /** Calculates the best sparse layout bytes for the AFMA creator. */
    protected long estimateBestSparseLayoutBytes(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) {
        SparseLayoutEstimate estimate = this.estimateSparseLayoutBytes(width, height, changedIndices, changedPixelCount);
        return Math.min(Math.min(estimate.bitmaskBytes(), estimate.rowSpanBytes()), Math.min(estimate.tileMaskBytes(), estimate.coordListBytes()));
    }

    /** Calculates the optimistic sparse layout bytes for the AFMA creator. */
    protected long estimateOptimisticSparseLayoutBytes(int width, int height, int changedPixelCount) {
        long bitmaskBytes = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        long coordListBytes = Math.max(1L, changedPixelCount) * 2L;
        long rowSpanBytes = Math.max(4L, changedPixelCount * 2L);
        long tileSize = AfmaSparsePayloadHelper.TILE_MASK_TILE_SIZE;
        long tileCountX = (width + tileSize - 1L) / tileSize;
        long tileCountY = (height + tileSize - 1L) / tileSize;
        long tileMaskBytes = AfmaResidualPayloadHelper.expectedSparseBitsetBytes(tileCountX * tileCountY)
                + (Math.max(1L, (changedPixelCount + 15L) / 16L) * 8L);
        return Math.min(Math.min(bitmaskBytes, coordListBytes), Math.min(rowSpanBytes, tileMaskBytes));
    }

    /** Analyzes the residual stats for the AFMA creator. */
    @NotNull
    protected ResidualRegionStats analyzeResidualStats(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                       int sampleCount, boolean includeAlpha) {
        if (sampleCount <= 0) {
            return ResidualRegionStats.EMPTY;
        }

        long totalMagnitude = 0L;
        int changedSamples = 0;
        long changedMagnitude = 0L;
        for (int i = 0; i < sampleCount; i++) {
            int predictedColor = predictedColors[i];
            int currentColor = currentColors[i];
            int magnitude = channelDifference(predictedColor >> 16, currentColor >> 16)
                    + channelDifference(predictedColor >> 8, currentColor >> 8)
                    + channelDifference(predictedColor, currentColor);
            if (includeAlpha) {
                magnitude += channelDifference(predictedColor >>> 24, currentColor >>> 24);
            }
            totalMagnitude += magnitude;
            if (predictedColor != currentColor) {
                changedSamples++;
                changedMagnitude += magnitude;
            }
        }
        return new ResidualRegionStats(
                totalMagnitude / (double) sampleCount,
                (changedSamples > 0) ? (changedMagnitude / (double) changedSamples) : 0D
        );
    }

    /** Copies the changed region for the AFMA creator. */
    @NotNull
    protected ChangedRegionData copyChangedRegion(@NotNull AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis) {
        int changedPixelCount = regionAnalysis.changedPixelCount();
        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        int[] changedIndices = Arrays.copyOf(workspace.changedIndices(changedPixelCount), changedPixelCount);
        int[] predictedColors = Arrays.copyOf(workspace.predictedColors(changedPixelCount), changedPixelCount);
        int[] currentColors = Arrays.copyOf(workspace.currentColors(changedPixelCount), changedPixelCount);
        regionAnalysis.copyChangedPixelsTo(changedIndices, predictedColors, currentColors);
        return new ChangedRegionData(changedIndices, predictedColors, currentColors, changedPixelCount);
    }

    /** Builds the residual payload for the AFMA creator. */
    @Nullable
    protected ResidualPayloadData buildResidualPayload(@NotNull AfmaFramePairAnalysis pairAnalysis, @NotNull AfmaRect deltaBounds) {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height()
        );
        return this.buildResidualPayload(regionAnalysis);
    }

    /** Builds the residual payload for the AFMA creator. */
    @Nullable
    protected ResidualPayloadData buildResidualPayload(@Nullable AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis) {
        if ((regionAnalysis == null) || (regionAnalysis.pixelCount() <= 0)) {
            return null;
        }

        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        AfmaResidualPayloadHelper.EncodedResidualPayload encodedPayload = AfmaResidualPayloadHelper.encodeBestResidualPayload(
                regionAnalysis.predictedColors(),
                regionAnalysis.currentColors(),
                regionAnalysis.pixelCount(),
                regionAnalysis.includeAlpha(),
                workspace.residualEncodeWorkspace()
        );
        return new ResidualPayloadData(
                encodedPayload.payloadSummary(),
                encodedPayload.payloadWriter(),
                encodedPayload.toResidualMetadata(),
                encodedPayload.complexityScore()
        );
    }

    /** Builds the copy residual payload for the AFMA creator. */
    @Nullable
    protected ResidualPayloadData buildCopyResidualPayload(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                           @NotNull AfmaCopyRect copyRect,
                                                           @NotNull AfmaRect patchBounds) {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.copyAdjustedRegionDiffAnalysis(
                copyRect,
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height()
        );
        return this.buildResidualPayload(regionAnalysis);
    }

    /** Builds the sparse delta payload for the AFMA creator. */
    @Nullable
    protected SparseResidualPayloadData buildSparseDeltaPayload(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                @NotNull AfmaRect deltaBounds) throws IOException {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height()
        );
        return this.buildSparseResidualPayload(regionAnalysis);
    }

    /** Builds the copy sparse payload for the AFMA creator. */
    @Nullable
    protected SparseResidualPayloadData buildCopySparsePayload(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                               @NotNull AfmaCopyRect copyRect,
                                                               @NotNull AfmaRect patchBounds) throws IOException {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.copyAdjustedRegionDiffAnalysis(
                copyRect,
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height()
        );
        return this.buildSparseResidualPayload(regionAnalysis);
    }

    /** Builds the sparse residual payload for the AFMA creator. */
    @Nullable
    protected SparseResidualPayloadData buildSparseResidualPayload(@Nullable AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis) throws IOException {
        if (regionAnalysis == null || !this.canAttemptSparse(regionAnalysis.pixelCount(), regionAnalysis.changedPixelCount())) {
            return null;
        }

        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        int changedPixelCount = regionAnalysis.changedPixelCount();
        int[] changedIndices = workspace.changedIndices(changedPixelCount);
        int[] predictedColors = workspace.predictedColors(changedPixelCount);
        int[] currentColors = workspace.currentColors(changedPixelCount);
        regionAnalysis.copyChangedPixelsTo(changedIndices, predictedColors, currentColors);
        return this.buildSparseResidualPayload(
                regionAnalysis.width(),
                regionAnalysis.height(),
                changedIndices,
                predictedColors,
                currentColors,
                changedPixelCount,
                regionAnalysis.includeAlpha()
        );
    }

    /** Builds the sparse residual payload for the AFMA creator. */
    @Nullable
    protected SparseResidualPayloadData buildSparseResidualPayload(int width, int height,
                                                                  @NotNull int[] changedIndices,
                                                                  @NotNull int[] predictedColors,
                                                                  @NotNull int[] currentColors,
                                                                  int changedPixelCount,
                                                                  boolean includeAlpha) throws IOException {
        if (!this.canAttemptSparse(width * height, changedPixelCount)) {
            return null;
        }

        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        AfmaResidualPayloadHelper.EncodedResidualPayload encodedResidual = AfmaResidualPayloadHelper.encodeBestResidualPayload(
                predictedColors,
                currentColors,
                changedPixelCount,
                includeAlpha,
                workspace.residualEncodeWorkspace()
        );
        SparseLayoutCandidate bestLayout = this.chooseBestSparseLayout(width, height, changedIndices, changedPixelCount);
        return new SparseResidualPayloadData(
                bestLayout.layoutPayload(),
                encodedResidual.payloadSummary(),
                encodedResidual.payloadWriter(),
                changedPixelCount,
                bestLayout.layoutCodec(),
                encodedResidual.channels(),
                encodedResidual.codec(),
                encodedResidual.alphaMode(),
                encodedResidual.alphaChangedPixelCount(),
                bestLayout.complexityScore() + encodedResidual.complexityScore()
        );
    }

    /** Selects the best sparse layout for the AFMA creator. */
    @NotNull
    protected SparseLayoutCandidate chooseBestSparseLayout(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) throws IOException {
        SparseLayoutCandidate bestCandidate = null;
        for (SparseLayoutPlan layoutPlan : this.selectSparseLayoutPlans(width, height, changedIndices, changedPixelCount)) {
            SparseLayoutCandidate candidate = this.buildSparseLayoutCandidate(layoutPlan.layoutCodec(), width, height, changedIndices, changedPixelCount);
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate;
            }
        }
        return Objects.requireNonNull(bestCandidate, "Failed to choose an AFMA v2 sparse layout");
    }

    /** Selects the sparse layout plans for the AFMA creator. */
    @NotNull
    protected ArrayList<SparseLayoutPlan> selectSparseLayoutPlans(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) {
        ArrayList<SparseLayoutPlan> layoutPlans = new ArrayList<>(4);
        SparseLayoutEstimate layoutEstimate = this.estimateSparseLayoutBytes(width, height, changedIndices, changedPixelCount);
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.BITMASK, layoutEstimate.bitmaskBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.ROW_SPANS, layoutEstimate.rowSpanBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.TILE_MASK, layoutEstimate.tileMaskBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.COORD_LIST, layoutEstimate.coordListBytes()));
        layoutPlans.sort((first, second) -> {
            int sizeCompare = Long.compare(first.estimatedRawBytes(), second.estimatedRawBytes());
            if (sizeCompare != 0) {
                return sizeCompare;
            }
            return Integer.compare(first.layoutCodec().getComplexityScore(), second.layoutCodec().getComplexityScore());
        });
        return layoutPlans;
    }

    /** Calculates the sparse layout bytes for the AFMA creator. */
    @NotNull
    protected SparseLayoutEstimate estimateSparseLayoutBytes(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) {
        long bitmaskBytes = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        long coordListBytes = 0L;
        int previousIndex = -1;

        int tileSize = AfmaSparsePayloadHelper.TILE_MASK_TILE_SIZE;
        int tileCountX = (width + tileSize - 1) / tileSize;
        int tileCountY = (height + tileSize - 1) / tileSize;
        int tileCount = tileCountX * tileCountY;
        long tileMaskBytes = AfmaResidualPayloadHelper.expectedSparseBitsetBytes(tileCount);
        boolean[] activeTiles = new boolean[Math.max(0, tileCount)];

        for (int i = 0; i < changedPixelCount; i++) {
            int changedIndex = changedIndices[i];
            coordListBytes += estimateVarIntBytes(changedIndex - previousIndex - 1);
            previousIndex = changedIndex;

            int x = changedIndex % width;
            int y = changedIndex / width;
            int tileX = x / tileSize;
            int tileY = y / tileSize;
            int tileIndex = (tileY * tileCountX) + tileX;
            if (!activeTiles[tileIndex]) {
                activeTiles[tileIndex] = true;
                int tileWidth = Math.min(tileSize, width - (tileX * tileSize));
                int tileHeight = Math.min(tileSize, height - (tileY * tileSize));
                tileMaskBytes += AfmaResidualPayloadHelper.expectedSparseMaskBytes(tileWidth, tileHeight);
            }
        }

        return new SparseLayoutEstimate(
                bitmaskBytes,
                this.estimateRowSpanLayoutBytes(width, changedIndices, changedPixelCount),
                tileMaskBytes,
                coordListBytes
        );
    }

    /** Calculates the row span layout bytes for the AFMA creator. */
    protected long estimateRowSpanLayoutBytes(int width, @NotNull int[] changedIndices, int changedPixelCount) {
        if (changedPixelCount <= 0) {
            return 0L;
        }

        long totalBytes = 0L;
        int changedRowCount = 0;
        int cursor = 0;
        int previousRow = -1;
        while (cursor < changedPixelCount) {
            changedRowCount++;
            int row = changedIndices[cursor] / width;
            totalBytes += estimateVarIntBytes(row - previousRow - 1);
            previousRow = row;

            int previousEndX = 0;
            int spanCount = 0;
            long rowSpanBytes = 0L;
            while ((cursor < changedPixelCount) && ((changedIndices[cursor] / width) == row)) {
                int startX = changedIndices[cursor] % width;
                int endX = startX + 1;
                cursor++;
                while ((cursor < changedPixelCount)
                        && ((changedIndices[cursor] / width) == row)
                        && ((changedIndices[cursor] % width) == endX)) {
                    endX++;
                    cursor++;
                }
                spanCount++;
                rowSpanBytes += estimateVarIntBytes(startX - previousEndX);
                rowSpanBytes += estimateVarIntBytes(endX - startX - 1);
                previousEndX = endX;
            }
            totalBytes += estimateVarIntBytes(spanCount) + rowSpanBytes;
        }
        return estimateVarIntBytes(changedRowCount) + totalBytes;
    }

    /** Builds the sparse layout candidate for the AFMA creator. */
    @NotNull
    protected SparseLayoutCandidate buildSparseLayoutCandidate(@NotNull AfmaSparseLayoutCodec layoutCodec, int width, int height,
                                                               @NotNull int[] changedIndices, int changedPixelCount) throws IOException {
        return switch (layoutCodec) {
            case BITMASK -> new SparseLayoutCandidate(
                    layoutCodec,
                    AfmaSparsePayloadHelper.buildBitmaskLayout(width, height, changedIndices, changedPixelCount),
                    layoutCodec.getComplexityScore()
            );
            case ROW_SPANS -> new SparseLayoutCandidate(
                    layoutCodec,
                    AfmaSparsePayloadHelper.buildRowSpanLayout(width, changedIndices, changedPixelCount),
                    layoutCodec.getComplexityScore()
            );
            case TILE_MASK -> new SparseLayoutCandidate(
                    layoutCodec,
                    AfmaSparsePayloadHelper.buildTileMaskLayout(width, height, changedIndices, changedPixelCount),
                    layoutCodec.getComplexityScore()
            );
            case COORD_LIST -> new SparseLayoutCandidate(
                    layoutCodec,
                    AfmaSparsePayloadHelper.buildCoordListLayout(changedIndices, changedPixelCount),
                    layoutCodec.getComplexityScore()
            );
        };
    }

    /** Builds the motion residual payload for the AFMA creator. */
    @Nullable
    protected byte[] buildMotionResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                int dstX, int dstY, int srcX, int srcY, int width, int height, boolean includeAlpha) {
        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
        int expectedBytes = AfmaBlockInterPayloadHelper.expectedDenseResidualBytes(width, height, channels);
        if (expectedBytes <= 0) {
            return null;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        byte[] payloadBytes = new byte[expectedBytes];
        int payloadOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            int previousRowOffset = ((srcY + localY) * frameWidth) + srcX;
            int currentRowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int predictedColor = previousPixels[previousRowOffset + localX];
                int currentColor = currentPixels[currentRowOffset + localX];
                payloadOffset = AfmaResidualPayloadHelper.writeResidual(payloadBytes, payloadOffset, predictedColor, currentColor, includeAlpha);
            }
        }
        return payloadBytes;
    }

    /** Builds the motion sparse residual payload for the AFMA creator. */
    @Nullable
    protected SparseResidualPayloadData buildMotionSparseResidualPayload(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                         @NotNull AfmaPixelFrame previousFrame,
                                                                         @NotNull AfmaPixelFrame currentFrame,
                                                                         int dstX, int dstY, int srcX, int srcY,
                                                                         int width, int height,
                                                                         @NotNull MotionTileStats tileStats) {
        if (tileStats.changedPixelCount() <= 0 || tileStats.changedPixelCount() >= (width * height)) {
            return null;
        }

        if ((dstX == srcX) && (dstY == srcY)) {
            try {
                AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(dstX, dstY, width, height);
                return (regionAnalysis != null) ? this.buildSparseResidualPayload(regionAnalysis) : null;
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to encode AFMA v2 block_inter sparse tile payload", ex);
            }
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int changedPixelCount = tileStats.changedPixelCount();
        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        int[] changedIndices = workspace.changedIndices(changedPixelCount);
        int[] predictedColors = workspace.predictedColors(changedPixelCount);
        int[] changedColors = workspace.currentColors(changedPixelCount);
        int changedOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            int previousRowOffset = ((srcY + localY) * frameWidth) + srcX;
            int currentRowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int predictedColor = previousPixels[previousRowOffset + localX];
                int currentColor = currentPixels[currentRowOffset + localX];
                if (predictedColor == currentColor) {
                    continue;
                }
                changedIndices[changedOffset] = (localY * width) + localX;
                predictedColors[changedOffset] = predictedColor;
                changedColors[changedOffset] = currentColor;
                changedOffset++;
            }
        }

        try {
            return this.buildSparseResidualPayload(width, height, changedIndices, predictedColors, changedColors, changedPixelCount, tileStats.includeAlpha());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to encode AFMA v2 block_inter sparse tile payload", ex);
        }
    }

    /** Iterates over the motion tile in the AFMA creator data. */
    @NotNull
    protected MotionTileStats scanMotionTile(@NotNull AfmaPixelFrame previousFrame,
                                             @NotNull AfmaPixelFrame currentFrame,
                                             int dstX, int dstY, int srcX, int srcY, int width, int height) {
        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int changedPixelCount = 0;
        boolean includeAlpha = false;
        for (int localY = 0; localY < height; localY++) {
            int previousRowOffset = ((srcY + localY) * frameWidth) + srcX;
            int currentRowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int predictedColor = previousPixels[previousRowOffset + localX];
                int currentColor = currentPixels[currentRowOffset + localX];
                if (predictedColor == currentColor) {
                    continue;
                }
                changedPixelCount++;
                if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                    includeAlpha = true;
                }
            }
        }
        return new MotionTileStats(changedPixelCount, includeAlpha);
    }

    /** Returns whether motion tile in bounds. */
    protected boolean isMotionTileInBounds(@NotNull AfmaPixelFrame previousFrame, int srcX, int srcY, int width, int height) {
        return srcX >= 0
                && srcY >= 0
                && (srcX + width) <= previousFrame.getWidth()
                && (srcY + height) <= previousFrame.getHeight();
    }

    /** Builds the raw tile bytes for the AFMA creator. */
    @NotNull
    protected byte[] buildRawTileBytes(@NotNull AfmaPixelFrame currentFrame, int dstX, int dstY, int width, int height, int channels) {
        int frameWidth = currentFrame.getWidth();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        boolean includeAlpha = channels == AfmaResidualPayloadHelper.RGBA_CHANNELS;
        byte[] payloadBytes = new byte[AfmaBlockInterPayloadHelper.expectedRawTileBytes(width, height, channels)];
        int payloadOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            int rowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int color = currentPixels[rowOffset + localX];
                payloadBytes[payloadOffset++] = (byte) ((color >> 16) & 0xFF);
                payloadBytes[payloadOffset++] = (byte) ((color >> 8) & 0xFF);
                payloadBytes[payloadOffset++] = (byte) (color & 0xFF);
                if (includeAlpha) {
                    payloadBytes[payloadOffset++] = (byte) ((color >>> 24) & 0xFF);
                }
            }
        }
        return payloadBytes;
    }

    /** Calculates the block inter tile bytes for the AFMA creator. */
    protected long estimateBlockInterTileBytes(@NotNull AfmaBlockInterPayloadHelper.TileMode mode, long primaryBytes, long secondaryBytes) {
        return switch (mode) {
            case SKIP -> 1L;
            case COPY -> 5L;
            case COPY_DENSE -> 6L + primaryBytes;
            case COPY_SPARSE -> 15L + primaryBytes + secondaryBytes;
            case RAW -> 2L + primaryBytes;
        };
    }

    /** Normalizes the bounds to tile grid for the AFMA creator. */
    @NotNull
    protected AfmaRect alignBoundsToTileGrid(@NotNull AfmaRect bounds, int tileSize, int canvasWidth, int canvasHeight) {
        int minX = Math.max(0, (bounds.x() / tileSize) * tileSize);
        int minY = Math.max(0, (bounds.y() / tileSize) * tileSize);
        int maxX = Math.min(canvasWidth, ((bounds.x() + bounds.width() + tileSize - 1) / tileSize) * tileSize);
        int maxY = Math.min(canvasHeight, ((bounds.y() + bounds.height() + tileSize - 1) / tileSize) * tileSize);
        return new AfmaRect(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
    }

    /** Stores the payload in the AFMA creator. */
    @NotNull
    protected DeferredPayload storePayload(@NotNull byte[] payloadBytes) {
        return DeferredPayload.fromBytes(payloadBytes);
    }

    /** Stores the payload in the AFMA creator. */
    @NotNull
    protected DeferredPayload storePayload(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary, @NotNull byte[] payloadBytes) {
        return DeferredPayload.fromBytes(payloadSummary, payloadBytes);
    }

    /** Stores the payload in the AFMA creator. */
    @NotNull
    protected DeferredPayload storePayload(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                           @NotNull AfmaStoredPayload.Writer payloadWriter) {
        return DeferredPayload.fromWriter(payloadSummary, payloadWriter);
    }

    /** Applies the near lossless temporal merge to the AFMA creator. */
    @NotNull
    protected AfmaPixelFrame applyNearLosslessTemporalMerge(@NotNull AfmaPixelFrame previousFrame,
                                                            @NotNull AfmaPixelFrame currentFrame,
                                                            int maxChannelDelta) {
        AfmaPixelFrameHelper.ensureSameSize(previousFrame, currentFrame);
        if (maxChannelDelta <= 0) {
            return currentFrame;
        }

        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int[] mergedPixels = null;
        for (int pixelIndex = 0; pixelIndex < currentPixels.length; pixelIndex++) {
            int previousColor = previousPixels[pixelIndex];
            int currentColor = currentPixels[pixelIndex];
            if (!shouldMergeNearLossless(previousColor, currentColor, maxChannelDelta)) {
                continue;
            }
            if (mergedPixels == null) {
                mergedPixels = Arrays.copyOf(currentPixels, currentPixels.length);
            }
            mergedPixels[pixelIndex] = previousColor;
        }
        if (mergedPixels == null) {
            return currentFrame;
        }
        return new AfmaPixelFrame(currentFrame.getWidth(), currentFrame.getHeight(), mergedPixels);
    }

    /** Returns whether merge near lossless. */
    protected static boolean shouldMergeNearLossless(int previousColor, int currentColor, int maxChannelDelta) {
        if (previousColor == currentColor) {
            return false;
        }
        if (((previousColor ^ currentColor) & 0xFF000000) != 0) {
            return false;
        }
        return channelDifference(previousColor >> 16, currentColor >> 16) <= maxChannelDelta
                && channelDifference(previousColor >> 8, currentColor >> 8) <= maxChannelDelta
                && channelDifference(previousColor, currentColor) <= maxChannelDelta;
    }

    /** Calculates the channel difference value for the AFMA creator. */
    protected static int channelDifference(int first, int second) {
        return Math.abs((first & 0xFF) - (second & 0xFF));
    }

    /** Returns whether allow perceptual continuation. */
    protected boolean shouldAllowPerceptualContinuation(int framesSinceKeyframe, @NotNull AfmaEncodeOptions options) {
        int maxInterval = options.isAdaptiveKeyframePlacementEnabled()
                ? options.getAdaptiveMaxKeyframeInterval()
                : options.getKeyframeInterval();
        return options.isPerceptualBinIntraEnabled() && ((framesSinceKeyframe + 1) < maxInterval);
    }

    /** Scores the bin intra payload for AFMA creator candidate selection. */
    @NotNull
    protected AfmaBinIntraPayloadHelper.ScoredPayloadResult scoreBinIntraPayload(@NotNull AfmaPixelFrame frame,
                                                                                 @NotNull AfmaEncodeOptions options,
                                                                                 boolean allowPerceptual) throws IOException {
        return AfmaBinIntraPayloadHelper.scorePayloadDetailed(
                frame.getWidth(),
                frame.getHeight(),
                frame.getPixelsUnsafe(),
                0,
                frame.getWidth(),
                this.resolveBinIntraEncodePreferences(options, allowPerceptual)
        );
    }

    /** Scores the bin intra payload region for AFMA creator candidate selection. */
    @NotNull
    protected AfmaBinIntraPayloadHelper.ScoredPayloadResult scoreBinIntraPayloadRegion(@NotNull AfmaPixelFrame frame,
                                                                                        int x, int y, int width, int height,
                                                                                        @NotNull AfmaEncodeOptions options,
                                                                                        boolean allowPerceptual) throws IOException {
        return AfmaBinIntraPayloadHelper.scorePayloadDetailed(
                width,
                height,
                frame.getPixelsUnsafe(),
                (y * frame.getWidth()) + x,
                frame.getWidth(),
                this.resolveBinIntraEncodePreferences(options, allowPerceptual)
        );
    }

    /** Calculates the full candidate metrics for the AFMA creator. */
    @NotNull
    protected EstimatedFullCandidateMetrics estimateFullCandidateMetrics(@NotNull AfmaPixelFrame frame,
                                                                         boolean introSequence,
                                                                         int frameIndex,
                                                                         @NotNull AfmaEncodeOptions options,
                                                                         boolean allowPerceptual) throws IOException {
        int width = frame.getWidth();
        int height = frame.getHeight();
        int pixelCount = width * height;
        boolean hasAlpha = this.frameHasNonOpaquePixels(frame);
        long estimatedPayloadBytes = this.estimateBinIntraRegionBytes(width, height, pixelCount, pixelCount, hasAlpha);
        double estimateScale = (allowPerceptual && options.isPerceptualBinIntraEnabled())
                ? PERCEPTUAL_FULL_ESTIMATE_SCALE
                : LOSSLESS_FULL_ESTIMATE_SCALE;
        estimatedPayloadBytes = Math.max(32L, Math.round(estimatedPayloadBytes * estimateScale));
        long estimatedDescriptorBytes = new AfmaV2DescriptorSizer().estimate(
                AfmaFrameDescriptor.full(this.buildPayloadPath(introSequence, frameIndex))
        );
        return new EstimatedFullCandidateMetrics(
                estimatedPayloadBytes + estimatedDescriptorBytes,
                !allowPerceptual
        );
    }

    /** Returns whether frame has non opaque pixels. */
    protected boolean frameHasNonOpaquePixels(@NotNull AfmaPixelFrame frame) {
        for (int color : frame.getPixelsUnsafe()) {
            if ((color >>> 24) != 0xFF) {
                return true;
            }
        }
        return false;
    }

    /** Resolves the bin intra encode preferences for the AFMA creator. */
    @NotNull
    protected AfmaBinIntraPayloadHelper.EncodePreferences resolveBinIntraEncodePreferences(@NotNull AfmaEncodeOptions options,
                                                                                           boolean allowPerceptual) {
        if (allowPerceptual && options.isPerceptualBinIntraEnabled()) {
            return AfmaBinIntraPayloadHelper.EncodePreferences.perceptual(
                    options.getPerceptualBinIntraMaxVisibleColorDelta(),
                    options.getPerceptualBinIntraMaxAlphaDelta(),
                    options.getPerceptualBinIntraMaxAverageError()
            );
        }
        return AfmaBinIntraPayloadHelper.EncodePreferences.lossless();
    }

    /** Resolves the source frame delay for the AFMA creator. */
    protected long resolveSourceFrameDelay(@NotNull AfmaEncodeOptions options, boolean introSequence, int frameIndex) {
        Map<Integer, Long> customFrameTimes = introSequence ? options.getCustomIntroFrameTimes() : options.getCustomFrameTimes();
        Long customDelay = customFrameTimes.get(frameIndex);
        if ((customDelay != null) && (customDelay > 0L)) {
            return customDelay;
        }
        return this.resolveSequenceDefaultDelay(options, introSequence);
    }

    /** Resolves the sequence default delay for the AFMA creator. */
    protected long resolveSequenceDefaultDelay(@NotNull AfmaEncodeOptions options, boolean introSequence) {
        return introSequence ? options.getIntroFrameTimeMs() : options.getFrameTimeMs();
    }

    /** Extends the planned frame delay for the AFMA creator. */
    protected void extendPlannedFrameDelay(@NotNull List<PlannedTimedFrame> plannedFrames, long additionalDelayMs) {
        if (plannedFrames.isEmpty()) {
            throw new IllegalStateException("AFMA v2 temporal frame collapsing requires at least one emitted frame");
        }

        int lastIndex = plannedFrames.size() - 1;
        plannedFrames.set(lastIndex, plannedFrames.get(lastIndex).withAdditionalDelay(additionalDelayMs));
    }

    /** Builds the planned sequence for the AFMA creator. */
    @NotNull
    protected PlannedSequence buildPlannedSequence(@NotNull List<PlannedTimedFrame> plannedFrames, long fallbackDefaultDelayMs) {
        List<AfmaFrameDescriptor> descriptors = new ArrayList<>(plannedFrames.size());
        List<Long> frameDelays = new ArrayList<>(plannedFrames.size());
        for (PlannedTimedFrame plannedFrame : plannedFrames) {
            descriptors.add(plannedFrame.descriptor());
            frameDelays.add(plannedFrame.delayMs());
        }
        AdaptiveTiming adaptiveTiming = this.buildAdaptiveTiming(frameDelays, fallbackDefaultDelayMs);
        return new PlannedSequence(descriptors, adaptiveTiming.defaultDelayMs(), adaptiveTiming.customFrameTimes());
    }

    /** Builds the adaptive timing for the AFMA creator. */
    @NotNull
    protected AdaptiveTiming buildAdaptiveTiming(@NotNull List<Long> frameDelays, long fallbackDefaultDelayMs) {
        long normalizedFallbackDelay = Math.max(1L, fallbackDefaultDelayMs);
        if (frameDelays.isEmpty()) {
            return new AdaptiveTiming(normalizedFallbackDelay, new LinkedHashMap<>());
        }

        List<Long> normalizedFrameDelays = new ArrayList<>(frameDelays.size());
        LinkedHashSet<Long> candidateDefaultDelays = new LinkedHashSet<>();
        candidateDefaultDelays.add(normalizedFallbackDelay);
        for (Long frameDelay : frameDelays) {
            long normalizedDelay = Math.max(1L, Objects.requireNonNull(frameDelay));
            normalizedFrameDelays.add(normalizedDelay);
            candidateDefaultDelays.add(normalizedDelay);
        }

        long bestDefaultDelay = normalizedFallbackDelay;
        long bestCost = Long.MAX_VALUE;
        int bestOverrideCount = Integer.MAX_VALUE;
        for (Long candidateDefaultDelay : candidateDefaultDelays) {
            long candidateDelay = Objects.requireNonNull(candidateDefaultDelay);
            long cost = this.estimateTimingMetadataBytes(normalizedFrameDelays, candidateDelay);
            int overrideCount = this.countTimingOverrides(normalizedFrameDelays, candidateDelay);
            if ((cost < bestCost)
                    || ((cost == bestCost) && (overrideCount < bestOverrideCount))
                    || ((cost == bestCost) && (overrideCount == bestOverrideCount)
                    && (candidateDelay == normalizedFallbackDelay) && (bestDefaultDelay != normalizedFallbackDelay))) {
                bestDefaultDelay = candidateDelay;
                bestCost = cost;
                bestOverrideCount = overrideCount;
            }
        }

        LinkedHashMap<Integer, Long> customFrameTimes = new LinkedHashMap<>();
        for (int frameIndex = 0; frameIndex < normalizedFrameDelays.size(); frameIndex++) {
            long frameDelay = normalizedFrameDelays.get(frameIndex);
            if (frameDelay != bestDefaultDelay) {
                customFrameTimes.put(frameIndex, frameDelay);
            }
        }
        return new AdaptiveTiming(bestDefaultDelay, customFrameTimes);
    }

    /** Calculates the timing metadata bytes for the AFMA creator. */
    protected long estimateTimingMetadataBytes(@NotNull List<Long> frameDelays, long defaultDelayMs) {
        long bytes = decimalLength(defaultDelayMs);
        boolean hasCustomFrameTimes = false;
        for (int frameIndex = 0; frameIndex < frameDelays.size(); frameIndex++) {
            long frameDelay = frameDelays.get(frameIndex);
            if (frameDelay == defaultDelayMs) {
                continue;
            }
            hasCustomFrameTimes = true;
            bytes += decimalLength(frameIndex) + decimalLength(frameDelay) + 4L;
        }
        if (hasCustomFrameTimes) {
            bytes += 2L;
        }
        return bytes;
    }

    /** Counts the timing overrides in the AFMA creator. */
    protected int countTimingOverrides(@NotNull List<Long> frameDelays, long defaultDelayMs) {
        int count = 0;
        for (Long frameDelay : frameDelays) {
            if (Objects.requireNonNull(frameDelay) != defaultDelayMs) {
                count++;
            }
        }
        return count;
    }

    /** Calculates the decimal length value for the AFMA creator. */
    protected static int decimalLength(long value) {
        return Long.toString(Math.max(0L, value)).length();
    }

    /** Registers the delays saturating with this AFMA creator component. */
    protected static long addDelaysSaturating(long left, long right) {
        if ((Long.MAX_VALUE - left) < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    /** Builds the payload path for the AFMA creator. */
    @NotNull
    protected String buildPayloadPath(boolean introSequence, int frameIndex) {
        return (introSequence ? "intro_frames/" : "frames/") + Integer.toUnsignedString(frameIndex, 36) + ".bin";
    }

    /** Builds the raw payload path for the AFMA creator. */
    @NotNull
    protected String buildRawPayloadPath(boolean introSequence, int frameIndex, @NotNull String suffix) {
        return (introSequence ? "intro_frames/" : "frames/") + Integer.toUnsignedString(frameIndex, 36) + "_" + suffix + ".bin";
    }

    /** Calculates the var int bytes for the AFMA creator. */
    protected static int estimateVarIntBytes(int value) {
        if ((value & ~0x7F) == 0) {
            return 1;
        }
        if ((value & ~0x3FFF) == 0) {
            return 2;
        }
        if ((value & ~0x1FFFFF) == 0) {
            return 3;
        }
        if ((value & ~0xFFFFFFF) == 0) {
            return 4;
        }
        return 5;
    }

    /** Calculates the signed var int bytes for the AFMA creator. */
    protected static int estimateSignedVarIntBytes(int value) {
        int zigZag = (value << 1) ^ (value >> 31);
        return estimateVarIntBytes(zigZag);
    }

    /** Calculates the descriptor bytes for the AFMA creator. */
    protected int estimateDescriptorBytes(@NotNull AfmaFrameDescriptor descriptor) {
        AfmaFrameOperationType type = descriptor.getType();
        if (type == null) {
            return 1;
        }

        int bytes = 1;
        bytes += switch (type) {
            case FULL -> 2;
            case DELTA_RECT -> 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight());
            case RESIDUAL_DELTA_RECT -> 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels())
                    + 2
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
            case SPARSE_DELTA_RECT -> 4
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels())
                    + 3
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
            case SAME -> 0;
            case COPY_RECT_PATCH -> {
                int copyBytes = this.estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()));
                AfmaPatchRegion patch = descriptor.getPatch();
                if (patch == null) {
                    yield copyBytes + 1;
                }
                yield copyBytes + 1 + 2
                        + estimateVarIntBytes(patch.getX())
                        + estimateVarIntBytes(patch.getY())
                        + estimateVarIntBytes(patch.getWidth())
                        + estimateVarIntBytes(patch.getHeight());
            }
            case MULTI_COPY_PATCH -> {
                int copyBytes = this.estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()));
                AfmaPatchRegion patch = descriptor.getPatch();
                if (patch == null) {
                    yield copyBytes + 1;
                }
                yield copyBytes + 1 + 2
                        + estimateVarIntBytes(patch.getX())
                        + estimateVarIntBytes(patch.getY())
                        + estimateVarIntBytes(patch.getWidth())
                        + estimateVarIntBytes(patch.getHeight());
            }
            case COPY_RECT_RESIDUAL_PATCH -> this.estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()))
                    + 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels())
                    + 2
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
            case MULTI_COPY_RESIDUAL_PATCH -> this.estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()))
                    + 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels())
                    + 2
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
            case COPY_RECT_SPARSE_PATCH -> this.estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()))
                    + 4
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels())
                    + 3
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
            case MULTI_COPY_SPARSE_PATCH -> this.estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()))
                    + 4
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels())
                    + 3
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
            case BLOCK_INTER -> 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getBlockInter()).getTileSize());
        };
        return bytes;
    }

    /** Calculates the copy rect bytes for the AFMA creator. */
    protected int estimateCopyRectBytes(@NotNull AfmaCopyRect copyRect) {
        return estimateVarIntBytes(copyRect.getSrcX())
                + estimateVarIntBytes(copyRect.getSrcY())
                + estimateVarIntBytes(copyRect.getDstX())
                + estimateVarIntBytes(copyRect.getDstY())
                + estimateVarIntBytes(copyRect.getWidth())
                + estimateVarIntBytes(copyRect.getHeight());
    }

    /** Calculates the multi copy bytes for the AFMA creator. */
    protected int estimateMultiCopyBytes(@NotNull AfmaMultiCopy multiCopy) {
        List<AfmaCopyRect> copyRects = multiCopy.getCopyRects();
        int bytes = estimateVarIntBytes(copyRects.size() - 1);
        AfmaCopyRect previousRect = null;
        for (AfmaCopyRect copyRect : copyRects) {
            AfmaCopyRect currentRect = Objects.requireNonNull(copyRect);
            bytes += 1;
            if (previousRect == null) {
                bytes += this.estimateCopyRectBytes(currentRect);
            } else {
                bytes += estimateSignedVarIntBytes(currentRect.getSrcX() - previousRect.getSrcX());
                bytes += estimateSignedVarIntBytes(currentRect.getSrcY() - previousRect.getSrcY());
                bytes += estimateSignedVarIntBytes(currentRect.getDstX() - previousRect.getDstX());
                bytes += estimateSignedVarIntBytes(currentRect.getDstY() - previousRect.getDstY());
                if (currentRect.getWidth() != previousRect.getWidth()) {
                    bytes += estimateVarIntBytes(currentRect.getWidth());
                }
                if (currentRect.getHeight() != previousRect.getHeight()) {
                    bytes += estimateVarIntBytes(currentRect.getHeight());
                }
            }
            previousRect = currentRect;
        }
        return bytes;
    }

    /** Loads the dimension frame for the AFMA creator. */
    @NotNull
    protected LoadedDimensionFrame loadDimensionFrame(@NotNull AfmaSourceSequence sequence,
                                                      @NotNull AfmaFastPixelBufferPool pixelBufferPool,
                                                      @Nullable BooleanSupplier cancellationRequested,
                                                      @Nullable AfmaEncodePlanner.ProgressListener progressListener) throws IOException {
        if (sequence.isEmpty()) {
            throw new IOException("AFMA v2 encoding requires at least one source frame");
        }

        reportProgress(progressListener, "Reading source frame dimensions...", 0.02D);
        checkCancelled(cancellationRequested);
        File firstFrame = Objects.requireNonNull(sequence.getFrame(0));
        AfmaPixelFrame firstImage = this.frameNormalizer.loadFrame(firstFrame, pixelBufferPool);
        return new LoadedDimensionFrame(new Dimension(firstImage.getWidth(), firstImage.getHeight()), firstImage);
    }

    /** Reports the progress from the AFMA creator. */
    protected static void reportProgress(@Nullable AfmaEncodePlanner.ProgressListener progressListener,
                                         @NotNull String detail, double progress) {
        if (progressListener != null) {
            progressListener.update(detail, progress);
        }
    }

    /** Reports the planning frame progress from the AFMA creator. */
    protected void reportPlanningFrameProgress(@Nullable AfmaEncodePlanner.ProgressListener progressListener,
                                               @NotNull String action,
                                               boolean introSequence,
                                               int sequenceFrameNumber,
                                               int sequenceFrameCount,
                                               double absoluteFrameProgress,
                                               int totalFrameCount) {
        if (sequenceFrameCount <= 0) {
            return;
        }
        int clampedSequenceFrameNumber = Math.max(1, Math.min(sequenceFrameCount, sequenceFrameNumber));
        reportProgress(
                progressListener,
                action + " " + (introSequence ? "intro" : "main") + " frame " + clampedSequenceFrameNumber + "/" + sequenceFrameCount,
                this.computePlanningProgress(absoluteFrameProgress, totalFrameCount)
        );
    }

    /** Calculates the planning progress for the AFMA creator. */
    protected double computePlanningProgress(double absoluteFrameProgress, int totalFrameCount) {
        double clampedFrameProgress = Math.max(0D, Math.min(Math.max(1, totalFrameCount), absoluteFrameProgress));
        return 0.08D + (0.92D * (clampedFrameProgress / Math.max(1D, (double) totalFrameCount)));
    }

    /** Checks the cancelled before continuing the AFMA creator operation. */
    protected static void checkCancelled(@Nullable BooleanSupplier cancellationRequested) {
        if ((cancellationRequested != null) && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("AFMA v2 encode planning was cancelled");
        }
    }

    /** Carries {@code Dimension} data between validated stages of the AFMA creator. */
    protected record Dimension(int width, int height) {
    }

    /** Carries {@code LoadedDimensionFrame} data between validated stages of the AFMA creator. */
    protected record LoadedDimensionFrame(@NotNull Dimension dimension, @NotNull AfmaPixelFrame frame) {
    }

    /** Carries {@code PlannedSequence} data between validated stages of the AFMA creator. */
    protected record PlannedSequence(@NotNull List<AfmaFrameDescriptor> frames,
                                     long defaultDelayMs,
                                     @NotNull Map<Integer, Long> customFrameTimes) {
    }

    /** Carries {@code PlannedTimedFrame} data between validated stages of the AFMA creator. */
    protected record PlannedTimedFrame(@NotNull AfmaFrameDescriptor descriptor, long delayMs) {

        /** Creates a copy with the additional delay changed for the AFMA creator. */
        @NotNull
        public PlannedTimedFrame withAdditionalDelay(long additionalDelayMs) {
            return new PlannedTimedFrame(this.descriptor, addDelaysSaturating(this.delayMs, Math.max(1L, additionalDelayMs)));
        }
    }

    /** Models {@code AdaptiveTiming} state used by the AFMA creator. */
    protected record AdaptiveTiming(long defaultDelayMs, @NotNull LinkedHashMap<Integer, Long> customFrameTimes) {
    }

    /** Models {@code FrameDecision} state used by the AFMA creator. */
    protected record FrameDecision(@NotNull FrameCandidate candidate, @NotNull AfmaPixelFrame outputFrame) {
    }

    /** Models {@code QuickReferenceDecision} state used by the AFMA creator. */
    protected enum QuickReferenceDecision {
        /** Selects the {@code FULL_REFERENCE} AFMA planning or encoding strategy. */
        FULL_REFERENCE,
        /** Selects the {@code MIXED} AFMA planning or encoding strategy. */
        MIXED,
        /** Selects the {@code UNKNOWN} AFMA planning or encoding strategy. */
        UNKNOWN
    }

    /** Reports {@code ReferencePlanProbeResult} measurements produced by the AFMA creator. */
    protected record ReferencePlanProbeResult(@NotNull List<AfmaFrameDescriptor> descriptors,
                                              int sampledFrames,
                                              int mixedWins,
                                              long selectedArchiveBytes,
                                              long fullReferenceArchiveBytes) {

        /** Creates the empty AFMA creator variant. */
        @NotNull
        public static ReferencePlanProbeResult empty() {
            return new ReferencePlanProbeResult(List.of(), 0, 0, 0L, 0L);
        }
    }

    /** Reports {@code CandidateQualityMetrics} measurements produced by the AFMA creator. */
    protected record CandidateQualityMetrics(boolean lossless,
                                             double averageError,
                                             int maxVisibleColorDelta,
                                             int maxAlphaDelta) {

        /** Creates the lossless metrics AFMA creator variant. */
        @NotNull
        public static CandidateQualityMetrics losslessMetrics() {
            return new CandidateQualityMetrics(true, 0D, 0, 0);
        }
    }

    /** Reports mutable or snapshot {@code QualityBudgetState} state for the AFMA creator. */
    protected record QualityBudgetState(int consecutiveLossyFrames,
                                        double cumulativeAverageError,
                                        int cumulativeVisibleColorDelta,
                                        int cumulativeAlphaDelta) {

        /** Creates the lossless AFMA creator variant. */
        @NotNull
        public static QualityBudgetState lossless() {
            return new QualityBudgetState(0, 0D, 0, 0);
        }

        /** Advances resource state in the AFMA creator. */
        @NotNull
        public QualityBudgetState advance(@NotNull FrameCandidate candidate) {
            CandidateQualityMetrics metrics = candidate.qualityMetrics();
            if (metrics.lossless()) {
                return lossless();
            }
            if (candidate.descriptor().isKeyframe()) {
                return new QualityBudgetState(1, metrics.averageError(), metrics.maxVisibleColorDelta(), metrics.maxAlphaDelta());
            }
            return new QualityBudgetState(
                    this.consecutiveLossyFrames + 1,
                    this.cumulativeAverageError + metrics.averageError(),
                    this.cumulativeVisibleColorDelta + metrics.maxVisibleColorDelta(),
                    this.cumulativeAlphaDelta + metrics.maxAlphaDelta()
            );
        }
    }

    /** Selects the {@code CandidateKind} representation or behavior used by the AFMA creator. */
    protected enum CandidateKind {
        /** Selects the {@code SAME} AFMA planning or encoding strategy. */
        SAME(0),
        /** Selects the {@code FULL} AFMA planning or encoding strategy. */
        FULL(1),
        /** Selects the {@code DELTA_BIN_INTRA} AFMA planning or encoding strategy. */
        DELTA_BIN_INTRA(2),
        /** Selects the {@code DELTA_RESIDUAL} AFMA planning or encoding strategy. */
        DELTA_RESIDUAL(3),
        /** Selects the {@code DELTA_SPARSE} AFMA planning or encoding strategy. */
        DELTA_SPARSE(4),
        /** Selects the {@code COPY_BIN_INTRA} AFMA planning or encoding strategy. */
        COPY_BIN_INTRA(5),
        /** Selects the {@code COPY_RESIDUAL} AFMA planning or encoding strategy. */
        COPY_RESIDUAL(6),
        /** Selects the {@code COPY_SPARSE} AFMA planning or encoding strategy. */
        COPY_SPARSE(7),
        /** Selects the {@code MULTI_COPY_BIN_INTRA} AFMA planning or encoding strategy. */
        MULTI_COPY_BIN_INTRA(8),
        /** Selects the {@code MULTI_COPY_RESIDUAL} AFMA planning or encoding strategy. */
        MULTI_COPY_RESIDUAL(9),
        /** Selects the {@code MULTI_COPY_SPARSE} AFMA planning or encoding strategy. */
        MULTI_COPY_SPARSE(10),
        /** Selects the {@code BLOCK_INTER} AFMA planning or encoding strategy. */
        BLOCK_INTER(11);

        private final int stabilityRank;

        CandidateKind(int stabilityRank) {
            this.stabilityRank = stabilityRank;
        }

        /** Returns the stability rank produced by the AFMA creator. */
        public int stabilityRank() {
            return this.stabilityRank;
        }
    }

    /** Reports {@code StrategyEstimate} measurements produced by the AFMA creator. */
    protected record StrategyEstimate(@NotNull CandidateKind kind, long estimatedBytes, int stabilityRank) {
    }

    /** Describes the validated {@code CopyPlan} work selected by the AFMA creator. */
    protected record CopyPlan(@Nullable AfmaPixelFrame referenceFrameAfterCopy,
                              @Nullable AfmaCopyRect copyRect,
                              @Nullable AfmaMultiCopy multiCopy,
                              @Nullable AfmaRect patchBounds,
                              @NotNull List<StrategyEstimate> rankedStrategies) {
    }

    /** Models {@code CopyEvaluation} state used by the AFMA creator. */
    protected record CopyEvaluation(@Nullable AfmaRectCopyDetector.Detection singleDetection,
                                    @Nullable AfmaRectCopyDetector.MultiDetection multiDetection) {
        /** Shared empty evaluation used when neither single- nor multi-copy detection produced a candidate. */
        protected static final CopyEvaluation EMPTY = new CopyEvaluation(null, null);
    }

    /** Models {@code MotionVector} state used by the AFMA creator. */
    protected record MotionVector(int dx, int dy) {
    }

    /** Tracks {@code MotionFamilyUsageTracker} lifecycle state for the AFMA creator. */
    protected static final class MotionFamilyUsageTracker {

        /** Current warmup attempts completed state for this AFMA creator instance. */
        protected int warmupAttemptsCompleted;
        /** Current probe attempts remaining state for this AFMA creator instance. */
        protected int probeAttemptsRemaining;
        /** Current next probe frame index measured or selected by this AFMA creator instance. */
        protected int nextProbeFrameIndex = Integer.MAX_VALUE;
        /** Whether saw motion family win currently applies to this AFMA creator instance. */
        protected boolean sawMotionFamilyWin;

        /** Returns whether evaluate. */
        protected boolean shouldEvaluate(int frameIndex) {
            if (this.sawMotionFamilyWin) {
                return true;
            }
            if (this.warmupAttemptsCompleted < MOTION_FAMILY_WARMUP_ATTEMPTS) {
                return true;
            }
            if ((this.probeAttemptsRemaining <= 0) && (frameIndex >= this.nextProbeFrameIndex)) {
                this.probeAttemptsRemaining = MOTION_FAMILY_PROBE_ATTEMPTS;
            }
            return this.probeAttemptsRemaining > 0;
        }

        /** Returns whether record frame result. */
        protected void recordFrameResult(int frameIndex, boolean motionFamiliesAttempted, @NotNull CandidateKind selectedKind) {
            if (!motionFamiliesAttempted || this.sawMotionFamilyWin) {
                return;
            }
            if (isMotionFamilyKind(selectedKind)) {
                this.sawMotionFamilyWin = true;
                this.probeAttemptsRemaining = 0;
                this.nextProbeFrameIndex = Integer.MAX_VALUE;
                return;
            }
            if (this.warmupAttemptsCompleted < MOTION_FAMILY_WARMUP_ATTEMPTS) {
                this.warmupAttemptsCompleted++;
                if (this.warmupAttemptsCompleted >= MOTION_FAMILY_WARMUP_ATTEMPTS) {
                    this.nextProbeFrameIndex = frameIndex + MOTION_FAMILY_PROBE_INTERVAL_FRAMES;
                }
                return;
            }
            if (this.probeAttemptsRemaining > 0) {
                this.probeAttemptsRemaining--;
                if (this.probeAttemptsRemaining <= 0) {
                    this.nextProbeFrameIndex = frameIndex + MOTION_FAMILY_PROBE_INTERVAL_FRAMES;
                }
            }
        }

        /** Returns whether motion family kind. */
        protected static boolean isMotionFamilyKind(@NotNull CandidateKind selectedKind) {
            return switch (selectedKind) {
                case COPY_BIN_INTRA, COPY_RESIDUAL, COPY_SPARSE,
                     MULTI_COPY_BIN_INTRA, MULTI_COPY_RESIDUAL, MULTI_COPY_SPARSE,
                     BLOCK_INTER -> true;
                default -> false;
            };
        }
    }

    /** Models {@code ApproxBlockInterTile} state used by the AFMA creator. */
    protected record ApproxBlockInterTile(@NotNull AfmaBlockInterPayloadHelper.TileMode mode,
                                          int dstX, int dstY, int width, int height,
                                          int dx, int dy,
                                          int channels,
                                          int changedPixelCount,
                                          int changedPixelBudget,
                                          long estimatedBytes) {
    }

    /** Reports {@code MotionTileStats} measurements produced by the AFMA creator. */
    protected record MotionTileStats(int changedPixelCount, boolean includeAlpha) {
    }

    /** Reports {@code ResidualRegionStats} measurements produced by the AFMA creator. */
    protected record ResidualRegionStats(double averageMagnitude, double averageChangedMagnitude) {
        /** Shared immutable empty value used by the AFMA creator. */
        protected static final ResidualRegionStats EMPTY = new ResidualRegionStats(0D, 0D);
    }

    /** Carries {@code ChangedRegionData} data between validated stages of the AFMA creator. */
    protected record ChangedRegionData(@NotNull int[] changedIndices,
                                       @NotNull int[] predictedColors,
                                       @NotNull int[] currentColors,
                                       int changedCount) {
    }

    /** Carries {@code ResidualPayloadData} data between validated stages of the AFMA creator. */
    protected record ResidualPayloadData(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                         @NotNull AfmaStoredPayload.Writer payloadWriter,
                                         @NotNull AfmaResidualPayload metadata,
                                         int complexityScore) {
    }

    /** Carries {@code SparseResidualPayloadData} data between validated stages of the AFMA creator. */
    protected record SparseResidualPayloadData(@NotNull byte[] layoutPayload,
                                               @NotNull AfmaStoredPayload.PayloadSummary residualPayloadSummary,
                                               @NotNull AfmaStoredPayload.Writer residualPayloadWriter,
                                               int changedPixelCount,
                                               @NotNull AfmaSparseLayoutCodec layoutCodec,
                                               int channels,
                                               @NotNull AfmaResidualCodec residualCodec,
                                               @NotNull AfmaAlphaResidualMode alphaMode,
                                               int alphaChangedPixelCount,
                                               int complexityScore) {

        /** Returns the residual payload length produced by the AFMA creator. */
        public int residualPayloadLength() {
            return this.residualPayloadSummary.length();
        }

        /** Materializes the residual payload for the AFMA creator. */
        @NotNull
        public byte[] materializeResidualPayload() {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream(Math.max(32, this.residualPayloadSummary.length()));
                this.residualPayloadWriter.write(byteStream);
                byte[] payloadBytes = byteStream.toByteArray();
                AfmaStoredPayload.PayloadSummary materializedSummary = AfmaStoredPayload.summarize(payloadBytes);
                if ((materializedSummary.length() != this.residualPayloadSummary.length())
                        || (materializedSummary.estimatedArchiveBytes() != this.residualPayloadSummary.estimatedArchiveBytes())
                        || !materializedSummary.fingerprint().equals(this.residualPayloadSummary.fingerprint())) {
                    throw new IOException("AFMA v2 sparse residual payload changed during materialization");
                }
                return payloadBytes;
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to materialize AFMA v2 sparse residual payload", ex);
            }
        }

        /** Converts the metadata from the AFMA creator representation. */
        @NotNull
        public AfmaSparsePayload toMetadata(@Nullable String residualPayloadPath) {
            return new AfmaSparsePayload(
                    residualPayloadPath,
                    this.changedPixelCount,
                    this.channels,
                    this.layoutCodec,
                    this.residualCodec,
                    this.alphaMode,
                    this.alphaChangedPixelCount
            );
        }
    }

    /** Carries {@code SparseLayoutCandidate} data between validated stages of the AFMA creator. */
    protected record SparseLayoutCandidate(@NotNull AfmaSparseLayoutCodec layoutCodec,
                                           @NotNull byte[] layoutPayload,
                                           int complexityScore,
                                           long estimatedArchiveBytes) {

        /** Initializes a new {@code SparseLayoutCandidate} for AFMA creator use. */
        protected SparseLayoutCandidate(@NotNull AfmaSparseLayoutCodec layoutCodec,
                                        @NotNull byte[] layoutPayload,
                                        int complexityScore) {
            this(layoutCodec, layoutPayload, complexityScore, AfmaPayloadMetricsHelper.estimateArchiveBytes(layoutPayload));
        }

        /** Returns whether better than. */
        public boolean isBetterThan(@NotNull SparseLayoutCandidate other) {
            if (this.estimatedArchiveBytes != other.estimatedArchiveBytes) {
                return this.estimatedArchiveBytes < other.estimatedArchiveBytes;
            }
            if (this.complexityScore != other.complexityScore) {
                return this.complexityScore < other.complexityScore;
            }
            return this.layoutPayload.length < other.layoutPayload.length;
        }
    }

    /** Reports {@code SparseLayoutEstimate} measurements produced by the AFMA creator. */
    protected record SparseLayoutEstimate(long bitmaskBytes, long rowSpanBytes, long tileMaskBytes, long coordListBytes) {
    }

    /** Describes the validated {@code SparseLayoutPlan} work selected by the AFMA creator. */
    protected record SparseLayoutPlan(@NotNull AfmaSparseLayoutCodec layoutCodec, long estimatedRawBytes) {
    }

    /** Carries {@code DeferredPayload} data between validated stages of the AFMA creator. */
    protected static final class DeferredPayload implements AutoCloseable {

        /** Current payload summary state for this AFMA creator instance. */
        @NotNull
        protected final AfmaStoredPayload.PayloadSummary payloadSummary;
        /** Owned materialized payload state for this AFMA creator instance. */
        @Nullable
        protected AfmaStoredPayload materializedPayload;
        /** Holds the payloadBytes collection used by this AFMA creator instance. */
        @Nullable
        protected byte[] payloadBytes;
        /** Current payload writer state for this AFMA creator instance. */
        @Nullable
        protected AfmaStoredPayload.Writer payloadWriter;
        /** Whether closed currently applies to this AFMA creator instance. */
        protected boolean closed = false;

        /** Initializes a new {@code DeferredPayload} for AFMA creator use. */
        protected DeferredPayload(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                  @Nullable AfmaStoredPayload materializedPayload,
                                  @Nullable byte[] payloadBytes,
                                  @Nullable AfmaStoredPayload.Writer payloadWriter) {
            this.payloadSummary = Objects.requireNonNull(payloadSummary);
            this.materializedPayload = materializedPayload;
            this.payloadBytes = payloadBytes;
            this.payloadWriter = payloadWriter;
        }

        /** Builds the bytes for the AFMA creator. */
        @NotNull
        public static DeferredPayload fromBytes(@NotNull byte[] payloadBytes) {
            Objects.requireNonNull(payloadBytes);
            return new DeferredPayload(AfmaStoredPayload.summarize(payloadBytes), null, payloadBytes, null);
        }

        /** Builds the bytes for the AFMA creator. */
        @NotNull
        public static DeferredPayload fromBytes(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary, @NotNull byte[] payloadBytes) {
            Objects.requireNonNull(payloadSummary);
            Objects.requireNonNull(payloadBytes);
            if (payloadSummary.length() != payloadBytes.length) {
                throw new IllegalArgumentException("AFMA v2 deferred payload summary length does not match payload bytes");
            }
            return new DeferredPayload(payloadSummary, null, payloadBytes, null);
        }

        /** Builds the writer for the AFMA creator. */
        @NotNull
        public static DeferredPayload fromWriter(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                                 @NotNull AfmaStoredPayload.Writer payloadWriter) {
            return new DeferredPayload(payloadSummary, null, null, payloadWriter);
        }

        /** Returns the estimated compressed archive size in bytes. */
        public long estimatedArchiveBytes() {
            this.ensureOpen();
            return this.payloadSummary.estimatedArchiveBytes();
        }

        /** Computes a stable fingerprint for resource state in the AFMA creator. */
        @NotNull
        public String fingerprint() {
            this.ensureOpen();
            return this.payloadSummary.fingerprint();
        }

        /** Returns the payload summary produced by the AFMA creator. */
        @NotNull
        public AfmaStoredPayload.PayloadSummary payloadSummary() {
            this.ensureOpen();
            return this.payloadSummary;
        }

        /** Calculates the chunk append bytes for the AFMA creator. */
        public long estimateChunkAppendBytes(@NotNull byte[] previousTail) {
            this.ensureOpen();
            Objects.requireNonNull(previousTail);
            if (this.payloadSummary.length() <= 0) {
                return 0L;
            }
            if (previousTail.length == 0) {
                return this.payloadSummary.estimatedArchiveBytes();
            }
            if (this.payloadBytes != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.payloadBytes);
            }
            if (this.payloadWriter != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.payloadSummary, this.payloadWriter);
            }
            if (this.materializedPayload != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.materializedPayload);
            }
            throw new IllegalStateException("AFMA v2 deferred payload no longer has bytes available for archive estimation");
        }

        /** Materializes resource state for the AFMA creator. */
        @NotNull
        public AfmaStoredPayload materialize() throws IOException {
            this.ensureOpen();
            if (this.materializedPayload != null) {
                return this.materializedPayload;
            }

            AfmaStoredPayload payload;
            if (this.payloadBytes != null) {
                payload = AfmaStoredPayload.fromBytes(this.payloadSummary, this.payloadBytes);
            } else if (this.payloadWriter != null) {
                payload = AfmaStoredPayload.write(this.payloadWriter);
            } else {
                throw new IOException("AFMA v2 deferred payload no longer has a materialization source");
            }

            AfmaStoredPayload.PayloadSummary materializedSummary = payload.summarize();
            if ((materializedSummary.length() != this.payloadSummary.length())
                    || (materializedSummary.estimatedArchiveBytes() != this.payloadSummary.estimatedArchiveBytes())
                    || !materializedSummary.fingerprint().equals(this.payloadSummary.fingerprint())) {
                payload.close();
                throw new IOException("AFMA v2 deferred payload metrics changed during materialization");
            }

            this.materializedPayload = payload;
            this.payloadBytes = null;
            this.payloadWriter = null;
            return payload;
        }

        /** Closes a materialized payload and discards deferred byte/writer sources; repeated calls are harmless. */
        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.materializedPayload != null) {
                this.materializedPayload.close();
                this.materializedPayload = null;
            }
            this.payloadBytes = null;
            this.payloadWriter = null;
        }

        /** Ensures the open is valid for the AFMA creator. */
        protected void ensureOpen() {
            if (this.closed) {
                throw new IllegalStateException("AFMA v2 deferred payload has already been closed");
            }
        }
    }

    /** Carries {@code FrameCandidate} data between validated stages of the AFMA creator. */
    protected record FrameCandidate(@NotNull CandidateKind kind,
                                    @NotNull AfmaFrameDescriptor descriptor,
                                    @Nullable String primaryPayloadPath,
                                    @Nullable DeferredPayload primaryPayload,
                                    @Nullable String patchPayloadPath,
                                    @Nullable DeferredPayload patchPayload,
                                    @NotNull AfmaPixelFrame outputFrame,
                                    int decodeComplexity,
                                    @NotNull CandidateQualityMetrics qualityMetrics) {

        /** Calculates the total archive bytes value for the AFMA creator. */
        public long totalArchiveBytes(@NotNull PayloadInterner payloadInterner) {
            return payloadInterner.estimateCandidateArchiveBytes(this);
        }

        /** Creates a copy with the quality metrics changed for the AFMA creator. */
        @NotNull
        public FrameCandidate withQualityMetrics(@NotNull CandidateQualityMetrics updatedQualityMetrics) {
            return new FrameCandidate(
                    this.kind,
                    this.descriptor,
                    this.primaryPayloadPath,
                    this.primaryPayload,
                    this.patchPayloadPath,
                    this.patchPayload,
                    this.outputFrame,
                    this.decodeComplexity,
                    updatedQualityMetrics
            );
        }
    }

    /** Reports mutable or snapshot {@code ArchivePreviewState} state for the AFMA creator. */
    protected static final class ArchivePreviewState {

        /** Current closed chunk bytes measured or selected by this AFMA creator instance. */
        protected long closedChunkBytes = 0L;
        /** Current closed chunk count measured or selected by this AFMA creator instance. */
        protected int closedChunkCount = 0;
        /** Current payload locator bytes measured or selected by this AFMA creator instance. */
        protected long payloadLocatorBytes = 0L;
        /** Current payload count measured or selected by this AFMA creator instance. */
        protected int payloadCount = 0;
        /** Current closed chunk length bytes measured or selected by this AFMA creator instance. */
        protected long closedChunkLengthBytes = 0L;
        /** Current current chunk length measured or selected by this AFMA creator instance. */
        protected int currentChunkLength = 0;
        /** Current current chunk compressed bytes measured or selected by this AFMA creator instance. */
        protected long currentChunkCompressedBytes = 0L;
        /** Holds the currentChunkTail collection used by this AFMA creator instance. */
        @NotNull
        protected byte[] currentChunkTail = AfmaEncodePlanner.EMPTY_BYTES;

        /** Initializes a new {@code ArchivePreviewState} for AFMA creator use. */
        protected ArchivePreviewState() {
        }

        /** Initializes a new {@code ArchivePreviewState} for AFMA creator use. */
        protected ArchivePreviewState(@NotNull ArchivePreviewState other) {
            this.closedChunkBytes = other.closedChunkBytes;
            this.closedChunkCount = other.closedChunkCount;
            this.payloadLocatorBytes = other.payloadLocatorBytes;
            this.payloadCount = other.payloadCount;
            this.closedChunkLengthBytes = other.closedChunkLengthBytes;
            this.currentChunkLength = other.currentChunkLength;
            this.currentChunkCompressedBytes = other.currentChunkCompressedBytes;
            this.currentChunkTail = other.currentChunkTail.clone();
        }

        /** Previews the archive bytes without mutating committed AFMA creator state. */
        public long previewArchiveBytes(@Nullable DeferredPayload primaryPayload,
                                        @Nullable DeferredPayload patchPayload,
                                        @NotNull Set<String> knownFingerprints) {
            long baseBytes = this.estimatedTotalBytes();
            ArchivePreviewState previewState = new ArchivePreviewState(this);
            HashSet<String> previewFingerprints = new HashSet<>(knownFingerprints);
            previewState.previewCommitPayload(primaryPayload, previewFingerprints);
            previewState.previewCommitPayload(patchPayload, previewFingerprints);
            return Math.max(0L, previewState.estimatedTotalBytes() - baseBytes);
        }

        /** Updates the commit payload state in the AFMA creator. */
        public void commitPayload(@NotNull DeferredPayload payload) {
            this.appendPayload(payload);
        }

        /** Previews the commit payload without mutating committed AFMA creator state. */
        protected void previewCommitPayload(@Nullable DeferredPayload payload, @NotNull Set<String> previewFingerprints) {
            if (payload == null) {
                return;
            }
            if (!previewFingerprints.add(payload.fingerprint())) {
                return;
            }
            this.appendPayload(payload);
        }

        /** Appends the payload to the AFMA creator output state. */
        protected void appendPayload(@NotNull DeferredPayload payload) {
            AfmaStoredPayload.PayloadSummary payloadSummary = payload.payloadSummary();
            if (payloadSummary.length() <= 0) {
                return;
            }

            if ((this.currentChunkLength > 0) && this.shouldStartNewChunk(payloadSummary.length())) {
                this.finalizeCurrentChunk();
            }

            int chunkId = this.closedChunkCount;
            this.payloadLocatorBytes += estimateVarIntBytes(chunkId)
                    + estimateVarIntBytes(this.currentChunkLength)
                    + estimateVarIntBytes(payloadSummary.length());
            this.payloadCount++;

            this.currentChunkCompressedBytes += payload.estimateChunkAppendBytes(this.currentChunkTail);
            this.currentChunkLength += payloadSummary.length();
            this.currentChunkTail = AfmaChunkedPayloadHelper.appendDeflateTail(this.currentChunkTail, payloadSummary);
        }

        /** Estimates the complete chunk archive footprint, including indexes and container overhead. */
        protected long estimatedTotalBytes() {
            long totalChunkBytes = this.closedChunkBytes;
            long totalChunkLengthBytes = this.closedChunkLengthBytes;
            int totalChunkCount = this.closedChunkCount;
            if (this.currentChunkLength > 0) {
                totalChunkBytes += Math.min(this.currentChunkCompressedBytes, (long) this.currentChunkLength);
                totalChunkLengthBytes += estimateVarIntBytes(this.currentChunkLength);
                totalChunkCount++;
            }
            return 5L
                    + estimateVarIntBytes(this.payloadCount)
                    + estimateVarIntBytes(totalChunkCount)
                    + this.payloadLocatorBytes
                    + totalChunkLengthBytes
                    + totalChunkBytes
                    + ((long) totalChunkCount * (long) AfmaContainerV2.CHUNK_DESCRIPTOR_BYTES);
        }

        /** Updates the finalize current chunk state in the AFMA creator. */
        protected void finalizeCurrentChunk() {
            if (this.currentChunkLength <= 0) {
                return;
            }
            this.closedChunkBytes += Math.min(this.currentChunkCompressedBytes, (long) this.currentChunkLength);
            this.closedChunkLengthBytes += estimateVarIntBytes(this.currentChunkLength);
            this.closedChunkCount++;
            this.currentChunkLength = 0;
            this.currentChunkCompressedBytes = 0L;
            this.currentChunkTail = AfmaEncodePlanner.EMPTY_BYTES;
        }

        /** Returns whether start new chunk. */
        protected boolean shouldStartNewChunk(int payloadLength) {
            return AfmaPayloadArchiveLayout.shouldStartNewChunk(this.currentChunkLength, payloadLength);
        }
    }

    /** Retains or reuses {@code PayloadInterner} state while the AFMA creator is active. */
    protected static final class PayloadInterner {

        /** Holds the payloads collection used by this AFMA creator instance. */
        @NotNull
        protected final LinkedHashMap<String, AfmaStoredPayload> payloads = new LinkedHashMap<>();
        /** Holds the payloadPathsByFingerprint collection used by this AFMA creator instance. */
        @NotNull
        protected final Map<String, String> payloadPathsByFingerprint = new LinkedHashMap<>();
        /** Holds the candidateArchiveBytesCache collection used by this AFMA creator instance. */
        @NotNull
        protected final Map<FrameCandidate, Long> candidateArchiveBytesCache = new IdentityHashMap<>();
        /** Owned archive preview state state for this AFMA creator instance. */
        @NotNull
        protected final ArchivePreviewState archivePreviewState = new ArchivePreviewState();

        /** Calculates the candidate archive bytes for the AFMA creator. */
        public long estimateCandidateArchiveBytes(@NotNull FrameCandidate candidate) {
            return this.candidateArchiveBytesCache.computeIfAbsent(candidate, this::computeCandidateArchiveBytes);
        }

        /** Calculates the candidate archive bytes for the AFMA creator. */
        protected long computeCandidateArchiveBytes(@NotNull FrameCandidate candidate) {
            return this.archivePreviewState.previewArchiveBytes(
                    candidate.primaryPayload(),
                    candidate.patchPayload(),
                    this.payloadPathsByFingerprint.keySet()
            ) + this.estimateDescriptorBytes(candidate.descriptor());
        }

        /** Calculates the descriptor bytes for the AFMA creator. */
        public int estimateDescriptorBytes(@NotNull AfmaFrameDescriptor descriptor) {
            return new AfmaV2DescriptorSizer().estimate(descriptor);
        }

        /** Calculates the packed archive bytes for the AFMA creator. */
        public long estimatePackedArchiveBytes(boolean introSequence,
                                               @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                               @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                               int loopCount) throws IOException {
            ArrayList<AfmaFrameDescriptor> introFrames;
            ArrayList<AfmaFrameDescriptor> mainFrames;
            if (introSequence) {
                introFrames = new ArrayList<>(currentSequenceFrames);
                mainFrames = new ArrayList<>(companionSequenceFrames);
            } else {
                introFrames = new ArrayList<>(companionSequenceFrames);
                mainFrames = new ArrayList<>(currentSequenceFrames);
            }
            AfmaFrameIndex frameIndex = new AfmaFrameIndex(mainFrames, introFrames);
            AfmaChunkedPayloadHelper.PackedPayloadArchive packedArchive = AfmaChunkedPayloadHelper.simulateArchiveLayout(
                    new LinkedHashMap<>(this.payloads),
                    AfmaChunkedPayloadHelper.buildPackingHints(frameIndex, loopCount)
            );
            return packedArchive.packingMetrics().predictedArchiveBytes();
        }

        /** Calculates the packed candidate archive bytes for the AFMA creator. */
        public long estimatePackedCandidateArchiveBytes(@NotNull FrameCandidate candidate,
                                                        boolean introSequence,
                                                        @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                                        @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                                        int loopCount) throws IOException {
            CandidateArchivePreview preview = this.previewCandidate(candidate);
            ArrayList<AfmaFrameDescriptor> introFrames;
            ArrayList<AfmaFrameDescriptor> mainFrames;
            if (introSequence) {
                introFrames = new ArrayList<>(currentSequenceFrames.size() + 1);
                introFrames.addAll(currentSequenceFrames);
                introFrames.add(preview.descriptor());
                mainFrames = new ArrayList<>(companionSequenceFrames);
            } else {
                introFrames = new ArrayList<>(companionSequenceFrames);
                mainFrames = new ArrayList<>(currentSequenceFrames.size() + 1);
                mainFrames.addAll(currentSequenceFrames);
                mainFrames.add(preview.descriptor());
            }

            AfmaFrameIndex frameIndex = new AfmaFrameIndex(mainFrames, introFrames);
            AfmaChunkedPayloadHelper.PackedPayloadArchive packedArchive = AfmaChunkedPayloadHelper.simulateArchiveLayout(
                    preview.payloads(),
                    AfmaChunkedPayloadHelper.buildPackingHints(frameIndex, loopCount)
            );
            return packedArchive.packingMetrics().predictedArchiveBytes() + this.estimateDescriptorBytes(preview.descriptor());
        }

        /** Stores resource state in the AFMA creator. */
        @NotNull
        public AfmaFrameDescriptor intern(@NotNull FrameCandidate candidate) throws IOException {
            AfmaFrameDescriptor descriptor = candidate.descriptor();
            String resolvedPrimaryPath = this.internPayload(candidate.primaryPayloadPath(), candidate.primaryPayload());
            if (!Objects.equals(resolvedPrimaryPath, candidate.primaryPayloadPath()) && (resolvedPrimaryPath != null)) {
                descriptor = descriptor.withPrimaryPath(resolvedPrimaryPath);
            }
            String resolvedPatchPath = this.internPayload(candidate.patchPayloadPath(), candidate.patchPayload());
            if (!Objects.equals(resolvedPatchPath, candidate.patchPayloadPath()) && (resolvedPatchPath != null)) {
                descriptor = descriptor.withPatchPath(resolvedPatchPath);
            }
            return descriptor;
        }

        /** Stores the payload in the AFMA creator. */
        @Nullable
        protected String internPayload(@Nullable String path, @Nullable DeferredPayload payload) throws IOException {
            if ((path == null) || (payload == null)) {
                return path;
            }

            String fingerprint = payload.fingerprint();
            String existingPath = this.payloadPathsByFingerprint.get(fingerprint);
            if (existingPath != null) {
                payload.close();
                return existingPath;
            }

            this.payloads.put(path, payload.materialize());
            this.payloadPathsByFingerprint.put(fingerprint, path);
            this.archivePreviewState.commitPayload(payload);
            this.candidateArchiveBytesCache.clear();
            return path;
        }

        /** Previews the candidate without mutating committed AFMA creator state. */
        @NotNull
        protected CandidateArchivePreview previewCandidate(@NotNull FrameCandidate candidate) throws IOException {
            LinkedHashMap<String, AfmaStoredPayload> previewPayloads = new LinkedHashMap<>(this.payloads);
            LinkedHashMap<String, String> previewPathsByFingerprint = new LinkedHashMap<>(this.payloadPathsByFingerprint);
            AfmaFrameDescriptor descriptor = candidate.descriptor();

            String resolvedPrimaryPath = this.previewPayload(candidate.primaryPayloadPath(), candidate.primaryPayload(), previewPayloads, previewPathsByFingerprint);
            if (!Objects.equals(resolvedPrimaryPath, candidate.primaryPayloadPath()) && (resolvedPrimaryPath != null)) {
                descriptor = descriptor.withPrimaryPath(resolvedPrimaryPath);
            }

            String resolvedPatchPath = this.previewPayload(candidate.patchPayloadPath(), candidate.patchPayload(), previewPayloads, previewPathsByFingerprint);
            if (!Objects.equals(resolvedPatchPath, candidate.patchPayloadPath()) && (resolvedPatchPath != null)) {
                descriptor = descriptor.withPatchPath(resolvedPatchPath);
            }

            return new CandidateArchivePreview(previewPayloads, descriptor);
        }

        /** Previews the payload without mutating committed AFMA creator state. */
        @Nullable
        protected String previewPayload(@Nullable String path,
                                        @Nullable DeferredPayload payload,
                                        @NotNull LinkedHashMap<String, AfmaStoredPayload> previewPayloads,
                                        @NotNull Map<String, String> previewPathsByFingerprint) throws IOException {
            if ((path == null) || (payload == null)) {
                return path;
            }

            String fingerprint = payload.fingerprint();
            String existingPath = previewPathsByFingerprint.get(fingerprint);
            if (existingPath != null) {
                return existingPath;
            }

            previewPayloads.put(path, payload.materialize());
            previewPathsByFingerprint.put(fingerprint, path);
            return path;
        }

        /** Returns a shallow copy of materialized payloads keyed by archive path. */
        @NotNull
        public LinkedHashMap<String, AfmaStoredPayload> payloads() {
            return new LinkedHashMap<>(this.payloads);
        }
    }

    /** Models {@code CandidateArchivePreview} state used by the AFMA creator. */
    protected record CandidateArchivePreview(@NotNull LinkedHashMap<String, AfmaStoredPayload> payloads,
                                             @NotNull AfmaFrameDescriptor descriptor) {
    }

    /** Reports {@code PackedArchiveCandidateMetrics} measurements produced by the AFMA creator. */
    protected record PackedArchiveCandidateMetrics(@NotNull Map<FrameCandidate, Long> totalBytesByCandidate,
                                                   @NotNull Map<FrameCandidate, Long> addedBytesByCandidate) {
    }

    /** Reports {@code EstimatedFullCandidateMetrics} measurements produced by the AFMA creator. */
    protected record EstimatedFullCandidateMetrics(long estimatedArchiveBytes, boolean lossless) {
    }

    /** Calculates serialized {@code AfmaV2DescriptorSizer} sizes for the AFMA creator. */
    protected static final class AfmaV2DescriptorSizer {

        /** Calculates the requested value for the AFMA creator. */
        protected int estimate(@NotNull AfmaFrameDescriptor descriptor) {
            AfmaFrameOperationType type = descriptor.getType();
            if (type == null) {
                return 1;
            }
            int bytes = 1;
            bytes += switch (type) {
                case FULL -> 2;
                case DELTA_RECT -> 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight());
                case RESIDUAL_DELTA_RECT -> 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels()) + 2
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
                case SPARSE_DELTA_RECT -> 4 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels()) + 3
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
                case SAME -> 0;
                case COPY_RECT_PATCH -> {
                    int copyBytes = estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()));
                    AfmaPatchRegion patch = descriptor.getPatch();
                    if (patch == null) {
                        yield copyBytes + 1;
                    }
                    yield copyBytes + 1 + 2 + estimateVarIntBytes(patch.getX()) + estimateVarIntBytes(patch.getY())
                            + estimateVarIntBytes(patch.getWidth()) + estimateVarIntBytes(patch.getHeight());
                }
                case MULTI_COPY_PATCH -> {
                    int copyBytes = estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()));
                    AfmaPatchRegion patch = descriptor.getPatch();
                    if (patch == null) {
                        yield copyBytes + 1;
                    }
                    yield copyBytes + 1 + 2 + estimateVarIntBytes(patch.getX()) + estimateVarIntBytes(patch.getY())
                            + estimateVarIntBytes(patch.getWidth()) + estimateVarIntBytes(patch.getHeight());
                }
                case COPY_RECT_RESIDUAL_PATCH -> estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()))
                        + 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels()) + 2
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
                case MULTI_COPY_RESIDUAL_PATCH -> estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()))
                        + 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels()) + 2
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
                case COPY_RECT_SPARSE_PATCH -> estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()))
                        + 4 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels()) + 3
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
                case MULTI_COPY_SPARSE_PATCH -> estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()))
                        + 4 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels()) + 3
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
                case BLOCK_INTER -> 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getBlockInter()).getTileSize());
            };
            return bytes;
        }

        /** Calculates the copy rect bytes for the AFMA creator. */
        protected static int estimateCopyRectBytes(@NotNull AfmaCopyRect copyRect) {
            return estimateVarIntBytes(copyRect.getSrcX()) + estimateVarIntBytes(copyRect.getSrcY())
                    + estimateVarIntBytes(copyRect.getDstX()) + estimateVarIntBytes(copyRect.getDstY())
                    + estimateVarIntBytes(copyRect.getWidth()) + estimateVarIntBytes(copyRect.getHeight());
        }

        /** Calculates the multi copy bytes for the AFMA creator. */
        protected static int estimateMultiCopyBytes(@NotNull AfmaMultiCopy multiCopy) {
            List<AfmaCopyRect> copyRects = multiCopy.getCopyRects();
            int bytes = estimateVarIntBytes(copyRects.size() - 1);
            AfmaCopyRect previousRect = null;
            for (AfmaCopyRect copyRect : copyRects) {
                AfmaCopyRect currentRect = Objects.requireNonNull(copyRect);
                bytes += 1;
                if (previousRect == null) {
                    bytes += estimateCopyRectBytes(currentRect);
                } else {
                    bytes += estimateSignedVarIntBytes(currentRect.getSrcX() - previousRect.getSrcX());
                    bytes += estimateSignedVarIntBytes(currentRect.getSrcY() - previousRect.getSrcY());
                    bytes += estimateSignedVarIntBytes(currentRect.getDstX() - previousRect.getDstX());
                    bytes += estimateSignedVarIntBytes(currentRect.getDstY() - previousRect.getDstY());
                    if (currentRect.getWidth() != previousRect.getWidth()) {
                        bytes += estimateVarIntBytes(currentRect.getWidth());
                    }
                    if (currentRect.getHeight() != previousRect.getHeight()) {
                        bytes += estimateVarIntBytes(currentRect.getHeight());
                    }
                }
                previousRect = currentRect;
            }
            return bytes;
        }
    }

    /** Retains or reuses {@code ResidualPlannerWorkspace} state while the AFMA creator is active. */
    protected static final class ResidualPlannerWorkspace {

        private int[] changedIndices = new int[0];
        private int[] predictedColors = new int[0];
        private int[] currentColors = new int[0];
        private final AfmaResidualPayloadHelper.ResidualEncodeWorkspace residualEncodeWorkspace = new AfmaResidualPayloadHelper.ResidualEncodeWorkspace();

        /** Returns the changed indices produced by the AFMA creator. */
        @NotNull
        protected int[] changedIndices(int minLength) {
            if (this.changedIndices.length < minLength) {
                this.changedIndices = new int[minLength];
            }
            return this.changedIndices;
        }

        /** Calculates the predicted colors value for the AFMA creator. */
        @NotNull
        protected int[] predictedColors(int minLength) {
            if (this.predictedColors.length < minLength) {
                this.predictedColors = new int[minLength];
            }
            return this.predictedColors;
        }

        /** Returns the current colors produced by the AFMA creator. */
        @NotNull
        protected int[] currentColors(int minLength) {
            if (this.currentColors.length < minLength) {
                this.currentColors = new int[minLength];
            }
            return this.currentColors;
        }

        /** Returns the residual encode workspace produced by the AFMA creator. */
        @NotNull
        protected AfmaResidualPayloadHelper.ResidualEncodeWorkspace residualEncodeWorkspace() {
            return this.residualEncodeWorkspace;
        }
    }

    /** Loads and validates {@code AsyncFrameLoader} state for the AFMA creator. */
    protected final class AsyncFrameLoader implements AutoCloseable {

        /** Current sequence state for this AFMA creator instance. */
        @NotNull
        protected final AfmaSourceSequence sequence;
        /** Current dimension state for this AFMA creator instance. */
        @NotNull
        protected final Dimension dimension;
        /** Current executor state for this AFMA creator instance. */
        @Nullable
        protected final ExecutorService executor;
        /** Current pixel buffer pool state for this AFMA creator instance. */
        @NotNull
        protected final AfmaFastPixelBufferPool pixelBufferPool;
        /** Current cancellation requested state for this AFMA creator instance. */
        @Nullable
        protected final BooleanSupplier cancellationRequested;
        /** Current next frame future state for this AFMA creator instance. */
        @Nullable
        protected CompletableFuture<AfmaPixelFrame> nextFrameFuture;
        /** Current next future index measured or selected by this AFMA creator instance. */
        protected int nextFutureIndex = -1;
        /** Owned current frame state for this AFMA creator instance. */
        @Nullable
        protected AfmaPixelFrame currentFrame;
        /** Current current index measured or selected by this AFMA creator instance. */
        protected int currentIndex = -1;
        /** Whether closed currently applies to this AFMA creator instance. */
        protected volatile boolean closed = false;

        /** Initializes a new {@code AsyncFrameLoader} for AFMA creator use. */
        protected AsyncFrameLoader(@NotNull AfmaSourceSequence sequence,
                                   @NotNull Dimension dimension,
                                   @Nullable AfmaPixelFrame firstFrameOverride,
                                   @NotNull AfmaFastPixelBufferPool pixelBufferPool,
                                   @Nullable ExecutorService executor,
                                   @Nullable BooleanSupplier cancellationRequested) {
            this.sequence = Objects.requireNonNull(sequence);
            this.dimension = Objects.requireNonNull(dimension);
            this.pixelBufferPool = Objects.requireNonNull(pixelBufferPool);
            this.executor = executor;
            this.cancellationRequested = cancellationRequested;
            this.currentFrame = firstFrameOverride;
            this.currentIndex = (firstFrameOverride != null) ? 0 : -1;
            if (firstFrameOverride != null) {
                this.validateFrameSize(firstFrameOverride, 0);
            }
            this.schedule(1);
        }

        /** Retrieves the frame from the AFMA creator. */
        @NotNull
        protected AfmaPixelFrame takeFrame(int frameIndex) throws IOException {
            checkCancelled(this.cancellationRequested);
            if ((this.currentFrame != null) && (this.currentIndex == frameIndex)) {
                AfmaPixelFrame frame = this.currentFrame;
                this.currentFrame = null;
                this.currentIndex = -1;
                this.schedule(frameIndex + 1);
                return frame;
            }

            if ((this.nextFrameFuture != null) && (this.nextFutureIndex == frameIndex)) {
                AfmaPixelFrame frame = this.joinFrameFuture(this.nextFrameFuture);
                this.nextFrameFuture = null;
                this.nextFutureIndex = -1;
                this.validateFrameSize(frame, frameIndex);
                this.schedule(frameIndex + 1);
                return frame;
            }

            AfmaPixelFrame frame = this.loadFrame(frameIndex);
            this.schedule(frameIndex + 1);
            return frame;
        }

        /** Schedules the schedule on the required AFMA creator lifecycle boundary. */
        protected void schedule(int frameIndex) {
            if ((frameIndex < 0) || (frameIndex >= this.sequence.size())) {
                return;
            }
            if ((this.nextFrameFuture != null) || (this.executor == null)) {
                return;
            }

            File frameFile = Objects.requireNonNull(this.sequence.getFrame(frameIndex));
            this.nextFutureIndex = frameIndex;
            this.nextFrameFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    checkCancelled(this.cancellationRequested);
                    AfmaPixelFrame loadedFrame = AfmaV2PlannerCore.this.frameNormalizer.loadFrame(
                            frameFile,
                            this.dimension.width(),
                            this.dimension.height(),
                            this.pixelBufferPool
                    );
                    try {
                        this.validateFrameSize(loadedFrame, frameIndex);
                        if (this.closed) {
                            throw new CancellationException("AFMA v2 frame prefetch was closed");
                        }
                        return loadedFrame;
                    } catch (RuntimeException | Error ex) {
                        CloseableUtils.closeQuietly(loadedFrame);
                        throw ex;
                    }
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            }, this.executor);
        }

        /** Loads the frame for the AFMA creator. */
        @NotNull
        protected AfmaPixelFrame loadFrame(int frameIndex) throws IOException {
            File frameFile = Objects.requireNonNull(this.sequence.getFrame(frameIndex));
            AfmaPixelFrame frame = AfmaV2PlannerCore.this.frameNormalizer.loadFrame(
                    frameFile,
                    this.dimension.width(),
                    this.dimension.height(),
                    this.pixelBufferPool
            );
            try {
                this.validateFrameSize(frame, frameIndex);
                if (this.closed) {
                    throw new CancellationException("AFMA v2 frame loader was closed");
                }
                return frame;
            } catch (RuntimeException | Error ex) {
                CloseableUtils.closeQuietly(frame);
                throw ex;
            }
        }

        /** Joins the frame future worker result for the AFMA creator. */
        @NotNull
        protected AfmaPixelFrame joinFrameFuture(@NotNull CompletableFuture<AfmaPixelFrame> frameFuture) throws IOException {
            try {
                return frameFuture.join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof IOException ioEx) {
                    throw ioEx;
                }
                if (cause instanceof RuntimeException runtimeEx) {
                    throw runtimeEx;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IOException("Failed to load AFMA v2 source frame", cause);
            }
        }

        /** Validates the frame size before AFMA creator processing. */
        protected void validateFrameSize(@NotNull AfmaPixelFrame frame, int frameIndex) {
            if ((frame.getWidth() != this.dimension.width()) || (frame.getHeight() != this.dimension.height())) {
                throw new IllegalArgumentException("AFMA v2 frame " + frameIndex + " has mismatched dimensions");
            }
        }

        /** Cancels frame prefetch and returns every completed or current pixel frame to its pool. */
        @Override
        public void close() {
            this.closed = true;
            if (this.nextFrameFuture != null) {
                CompletableFuture<AfmaPixelFrame> future = this.nextFrameFuture;
                future.cancel(true);
                if (future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled()) {
                    try {
                        CloseableUtils.closeQuietly(future.getNow(null));
                    } catch (CancellationException | CompletionException ignored) {
                    }
                }
                this.nextFrameFuture = null;
                this.nextFutureIndex = -1;
            }
            CloseableUtils.closeQuietly(this.currentFrame);
            this.currentFrame = null;
            this.currentIndex = -1;
        }
    }

}
