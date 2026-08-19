package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.file.FilenameComparator;
import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaDecoder;
import de.keksuccino.konkrete.util.resource.resources.texture.afma.AfmaIoHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns reusable AFMA creator inputs, options, validation state, and background job lifecycle. */
public class AfmaCreatorState {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final @NotNull AfmaOptimizationPreset DEFAULT_PRESET = AfmaOptimizationPreset.BALANCED;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Konkrete-AFMA-Creator");
        thread.setDaemon(true);
        return thread;
    });
    private final @NotNull AfmaEncodePlanner planner = new AfmaEncodePlanner();
    private final @NotNull AfmaArchiveWriter archiveWriter = new AfmaArchiveWriter();

    private volatile @Nullable File mainFramesDirectory;
    private volatile @Nullable File introFramesDirectory;
    private volatile @Nullable AfmaSourceSequence mainFramesSequenceOverride;
    private volatile @Nullable AfmaSourceSequence introFramesSequenceOverride;
    private volatile @NotNull String mainFramesInputText = "";
    private volatile @NotNull String introFramesInputText = "";
    private volatile @Nullable File outputFile;
    private volatile long frameTimeMs = 41L;
    private volatile long introFrameTimeMs = 41L;
    private volatile int loopCount = 0;
    private volatile int keyframeInterval = DEFAULT_PRESET.getKeyframeInterval();
    private volatile boolean rectCopyEnabled = DEFAULT_PRESET.isRectCopyEnabled();
    private volatile boolean duplicateFrameElision = DEFAULT_PRESET.isDuplicateFrameElision();
    private volatile boolean nearLosslessEnabled = DEFAULT_PRESET.isNearLosslessEnabledByDefault();
    private volatile boolean strictPostWriteValidation = false;
    private volatile int maxCopySearchDistance = DEFAULT_PRESET.getMaxCopySearchDistance();
    private volatile int maxCandidateAxisOffsets = DEFAULT_PRESET.getMaxCandidateAxisOffsets();
    private volatile boolean adaptiveKeyframePlacement = defaultAdaptiveKeyframePlacement(DEFAULT_PRESET);
    private volatile int adaptiveMaxKeyframeInterval = defaultAdaptiveMaxKeyframeInterval(DEFAULT_PRESET);
    private volatile long adaptiveContinuationMinSavingsBytes = defaultAdaptiveContinuationMinSavingsBytes(DEFAULT_PRESET);
    private volatile double adaptiveContinuationMinSavingsRatio = defaultAdaptiveContinuationMinSavingsRatio(DEFAULT_PRESET);
    private volatile int perceptualBinIntraMaxVisibleColorDelta = defaultPerceptualVisibleColorDelta(DEFAULT_PRESET);
    private volatile int perceptualBinIntraMaxAlphaDelta = defaultPerceptualAlphaDelta(DEFAULT_PRESET);
    private volatile double perceptualBinIntraMaxAverageError = defaultPerceptualAverageError(DEFAULT_PRESET);
    private volatile @NotNull AfmaOptimizationPreset optimizationPreset = DEFAULT_PRESET;
    private volatile @Nullable AfmaEncodeJob currentJob;

    /** Returns the main frames directory, or {@code null} when it is not available. */
    public @Nullable File getMainFramesDirectory() {
        return this.mainFramesDirectory;
    }

    /** Returns the main frames input text used by this AFMA creator instance. */
    public @NotNull String getMainFramesInputText() {
        return this.mainFramesInputText;
    }

    /** Sets the main frames directory used by subsequent AFMA creator operations. */
    public void setMainFramesDirectory(@Nullable File mainFramesDirectory) {
        this.mainFramesDirectory = normalizeFile(mainFramesDirectory);
        this.mainFramesSequenceOverride = null;
        this.mainFramesInputText = (this.mainFramesDirectory != null) ? this.mainFramesDirectory.getPath().replace("\\", "/") : "";
        this.markDirty();
    }

    /** Returns the intro frames directory, or {@code null} when it is not available. */
    public @Nullable File getIntroFramesDirectory() {
        return this.introFramesDirectory;
    }

    /** Returns the intro frames input text used by this AFMA creator instance. */
    public @NotNull String getIntroFramesInputText() {
        return this.introFramesInputText;
    }

    /** Sets the intro frames directory used by subsequent AFMA creator operations. */
    public void setIntroFramesDirectory(@Nullable File introFramesDirectory) {
        this.introFramesDirectory = normalizeFile(introFramesDirectory);
        this.introFramesSequenceOverride = null;
        this.introFramesInputText = (this.introFramesDirectory != null) ? this.introFramesDirectory.getPath().replace("\\", "/") : "";
        this.markDirty();
    }

    /** Sets the main frames list used by subsequent AFMA creator operations. */
    public void setMainFramesList(@NotNull List<File> frames) {
        this.mainFramesSequenceOverride = AfmaSourceSequence.ofFiles(frames);
        this.mainFramesDirectory = null;
        this.mainFramesInputText = summarizeSourceList("Ordered PNG List", frames);
        this.markDirty();
    }

    /** Sets the intro frames list used by subsequent AFMA creator operations. */
    public void setIntroFramesList(@NotNull List<File> frames) {
        this.introFramesSequenceOverride = AfmaSourceSequence.ofFiles(frames);
        this.introFramesDirectory = null;
        this.introFramesInputText = summarizeSourceList("Ordered PNG List", frames);
        this.markDirty();
    }

    /** Returns the output file, or {@code null} when it is not available. */
    public @Nullable File getOutputFile() {
        return this.outputFile;
    }

    /** Sets the output file used by subsequent AFMA creator operations. */
    public void setOutputFile(@Nullable File outputFile) {
        this.outputFile = normalizeFile(outputFile);
        this.markDirty();
    }

    /** Returns the frame time ms used by this AFMA creator instance. */
    public long getFrameTimeMs() {
        return this.frameTimeMs;
    }

    /** Sets the frame time ms used by subsequent AFMA creator operations. */
    public void setFrameTimeMs(long frameTimeMs) {
        this.frameTimeMs = frameTimeMs;
        this.markDirty();
    }

    /** Returns the intro frame time ms used by this AFMA creator instance. */
    public long getIntroFrameTimeMs() {
        return this.introFrameTimeMs;
    }

    /** Sets the intro frame time ms used by subsequent AFMA creator operations. */
    public void setIntroFrameTimeMs(long introFrameTimeMs) {
        this.introFrameTimeMs = introFrameTimeMs;
        this.markDirty();
    }

    /** Returns the loop count used by this AFMA creator instance. */
    public int getLoopCount() {
        return this.loopCount;
    }

    /** Sets the loop count used by subsequent AFMA creator operations. */
    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
        this.markDirty();
    }

    /** Returns the keyframe interval used by this AFMA creator instance. */
    public int getKeyframeInterval() {
        return this.keyframeInterval;
    }

    /** Sets the keyframe interval used by subsequent AFMA creator operations. */
    public void setKeyframeInterval(int keyframeInterval) {
        this.keyframeInterval = keyframeInterval;
        this.markDirty();
    }

    /** Returns whether rect copy enabled. */
    public boolean isRectCopyEnabled() {
        return this.rectCopyEnabled;
    }

    /** Sets the rect copy enabled used by subsequent AFMA creator operations. */
    public void setRectCopyEnabled(boolean rectCopyEnabled) {
        this.rectCopyEnabled = rectCopyEnabled;
        this.markDirty();
    }

    /** Returns whether duplicate frame elision. */
    public boolean isDuplicateFrameElision() {
        return this.duplicateFrameElision;
    }

    /** Sets the duplicate frame elision used by subsequent AFMA creator operations. */
    public void setDuplicateFrameElision(boolean duplicateFrameElision) {
        this.duplicateFrameElision = duplicateFrameElision;
        this.markDirty();
    }

    /** Returns whether near lossless enabled. */
    public boolean isNearLosslessEnabled() {
        return this.nearLosslessEnabled;
    }

    /** Sets the near lossless enabled used by subsequent AFMA creator operations. */
    public void setNearLosslessEnabled(boolean nearLosslessEnabled) {
        this.nearLosslessEnabled = nearLosslessEnabled;
        this.markDirty();
    }

    /** Returns whether strict post write validation. */
    public boolean isStrictPostWriteValidation() {
        return this.strictPostWriteValidation;
    }

    /** Sets the strict post write validation used by subsequent AFMA creator operations. */
    public void setStrictPostWriteValidation(boolean strictPostWriteValidation) {
        this.strictPostWriteValidation = strictPostWriteValidation;
        this.markDirty();
    }

    /** Returns the max copy search distance used by this AFMA creator instance. */
    public int getMaxCopySearchDistance() {
        return this.maxCopySearchDistance;
    }

    /** Sets the max copy search distance used by subsequent AFMA creator operations. */
    public void setMaxCopySearchDistance(int maxCopySearchDistance) {
        this.maxCopySearchDistance = maxCopySearchDistance;
        this.markDirty();
    }

    /** Returns the max candidate axis offsets used by this AFMA creator instance. */
    public int getMaxCandidateAxisOffsets() {
        return this.maxCandidateAxisOffsets;
    }

    /** Sets the max candidate axis offsets used by subsequent AFMA creator operations. */
    public void setMaxCandidateAxisOffsets(int maxCandidateAxisOffsets) {
        this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
        this.markDirty();
    }

    /** Returns whether adaptive keyframe placement. */
    public boolean isAdaptiveKeyframePlacement() {
        return this.adaptiveKeyframePlacement;
    }

    /** Sets the adaptive keyframe placement used by subsequent AFMA creator operations. */
    public void setAdaptiveKeyframePlacement(boolean adaptiveKeyframePlacement) {
        this.adaptiveKeyframePlacement = adaptiveKeyframePlacement;
        this.markDirty();
    }

    /** Returns the adaptive max keyframe interval used by this AFMA creator instance. */
    public int getAdaptiveMaxKeyframeInterval() {
        return this.adaptiveMaxKeyframeInterval;
    }

    /** Sets the adaptive max keyframe interval used by subsequent AFMA creator operations. */
    public void setAdaptiveMaxKeyframeInterval(int adaptiveMaxKeyframeInterval) {
        this.adaptiveMaxKeyframeInterval = adaptiveMaxKeyframeInterval;
        this.markDirty();
    }

    /** Returns the adaptive continuation min savings bytes used by this AFMA creator instance. */
    public long getAdaptiveContinuationMinSavingsBytes() {
        return this.adaptiveContinuationMinSavingsBytes;
    }

    /** Sets the adaptive continuation min savings bytes used by subsequent AFMA creator operations. */
    public void setAdaptiveContinuationMinSavingsBytes(long adaptiveContinuationMinSavingsBytes) {
        this.adaptiveContinuationMinSavingsBytes = adaptiveContinuationMinSavingsBytes;
        this.markDirty();
    }

    /** Returns the adaptive continuation min savings ratio used by this AFMA creator instance. */
    public double getAdaptiveContinuationMinSavingsRatio() {
        return this.adaptiveContinuationMinSavingsRatio;
    }

    /** Sets the adaptive continuation min savings ratio used by subsequent AFMA creator operations. */
    public void setAdaptiveContinuationMinSavingsRatio(double adaptiveContinuationMinSavingsRatio) {
        this.adaptiveContinuationMinSavingsRatio = adaptiveContinuationMinSavingsRatio;
        this.markDirty();
    }

    /** Returns the perceptual bin intra max visible color delta used by this AFMA creator instance. */
    public int getPerceptualBinIntraMaxVisibleColorDelta() {
        return this.perceptualBinIntraMaxVisibleColorDelta;
    }

    /** Sets the perceptual bin intra max visible color delta used by subsequent AFMA creator operations. */
    public void setPerceptualBinIntraMaxVisibleColorDelta(int perceptualBinIntraMaxVisibleColorDelta) {
        this.perceptualBinIntraMaxVisibleColorDelta = perceptualBinIntraMaxVisibleColorDelta;
        this.markDirty();
    }

    /** Returns the perceptual bin intra max alpha delta used by this AFMA creator instance. */
    public int getPerceptualBinIntraMaxAlphaDelta() {
        return this.perceptualBinIntraMaxAlphaDelta;
    }

    /** Sets the perceptual bin intra max alpha delta used by subsequent AFMA creator operations. */
    public void setPerceptualBinIntraMaxAlphaDelta(int perceptualBinIntraMaxAlphaDelta) {
        this.perceptualBinIntraMaxAlphaDelta = perceptualBinIntraMaxAlphaDelta;
        this.markDirty();
    }

    /** Returns the perceptual bin intra max average error used by this AFMA creator instance. */
    public double getPerceptualBinIntraMaxAverageError() {
        return this.perceptualBinIntraMaxAverageError;
    }

    /** Sets the perceptual bin intra max average error used by subsequent AFMA creator operations. */
    public void setPerceptualBinIntraMaxAverageError(double perceptualBinIntraMaxAverageError) {
        this.perceptualBinIntraMaxAverageError = perceptualBinIntraMaxAverageError;
        this.markDirty();
    }

    /** Returns the optimization preset used by this AFMA creator instance. */
    public @NotNull AfmaOptimizationPreset getOptimizationPreset() {
        return this.optimizationPreset;
    }

    /** Applies the preset to the AFMA creator. */
    public void applyPreset(@NotNull AfmaOptimizationPreset preset) {
        this.optimizationPreset = Objects.requireNonNull(preset);
        this.keyframeInterval = preset.getKeyframeInterval();
        this.rectCopyEnabled = preset.isRectCopyEnabled();
        this.duplicateFrameElision = preset.isDuplicateFrameElision();
        this.nearLosslessEnabled = preset.isNearLosslessEnabledByDefault();
        this.maxCopySearchDistance = preset.getMaxCopySearchDistance();
        this.maxCandidateAxisOffsets = preset.getMaxCandidateAxisOffsets();
        this.adaptiveKeyframePlacement = defaultAdaptiveKeyframePlacement(preset);
        this.adaptiveMaxKeyframeInterval = defaultAdaptiveMaxKeyframeInterval(preset);
        this.adaptiveContinuationMinSavingsBytes = defaultAdaptiveContinuationMinSavingsBytes(preset);
        this.adaptiveContinuationMinSavingsRatio = defaultAdaptiveContinuationMinSavingsRatio(preset);
        this.perceptualBinIntraMaxVisibleColorDelta = defaultPerceptualVisibleColorDelta(preset);
        this.perceptualBinIntraMaxAlphaDelta = defaultPerceptualAlphaDelta(preset);
        this.perceptualBinIntraMaxAverageError = defaultPerceptualAverageError(preset);
        this.markDirty();
    }

    /** Returns the current job, or {@code null} when it is not available. */
    public @Nullable AfmaEncodeJob getCurrentJob() {
        return this.currentJob;
    }

    /** Returns whether job running. */
    public boolean isJobRunning() {
        AfmaEncodeJob job = this.currentJob;
        return (job != null) && job.isRunning();
    }

    /** Requests cancellation of the current encode job, if one exists. */
    public void cancelCurrentJob() {
        AfmaEncodeJob job = this.currentJob;
        if (job != null) {
            job.cancel();
        }
    }

    /** Clears the intro frames directory from this AFMA creator component. */
    public void clearIntroFramesDirectory() {
        this.introFramesDirectory = null;
        this.introFramesSequenceOverride = null;
        this.introFramesInputText = "";
        this.markDirty();
    }

    /** Marks the dirty state for the AFMA creator. */
    public void markDirty() {
    }

    /** Starts the export for the AFMA creator. */
    public void startExport() {
        if (this.isJobRunning()) {
            throw new IllegalStateException("Another AFMA creator job is already running");
        }

        PreparedInputs prepared = this.prepare(true);
        AfmaEncodeJob job = new AfmaEncodeJob();
        this.currentJob = job;
        this.executor.execute(() -> this.runExportJob(job, prepared));
    }

    /** Runs the export job for the AFMA creator. */
    protected void runExportJob(@NotNull AfmaEncodeJob job, @NotNull PreparedInputs prepared) {
        File tempFile = null;
        try {
            AfmaIoHelper.configureBaseTempDirectory(ResourceRuntime.temporaryDirectory("afma_creator"));
            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.VALIDATING_SOURCES, "Validating AFMA creator input...", null, 0.10D));
            checkCancelled(job);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames for export...", null, 0.15D));
            try (AfmaEncodePlan exportPlan = this.planner.plan(prepared.mainSequence, prepared.introSequence, prepared.options, job::isCancellationRequested,
                    (detail, progress) -> job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames for export...", detail, 0.15D + (0.63D * progress))))) {
                checkCancelled(job);

                job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.PACKING_ARCHIVE, "Packing AFMA archive...", prepared.outputFile.getName(), 0.82D));
                tempFile = new File(prepared.outputFile.getParentFile(), prepared.outputFile.getName() + ".tmp");
                org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
                this.archiveWriter.write(exportPlan, tempFile, job::isCancellationRequested,
                        (path, progress) -> job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.PACKING_ARCHIVE, "Packing AFMA archive...", path, 0.82D + (0.16D * progress))));
                checkCancelled(job);

                if (prepared.strictPostWriteValidation) {
                    job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.PACKING_ARCHIVE, "Validating AFMA archive...", prepared.outputFile.getName(), 0.985D));
                    this.validateWrittenArchive(tempFile);
                    checkCancelled(job);
                }
            }

            Files.move(tempFile.toPath(), prepared.outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tempFile = null;

            job.completeSuccess(prepared.outputFile);
        } catch (CancellationException ex) {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            job.completeCancelled();
        } catch (Throwable throwable) {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            LOGGER.error("[KONKRETE] AFMA creator export job failed", throwable);
            job.completeFailure(throwable);
        }
    }

    /** Prepares resource state for the AFMA creator. */
    protected @NotNull PreparedInputs prepare(boolean requireOutput) {
        File mainDirectory = normalizeFile(this.mainFramesDirectory);
        File introDirectory = normalizeFile(this.introFramesDirectory);
        File output = normalizeFile(this.outputFile);
        if ((mainDirectory != null) && !mainDirectory.isDirectory()) {
            throw new IllegalArgumentException("Select a valid main frames directory or clear the main path.");
        }
        if ((introDirectory != null) && !introDirectory.isDirectory()) {
            throw new IllegalArgumentException("Select a valid intro frames directory or clear the intro path.");
        }
        if ((mainDirectory == null) && (introDirectory == null) && (this.mainFramesSequenceOverride == null) && (this.introFramesSequenceOverride == null)) {
            throw new IllegalArgumentException("Select a valid main or intro frames directory first.");
        }
        if (requireOutput) {
            if (output == null) {
                throw new IllegalArgumentException("Select a valid AFMA output path first.");
            }
            if (output.isDirectory()) {
                throw new IllegalArgumentException("The AFMA output path must point to a file, not a directory.");
            }
            if (!output.getName().toLowerCase(Locale.ROOT).endsWith(".afma")) {
                output = (output.getParentFile() != null)
                        ? new File(output.getParentFile(), output.getName() + ".afma")
                        : new File(output.getPath() + ".afma");
            }
        }

        ResolvedSource main = (this.mainFramesSequenceOverride != null)
                ? new ResolvedSource(this.mainFramesSequenceOverride)
                : ((mainDirectory != null) ? this.resolveSource(mainDirectory, true, "main") : ResolvedSource.empty());
        ResolvedSource intro = (this.introFramesSequenceOverride != null)
                ? new ResolvedSource(this.introFramesSequenceOverride)
                : ((introDirectory != null && introDirectory.isDirectory()) ? this.resolveSource(introDirectory, true, "intro") : ResolvedSource.empty());
        if (main.sequence.isEmpty() && intro.sequence.isEmpty()) {
            throw new IllegalArgumentException("The selected source directories do not contain any PNG frame files.");
        }

        AfmaEncodeOptions options = new AfmaEncodeOptions()
                .setFrameTimeMs(parsePositiveLong(this.frameTimeMs, "main frame time"))
                .setIntroFrameTimeMs(parsePositiveLong(this.introFrameTimeMs, "intro frame time"))
                .setLoopCount(this.loopCount)
                .setKeyframeInterval(parsePositiveInt(this.keyframeInterval, "keyframe interval"))
                .setRectCopyEnabled(this.rectCopyEnabled)
                .setDuplicateFrameElision(this.duplicateFrameElision)
                .setNearLosslessMaxChannelDelta(this.nearLosslessEnabled ? AfmaEncodeOptions.DEFAULT_NEAR_LOSSLESS_MAX_CHANNEL_DELTA : 0)
                .setMaxCopySearchDistance(this.maxCopySearchDistance)
                .setMaxCandidateAxisOffsets(this.maxCandidateAxisOffsets);
        this.applyAdvancedCompressionDefaults(options);

        return new PreparedInputs(
                main.sequence,
                intro.sequence,
                output,
                options,
                this.strictPostWriteValidation
        );
    }

    /** Applies the advanced compression defaults to the AFMA creator. */
    protected void applyAdvancedCompressionDefaults(@NotNull AfmaEncodeOptions options) {
        int preferredKeyframeInterval = Math.max(1, options.getKeyframeInterval());
        switch (this.optimizationPreset) {
            case BEST_QUALITY -> options
                    .setPlannerSearchWindowFrames(24)
                    .setPlannerBeamWidth(14)
                    .setPlannerDecodeCostPenaltyBytes(80L)
                    .setPlannerComplexityPenaltyBytes(8L)
                    .setPlannerAverageDriftPenaltyBytes(16D)
                    .setPlannerVisibleColorDriftPenaltyBytes(4L)
                    .setPlannerAlphaDriftPenaltyBytes(4L)
                    .setPlannerLossyContinuationPenaltyBytes(96L)
                    .setPlannerKeyframeDistancePenaltyBytes(96L)
                    .setMaxDeltaAreaRatioWithoutStrongSavings(1.0D)
                    .setMaxCopyPatchAreaRatioWithoutStrongSavings(0.97D)
                    .setMinComplexCandidateSavingsBytes(8L * 1024L)
                    .setMinStrongComplexCandidateSavingsBytes(32L * 1024L)
                    .setMinComplexCandidateSavingsRatio(0.005D)
                    .setMinStrongComplexCandidateSavingsRatio(0.02D);
            case SMALLEST_FILE -> options
                    .setPlannerSearchWindowFrames(24)
                    .setPlannerBeamWidth(14)
                    .setPlannerDecodeCostPenaltyBytes(80L)
                    .setPlannerComplexityPenaltyBytes(8L)
                    .setPlannerAverageDriftPenaltyBytes(24D)
                    .setPlannerVisibleColorDriftPenaltyBytes(8L)
                    .setPlannerAlphaDriftPenaltyBytes(8L)
                    .setPlannerLossyContinuationPenaltyBytes(40L)
                    .setPlannerKeyframeDistancePenaltyBytes(96L)
                    .setMaxDeltaAreaRatioWithoutStrongSavings(1.0D)
                    .setMaxCopyPatchAreaRatioWithoutStrongSavings(0.97D)
                    .setMinComplexCandidateSavingsBytes(8L * 1024L)
                    .setMinStrongComplexCandidateSavingsBytes(32L * 1024L)
                    .setMinComplexCandidateSavingsRatio(0.005D)
                    .setMinStrongComplexCandidateSavingsRatio(0.02D);
            case BALANCED -> options
                    .setPlannerSearchWindowFrames(16)
                    .setPlannerBeamWidth(10)
                    .setPlannerDecodeCostPenaltyBytes(144L)
                    .setPlannerComplexityPenaltyBytes(12L)
                    .setPlannerAverageDriftPenaltyBytes(40D)
                    .setPlannerVisibleColorDriftPenaltyBytes(10L)
                    .setPlannerAlphaDriftPenaltyBytes(10L)
                    .setPlannerLossyContinuationPenaltyBytes(80L)
                    .setPlannerKeyframeDistancePenaltyBytes(160L);
        }

        options
                .setFullFrameReferencePlanEnabled((this.optimizationPreset == AfmaOptimizationPreset.SMALLEST_FILE)
                        || (this.optimizationPreset == AfmaOptimizationPreset.BEST_QUALITY))
                .setAdaptiveKeyframePlacement(this.adaptiveKeyframePlacement)
                .setAdaptiveMaxKeyframeInterval(Math.max(preferredKeyframeInterval, parsePositiveInt(this.adaptiveMaxKeyframeInterval, "adaptive max keyframe interval")))
                .setAdaptiveContinuationMinSavingsBytes(parseNonNegativeLong(this.adaptiveContinuationMinSavingsBytes, "adaptive continuation savings bytes"))
                .setAdaptiveContinuationMinSavingsRatio(parseNonNegativeDouble(this.adaptiveContinuationMinSavingsRatio, "adaptive continuation savings ratio"));

        if (!this.nearLosslessEnabled) {
            options
                    .setPerceptualBinIntraMaxVisibleColorDelta(0)
                    .setPerceptualBinIntraMaxAlphaDelta(0)
                    .setPerceptualBinIntraMaxAverageError(0D)
                    .setPlannerMaxCumulativeAverageError(0D)
                    .setPlannerMaxCumulativeVisibleColorDelta(0)
                    .setPlannerMaxCumulativeAlphaDelta(0)
                    .setPlannerMaxConsecutiveLossyFrames(0);
            return;
        }

        switch (this.optimizationPreset) {
            case BEST_QUALITY -> options
                    .setPerceptualBinIntraMaxVisibleColorDelta(parseNonNegativeInt(this.perceptualBinIntraMaxVisibleColorDelta, "perceptual BIN_INTRA visible color delta"))
                    .setPerceptualBinIntraMaxAlphaDelta(parseNonNegativeInt(this.perceptualBinIntraMaxAlphaDelta, "perceptual BIN_INTRA alpha delta"))
                    .setPerceptualBinIntraMaxAverageError(parseNonNegativeDouble(this.perceptualBinIntraMaxAverageError, "perceptual BIN_INTRA average error"));
            case SMALLEST_FILE -> options
                    .setPerceptualBinIntraMaxVisibleColorDelta(parseNonNegativeInt(this.perceptualBinIntraMaxVisibleColorDelta, "perceptual BIN_INTRA visible color delta"))
                    .setPerceptualBinIntraMaxAlphaDelta(parseNonNegativeInt(this.perceptualBinIntraMaxAlphaDelta, "perceptual BIN_INTRA alpha delta"))
                    .setPerceptualBinIntraMaxAverageError(parseNonNegativeDouble(this.perceptualBinIntraMaxAverageError, "perceptual BIN_INTRA average error"));
            case BALANCED -> options
                    .setPerceptualBinIntraMaxVisibleColorDelta(parseNonNegativeInt(this.perceptualBinIntraMaxVisibleColorDelta, "perceptual BIN_INTRA visible color delta"))
                    .setPerceptualBinIntraMaxAlphaDelta(parseNonNegativeInt(this.perceptualBinIntraMaxAlphaDelta, "perceptual BIN_INTRA alpha delta"))
                    .setPerceptualBinIntraMaxAverageError(parseNonNegativeDouble(this.perceptualBinIntraMaxAverageError, "perceptual BIN_INTRA average error"));
        }

        switch (this.optimizationPreset) {
            case BEST_QUALITY -> options
                    .setPlannerMaxCumulativeAverageError(Math.max(4D, options.getPerceptualBinIntraMaxAverageError() * 1.5D))
                    .setPlannerMaxCumulativeVisibleColorDelta(Math.max(8, options.getPerceptualBinIntraMaxVisibleColorDelta() * 2))
                    .setPlannerMaxCumulativeAlphaDelta(Math.max(16, options.getPerceptualBinIntraMaxAlphaDelta()))
                    .setPlannerMaxConsecutiveLossyFrames(1);
            case SMALLEST_FILE -> options
                    .setPlannerMaxCumulativeAverageError(Math.max(18D, options.getPerceptualBinIntraMaxAverageError() * 4.0D))
                    .setPlannerMaxCumulativeVisibleColorDelta(Math.max(48, options.getPerceptualBinIntraMaxVisibleColorDelta() * 4))
                    .setPlannerMaxCumulativeAlphaDelta(Math.max(80, options.getPerceptualBinIntraMaxAlphaDelta() * 3))
                    .setPlannerMaxConsecutiveLossyFrames(10);
            case BALANCED -> options
                    .setPlannerMaxCumulativeAverageError(Math.max(10D, options.getPerceptualBinIntraMaxAverageError() * 2.75D))
                    .setPlannerMaxCumulativeVisibleColorDelta(Math.max(24, options.getPerceptualBinIntraMaxVisibleColorDelta() * 3))
                    .setPlannerMaxCumulativeAlphaDelta(Math.max(48, options.getPerceptualBinIntraMaxAlphaDelta() * 2))
                    .setPlannerMaxConsecutiveLossyFrames(6);
        }
    }

    /** Validates the written archive before AFMA creator processing. */
    protected void validateWrittenArchive(@NotNull File archiveFile) throws IOException {
        try (AfmaDecoder decoder = new AfmaDecoder()) {
            decoder.read(archiveFile);
            decoder.validateAllReferencedPayloadHeaders();
        }
    }

    /** Cancels the active encode job, stops the worker executor, and forgets the terminal job reference. */
    public void close() {
        this.cancelCurrentJob();
        this.executor.shutdownNow();
        this.currentJob = null;
    }

    /** Resolves the source for the AFMA creator. */
    protected @NotNull ResolvedSource resolveSource(@NotNull File directory, boolean optional, @NotNull String sequenceName) {
        File[] files = directory.listFiles(file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".png"));
        if ((files == null) || (files.length == 0)) {
            if (optional) {
                return ResolvedSource.empty();
            }
            throw new IllegalArgumentException("The selected " + sequenceName + " directory does not contain any PNG frame files.");
        }

        List<File> naturalOrder = new ArrayList<>(List.of(files));
        naturalOrder.sort(Comparator.comparing(File::getName, new FilenameComparator()));

        return new ResolvedSource(AfmaSourceSequence.ofFiles(naturalOrder));
    }

    /** Parses the positive int from the AFMA creator input. */
    protected static int parsePositiveInt(int value, @NotNull String label) {
        if (value <= 0) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than 0.");
        }
        return value;
    }

    /** Parses the non negative int from the AFMA creator input. */
    protected static int parseNonNegativeInt(int value, @NotNull String label) {
        if (value < 0) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than or equal to 0.");
        }
        return value;
    }

    /** Parses the positive long from the AFMA creator input. */
    protected static long parsePositiveLong(long value, @NotNull String label) {
        if (value <= 0L) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than 0.");
        }
        return value;
    }

    /** Parses the non negative long from the AFMA creator input. */
    protected static long parseNonNegativeLong(long value, @NotNull String label) {
        if (value < 0L) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than or equal to 0.");
        }
        return value;
    }

    /** Parses the non negative double from the AFMA creator input. */
    protected static double parseNonNegativeDouble(double value, @NotNull String label) {
        if (!Double.isFinite(value) || (value < 0D)) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a finite value greater than or equal to 0.");
        }
        return value;
    }

    /** Checks the cancelled before continuing the AFMA creator operation. */
    protected static void checkCancelled(@Nullable AfmaEncodeJob job) {
        if ((job != null) && job.isCancellationRequested()) {
            throw new CancellationException("AFMA creator job was cancelled");
        }
    }

    /** Normalizes the file for the AFMA creator. */
    protected static @Nullable File normalizeFile(@Nullable File file) {
        if (file == null) return null;
        return new File(file.getPath().replace("\\", "/"));
    }

    /** Returns whether default adaptive keyframe placement. */
    protected static boolean defaultAdaptiveKeyframePlacement(@NotNull AfmaOptimizationPreset preset) {
        return true;
    }

    /** Returns the default adaptive max keyframe interval produced by the AFMA creator. */
    protected static int defaultAdaptiveMaxKeyframeInterval(@NotNull AfmaOptimizationPreset preset) {
        int preferredKeyframeInterval = Math.max(1, preset.getKeyframeInterval());
        return switch (preset) {
            case BEST_QUALITY -> Math.max(preferredKeyframeInterval * 4, preferredKeyframeInterval + 180);
            case SMALLEST_FILE -> Math.max(preferredKeyframeInterval * 4, preferredKeyframeInterval + 180);
            case BALANCED -> Math.max(preferredKeyframeInterval * 2, preferredKeyframeInterval + 36);
        };
    }

    /** Returns the default adaptive continuation min savings bytes produced by the AFMA creator. */
    protected static long defaultAdaptiveContinuationMinSavingsBytes(@NotNull AfmaOptimizationPreset preset) {
        return switch (preset) {
            case BEST_QUALITY -> 128L;
            case SMALLEST_FILE -> 128L;
            case BALANCED -> 768L;
        };
    }

    /** Returns the default adaptive continuation min savings ratio produced by the AFMA creator. */
    protected static double defaultAdaptiveContinuationMinSavingsRatio(@NotNull AfmaOptimizationPreset preset) {
        return switch (preset) {
            case BEST_QUALITY -> 0.0025D;
            case SMALLEST_FILE -> 0.0025D;
            case BALANCED -> 0.0075D;
        };
    }

    /** Returns the default perceptual visible color delta produced by the AFMA creator. */
    protected static int defaultPerceptualVisibleColorDelta(@NotNull AfmaOptimizationPreset preset) {
        return switch (preset) {
            case BEST_QUALITY -> 0;
            case SMALLEST_FILE -> 24;
            case BALANCED -> 14;
        };
    }

    /** Returns the default perceptual alpha delta produced by the AFMA creator. */
    protected static int defaultPerceptualAlphaDelta(@NotNull AfmaOptimizationPreset preset) {
        return switch (preset) {
            case BEST_QUALITY -> 0;
            case SMALLEST_FILE -> 64;
            case BALANCED -> 20;
        };
    }

    /** Returns the default perceptual average error produced by the AFMA creator. */
    protected static double defaultPerceptualAverageError(@NotNull AfmaOptimizationPreset preset) {
        return switch (preset) {
            case BEST_QUALITY -> 0D;
            case SMALLEST_FILE -> 12.0D;
            case BALANCED -> 6.0D;
        };
    }

    /** Summarizes the source list for the AFMA creator. */
    protected static @NotNull String summarizeSourceList(@NotNull String label, @NotNull List<File> frames) {
        String summary = label + " (" + frames.size() + " frames)";
        if (!frames.isEmpty()) {
            summary += ": " + frames.get(0).getName();
        }
        return summary;
    }

    /** Models {@code PreparedInputs} state used by the AFMA creator. */
    protected record PreparedInputs(
            @NotNull AfmaSourceSequence mainSequence,
            @NotNull AfmaSourceSequence introSequence,
            @Nullable File outputFile,
            @NotNull AfmaEncodeOptions options,
            boolean strictPostWriteValidation
    ) {
    }

    /** Carries {@code ResolvedSource} data between validated stages of the AFMA creator. */
    protected record ResolvedSource(@NotNull AfmaSourceSequence sequence) {
        /** Creates the empty AFMA creator variant. */
        public static @NotNull ResolvedSource empty() {
            return new ResolvedSource(AfmaSourceSequence.empty());
        }
    }

}
