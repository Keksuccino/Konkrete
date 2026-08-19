package de.keksuccino.konkrete.util.rendering.ui.widget;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinAbstractWidget;
import de.keksuccino.konkrete.util.ClassExtender;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.resource.PlayableResource;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Mixin-applied texture and nine-slice state for {@link AbstractSliderButton} tracks and handles.
 */
@ClassExtender(AbstractSliderButton.class)
public interface CustomizableSlider {

    /** Sets nine slice custom slider background for this widget. */
    void setNineSliceCustomSliderBackground_Konkrete(boolean nineSlice);

    /** Returns whether nine slice custom slider background. */
    boolean isNineSliceCustomSliderBackground_Konkrete();

    /** Sets nine slice slider background border x for this widget. */
    void setNineSliceSliderBackgroundBorderX_Konkrete(int borderX);

    /** Returns nine slice slider background border x for this widget. */
    int getNineSliceSliderBackgroundBorderX_Konkrete();

    /** Sets nine slice slider background border y for this widget. */
    void setNineSliceSliderBackgroundBorderY_Konkrete(int borderY);

    /** Returns nine slice slider background border y for this widget. */
    int getNineSliceSliderBackgroundBorderY_Konkrete();

    /** Sets nine slice slider background border top for this widget. */
    default void setNineSliceSliderBackgroundBorderTop_Konkrete(int borderTop) {
        setNineSliceSliderBackgroundBorderY_Konkrete(borderTop);
    }

    /** Returns nine slice slider background border top for this widget. */
    default int getNineSliceSliderBackgroundBorderTop_Konkrete() {
        return getNineSliceSliderBackgroundBorderY_Konkrete();
    }

    /** Sets nine slice slider background border right for this widget. */
    default void setNineSliceSliderBackgroundBorderRight_Konkrete(int borderRight) {
        setNineSliceSliderBackgroundBorderX_Konkrete(borderRight);
    }

    /** Returns nine slice slider background border right for this widget. */
    default int getNineSliceSliderBackgroundBorderRight_Konkrete() {
        return getNineSliceSliderBackgroundBorderX_Konkrete();
    }

    /** Sets nine slice slider background border bottom for this widget. */
    default void setNineSliceSliderBackgroundBorderBottom_Konkrete(int borderBottom) {
        setNineSliceSliderBackgroundBorderY_Konkrete(borderBottom);
    }

    /** Returns nine slice slider background border bottom for this widget. */
    default int getNineSliceSliderBackgroundBorderBottom_Konkrete() {
        return getNineSliceSliderBackgroundBorderY_Konkrete();
    }

    /** Sets nine slice slider background border left for this widget. */
    default void setNineSliceSliderBackgroundBorderLeft_Konkrete(int borderLeft) {
        setNineSliceSliderBackgroundBorderX_Konkrete(borderLeft);
    }

    /** Returns nine slice slider background border left for this widget. */
    default int getNineSliceSliderBackgroundBorderLeft_Konkrete() {
        return getNineSliceSliderBackgroundBorderX_Konkrete();
    }

    /** Sets nine slice custom slider handle for this widget. */
    void setNineSliceCustomSliderHandle_Konkrete(boolean nineSlice);

    /** Returns whether nine slice custom slider handle. */
    boolean isNineSliceCustomSliderHandle_Konkrete();

    /** Sets nine slice slider handle border x for this widget. */
    void setNineSliceSliderHandleBorderX_Konkrete(int borderX);

    /** Returns nine slice slider handle border x for this widget. */
    int getNineSliceSliderHandleBorderX_Konkrete();

    /** Sets nine slice slider handle border y for this widget. */
    void setNineSliceSliderHandleBorderY_Konkrete(int borderY);

    /** Returns nine slice slider handle border y for this widget. */
    int getNineSliceSliderHandleBorderY_Konkrete();

    /** Sets nine slice slider handle border top for this widget. */
    default void setNineSliceSliderHandleBorderTop_Konkrete(int borderTop) {
        setNineSliceSliderHandleBorderY_Konkrete(borderTop);
    }

    /** Returns nine slice slider handle border top for this widget. */
    default int getNineSliceSliderHandleBorderTop_Konkrete() {
        return getNineSliceSliderHandleBorderY_Konkrete();
    }

    /** Sets nine slice slider handle border right for this widget. */
    default void setNineSliceSliderHandleBorderRight_Konkrete(int borderRight) {
        setNineSliceSliderHandleBorderX_Konkrete(borderRight);
    }

    /** Returns nine slice slider handle border right for this widget. */
    default int getNineSliceSliderHandleBorderRight_Konkrete() {
        return getNineSliceSliderHandleBorderX_Konkrete();
    }

    /** Sets nine slice slider handle border bottom for this widget. */
    default void setNineSliceSliderHandleBorderBottom_Konkrete(int borderBottom) {
        setNineSliceSliderHandleBorderY_Konkrete(borderBottom);
    }

    /** Returns nine slice slider handle border bottom for this widget. */
    default int getNineSliceSliderHandleBorderBottom_Konkrete() {
        return getNineSliceSliderHandleBorderY_Konkrete();
    }

    /** Sets nine slice slider handle border left for this widget. */
    default void setNineSliceSliderHandleBorderLeft_Konkrete(int borderLeft) {
        setNineSliceSliderHandleBorderX_Konkrete(borderLeft);
    }

    /** Returns nine slice slider handle border left for this widget. */
    default int getNineSliceSliderHandleBorderLeft_Konkrete() {
        return getNineSliceSliderHandleBorderX_Konkrete();
    }

    /** Sets custom slider background normal for this widget. */
    void setCustomSliderBackgroundNormalKonkrete(@Nullable RenderableResource background);

    /** Returns custom slider background normal for this widget. */
    @Nullable
    RenderableResource getCustomSliderBackgroundNormalKonkrete();

    /** Sets custom slider background highlighted for this widget. */
    void setCustomSliderBackgroundHighlightedKonkrete(@Nullable RenderableResource background);

    /** Returns custom slider background highlighted for this widget. */
    @Nullable
    RenderableResource getCustomSliderBackgroundHighlightedKonkrete();

    /**
     * Returns if the slider should render its Vanilla background (true) or not (false).
     */
    default boolean renderSliderBackgroundKonkrete(GuiGraphicsExtractor graphics, AbstractSliderButton widget, boolean canChangeValue) {
        Identifier location = null;
        RenderableResource texture = null;
        if (widget.isFocused() && !canChangeValue) {
            if (this.getCustomSliderBackgroundNormalKonkrete() instanceof PlayableResource p) p.pause();
            if (this.getCustomSliderBackgroundHighlightedKonkrete() != null) {
                if (this.getCustomSliderBackgroundHighlightedKonkrete() instanceof PlayableResource p) p.play();
                texture = this.getCustomSliderBackgroundHighlightedKonkrete();
                location = this.getCustomSliderBackgroundHighlightedKonkrete().getResourceLocation();
            }
        } else {
            if (this.getCustomSliderBackgroundHighlightedKonkrete() instanceof PlayableResource p) p.pause();
            if (this.getCustomSliderBackgroundNormalKonkrete() != null) {
                if (this.getCustomSliderBackgroundNormalKonkrete() instanceof PlayableResource p) p.play();
                texture = this.getCustomSliderBackgroundNormalKonkrete();
                location = this.getCustomSliderBackgroundNormalKonkrete().getResourceLocation();
            }
        }
        if (location != null) {
            de.keksuccino.konkrete.util.rendering.RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, ((AccessorMixinAbstractWidget)this).get_alpha_Konkrete());
            if (this.isNineSliceCustomSliderBackground_Konkrete()) {
                RenderingUtils.blitNineSlicedTexture(graphics, location, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), texture.getWidth(), texture.getHeight(),
                        this.getNineSliceSliderBackgroundBorderTop_Konkrete(), this.getNineSliceSliderBackgroundBorderRight_Konkrete(),
                        this.getNineSliceSliderBackgroundBorderBottom_Konkrete(), this.getNineSliceSliderBackgroundBorderLeft_Konkrete());
            } else {
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, location, widget.getX(), widget.getY(), 0.0F, 0.0F, widget.getWidth(), widget.getHeight(), widget.getWidth(), widget.getHeight());
            }
            RenderingUtils.resetShaderColor(graphics);
            return false;
        }
        return true;
    }

}
