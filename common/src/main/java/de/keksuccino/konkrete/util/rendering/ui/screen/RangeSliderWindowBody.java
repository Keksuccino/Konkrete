package de.keksuccino.konkrete.util.rendering.ui.screen;

import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.widget.slider.RangeSlider;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/** Implements the interactive window body for range slider. */
public class RangeSliderWindowBody extends PiPWindowBody {

    /** Width in GUI units for PiP window. */
    public static final int PIP_WINDOW_WIDTH = 420;
    /** Height in GUI units for PiP window. */
    public static final int PIP_WINDOW_HEIGHT = 220;

    /** Receives each value produced while the slider moves. */
    @NotNull
    protected final Consumer<Double> onValueUpdate;
    /** Receives the accepted value when the window completes. */
    @NotNull
    protected final Consumer<Double> onDone;
    /** Receives the last value when the window is cancelled. */
    @NotNull
    protected final Consumer<Double> onCancel;
    /** Lowest model value represented by the slider. */
    protected final double minValue;
    /** Highest model value represented by the slider. */
    protected final double maxValue;
    /** Value selected when the window opens. */
    protected final double presetValue;
    /** Latest mapped slider value delivered to the callbacks. */
    protected double currentValue;
    /** Formats the current numeric value as the slider label. */
    @NotNull
    protected final ConsumingSupplier<Double, Component> labelSupplier;
    /** Scroll component displaying slider. */
    protected RangeSlider slider;
    /** Button that accepts the selected value. */
    protected ExtendedButton doneButton;
    /** Button that cancels the current operation. */
    protected ExtendedButton cancelButton;

    /** Initializes a bounded numeric slider and update, completion, and cancellation callbacks. */
    public RangeSliderWindowBody(double minValue, double maxValue, double valuePreset, @NotNull ConsumingSupplier<Double, Component> labelSupplier, @NotNull Consumer<Double> onValueUpdate, @NotNull Consumer<Double> onDone, @NotNull Consumer<Double> onCancel) {
        super(Component.empty());
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.presetValue = valuePreset;
        this.currentValue = valuePreset;
        this.labelSupplier = labelSupplier;
        this.onValueUpdate = onValueUpdate;
        this.onDone = onDone;
        this.onCancel = onCancel;
    }

    /** Initializes resources required by this range slider window body. */
    @Override
    protected void init() {

        int sliderWidth = Math.max(160, this.width - 80);
        int sliderX = (this.width - sliderWidth) / 2;
        int sliderY = (this.height / 2) - 20;

        this.slider = new RangeSlider(sliderX, sliderY, sliderWidth, 20, Component.empty(), this.minValue, this.maxValue, this.currentValue);
        this.slider.setRoundingDecimalPlace(2);
        this.slider.setLabelSupplier(consumes -> this.labelSupplier.get(((RangeSlider)consumes).getRangeValue()));
        this.slider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> {
            this.currentValue = ((RangeSlider)slider1).getRangeValue();
            this.onValueUpdate.accept(this.currentValue);
        });
        UIBase.applyDefaultWidgetSkinTo(this.slider, UIBase.shouldBlur());
        this.addRenderableWidget(this.slider);

        this.cancelButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) - 5 - 100, this.height - 40, 100, 20, Component.translatable("konkrete.common_components.cancel"), button -> {
            this.onCancel.accept(this.presetValue);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

        this.doneButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) + 5, this.height - 40, 100, 20, Component.translatable("konkrete.common_components.done"), button -> {
            this.onDone.accept(this.currentValue);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());

    }

    /** Handles window closed externally for this range slider window body. */
    @Override
    public void onWindowClosedExternally() {
        this.onCancel.accept(this.presetValue);
    }

}
