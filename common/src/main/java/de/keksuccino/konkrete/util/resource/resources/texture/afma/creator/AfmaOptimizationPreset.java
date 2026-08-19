package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.enums.LocalizedEnum;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Configures {@code AfmaOptimizationPreset} limits and tradeoffs for the AFMA creator. */
public enum AfmaOptimizationPreset implements LocalizedEnum<AfmaOptimizationPreset> {

    /** Balances AFMA encode quality, size, and planning time. */
    BALANCED("balanced", 90, true, true, true, 512, 8, LocalizedEnum.WARNING_TEXT_STYLE),
    /** Uses the most quality-preserving AFMA encode preset. */
    BEST_QUALITY("best_quality", 90, true, true, false, 2048, 12, LocalizedEnum.WARNING_TEXT_STYLE),
    /** Uses the most size-focused AFMA encode preset. */
    SMALLEST_FILE("smallest_file", 90, true, true, true, 2048, 12, LocalizedEnum.WARNING_TEXT_STYLE);

    private final @NotNull String name;
    private final int keyframeInterval;
    private final boolean rectCopyEnabled;
    private final boolean duplicateFrameElision;
    private final boolean nearLosslessEnabledByDefault;
    private final int maxCopySearchDistance;
    private final int maxCandidateAxisOffsets;
    private final @NotNull Supplier<Style> style;

    AfmaOptimizationPreset(@NotNull String name, int keyframeInterval, boolean rectCopyEnabled,
                           boolean duplicateFrameElision, boolean nearLosslessEnabledByDefault, int maxCopySearchDistance, int maxCandidateAxisOffsets,
                           @NotNull Supplier<Style> style) {
        this.name = name;
        this.keyframeInterval = keyframeInterval;
        this.rectCopyEnabled = rectCopyEnabled;
        this.duplicateFrameElision = duplicateFrameElision;
        this.nearLosslessEnabledByDefault = nearLosslessEnabledByDefault;
        this.maxCopySearchDistance = maxCopySearchDistance;
        this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
        this.style = style;
    }

    /** Returns the localization key base used by this AFMA creator instance. */
    @Override
    public @NotNull String getLocalizationKeyBase() {
        return "konkrete.afma.creator.optimization_preset";
    }

    /** Returns the name used by this AFMA creator instance. */
    @Override
    public @NotNull String getName() {
        return this.name;
    }

    /** Returns the values used by this AFMA creator instance. */
    @Override
    public @NotNull AfmaOptimizationPreset[] getValues() {
        return values();
    }

    /** Returns the by name internal, or {@code null} when it is not available. */
    @Override
    public @Nullable AfmaOptimizationPreset getByNameInternal(@NotNull String name) {
        for (AfmaOptimizationPreset preset : values()) {
            if (preset.getName().equals(name)) return preset;
        }
        return null;
    }

    /** Returns the value component style used by this AFMA creator instance. */
    @Override
    public @NotNull Style getValueComponentStyle() {
        return this.style.get();
    }

    /** Returns the keyframe interval used by this AFMA creator instance. */
    public int getKeyframeInterval() {
        return this.keyframeInterval;
    }

    /** Returns whether rect copy enabled. */
    public boolean isRectCopyEnabled() {
        return this.rectCopyEnabled;
    }

    /** Returns whether duplicate frame elision. */
    public boolean isDuplicateFrameElision() {
        return this.duplicateFrameElision;
    }

    /** Returns whether near lossless enabled by default. */
    public boolean isNearLosslessEnabledByDefault() {
        return this.nearLosslessEnabledByDefault;
    }

    /** Returns the max copy search distance used by this AFMA creator instance. */
    public int getMaxCopySearchDistance() {
        return this.maxCopySearchDistance;
    }

    /** Returns the max candidate axis offsets used by this AFMA creator instance. */
    public int getMaxCandidateAxisOffsets() {
        return this.maxCandidateAxisOffsets;
    }

}
