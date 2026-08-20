package de.keksuccino.konkrete.util.rendering.ui.widget;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinAbstractWidget;
import de.keksuccino.konkrete.util.ClassExtender;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.resource.PlayableResource;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mixin-applied state contract for labels, sounds, backgrounds, bounds, and hitboxes on every {@link AbstractWidget}.
 */
@SuppressWarnings("unused")
@ClassExtender(AbstractWidget.class)
public interface CustomizableWidget {

    /** Returns original message for this widget. */
    @Nullable
    default Component getOriginalMessageKonkrete() {
        Component custom = this.getCustomLabelKonkrete();
        Component hover = this.getHoverLabelKonkrete();
        this.setCustomLabelKonkrete(null);
        this.setHoverLabelKonkrete(null);
        Component original = null;
        if (this instanceof AbstractWidget w) original = w.getMessage();
        this.setCustomLabelKonkrete(custom);
        this.setHoverLabelKonkrete(hover);
        return original;
    }

    /**
     * Returns if the widget should render its Vanilla background (true) or not (false).
     */
    default boolean renderCustomBackgroundKonkrete(@NotNull AbstractWidget widget, @NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        RenderableResource customBackground;
        RenderableResource customBackgroundNormal = this.getCustomBackgroundNormalKonkrete();
        RenderableResource customBackgroundHover = this.getCustomBackgroundHoverKonkrete();
        RenderableResource customBackgroundInactive = this.getCustomBackgroundInactiveKonkrete();
        if (widget.active) {
            if (widget.isHoveredOrFocused()) {
                customBackground = customBackgroundHover;
                if (customBackgroundNormal instanceof PlayableResource p) p.pause();
            } else {
                customBackground = customBackgroundNormal;
                if (customBackgroundHover instanceof PlayableResource p) p.pause();
            }
            if (customBackgroundInactive instanceof PlayableResource p) p.pause();
        } else {
            customBackground = customBackgroundInactive;
            if (customBackgroundNormal instanceof PlayableResource p) p.pause();
            if (customBackgroundHover instanceof PlayableResource p) p.pause();
        }
        boolean renderVanilla = true;
        if (customBackground != null) {
            if (customBackground instanceof PlayableResource p) p.play();
            Identifier location = customBackground.getResourceLocation();
            if (location != null) {
                renderVanilla = false;
                de.keksuccino.konkrete.util.rendering.RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, ((AccessorMixinAbstractWidget)widget).get_alpha_Konkrete());
                if ((widget instanceof CustomizableSlider s) && s.isNineSliceCustomSliderHandle_Konkrete()) {
                    RenderingUtils.blitNineSlicedTexture(graphics, location, x, y, width, height, customBackground.getWidth(), customBackground.getHeight(),
                            s.getNineSliceSliderHandleBorderTop_Konkrete(), s.getNineSliceSliderHandleBorderRight_Konkrete(),
                            s.getNineSliceSliderHandleBorderBottom_Konkrete(), s.getNineSliceSliderHandleBorderLeft_Konkrete());
                } else if (!(widget instanceof CustomizableSlider) && this.isNineSliceCustomBackgroundTexture_Konkrete()) {
                    RenderingUtils.blitNineSlicedTexture(graphics, location, x, y, width, height, customBackground.getWidth(), customBackground.getHeight(),
                            getNineSliceCustomBackgroundBorderTop_Konkrete(), getNineSliceCustomBackgroundBorderRight_Konkrete(),
                            getNineSliceCustomBackgroundBorderBottom_Konkrete(), getNineSliceCustomBackgroundBorderLeft_Konkrete());
                } else {
                    graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, location, x, y, 0.0F, 0.0F, width, height, width, height);
                }
                RenderingUtils.resetShaderColor(graphics);
            }
        }
        return renderVanilla;
    }

    /** Restores widget customizations to its default state. */
    void resetWidgetCustomizationsKonkrete();

    /** Restores widget size and position to its default state. */
    void resetWidgetSizeAndPositionKonkrete();

    /** Registers reset customizations listener with this widget. */
    void addResetCustomizationsListenerKonkrete(@NotNull Runnable listener);

    /** Returns reset customizations listeners for this widget. */
    @NotNull
    List<Runnable> getResetCustomizationsListenersKonkrete();

    /** Registers hover state listener with this widget. */
    void addHoverStateListenerKonkrete(@NotNull Consumer<Boolean> listener);

    /** Registers focus state listener with this widget. */
    void addFocusStateListenerKonkrete(@NotNull Consumer<Boolean> listener);

    /** Registers hover or focus state listener with this widget. */
    void addHoverOrFocusStateListenerKonkrete(@NotNull Consumer<Boolean> listener);

    /** Returns hover state listeners for this widget. */
    @NotNull
    List<Consumer<Boolean>> getHoverStateListenersKonkrete();

    /** Returns focus state listeners for this widget. */
    @NotNull
    List<Consumer<Boolean>> getFocusStateListenersKonkrete();

    /** Returns hover or focus state listeners for this widget. */
    @NotNull
    List<Consumer<Boolean>> getHoverOrFocusStateListenersKonkrete();

    /** Returns last hover state for this widget. */
    boolean getLastHoverStateKonkrete();

    /** Sets last hover state for this widget. */
    void setLastHoverStateKonkrete(boolean hovered);

    /** Returns last focus state for this widget. */
    boolean getLastFocusStateKonkrete();

    /** Sets last focus state for this widget. */
    void setLastFocusStateKonkrete(boolean focused);

    /** Returns last hover or focus state for this widget. */
    boolean getLastHoverOrFocusStateKonkrete();

    /** Sets last hover or focus state for this widget. */
    void setLastHoverOrFocusStateKonkrete(boolean hoveredOrFocused);

    /** Refreshes hover listeners when the hover state changes. */
    default void tickHoverStateListenersKonkrete(boolean hovered) {
        if (this.getLastHoverStateKonkrete() != hovered) {
            for (Consumer<Boolean> listener : this.getHoverStateListenersKonkrete()) {
                listener.accept(hovered);
            }
        }
        this.setLastHoverStateKonkrete(hovered);
    }

    /** Refreshes focus listeners when the focus state changes. */
    default void tickFocusStateListenersKonkrete(boolean focused) {
        if (this.getLastFocusStateKonkrete() != focused) {
            for (Consumer<Boolean> listener : this.getFocusStateListenersKonkrete()) {
                listener.accept(focused);
            }
        }
        this.setLastFocusStateKonkrete(focused);
    }

    /** Refreshes combined hover/focus listeners when that state changes. */
    default void tickHoverOrFocusStateListenersKonkrete(boolean hoveredOrFocused) {
        if (this.getLastHoverOrFocusStateKonkrete() != hoveredOrFocused) {
            for (Consumer<Boolean> listener : this.getHoverOrFocusStateListenersKonkrete()) {
                listener.accept(hoveredOrFocused);
            }
        }
        this.setLastHoverOrFocusStateKonkrete(hoveredOrFocused);
    }

    /** Sets custom label for this widget. */
    void setCustomLabelKonkrete(@Nullable Component label);

    /** Returns custom label for this widget. */
    @Nullable
    Component getCustomLabelKonkrete();

    /** Sets hover label for this widget. */
    void setHoverLabelKonkrete(@Nullable Component hoverLabel);

    /** Returns hover label for this widget. */
    @Nullable
    Component getHoverLabelKonkrete();

    /** Sets label hover color for this widget. */
    void setLabelHoverColorKonkrete(@Nullable DrawableColor color);

    /** Returns label hover color for this widget. */
    @Nullable
    DrawableColor getLabelHoverColorKonkrete();

    /** Sets label base color for this widget. */
    void setLabelBaseColorKonkrete(@Nullable DrawableColor color);

    /** Returns label base color for this widget. */
    @Nullable
    DrawableColor getLabelBaseColorKonkrete();

    /** Sets label scale for this widget. */
    void setLabelScaleKonkrete(float scale);

    /** Returns label scale for this widget. */
    float getLabelScaleKonkrete();

    /** Returns the effective label scale after widget-specific adjustments. */
    default float resolveLabelScaleKonkrete() {
        return this.getLabelScaleKonkrete();
    }

    /** Sets underline label on hover for this widget. */
    void setUnderlineLabelOnHoverKonkrete(boolean underline);

    /** Returns whether underline label on hover. */
    boolean isUnderlineLabelOnHoverKonkrete();

    /** Sets label shadow for this widget. */
    void setLabelShadowKonkrete(boolean shadow);

    /** Reports whether the control label is drawn with a shadow. */
    boolean isLabelShadowKonkrete();

    /** Sets custom click sound for this widget. */
    void setCustomClickSoundKonkrete(@Nullable IAudio sound);

    /** Returns custom click sound for this widget. */
    @Nullable
    IAudio getCustomClickSoundKonkrete();

    /** Stops the configured custom click sound if it is active. */
    default void stopCustomClickSoundKonkrete() {
        IAudio a = this.getCustomClickSoundKonkrete();
        if (a != null) a.stop();
    }

    /** Sets hover sound for this widget. */
    void setHoverSoundKonkrete(@Nullable IAudio sound);

    /** Returns hover sound for this widget. */
    @Nullable
    IAudio getHoverSoundKonkrete();

    /** Stops the configured hover sound if it is active. */
    default void stopHoverSoundKonkrete() {
        IAudio a = this.getHoverSoundKonkrete();
        if (a != null) a.stop();
    }

    /** Sets unhover sound for this widget. */
    void setUnhoverSoundKonkrete(@Nullable IAudio sound);

    /** Returns unhover sound for this widget. */
    @Nullable
    IAudio getUnhoverSoundKonkrete();

    /** Stops the configured unhover sound if it is active. */
    default void stopUnhoverSoundKonkrete() {
        IAudio a = this.getUnhoverSoundKonkrete();
        if (a != null) a.stop();
    }

    /** Sets hidden for this widget. */
    void setHiddenKonkrete(boolean hidden);

    /** Returns whether hidden. */
    boolean isHiddenKonkrete();

    /** Sets custom background normal for this widget. */
    void setCustomBackgroundNormalKonkrete(@Nullable RenderableResource background);

    /** Returns custom background normal for this widget. */
    @Nullable
    RenderableResource getCustomBackgroundNormalKonkrete();

    /** Sets custom background hover for this widget. */
    void setCustomBackgroundHoverKonkrete(@Nullable RenderableResource background);

    /** Returns custom background hover for this widget. */
    @Nullable
    RenderableResource getCustomBackgroundHoverKonkrete();

    /** Sets custom background inactive for this widget. */
    void setCustomBackgroundInactiveKonkrete(@Nullable RenderableResource background);

    /** Returns custom background inactive for this widget. */
    @Nullable
    RenderableResource getCustomBackgroundInactiveKonkrete();

    /** Sets nine slice custom background for this widget. */
    void setNineSliceCustomBackground_Konkrete(boolean repeat);

    /** Returns whether nine slice custom background texture. */
    boolean isNineSliceCustomBackgroundTexture_Konkrete();

    /** Sets nine slice border x for this widget. */
    void setNineSliceBorderX_Konkrete(int borderX);

    /** Returns nine slice custom background border x for this widget. */
    int getNineSliceCustomBackgroundBorderX_Konkrete();

    /** Sets nine slice border y for this widget. */
    void setNineSliceBorderY_Konkrete(int borderY);

    /** Returns nine slice custom background border y for this widget. */
    int getNineSliceCustomBackgroundBorderY_Konkrete();

    /** Sets nine slice border top for this widget. */
    default void setNineSliceBorderTop_Konkrete(int borderTop) {
        setNineSliceBorderY_Konkrete(borderTop);
    }

    /** Returns nine slice custom background border top for this widget. */
    default int getNineSliceCustomBackgroundBorderTop_Konkrete() {
        return getNineSliceCustomBackgroundBorderY_Konkrete();
    }

    /** Sets nine slice border right for this widget. */
    default void setNineSliceBorderRight_Konkrete(int borderRight) {
        setNineSliceBorderX_Konkrete(borderRight);
    }

    /** Returns nine slice custom background border right for this widget. */
    default int getNineSliceCustomBackgroundBorderRight_Konkrete() {
        return getNineSliceCustomBackgroundBorderX_Konkrete();
    }

    /** Sets nine slice border bottom for this widget. */
    default void setNineSliceBorderBottom_Konkrete(int borderBottom) {
        setNineSliceBorderY_Konkrete(borderBottom);
    }

    /** Returns nine slice custom background border bottom for this widget. */
    default int getNineSliceCustomBackgroundBorderBottom_Konkrete() {
        return getNineSliceCustomBackgroundBorderY_Konkrete();
    }

    /** Sets nine slice border left for this widget. */
    default void setNineSliceBorderLeft_Konkrete(int borderLeft) {
        setNineSliceBorderX_Konkrete(borderLeft);
    }

    /** Returns nine slice custom background border left for this widget. */
    default int getNineSliceCustomBackgroundBorderLeft_Konkrete() {
        return getNineSliceCustomBackgroundBorderX_Konkrete();
    }

    /** Sets custom background reset behavior for this widget. */
    void setCustomBackgroundResetBehaviorKonkrete(@NotNull CustomBackgroundResetBehavior resetBehavior);

    /** Returns custom background reset behavior for this widget. */
    @NotNull
    CustomBackgroundResetBehavior getCustomBackgroundResetBehaviorKonkrete();

    /** Returns custom width for this widget. */
    @Nullable
    Integer getCustomWidthKonkrete();

    /** Sets custom width for this widget. */
    void setCustomWidthKonkrete(@Nullable Integer width);

    /** Returns custom height for this widget. */
    @Nullable
    Integer getCustomHeightKonkrete();

    /** Sets custom height for this widget. */
    void setCustomHeightKonkrete(@Nullable Integer height);

    /** Returns custom x for this widget. */
    @Nullable
    Integer getCustomXKonkrete();

    /** Sets custom x for this widget. */
    void setCustomXKonkrete(@Nullable Integer x);

    /** Returns custom y for this widget. */
    @Nullable
    Integer getCustomYKonkrete();

    /** Sets custom y for this widget. */
    void setCustomYKonkrete(@Nullable Integer y);

    /** Sets hitbox rotation for this widget. */
    void setHitboxRotationKonkrete(float rotationDegrees, float verticalTiltDegrees, float horizontalTiltDegrees);

    /** Returns hitbox rotation degrees for this widget. */
    float getHitboxRotationDegreesKonkrete();

    /** Returns hitbox vertical tilt degrees for this widget. */
    float getHitboxVerticalTiltDegreesKonkrete();

    /** Returns hitbox horizontal tilt degrees for this widget. */
    float getHitboxHorizontalTiltDegreesKonkrete();

    /** Defines when a customized widget background resets its playback state. */
    enum CustomBackgroundResetBehavior {

        /** Resets customized background playback on reset never. */
        RESET_NEVER,
        /** Resets customized background playback on reset on hover. */
        RESET_ON_HOVER,
        /** Resets customized background playback on reset on unhover. */
        RESET_ON_UNHOVER,
        /** Resets customized background playback on reset on hover and unhover. */
        RESET_ON_HOVER_AND_UNHOVER

    }

}
