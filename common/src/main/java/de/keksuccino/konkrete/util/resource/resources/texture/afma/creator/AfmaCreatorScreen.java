package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.ScreenUtils;

import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.VanillaEvents;
import de.keksuccino.konkrete.util.cycle.CommonCycles;
import de.keksuccino.konkrete.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.konkrete.util.file.FilenameComparator;
import de.keksuccino.konkrete.util.file.GameDirectoryUtils;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.konkrete.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser.ChooseDirectoryWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser.SaveFileWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Edits an {@link AfmaCreatorState} and drives its cancellable AFMA export job through reusable Konkrete widgets. */
public class AfmaCreatorScreen extends Screen {

    private static final int OUTER_PADDING = 20;
    private static final int PANEL_PADDING = 12;
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int SECTION_GAP = 46;
    private static final int PANEL_TOP = 40;
    private static final int INLINE_LABEL_GAP = 8;
    private static final int COLUMN_GAP = 14;
    private static final int FIELD_BUTTON_GAP = 8;
    private static final int BUTTON_GROUP_GAP = 4;
    private static final int MIN_INPUT_WIDTH = 120;
    private static final int CONTENT_SCROLL_BAR_GAP = 6;
    private static final int SCROLL_CONTENT_VERTICAL_PADDING = 10;
    private static final int SCROLL_CONTENT_HORIZONTAL_PADDING = 2;

    private final @NotNull Screen parentScreen;
    private final @NotNull AfmaCreatorState state;
    private final @NotNull List<PiPWindow> childWindows = new ArrayList<>();
    private final @NotNull ScrollBar contentScrollBar;
    private final @NotNull List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final @NotNull List<AbstractWidget> fixedWidgets = new ArrayList<>();
    private boolean syncingWidgets = false;
    private @Nullable AfmaEncodeJob handledTerminalJob = null;
    private int scrollableContentWidth = 0;
    private int scrollableContentHeight = 0;
    private int diagnosticsY = 0;

    private @Nullable ExtendedEditBox mainFramesPathEditBox;
    private @Nullable ExtendedEditBox introFramesPathEditBox;
    private @Nullable ExtendedEditBox outputPathEditBox;
    private @Nullable ExtendedEditBox frameTimeEditBox;
    private @Nullable ExtendedEditBox introFrameTimeEditBox;
    private @Nullable ExtendedEditBox loopCountEditBox;
    private @Nullable ExtendedEditBox keyframeIntervalEditBox;
    private @Nullable ExtendedEditBox adaptiveMaxKeyframeIntervalEditBox;
    private @Nullable ExtendedEditBox adaptiveContinuationMinSavingsBytesEditBox;
    private @Nullable ExtendedEditBox adaptiveContinuationMinSavingsRatioEditBox;
    private @Nullable ExtendedEditBox perceptualVisibleColorDeltaEditBox;
    private @Nullable ExtendedEditBox perceptualAlphaDeltaEditBox;
    private @Nullable ExtendedEditBox perceptualAverageErrorEditBox;
    private @Nullable ExtendedEditBox maxCopySearchDistanceEditBox;
    private @Nullable ExtendedEditBox maxCandidateAxisOffsetsEditBox;

    private @Nullable ExtendedButton browseMainFramesButton;
    private @Nullable ExtendedButton browseIntroFramesButton;
    private @Nullable ExtendedButton clearIntroFramesButton;
    private @Nullable ExtendedButton browseOutputButton;
    private @Nullable ExtendedButton exportButton;
    private @Nullable ExtendedButton cancelJobButton;
    private @Nullable ExtendedButton closeButton;

    private @Nullable CycleButton<AfmaOptimizationPreset> presetCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> rectCopyCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> duplicateCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> nearLosslessCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> strictPostWriteValidationCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> adaptiveKeyframeCycleButton;

    /** Creates a screen with a fresh creator state. */
    public AfmaCreatorScreen(@NotNull Screen parentScreen) {
        this(parentScreen, new AfmaCreatorState());
    }

    /** Creates a screen that takes ownership of the supplied creator state and closes it when removed. */
    public AfmaCreatorScreen(@NotNull Screen parentScreen, @NotNull AfmaCreatorState state) {
        super(Component.translatable("konkrete.afma.creator.title"));
        this.parentScreen = java.util.Objects.requireNonNull(parentScreen, "parentScreen");
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.contentScrollBar = this.createContentScrollBar();
    }

    /** Returns the screen-owned creator state, which remains usable until this screen is removed. */
    @NotNull
    public AfmaCreatorState getState() {
        return this.state;
    }

    /** {@inheritDoc} */
    @Override
    protected void init() {
        this.clearWidgets();
        this.childWindows.removeIf(window -> window == null);
        this.scrollableWidgets.clear();
        this.fixedWidgets.clear();

        this.mainFramesPathEditBox = this.addStyledEditBox(Component.translatable("konkrete.afma.creator.main_frames"));
        this.mainFramesPathEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            this.state.setMainFramesDirectory(pathToDirectory(value));
        });

        this.introFramesPathEditBox = this.addStyledEditBox(Component.translatable("konkrete.afma.creator.intro_frames"));
        this.introFramesPathEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            this.state.setIntroFramesDirectory(pathToDirectory(value));
        });

        this.outputPathEditBox = this.addStyledEditBox(Component.translatable("konkrete.afma.creator.output_file"));
        this.outputPathEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            this.state.setOutputFile(pathToFile(value));
        });

        this.frameTimeEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.frame_time"));
        this.frameTimeEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setFrameTimeMs(Long.parseLong(value));
        });

        this.introFrameTimeEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.intro_frame_time"));
        this.introFrameTimeEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setIntroFrameTimeMs(Long.parseLong(value));
        });

        this.loopCountEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.loop_count"));
        this.loopCountEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInt(value)) this.state.setLoopCount(Integer.parseInt(value));
        });

        this.keyframeIntervalEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.keyframe_interval"));
        this.keyframeIntervalEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInt(value)) this.state.setKeyframeInterval(Integer.parseInt(value));
        });

        this.adaptiveMaxKeyframeIntervalEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.adaptive_max_keyframe_interval"));
        this.adaptiveMaxKeyframeIntervalEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInt(value)) this.state.setAdaptiveMaxKeyframeInterval(Integer.parseInt(value));
        });

        this.adaptiveContinuationMinSavingsBytesEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.adaptive_continuation_min_savings_bytes"));
        this.adaptiveContinuationMinSavingsBytesEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setAdaptiveContinuationMinSavingsBytes(Long.parseLong(value));
        });

        this.adaptiveContinuationMinSavingsRatioEditBox = this.addStyledDecimalEditBox(Component.translatable("konkrete.afma.creator.adaptive_continuation_min_savings_ratio"));
        this.adaptiveContinuationMinSavingsRatioEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            double parsedValue = parseDoubleOrDefault(value, Double.NaN);
            if (Double.isFinite(parsedValue)) this.state.setAdaptiveContinuationMinSavingsRatio(parsedValue);
        });

        this.perceptualVisibleColorDeltaEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.perceptual_visible_color_delta"));
        this.perceptualVisibleColorDeltaEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInt(value)) this.state.setPerceptualBinIntraMaxVisibleColorDelta(Integer.parseInt(value));
        });

        this.perceptualAlphaDeltaEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.perceptual_alpha_delta"));
        this.perceptualAlphaDeltaEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInt(value)) this.state.setPerceptualBinIntraMaxAlphaDelta(Integer.parseInt(value));
        });

        this.perceptualAverageErrorEditBox = this.addStyledDecimalEditBox(Component.translatable("konkrete.afma.creator.perceptual_average_error"));
        this.perceptualAverageErrorEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            double parsedValue = parseDoubleOrDefault(value, Double.NaN);
            if (Double.isFinite(parsedValue)) this.state.setPerceptualBinIntraMaxAverageError(parsedValue);
        });

        this.maxCopySearchDistanceEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.max_copy_search_distance"));
        this.maxCopySearchDistanceEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInt(value)) this.state.setMaxCopySearchDistance(Integer.parseInt(value));
        });

        this.maxCandidateAxisOffsetsEditBox = this.addStyledNumberEditBox(Component.translatable("konkrete.afma.creator.max_candidate_axis_offsets"));
        this.maxCandidateAxisOffsetsEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInt(value)) this.state.setMaxCandidateAxisOffsets(Integer.parseInt(value));
        });

        this.browseMainFramesButton = this.addStyledButton(Component.translatable("konkrete.afma.creator.browse"), button -> this.openDirectoryChooser(this.state.getMainFramesDirectory(), this.state::setMainFramesDirectory));
        this.browseIntroFramesButton = this.addStyledButton(Component.translatable("konkrete.afma.creator.browse"), button -> this.openDirectoryChooser(this.state.getIntroFramesDirectory(), this.state::setIntroFramesDirectory));
        this.clearIntroFramesButton = this.addStyledButton(Component.translatable("konkrete.afma.creator.clear"), button -> {
            this.state.clearIntroFramesDirectory();
            this.syncWidgetsFromState();
        });
        this.browseOutputButton = this.addStyledButton(Component.translatable("konkrete.afma.creator.browse"), button -> this.openOutputChooser(this.state.getOutputFile()));

        LocalizedEnumValueCycle<AfmaOptimizationPreset> presetCycle = LocalizedEnumValueCycle.ofArray(
                "konkrete.afma.creator.optimization_preset.label",
                AfmaOptimizationPreset.values()
        );
        presetCycle.setCurrentValue(this.state.getOptimizationPreset(), false);
        this.presetCycleButton = new CycleButton<>(0, 0, 200, FIELD_HEIGHT,
                presetCycle,
                (value, button) -> {
                    this.state.applyPreset(value);
                    this.syncWidgetsFromState();
                });
        UIBase.applyDefaultWidgetSkinTo(this.presetCycleButton, UIBase.shouldBlur());
        this.addRenderableWidget(this.presetCycleButton);

        this.rectCopyCycleButton = this.addToggleButton("konkrete.afma.creator.rect_copy", this.state.isRectCopyEnabled(), value -> this.state.setRectCopyEnabled(value));
        this.duplicateCycleButton = this.addToggleButton("konkrete.afma.creator.duplicate_elision", this.state.isDuplicateFrameElision(), value -> this.state.setDuplicateFrameElision(value));
        this.nearLosslessCycleButton = this.addToggleButton("konkrete.afma.creator.near_lossless", this.state.isNearLosslessEnabled(), value -> this.state.setNearLosslessEnabled(value));
        this.strictPostWriteValidationCycleButton = this.addToggleButton("konkrete.afma.creator.strict_post_write_validation", this.state.isStrictPostWriteValidation(), value -> this.state.setStrictPostWriteValidation(value));
        this.adaptiveKeyframeCycleButton = this.addToggleButton("konkrete.afma.creator.adaptive_keyframes", this.state.isAdaptiveKeyframePlacement(), value -> this.state.setAdaptiveKeyframePlacement(value));

        this.exportButton = this.addStyledButton(Component.translatable("konkrete.afma.creator.export"), button -> this.startExport());
        this.cancelJobButton = this.addStyledButton(Component.translatable("konkrete.common_components.cancel"), button -> this.state.cancelCurrentJob());
        this.closeButton = this.addStyledButton(Component.translatable("konkrete.common.close"), button -> this.onClose());

        this.rebuildTrackedWidgetLists();
        this.configureTooltips();
        this.syncWidgetsFromState();
        this.repositionWidgets();
    }

    /** {@inheritDoc} */
    @Override
    public void tick() {
        super.tick();

        this.handleCompletedJobIfNeeded();
        this.updateButtonStates();
        this.repositionWidgets();
    }

    /** Reports a newly terminal encode job once and remembers it until another job starts. */
    protected void handleCompletedJobIfNeeded() {
        AfmaEncodeJob job = this.state.getCurrentJob();
        if ((job != null) && !job.isRunning() && (job != this.handledTerminalJob)) {
            this.handledTerminalJob = job;
            if ((job.getStatus() == AfmaEncodeJob.Status.FAILED) && (job.getFailure() != null)) {
                Dialogs.openMessage(Component.literal(job.getFailure().getMessage() != null ? job.getFailure().getMessage() : "AFMA creator job failed."), MessageDialogStyle.ERROR);
            } else if ((job.getStatus() == AfmaEncodeJob.Status.SUCCEEDED) && (job.getOutputFile() != null)) {
                Dialogs.openMessage(Component.translatable("konkrete.afma.creator.export.success", fileToPath(job.getOutputFile())), MessageDialogStyle.INFO);
            }
        }
    }

    /** Synchronizes widget values, validates the state, and starts a background AFMA export. */
    protected void startExport() {
        try {
            this.syncStateFromWidgets();
            this.state.startExport();
            this.scrollScrollableContentToBottom();
        } catch (Exception ex) {
            Dialogs.openMessage(Component.literal(ex.getMessage() != null ? ex.getMessage() : "AFMA export failed to start."), MessageDialogStyle.ERROR);
        }
    }

    /** Enables editing, export, cancellation, and close controls for the current job state. */
    protected void updateButtonStates() {
        boolean jobRunning = this.state.isJobRunning();
        if (this.exportButton != null) this.exportButton.active = !jobRunning;
        if (this.cancelJobButton != null) {
            this.cancelJobButton.active = jobRunning;
            this.cancelJobButton.visible = jobRunning;
        }
        if (this.clearIntroFramesButton != null) this.clearIntroFramesButton.active = !this.state.getIntroFramesInputText().isBlank() && !jobRunning;

        boolean adaptiveKeyframesEnabled = this.state.isAdaptiveKeyframePlacement() && !jobRunning;
        this.setWidgetActive(this.adaptiveMaxKeyframeIntervalEditBox, adaptiveKeyframesEnabled);
        this.setWidgetActive(this.adaptiveContinuationMinSavingsBytesEditBox, adaptiveKeyframesEnabled);
        this.setWidgetActive(this.adaptiveContinuationMinSavingsRatioEditBox, adaptiveKeyframesEnabled);

        boolean perceptualControlsEnabled = !jobRunning;
        this.setWidgetActive(this.perceptualVisibleColorDeltaEditBox, perceptualControlsEnabled);
        this.setWidgetActive(this.perceptualAlphaDeltaEditBox, perceptualControlsEnabled);
        this.setWidgetActive(this.perceptualAverageErrorEditBox, perceptualControlsEnabled);

        boolean generalAdvancedControlsEnabled = !jobRunning;
        this.setWidgetActive(this.mainFramesPathEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.introFramesPathEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.outputPathEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.frameTimeEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.introFrameTimeEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.loopCountEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.keyframeIntervalEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.maxCopySearchDistanceEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.maxCandidateAxisOffsetsEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.presetCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.rectCopyCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.duplicateCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.nearLosslessCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.strictPostWriteValidationCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.adaptiveKeyframeCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.browseMainFramesButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.browseIntroFramesButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.browseOutputButton, generalAdvancedControlsEnabled);
    }

    /** Opens a child directory chooser and forwards the accepted directory to the caller callback. */
    protected void openDirectoryChooser(@Nullable File initialDirectory, @NotNull java.util.function.Consumer<File> callback) {
        File startDirectory = (initialDirectory != null && initialDirectory.isDirectory()) ? initialDirectory : GameDirectoryUtils.getGameDirectory();
        ChooseDirectoryWindowBody chooser = new ChooseDirectoryWindowBody(GameDirectoryUtils.getGameDirectory(), startDirectory, directory -> {
            if (directory != null) {
                callback.accept(directory);
                this.syncWidgetsFromState();
            }
        });
        this.childWindows.add(chooser.openInWindow(null));
    }

    /** Opens a child save-file chooser initialized from the current AFMA output path. */
    protected void openOutputChooser(@Nullable File initialOutput) {
        File gameDir = GameDirectoryUtils.getGameDirectory();
        File startDir = (initialOutput != null && initialOutput.getParentFile() != null && initialOutput.getParentFile().isDirectory()) ? initialOutput.getParentFile() : gameDir;
        String fileName = (initialOutput != null) ? initialOutput.getName() : "animation.afma";
        SaveFileWindowBody chooser = new SaveFileWindowBody(gameDir, startDir, fileName, "afma", file -> {
            if (file != null) {
                this.state.setOutputFile(file);
                this.syncWidgetsFromState();
            }
        });
        chooser.setForceResourceFriendlyFileNames(false);
        this.childWindows.add(chooser.openInWindow(null));
    }

    /** Copies creator-state values into widgets without firing their responders. */
    protected void syncWidgetsFromState() {
        this.syncingWidgets = true;
        try {
            if (this.mainFramesPathEditBox != null) this.mainFramesPathEditBox.setValue(this.state.getMainFramesInputText());
            if (this.introFramesPathEditBox != null) this.introFramesPathEditBox.setValue(this.state.getIntroFramesInputText());
            if (this.outputPathEditBox != null) this.outputPathEditBox.setValue(fileToPath(this.state.getOutputFile()));
            if (this.frameTimeEditBox != null) this.frameTimeEditBox.setValue(String.valueOf(this.state.getFrameTimeMs()));
            if (this.introFrameTimeEditBox != null) this.introFrameTimeEditBox.setValue(String.valueOf(this.state.getIntroFrameTimeMs()));
            if (this.loopCountEditBox != null) this.loopCountEditBox.setValue(String.valueOf(this.state.getLoopCount()));
            if (this.keyframeIntervalEditBox != null) this.keyframeIntervalEditBox.setValue(String.valueOf(this.state.getKeyframeInterval()));
            if (this.adaptiveMaxKeyframeIntervalEditBox != null) this.adaptiveMaxKeyframeIntervalEditBox.setValue(String.valueOf(this.state.getAdaptiveMaxKeyframeInterval()));
            if (this.adaptiveContinuationMinSavingsBytesEditBox != null) this.adaptiveContinuationMinSavingsBytesEditBox.setValue(String.valueOf(this.state.getAdaptiveContinuationMinSavingsBytes()));
            if (this.adaptiveContinuationMinSavingsRatioEditBox != null) this.adaptiveContinuationMinSavingsRatioEditBox.setValue(Double.toString(this.state.getAdaptiveContinuationMinSavingsRatio()));
            if (this.perceptualVisibleColorDeltaEditBox != null) this.perceptualVisibleColorDeltaEditBox.setValue(String.valueOf(this.state.getPerceptualBinIntraMaxVisibleColorDelta()));
            if (this.perceptualAlphaDeltaEditBox != null) this.perceptualAlphaDeltaEditBox.setValue(String.valueOf(this.state.getPerceptualBinIntraMaxAlphaDelta()));
            if (this.perceptualAverageErrorEditBox != null) this.perceptualAverageErrorEditBox.setValue(Double.toString(this.state.getPerceptualBinIntraMaxAverageError()));
            if (this.maxCopySearchDistanceEditBox != null) this.maxCopySearchDistanceEditBox.setValue(String.valueOf(this.state.getMaxCopySearchDistance()));
            if (this.maxCandidateAxisOffsetsEditBox != null) this.maxCandidateAxisOffsetsEditBox.setValue(String.valueOf(this.state.getMaxCandidateAxisOffsets()));
            if (this.presetCycleButton != null) this.presetCycleButton.setSelectedValue(this.state.getOptimizationPreset(), false);
            if (this.rectCopyCycleButton != null) this.rectCopyCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isRectCopyEnabled()), false);
            if (this.duplicateCycleButton != null) this.duplicateCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isDuplicateFrameElision()), false);
            if (this.nearLosslessCycleButton != null) this.nearLosslessCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isNearLosslessEnabled()), false);
            if (this.strictPostWriteValidationCycleButton != null) this.strictPostWriteValidationCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isStrictPostWriteValidation()), false);
            if (this.adaptiveKeyframeCycleButton != null) this.adaptiveKeyframeCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isAdaptiveKeyframePlacement()), false);
        } finally {
            this.syncingWidgets = false;
        }
    }

    /** Parses valid widget values into the creator state before validation or export. */
    protected void syncStateFromWidgets() {
        if (this.syncingWidgets) return;
        if (this.mainFramesPathEditBox != null && !this.state.getMainFramesInputText().equals(this.mainFramesPathEditBox.getValue())) {
            this.state.setMainFramesDirectory(pathToDirectory(this.mainFramesPathEditBox.getValue()));
        }
        if (this.introFramesPathEditBox != null && !this.state.getIntroFramesInputText().equals(this.introFramesPathEditBox.getValue())) {
            this.state.setIntroFramesDirectory(pathToDirectory(this.introFramesPathEditBox.getValue()));
        }
        if (this.outputPathEditBox != null && !fileToPath(this.state.getOutputFile()).equals(this.outputPathEditBox.getValue())) {
            this.state.setOutputFile(pathToFile(this.outputPathEditBox.getValue()));
        }
        if (this.frameTimeEditBox != null) {
            long value = parseLongOrDefault(this.frameTimeEditBox.getValue(), 0L);
            if (value != this.state.getFrameTimeMs()) this.state.setFrameTimeMs(value);
        }
        if (this.introFrameTimeEditBox != null) {
            long value = parseLongOrDefault(this.introFrameTimeEditBox.getValue(), 0L);
            if (value != this.state.getIntroFrameTimeMs()) this.state.setIntroFrameTimeMs(value);
        }
        if (this.loopCountEditBox != null) {
            int value = parseIntOrDefault(this.loopCountEditBox.getValue(), 0);
            if (value != this.state.getLoopCount()) this.state.setLoopCount(value);
        }
        if (this.keyframeIntervalEditBox != null) {
            int value = parseIntOrDefault(this.keyframeIntervalEditBox.getValue(), 0);
            if (value != this.state.getKeyframeInterval()) this.state.setKeyframeInterval(value);
        }
        if (this.adaptiveMaxKeyframeIntervalEditBox != null) {
            int value = parseIntOrDefault(this.adaptiveMaxKeyframeIntervalEditBox.getValue(), 0);
            if (value != this.state.getAdaptiveMaxKeyframeInterval()) this.state.setAdaptiveMaxKeyframeInterval(value);
        }
        if (this.adaptiveContinuationMinSavingsBytesEditBox != null) {
            long value = parseLongOrDefault(this.adaptiveContinuationMinSavingsBytesEditBox.getValue(), 0L);
            if (value != this.state.getAdaptiveContinuationMinSavingsBytes()) this.state.setAdaptiveContinuationMinSavingsBytes(value);
        }
        if (this.adaptiveContinuationMinSavingsRatioEditBox != null) {
            double value = parseDoubleOrDefault(this.adaptiveContinuationMinSavingsRatioEditBox.getValue(), Double.NaN);
            if (Double.isFinite(value) && (Double.compare(value, this.state.getAdaptiveContinuationMinSavingsRatio()) != 0)) {
                this.state.setAdaptiveContinuationMinSavingsRatio(value);
            }
        }
        if (this.perceptualVisibleColorDeltaEditBox != null) {
            int value = parseIntOrDefault(this.perceptualVisibleColorDeltaEditBox.getValue(), 0);
            if (value != this.state.getPerceptualBinIntraMaxVisibleColorDelta()) this.state.setPerceptualBinIntraMaxVisibleColorDelta(value);
        }
        if (this.perceptualAlphaDeltaEditBox != null) {
            int value = parseIntOrDefault(this.perceptualAlphaDeltaEditBox.getValue(), 0);
            if (value != this.state.getPerceptualBinIntraMaxAlphaDelta()) this.state.setPerceptualBinIntraMaxAlphaDelta(value);
        }
        if (this.perceptualAverageErrorEditBox != null) {
            double value = parseDoubleOrDefault(this.perceptualAverageErrorEditBox.getValue(), Double.NaN);
            if (Double.isFinite(value) && (Double.compare(value, this.state.getPerceptualBinIntraMaxAverageError()) != 0)) {
                this.state.setPerceptualBinIntraMaxAverageError(value);
            }
        }
        if (this.maxCopySearchDistanceEditBox != null) {
            int value = parseIntOrDefault(this.maxCopySearchDistanceEditBox.getValue(), 0);
            if (value != this.state.getMaxCopySearchDistance()) this.state.setMaxCopySearchDistance(value);
        }
        if (this.maxCandidateAxisOffsetsEditBox != null) {
            int value = parseIntOrDefault(this.maxCandidateAxisOffsetsEditBox.getValue(), 0);
            if (value != this.state.getMaxCandidateAxisOffsets()) this.state.setMaxCandidateAxisOffsets(value);
        }
    }

    /** Recomputes fixed and scrollable widget bounds for the current screen dimensions. */
    protected void repositionWidgets() {
        int fullContentX = this.getContentLeft();
        int fullContentWidth = this.getContentWidth();
        int viewportHeight = this.getScrollableViewportHeight();

        ContentLayout contentLayout = this.applyScrollableLayout(fullContentX, fullContentWidth, 0);
        boolean showScrollBar = contentLayout.totalHeight() > viewportHeight;
        if (showScrollBar) {
            this.scrollableContentWidth = Math.max(0, fullContentWidth - this.getContentScrollBarReservedWidth());
            ContentLayout measuredLayout = this.applyScrollableLayout(fullContentX, this.scrollableContentWidth, 0);
            this.scrollableContentHeight = measuredLayout.totalHeight();
            this.updateContentScrollBar(fullContentX, this.scrollableContentWidth, true);
            contentLayout = this.applyScrollableLayout(fullContentX, this.scrollableContentWidth, this.getScrollableContentScrollOffset());
        } else {
            this.scrollableContentWidth = fullContentWidth;
            this.scrollableContentHeight = contentLayout.totalHeight();
            this.contentScrollBar.setScroll(0.0F);
            this.updateContentScrollBar(fullContentX, this.scrollableContentWidth, false);
        }

        this.scrollableContentHeight = contentLayout.totalHeight();
        this.diagnosticsY = contentLayout.diagnosticsY();
        this.layoutFixedButtons(fullContentX, fullContentWidth);
    }

    /** Positions a labeled path field and its primary and optional secondary action buttons. */
    protected int layoutLabeledPathRow(int x, int y, int rowWidth, int labelWidth, @Nullable AbstractWidget field, @Nullable AbstractWidget primaryButton, int primaryButtonWidth, @Nullable AbstractWidget secondaryButton, int secondaryButtonWidth) {
        int buttonWidth = 0;
        if (primaryButton != null) buttonWidth += primaryButtonWidth + FIELD_BUTTON_GAP;
        if (secondaryButton != null) buttonWidth += secondaryButtonWidth + ((primaryButton != null) ? BUTTON_GROUP_GAP : 0);

        int fieldX = x + labelWidth + INLINE_LABEL_GAP;
        int fieldWidth = Math.max(MIN_INPUT_WIDTH, rowWidth - labelWidth - INLINE_LABEL_GAP - buttonWidth);
        this.layoutWidget(field, fieldX, y, fieldWidth, FIELD_HEIGHT);

        int buttonX = fieldX + fieldWidth + FIELD_BUTTON_GAP;
        if (primaryButton != null) {
            this.layoutWidget(primaryButton, buttonX, y, primaryButtonWidth, FIELD_HEIGHT);
            buttonX += primaryButtonWidth + BUTTON_GROUP_GAP;
        }
        if (secondaryButton != null) {
            this.layoutWidget(secondaryButton, buttonX, y, secondaryButtonWidth, FIELD_HEIGHT);
        }
        return y;
    }

    /** Positions one labeled field row and returns the next row position. */
    protected int layoutLabeledFieldRow(int x, int y, int rowWidth, int labelWidth, @Nullable AbstractWidget field) {
        int fieldX = x + labelWidth + INLINE_LABEL_GAP;
        int fieldWidth = Math.max(MIN_INPUT_WIDTH, rowWidth - labelWidth - INLINE_LABEL_GAP);
        this.layoutWidget(field, fieldX, y, fieldWidth, FIELD_HEIGHT);
        return y;
    }

    /** Applies bounds and visibility to a widget when it is present. */
    protected void layoutWidget(@Nullable AbstractWidget widget, int x, int y, int width, int height) {
        if (widget == null) return;
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(width);
        widget.setHeight(height);
    }

    /** Updates widget activation when the optional widget is present. */
    protected void setWidgetActive(@Nullable AbstractWidget widget, boolean active) {
        if (widget != null) {
            widget.active = active;
        }
    }

    /** Creates, skins, registers, and tracks a general text field. */
    protected @NotNull ExtendedEditBox addStyledEditBox(@NotNull Component narrationMessage) {
        ExtendedEditBox editBox = new ExtendedEditBox(this.font, 0, 0, 100, FIELD_HEIGHT, narrationMessage);
        editBox.setMaxLength(100000);
        UIBase.applyDefaultWidgetSkinTo(editBox, UIBase.shouldBlur());
        return this.addRenderableWidget(editBox);
    }

    /** Creates a styled text field restricted to signed integer input. */
    protected @NotNull ExtendedEditBox addStyledNumberEditBox(@NotNull Component narrationMessage) {
        ExtendedEditBox editBox = this.addStyledEditBox(narrationMessage);
        editBox.setCharacterFilter(CharacterFilter.buildIntegerFilter());
        return editBox;
    }

    /** Creates a styled text field restricted to finite decimal input. */
    protected @NotNull ExtendedEditBox addStyledDecimalEditBox(@NotNull Component narrationMessage) {
        ExtendedEditBox editBox = this.addStyledEditBox(narrationMessage);
        editBox.setCharacterFilter(CharacterFilter.buildDecimalFiler());
        return editBox;
    }

    /** Creates, skins, registers, and tracks an action button. */
    protected @NotNull ExtendedButton addStyledButton(@NotNull Component label, @NotNull Button.OnPress onPress) {
        ExtendedButton button = new ExtendedButton(0, 0, 100, FIELD_HEIGHT, label, onPress);
        UIBase.applyDefaultWidgetSkinTo(button, UIBase.shouldBlur());
        return this.addRenderableWidget(button);
    }

    /** Creates a localized enabled/disabled cycle button backed by a boolean setter. */
    protected @NotNull CycleButton<CommonCycles.CycleEnabledDisabled> addToggleButton(@NotNull String key, boolean value, @NotNull java.util.function.Consumer<Boolean> setter) {
        CycleButton<CommonCycles.CycleEnabledDisabled> button = new CycleButton<>(0, 0, 120, FIELD_HEIGHT, CommonCycles.cycleEnabledDisabled(key, value), (cycleValue, cycleButton) -> setter.accept(cycleValue.getAsBoolean()));
        UIBase.applyDefaultWidgetSkinTo(button, UIBase.shouldBlur());
        this.addRenderableWidget(button);
        return button;
    }

    /** Attaches localized descriptions to every creator control. */
    protected void configureTooltips() {
        this.setEditBoxTooltip(this.mainFramesPathEditBox, "konkrete.afma.creator.main_frames.desc");
        this.setEditBoxTooltip(this.introFramesPathEditBox, "konkrete.afma.creator.intro_frames.desc");
        this.setEditBoxTooltip(this.outputPathEditBox, "konkrete.afma.creator.output_file.desc");
        this.setEditBoxTooltip(this.frameTimeEditBox, "konkrete.afma.creator.frame_time.desc");
        this.setEditBoxTooltip(this.introFrameTimeEditBox, "konkrete.afma.creator.intro_frame_time.desc");
        this.setEditBoxTooltip(this.loopCountEditBox, "konkrete.afma.creator.loop_count.desc");
        this.setEditBoxTooltip(this.keyframeIntervalEditBox, "konkrete.afma.creator.keyframe_interval.desc");
        this.setEditBoxTooltip(this.adaptiveMaxKeyframeIntervalEditBox, "konkrete.afma.creator.adaptive_max_keyframe_interval.desc");
        this.setEditBoxTooltip(this.adaptiveContinuationMinSavingsBytesEditBox, "konkrete.afma.creator.adaptive_continuation_min_savings_bytes.desc");
        this.setEditBoxTooltip(this.adaptiveContinuationMinSavingsRatioEditBox, "konkrete.afma.creator.adaptive_continuation_min_savings_ratio.desc");
        this.setEditBoxTooltip(this.maxCopySearchDistanceEditBox, "konkrete.afma.creator.max_copy_search_distance.desc");
        this.setEditBoxTooltip(this.maxCandidateAxisOffsetsEditBox, "konkrete.afma.creator.max_candidate_axis_offsets.desc");
        this.setEditBoxTooltip(this.perceptualVisibleColorDeltaEditBox, "konkrete.afma.creator.perceptual_visible_color_delta.desc");
        this.setEditBoxTooltip(this.perceptualAlphaDeltaEditBox, "konkrete.afma.creator.perceptual_alpha_delta.desc");
        this.setEditBoxTooltip(this.perceptualAverageErrorEditBox, "konkrete.afma.creator.perceptual_average_error.desc");

        this.setButtonTooltip(this.browseMainFramesButton, "konkrete.afma.creator.browse_main_frames.desc");
        this.setButtonTooltip(this.browseIntroFramesButton, "konkrete.afma.creator.browse_intro_frames.desc");
        this.setButtonTooltip(this.clearIntroFramesButton, "konkrete.afma.creator.clear_intro_frames.desc");
        this.setButtonTooltip(this.browseOutputButton, "konkrete.afma.creator.browse_output.desc");
        this.setButtonTooltip(this.exportButton, "konkrete.afma.creator.export.desc");
        this.setButtonTooltip(this.cancelJobButton, "konkrete.afma.creator.cancel.desc");
        this.setButtonTooltip(this.closeButton, "konkrete.afma.creator.close.desc");

        this.setButtonTooltip(this.presetCycleButton, "konkrete.afma.creator.optimization_preset.desc");
        this.setButtonTooltip(this.rectCopyCycleButton, "konkrete.afma.creator.rect_copy.desc");
        this.setButtonTooltip(this.duplicateCycleButton, "konkrete.afma.creator.duplicate_elision.desc");
        this.setButtonTooltip(this.nearLosslessCycleButton, "konkrete.afma.creator.near_lossless.desc");
        this.setButtonTooltip(this.strictPostWriteValidationCycleButton, "konkrete.afma.creator.strict_post_write_validation.desc");
        this.setButtonTooltip(this.adaptiveKeyframeCycleButton, "konkrete.afma.creator.adaptive_keyframes.desc");
    }

    /** Attaches a lazy localized tooltip to an optional edit box. */
    protected void setEditBoxTooltip(@Nullable ExtendedEditBox editBox, @NotNull String localizationKey) {
        if (editBox != null) {
            editBox.setUITooltip(this.createTooltipSupplier(localizationKey));
        }
    }

    /** Attaches a lazy localized tooltip to an optional button. */
    protected void setButtonTooltip(@Nullable ExtendedButton button, @NotNull String localizationKey) {
        if (button != null) {
            button.setUITooltip(this.createTooltip(localizationKey));
        }
    }

    /** Returns a lazy supplier that resolves localized tooltip lines when displayed. */
    protected @NotNull Supplier<UITooltip> createTooltipSupplier(@NotNull String localizationKey) {
        UITooltip tooltip = this.createTooltip(localizationKey);
        return () -> tooltip;
    }

    /** Builds a tooltip from the current localized lines for the supplied key. */
    protected @NotNull UITooltip createTooltip(@NotNull String localizationKey) {
        return UITooltip.of(LocalizationUtils.splitLocalizedLines(localizationKey));
    }

    /** {@inheritDoc} */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.repositionWidgets();
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.renderScrollableContent(graphics, mouseX, mouseY, partialTick);
        if (this.isContentScrollBarVisible()) {
            this.contentScrollBar.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        for (AbstractWidget widget : this.fixedWidgets) {
            widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());
        RenderingUtils.resetShaderColor(graphics);
        this.renderCreatorPanels(graphics, mouseX, mouseY, partialTick);
    }

    /** Extracts panel, title, scrollable-form, diagnostics, and child-window render state. */
    protected void renderCreatorPanels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        int panelColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                graphics,
                this.getPanelLeft(),
                this.getPanelTop(),
                this.getPanelRight() - this.getPanelLeft(),
                this.getPanelBottom() - this.getPanelTop(),
                radius,
                radius,
                radius,
                radius,
                panelColor,
                partialTick
        );

        UIBase.renderText(graphics, this.title, OUTER_PADDING, 16, this.getThemeLabelColor(false), UIBase.getUITextSizeNormal());
    }

    /** Extracts section headers and inline labels for visible form controls. */
    protected void renderFieldLabels(@NotNull GuiGraphicsExtractor graphics) {
        int inlineLabelWidth = this.getInlineLabelWidth();

        this.drawInlineLabel(graphics, this.mainFramesPathEditBox, Component.translatable("konkrete.afma.creator.main_frames"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.introFramesPathEditBox, Component.translatable("konkrete.afma.creator.intro_frames"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.outputPathEditBox, Component.translatable("konkrete.afma.creator.output_file"), inlineLabelWidth);

        if (this.frameTimeEditBox != null) {
            this.drawFieldLabel(graphics, Component.translatable("konkrete.afma.creator.section.playback"), this.getScrollableContentInnerLeft(), this.frameTimeEditBox.getY() - 18, true);
        }
        this.drawInlineLabel(graphics, this.frameTimeEditBox, Component.translatable("konkrete.afma.creator.frame_time"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.introFrameTimeEditBox, Component.translatable("konkrete.afma.creator.intro_frame_time"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.loopCountEditBox, Component.translatable("konkrete.afma.creator.loop_count"), inlineLabelWidth);

        if (this.presetCycleButton != null) {
            this.drawFieldLabel(graphics, Component.translatable("konkrete.afma.creator.section.optimization"), this.getScrollableContentInnerLeft(), this.presetCycleButton.getY() - 18, true);
        }
        this.drawInlineLabel(graphics, this.keyframeIntervalEditBox, Component.translatable("konkrete.afma.creator.keyframe_interval"), inlineLabelWidth);
        if (this.adaptiveKeyframeCycleButton != null) {
            this.drawFieldLabel(graphics, Component.translatable("konkrete.afma.creator.section.advanced"), this.getScrollableContentInnerLeft(), this.adaptiveKeyframeCycleButton.getY() - 18, true);
        }
        this.drawInlineLabel(graphics, this.adaptiveMaxKeyframeIntervalEditBox, Component.translatable("konkrete.afma.creator.adaptive_max_keyframe_interval"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.adaptiveContinuationMinSavingsBytesEditBox, Component.translatable("konkrete.afma.creator.adaptive_continuation_min_savings_bytes"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.adaptiveContinuationMinSavingsRatioEditBox, Component.translatable("konkrete.afma.creator.adaptive_continuation_min_savings_ratio"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.maxCopySearchDistanceEditBox, Component.translatable("konkrete.afma.creator.max_copy_search_distance"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.maxCandidateAxisOffsetsEditBox, Component.translatable("konkrete.afma.creator.max_candidate_axis_offsets"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.perceptualVisibleColorDeltaEditBox, Component.translatable("konkrete.afma.creator.perceptual_visible_color_delta"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.perceptualAlphaDeltaEditBox, Component.translatable("konkrete.afma.creator.perceptual_alpha_delta"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.perceptualAverageErrorEditBox, Component.translatable("konkrete.afma.creator.perceptual_average_error"), inlineLabelWidth);
    }

    /** Draws one header or ordinary field label with the active theme color. */
    protected void drawFieldLabel(@NotNull GuiGraphicsExtractor graphics, @NotNull Component component, int x, int y, boolean header) {
        UIBase.renderText(graphics, component, x, y, this.getThemeLabelColor(!header), UIBase.getUITextSizeNormal());
    }

    /** Draws a label aligned immediately before a present field widget. */
    protected void drawInlineLabel(@NotNull GuiGraphicsExtractor graphics, @Nullable AbstractWidget widget, @NotNull Component component, int labelWidth) {
        if (widget == null) return;
        float textWidth = UIBase.getUITextWidthNormal(component);
        float labelX = widget.getX() - INLINE_LABEL_GAP - labelWidth + Math.max(0.0F, labelWidth - textWidth);
        float labelY = widget.getY() + ((FIELD_HEIGHT - UIBase.getUITextHeightNormal()) / 2.0F);
        UIBase.renderText(graphics, component, labelX, labelY, this.getThemeLabelColor(true), UIBase.getUITextSizeNormal());
    }

    /** Draws validation errors and current encode progress below the form. */
    protected void renderDiagnostics(@NotNull GuiGraphicsExtractor graphics) {
        AfmaEncodeJob job = this.state.getCurrentJob();
        if (job == null) return;

        int contentX = this.getScrollableContentInnerLeft();
        int contentWidth = this.getScrollableContentInnerWidth();
        int textY = this.diagnosticsY;
        AfmaEncodeProgress progress = job.getProgress();
        textY = this.renderWrappedUiText(graphics, Component.translatable("konkrete.afma.creator.job_status", progress.task()), contentX, textY, contentWidth, this.getThemeLabelColor(false));
        if (progress.detail() != null && !progress.detail().isBlank()) {
            textY = this.renderWrappedUiText(graphics, Component.literal(progress.detail()), contentX, textY, contentWidth, this.getThemeLabelColor(true));
        }
        graphics.fill(contentX, textY, contentX + contentWidth, textY + 8, 0xFF202020);
        graphics.fill(contentX, textY, contentX + Math.round(contentWidth * (float) progress.progress()), textY + 8, UIBase.getUITheme().success_color.getColorInt());
    }

    /** Draws wrapped UI text and returns the first vertical position after it. */
    protected int renderWrappedUiText(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, int x, int y, int maxWidth, int color) {
        List<MutableComponent> lines = UIBase.lineWrapUIComponentsNormal(text, Math.max(20, maxWidth));
        int lineHeight = Math.max(10, Math.round(UIBase.getUITextHeightNormal()));
        for (MutableComponent line : lines) {
            UIBase.renderText(graphics, line, x, y, color, UIBase.getUITextSizeNormal());
            y += lineHeight + 2;
        }
        return y;
    }

    /** Returns the left edge of the creator panel. */
    protected int getPanelLeft() {
        return OUTER_PADDING;
    }

    /** Returns the top edge of the creator panel. */
    protected int getPanelTop() {
        return PANEL_TOP;
    }

    /** Returns the right edge of the creator panel. */
    protected int getPanelRight() {
        return this.width - OUTER_PADDING;
    }

    /** Returns the bottom edge of the creator panel. */
    protected int getPanelBottom() {
        return this.getBottomButtonY() - PANEL_PADDING;
    }

    /** Returns the vertical position of the fixed bottom action row. */
    protected int getBottomButtonY() {
        return this.height - OUTER_PADDING - FIELD_HEIGHT;
    }

    /** Returns the left edge of content inside the panel padding. */
    protected int getContentLeft() {
        return this.getPanelLeft() + PANEL_PADDING;
    }

    /** Returns the usable content width inside the panel padding. */
    protected int getContentWidth() {
        return Math.max(0, (this.getPanelRight() - this.getPanelLeft()) - (PANEL_PADDING * 2));
    }

    /** Returns the top edge of the scrollable form viewport. */
    protected int getContentStartY() {
        return this.getPanelTop() + PANEL_PADDING;
    }

    /** Returns the visible height available to scrollable form content. */
    protected int getScrollableViewportHeight() {
        return Math.max(0, this.getPanelBottom() - this.getContentStartY());
    }

    /** Returns the left edge of form content after scroll padding. */
    protected int getScrollableContentInnerLeft() {
        return this.getContentLeft() + SCROLL_CONTENT_HORIZONTAL_PADDING;
    }

    /** Returns form width after scroll padding and visible-scrollbar reservation. */
    protected int getScrollableContentInnerWidth() {
        return Math.max(0, this.scrollableContentWidth - (SCROLL_CONTENT_HORIZONTAL_PADDING * 2));
    }

    /** Returns a bounded width that accommodates the widest localized inline label. */
    protected int getInlineLabelWidth() {
        float widestLabel = 0.0F;
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.main_frames")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.intro_frames")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.output_file")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.frame_time")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.intro_frame_time")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.loop_count")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.keyframe_interval")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.adaptive_max_keyframe_interval")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.adaptive_continuation_min_savings_bytes")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.adaptive_continuation_min_savings_ratio")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.max_copy_search_distance")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.max_candidate_axis_offsets")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.perceptual_visible_color_delta")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.perceptual_alpha_delta")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("konkrete.afma.creator.perceptual_average_error")));
        return Math.min(190, Math.max(118, Math.round(widestLabel) + 8));
    }

    /** Returns whether numeric controls need a single-column layout at the available width. */
    protected boolean useSingleColumnNumericLayout(int columnWidth, int labelWidth) {
        return (columnWidth - labelWidth - INLINE_LABEL_GAP) < 150;
    }

    /** Returns the active or inactive label color from the current UI theme. */
    protected int getThemeLabelColor(boolean inactive) {
        UITheme theme = UIBase.getUITheme();
        if (UIBase.shouldBlur()) {
            return inactive
                    ? theme.ui_blur_interface_widget_label_color_inactive.getColorInt()
                    : theme.ui_blur_interface_widget_label_color_normal.getColorInt();
        }
        return inactive
                ? theme.ui_interface_widget_label_color_inactive.getColorInt()
                : theme.ui_interface_widget_label_color_normal.getColorInt();
    }

    /** {@inheritDoc} */
    @Override
    public void onFilesDrop(@NotNull List<Path> paths) {
        if (paths.isEmpty()) return;

        List<File> droppedPngFiles = new ArrayList<>();
        for (Path path : paths) {
            File file = path.toFile();
            if (file.isDirectory()) {
                if (this.state.getMainFramesDirectory() == null) {
                    this.state.setMainFramesDirectory(file);
                } else if (this.state.getIntroFramesDirectory() == null) {
                    this.state.setIntroFramesDirectory(file);
                }
            } else if (file.isFile()) {
                String lowerName = file.getName().toLowerCase(Locale.ROOT);
                if (lowerName.endsWith(".afma")) {
                    this.state.setOutputFile(file);
                } else if (lowerName.endsWith(".png")) {
                    droppedPngFiles.add(file);
                }
            }
        }

        if (!droppedPngFiles.isEmpty()) {
            FilenameComparator comparator = new FilenameComparator();
            droppedPngFiles.sort((first, second) -> comparator.compare(first.getName(), second.getName()));
            if (this.state.getMainFramesDirectory() == null && this.state.getMainFramesInputText().isBlank()) {
                this.state.setMainFramesList(droppedPngFiles);
            } else if (this.state.getIntroFramesDirectory() == null && this.state.getIntroFramesInputText().isBlank()) {
                this.state.setIntroFramesList(droppedPngFiles);
            }
        }
        this.syncWidgetsFromState();
    }

    /** {@inheritDoc} */
    @Override
    public void onClose() {
        this.state.cancelCurrentJob();
        ScreenUtils.setScreen(this.parentScreen);
    }

    /** {@inheritDoc} */
    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        super.setFocused(focused);
        if (focused instanceof AbstractWidget widget) {
            this.ensureScrollableWidgetVisible(widget);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Dispatches coordinate-based clicks to the scrollbar, scrollable form, then fixed controls. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isContentScrollBarVisible() && this.contentScrollBar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.isMouseOverScrollableViewport(mouseX, mouseY) && this.mouseClickedOnWidgets(this.scrollableWidgets, mouseX, mouseY, button)) {
            return true;
        }
        return this.mouseClickedOnWidgets(this.fixedWidgets, mouseX, mouseY, button);
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Releases scrollbar dragging or dispatches release to the active hovered control. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.contentScrollBar.isGrabberGrabbed()) {
            this.contentScrollBar.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        if (button == 0 && this.isDragging()) {
            this.setDragging(false);
            if (this.getFocused() != null) {
                return this.getFocused().mouseReleased(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button));
            }
        }

        GuiEventListener hoveredWidget = this.getInteractiveWidgetAt(mouseX, mouseY);
        return hoveredWidget != null && hoveredWidget.mouseReleased(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button));
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    /** Dispatches coordinate-based dragging to the scrollbar or focused control. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.contentScrollBar.isGrabberGrabbed()) {
            return this.contentScrollBar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return this.getFocused() != null && this.isDragging() && button == 0 && this.getFocused().mouseDragged(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), dragX, dragY);
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (this.isContentScrollBarVisible() && this.contentScrollBar.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
            return true;
        }
        GuiEventListener hoveredWidget = this.getInteractiveWidgetAt(mouseX, mouseY);
        return hoveredWidget != null && hoveredWidget.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    /** {@inheritDoc} */
    @Override
    public void removed() {
        this.state.cancelCurrentJob();
        this.state.close();
        for (PiPWindow window : List.copyOf(this.childWindows)) {
            if (window != null) {
                try {
                    window.close();
                } catch (Exception ignored) {
                }
            }
        }
        this.childWindows.clear();
        super.removed();
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    /** Parses a non-blank path as a directory value, or returns null for empty input. */
    protected static @Nullable File pathToDirectory(@Nullable String value) {
        if ((value == null) || value.isBlank()) return null;
        return new File(value.replace("\\", "/"));
    }

    /** Parses a non-blank path as a file value, or returns null for empty input. */
    protected static @Nullable File pathToFile(@Nullable String value) {
        if ((value == null) || value.isBlank()) return null;
        return new File(value.replace("\\", "/"));
    }

    /** Returns a file path string, or an empty string for a null file. */
    protected static @NotNull String fileToPath(@Nullable File file) {
        return (file != null) ? file.getPath().replace("\\", "/") : "";
    }

    /** Returns whether the entire non-blank value is a signed base-10 integer. */
    protected static boolean isInteger(@Nullable String value) {
        if ((value == null) || value.isBlank()) return false;
        try {
            Long.parseLong(value.trim());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Returns whether the entire non-blank value fits in a signed 32-bit integer. */
    protected static boolean isInt(@Nullable String value) {
        if ((value == null) || value.isBlank()) return false;
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Parses an integer value and returns the fallback for blank, malformed, or out-of-range input. */
    protected static int parseIntOrDefault(@Nullable String value, int fallback) {
        if (!isInt(value)) return fallback;
        return Integer.parseInt(value.trim());
    }

    /** Parses a long value and returns the fallback for blank or malformed input. */
    protected static long parseLongOrDefault(@Nullable String value, long fallback) {
        if (!isInteger(value)) return fallback;
        return Long.parseLong(value.trim());
    }

    /** Parses a double value and returns the fallback for blank or malformed input. */
    protected static double parseDoubleOrDefault(@Nullable String value, double fallback) {
        if ((value == null) || value.isBlank()) return fallback;
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /** Creates the vertical scrollbar that controls form-content translation. */
    protected @NotNull ScrollBar createContentScrollBar() {
        ScrollBar scrollBar = new ScrollBar(
                ScrollBar.ScrollBarDirection.VERTICAL,
                UIBase.VERTICAL_SCROLL_BAR_WIDTH,
                UIBase.VERTICAL_SCROLL_BAR_HEIGHT,
                0,
                0,
                0,
                0,
                () -> UIBase.getUITheme().scroll_grabber_color_normal,
                () -> UIBase.getUITheme().scroll_grabber_color_hover
        );
        scrollBar.setScrollWheelAllowed(true);
        scrollBar.setRoundedGrabberEnabled(true);
        return scrollBar;
    }

    /** Rebuilds fixed and scrollable widget groups after screen initialization. */
    protected void rebuildTrackedWidgetLists() {
        this.scrollableWidgets.clear();
        this.fixedWidgets.clear();

        this.addScrollableWidget(this.mainFramesPathEditBox);
        this.addScrollableWidget(this.introFramesPathEditBox);
        this.addScrollableWidget(this.outputPathEditBox);
        this.addScrollableWidget(this.frameTimeEditBox);
        this.addScrollableWidget(this.introFrameTimeEditBox);
        this.addScrollableWidget(this.loopCountEditBox);
        this.addScrollableWidget(this.keyframeIntervalEditBox);
        this.addScrollableWidget(this.presetCycleButton);
        this.addScrollableWidget(this.rectCopyCycleButton);
        this.addScrollableWidget(this.duplicateCycleButton);
        this.addScrollableWidget(this.nearLosslessCycleButton);
        this.addScrollableWidget(this.strictPostWriteValidationCycleButton);
        this.addScrollableWidget(this.adaptiveKeyframeCycleButton);
        this.addScrollableWidget(this.adaptiveMaxKeyframeIntervalEditBox);
        this.addScrollableWidget(this.adaptiveContinuationMinSavingsBytesEditBox);
        this.addScrollableWidget(this.adaptiveContinuationMinSavingsRatioEditBox);
        this.addScrollableWidget(this.maxCopySearchDistanceEditBox);
        this.addScrollableWidget(this.maxCandidateAxisOffsetsEditBox);
        this.addScrollableWidget(this.perceptualVisibleColorDeltaEditBox);
        this.addScrollableWidget(this.perceptualAlphaDeltaEditBox);
        this.addScrollableWidget(this.perceptualAverageErrorEditBox);
        this.addScrollableWidget(this.browseMainFramesButton);
        this.addScrollableWidget(this.browseIntroFramesButton);
        this.addScrollableWidget(this.clearIntroFramesButton);
        this.addScrollableWidget(this.browseOutputButton);

        this.addFixedWidget(this.exportButton);
        this.addFixedWidget(this.cancelJobButton);
        this.addFixedWidget(this.closeButton);
    }

    /** Adds a present widget to the group translated and clipped with form scrolling. */
    protected void addScrollableWidget(@Nullable AbstractWidget widget) {
        if (widget != null) {
            this.scrollableWidgets.add(widget);
        }
    }

    /** Adds a present widget to the group that remains fixed while the form scrolls. */
    protected void addFixedWidget(@Nullable AbstractWidget widget) {
        if (widget != null) {
            this.fixedWidgets.add(widget);
        }
    }

    /** Positions export, cancel, and close buttons along the fixed bottom row. */
    protected void layoutFixedButtons(int contentX, int contentWidth) {
        int bottomY = this.getBottomButtonY();
        this.layoutWidget(this.exportButton, contentX, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.cancelJobButton, contentX + 128, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.closeButton, contentX + contentWidth - 120, bottomY, 120, FIELD_HEIGHT);
    }

    /** Moves the form scrollbar to the final scrollable row. */
    protected void scrollScrollableContentToBottom() {
        this.repositionWidgets();
        this.contentScrollBar.setScroll(1.0F);
        this.repositionWidgets();
    }

    /** Positions all form controls for an offset and returns total content and diagnostics bounds. */
    protected @NotNull ContentLayout applyScrollableLayout(int contentX, int contentWidth, int scrollOffset) {
        contentX += SCROLL_CONTENT_HORIZONTAL_PADDING;
        contentWidth = Math.max(0, contentWidth - (SCROLL_CONTENT_HORIZONTAL_PADDING * 2));
        int inlineLabelWidth = this.getInlineLabelWidth();
        int browseWidth = 84;
        int clearWidth = 64;
        int contentY = SCROLL_CONTENT_VERTICAL_PADDING;
        int contentBottom = SCROLL_CONTENT_VERTICAL_PADDING;

        this.layoutLabeledPathRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.mainFramesPathEditBox, this.browseMainFramesButton, browseWidth, null, 0);
        contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        contentY += ROW_GAP;
        this.layoutLabeledPathRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.introFramesPathEditBox, this.browseIntroFramesButton, browseWidth, this.clearIntroFramesButton, clearWidth);
        contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        contentY += ROW_GAP;
        this.layoutLabeledPathRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.outputPathEditBox, this.browseOutputButton, browseWidth, null, 0);
        contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        contentY += SECTION_GAP;

        int columnWidth = (contentWidth - COLUMN_GAP) / 2;
        boolean useSingleColumnNumericLayout = this.useSingleColumnNumericLayout(columnWidth, inlineLabelWidth);
        if (useSingleColumnNumericLayout) {
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.frameTimeEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.introFrameTimeEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.loopCountEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += SECTION_GAP;
        } else {
            int rowY = this.toScrollableScreenY(contentY, scrollOffset);
            this.layoutLabeledFieldRow(contentX, rowY, columnWidth, inlineLabelWidth, this.frameTimeEditBox);
            this.layoutLabeledFieldRow(contentX + columnWidth + COLUMN_GAP, rowY, columnWidth, inlineLabelWidth, this.introFrameTimeEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.loopCountEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += SECTION_GAP;
        }

        this.layoutWidget(this.presetCycleButton, contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, FIELD_HEIGHT);
        contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        contentY += ROW_GAP;
        this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.keyframeIntervalEditBox);
        contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        contentY += ROW_GAP;

        int toggleWidth = (contentWidth - COLUMN_GAP) / 2;
        boolean useSingleColumnToggleLayout = toggleWidth < 220;
        if (useSingleColumnToggleLayout) {
            this.layoutWidget(this.rectCopyCycleButton, contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, FIELD_HEIGHT);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutWidget(this.duplicateCycleButton, contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, FIELD_HEIGHT);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutWidget(this.nearLosslessCycleButton, contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, FIELD_HEIGHT);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutWidget(this.strictPostWriteValidationCycleButton, contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, FIELD_HEIGHT);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        } else {
            int rowY = this.toScrollableScreenY(contentY, scrollOffset);
            this.layoutWidget(this.rectCopyCycleButton, contentX, rowY, toggleWidth, FIELD_HEIGHT);
            this.layoutWidget(this.duplicateCycleButton, contentX + toggleWidth + COLUMN_GAP, rowY, toggleWidth, FIELD_HEIGHT);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            rowY = this.toScrollableScreenY(contentY, scrollOffset);
            this.layoutWidget(this.nearLosslessCycleButton, contentX, rowY, toggleWidth, FIELD_HEIGHT);
            this.layoutWidget(this.strictPostWriteValidationCycleButton, contentX + toggleWidth + COLUMN_GAP, rowY, toggleWidth, FIELD_HEIGHT);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        }

        contentY += SECTION_GAP;
        this.layoutWidget(this.adaptiveKeyframeCycleButton, contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, FIELD_HEIGHT);
        contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        contentY += ROW_GAP;

        int advancedColumnWidth = (contentWidth - COLUMN_GAP) / 2;
        boolean useSingleColumnAdvancedLayout = this.useSingleColumnNumericLayout(advancedColumnWidth, inlineLabelWidth);
        if (useSingleColumnAdvancedLayout) {
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.adaptiveMaxKeyframeIntervalEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.adaptiveContinuationMinSavingsBytesEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.adaptiveContinuationMinSavingsRatioEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.maxCopySearchDistanceEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.maxCandidateAxisOffsetsEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.perceptualVisibleColorDeltaEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.perceptualAlphaDeltaEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            this.layoutLabeledFieldRow(contentX, this.toScrollableScreenY(contentY, scrollOffset), contentWidth, inlineLabelWidth, this.perceptualAverageErrorEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        } else {
            int rowY = this.toScrollableScreenY(contentY, scrollOffset);
            this.layoutLabeledFieldRow(contentX, rowY, advancedColumnWidth, inlineLabelWidth, this.adaptiveMaxKeyframeIntervalEditBox);
            this.layoutLabeledFieldRow(contentX + advancedColumnWidth + COLUMN_GAP, rowY, advancedColumnWidth, inlineLabelWidth, this.adaptiveContinuationMinSavingsBytesEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            rowY = this.toScrollableScreenY(contentY, scrollOffset);
            this.layoutLabeledFieldRow(contentX, rowY, advancedColumnWidth, inlineLabelWidth, this.adaptiveContinuationMinSavingsRatioEditBox);
            this.layoutLabeledFieldRow(contentX + advancedColumnWidth + COLUMN_GAP, rowY, advancedColumnWidth, inlineLabelWidth, this.maxCopySearchDistanceEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            rowY = this.toScrollableScreenY(contentY, scrollOffset);
            this.layoutLabeledFieldRow(contentX, rowY, advancedColumnWidth, inlineLabelWidth, this.maxCandidateAxisOffsetsEditBox);
            this.layoutLabeledFieldRow(contentX + advancedColumnWidth + COLUMN_GAP, rowY, advancedColumnWidth, inlineLabelWidth, this.perceptualVisibleColorDeltaEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
            contentY += ROW_GAP;
            rowY = this.toScrollableScreenY(contentY, scrollOffset);
            this.layoutLabeledFieldRow(contentX, rowY, advancedColumnWidth, inlineLabelWidth, this.perceptualAlphaDeltaEditBox);
            this.layoutLabeledFieldRow(contentX + advancedColumnWidth + COLUMN_GAP, rowY, advancedColumnWidth, inlineLabelWidth, this.perceptualAverageErrorEditBox);
            contentBottom = Math.max(contentBottom, contentY + FIELD_HEIGHT);
        }

        int diagnosticsY = this.toScrollableScreenY(contentBottom + 18, scrollOffset);
        int totalHeight = contentBottom + SCROLL_CONTENT_VERTICAL_PADDING;
        if (this.state.getCurrentJob() != null) {
            totalHeight = contentBottom + 18 + this.getDiagnosticsHeight(contentWidth) + SCROLL_CONTENT_VERTICAL_PADDING;
        }
        return new ContentLayout(totalHeight, diagnosticsY);
    }

    /** Clips and renders scrollable widgets and labels at the current scrollbar offset. */
    protected void renderScrollableContent(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int scissorMinX = this.getContentLeft();
        int scissorMinY = this.getContentStartY();
        int scissorMaxX = scissorMinX + this.scrollableContentWidth;
        int scissorMaxY = scissorMinY + this.getScrollableViewportHeight();
        if ((scissorMaxX <= scissorMinX) || (scissorMaxY <= scissorMinY)) {
            return;
        }

        graphics.enableScissor(scissorMinX, scissorMinY, scissorMaxX, scissorMaxY);
        for (AbstractWidget widget : this.scrollableWidgets) {
            widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        this.renderFieldLabels(graphics);
        this.renderDiagnostics(graphics);
        graphics.disableScissor();
    }

    /** Returns the wrapped validation and progress text height for the available width. */
    protected int getDiagnosticsHeight(int maxWidth) {
        AfmaEncodeJob job = this.state.getCurrentJob();
        if (job == null) return 0;

        AfmaEncodeProgress progress = job.getProgress();
        int height = this.getWrappedUiTextHeight(Component.translatable("konkrete.afma.creator.job_status", progress.task()), maxWidth);
        if (progress.detail() != null && !progress.detail().isBlank()) {
            height += this.getWrappedUiTextHeight(Component.literal(progress.detail()), maxWidth);
        }
        return height + 8;
    }

    /** Returns the pixel height required to wrap text to the supplied width. */
    protected int getWrappedUiTextHeight(@NotNull Component text, int maxWidth) {
        List<MutableComponent> lines = UIBase.lineWrapUIComponentsNormal(text, Math.max(20, maxWidth));
        int lineHeight = Math.max(10, Math.round(UIBase.getUITextHeightNormal()));
        return lines.size() * (lineHeight + 2);
    }

    /** Converts a form-content Y coordinate to its scrolled screen coordinate. */
    protected int toScrollableScreenY(int contentY, int scrollOffset) {
        return this.getContentStartY() + contentY - scrollOffset;
    }

    /** Returns horizontal space reserved only while the content scrollbar is visible. */
    protected int getContentScrollBarReservedWidth() {
        return Math.round(this.contentScrollBar.grabberWidth) + CONTENT_SCROLL_BAR_GAP;
    }

    /** Returns the non-negative number of pixels by which form content can scroll. */
    protected int getScrollableContentScrollRange() {
        return Math.max(0, this.scrollableContentHeight - this.getScrollableViewportHeight());
    }

    /** Returns the current form translation derived from the scrollbar percentage. */
    protected int getScrollableContentScrollOffset() {
        int scrollRange = this.getScrollableContentScrollRange();
        if (scrollRange <= 0) {
            return 0;
        }
        return Math.round(scrollRange * this.contentScrollBar.getScroll());
    }

    /** Returns whether form content exceeds its viewport height. */
    protected boolean isContentScrollBarVisible() {
        return this.contentScrollBar.active && (this.getScrollableContentScrollRange() > 0);
    }

    /** Updates scrollbar bounds, range, visibility, and active state for the current layout. */
    protected void updateContentScrollBar(int contentX, int contentWidth, boolean visible) {
        this.contentScrollBar.active = visible;
        int scrollBarEndX = contentX + contentWidth + CONTENT_SCROLL_BAR_GAP + Math.round(this.contentScrollBar.grabberWidth);
        this.contentScrollBar.scrollAreaStartX = contentX;
        this.contentScrollBar.scrollAreaStartY = this.getContentStartY() + 1;
        this.contentScrollBar.scrollAreaEndX = scrollBarEndX;
        this.contentScrollBar.scrollAreaEndY = this.getContentStartY() + this.getScrollableViewportHeight() - 1;

        float scrollRange = Math.max(1.0F, this.getScrollableContentScrollRange());
        this.contentScrollBar.setWheelScrollSpeed(1.0F / (scrollRange / 500.0F));
        if (!visible || (this.getScrollableContentScrollRange() <= 0)) {
            this.contentScrollBar.setScroll(0.0F);
        }
    }

    /** Returns whether the pointer lies inside the clipped form viewport. */
    protected boolean isMouseOverScrollableViewport(double mouseX, double mouseY) {
        return mouseX >= this.getContentLeft()
                && mouseX <= (this.getContentLeft() + this.scrollableContentWidth)
                && mouseY >= this.getContentStartY()
                && mouseY <= (this.getContentStartY() + this.getScrollableViewportHeight());
    }

    /** Dispatches a click to the first active visible widget that consumes it. */
    protected boolean mouseClickedOnWidgets(@NotNull List<AbstractWidget> widgets, double mouseX, double mouseY, int button) {
        for (AbstractWidget widget : widgets) {
            if (widget.mouseClicked(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), false)) {
                this.setFocused(widget);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    /** Returns the topmost active visible widget under the pointer, or null when none matches. */
    protected @Nullable GuiEventListener getInteractiveWidgetAt(double mouseX, double mouseY) {
        if (this.isMouseOverScrollableViewport(mouseX, mouseY)) {
            for (AbstractWidget widget : this.scrollableWidgets) {
                if (widget.isMouseOver(mouseX, mouseY)) {
                    return widget;
                }
            }
        }
        for (AbstractWidget widget : this.fixedWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                return widget;
            }
        }
        return null;
    }

    /** Adjusts scrolling so the focused form widget is fully visible. */
    protected void ensureScrollableWidgetVisible(@Nullable AbstractWidget widget) {
        if ((widget == null) || !this.scrollableWidgets.contains(widget) || !this.isContentScrollBarVisible()) {
            return;
        }

        int viewportTop = this.getContentStartY();
        int viewportBottom = viewportTop + this.getScrollableViewportHeight();
        int widgetTop = widget.getY();
        int widgetBottom = widget.getY() + widget.getHeight();
        int scrollRange = this.getScrollableContentScrollRange();
        if (scrollRange <= 0) return;

        float newScroll = this.contentScrollBar.getScroll();
        if (widgetTop < viewportTop) {
            newScroll -= (float) (viewportTop - widgetTop) / (float) scrollRange;
        } else if (widgetBottom > viewportBottom) {
            newScroll += (float) (widgetBottom - viewportBottom) / (float) scrollRange;
        }

        if (Float.compare(newScroll, this.contentScrollBar.getScroll()) != 0) {
            this.contentScrollBar.setScroll(newScroll);
            this.repositionWidgets();
        }
    }

    /** Carries the measured scrollable-content height and diagnostics position for one layout pass. */
    protected record ContentLayout(int totalHeight, int diagnosticsY) {
    }

}
