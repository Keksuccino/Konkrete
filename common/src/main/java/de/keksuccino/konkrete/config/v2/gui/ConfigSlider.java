package de.keksuccino.konkrete.config.v2.gui;

import de.keksuccino.konkrete.config.v2.ConfigValue;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Slider widget used by {@link ConfigScreen}. Integer and float ranges snap mouse and keyboard input
 * to explicit steps before a value is stored, so the visual handle and config can never diverge.
 */
public final class ConfigSlider<T> extends AbstractSliderButton {

    private final ConfigValue<T> option;
    private final SliderRange<T> range;
    private final Function<T, Component> messageFactory;
    private final BiConsumer<ConfigSlider<T>, T> valueChanged;
    private final Runnable controlsChanged;

    private ConfigSlider(@NotNull ConfigValue<T> option, @NotNull SliderRange<T> range, @NotNull Function<T, Component> messageFactory, @NotNull BiConsumer<ConfigSlider<T>, T> valueChanged, @NotNull Runnable controlsChanged, int width) {
        super(0, 0, width, DEFAULT_HEIGHT, CommonComponents.EMPTY, range.toSliderValue(option.getValue(), option.getDefaultValue()));
        this.option = Objects.requireNonNull(option);
        this.range = Objects.requireNonNull(range);
        this.messageFactory = Objects.requireNonNull(messageFactory);
        this.valueChanged = Objects.requireNonNull(valueChanged);
        this.controlsChanged = Objects.requireNonNull(controlsChanged);
        this.updateMessage();
    }

    @NotNull
    static ConfigSlider<Integer> integer(@NotNull ConfigValue<Integer> option, int minimum, int maximum, int step, @NotNull Function<Integer, Component> messageFactory, @NotNull BiConsumer<ConfigSlider<Integer>, Integer> valueChanged, @NotNull Runnable controlsChanged, int width) {
        return new ConfigSlider<>(option, new IntegerSliderRange(minimum, maximum, step), messageFactory, valueChanged, controlsChanged, width);
    }

    @NotNull
    static ConfigSlider<Float> floatingPoint(@NotNull ConfigValue<Float> option, float minimum, float maximum, float step, @NotNull Function<Float, Component> messageFactory, @NotNull BiConsumer<ConfigSlider<Float>, Float> valueChanged, @NotNull Runnable controlsChanged, int width) {
        return new ConfigSlider<>(option, new FloatSliderRange(minimum, maximum, step), messageFactory, valueChanged, controlsChanged, width);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(this.messageFactory.apply(this.getSelectedValue()));
    }

    @Override
    protected void setValue(double newValue) {
        T snappedValue = this.range.fromSliderValue(newValue, this.option.getDefaultValue());
        super.setValue(this.range.toSliderValue(snappedValue, this.option.getDefaultValue()));
    }

    @Override
    protected void applyValue() {
        T selectedValue = this.getSelectedValue();
        if (!Objects.equals(this.option.getValue(), selectedValue)) {
            this.option.setValue(selectedValue);
            if (Objects.equals(this.option.getValue(), selectedValue)) {
                this.valueChanged.accept(this, selectedValue);
            } else {
                // ConfigValue rolls back failed writes. Keep the slider handle aligned with the restored value.
                this.refreshFromOption();
            }
        }
        this.controlsChanged.run();
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (!this.canChangeValue || !event.isLeft() && !event.isRight()) return super.keyPressed(event);
        double sliderStep = 1.0D / this.range.stepCount();
        this.setValue(this.value + (event.isLeft() ? -sliderStep : sliderStep));
        return true;
    }

    void refreshFromOption() {
        this.value = this.range.toSliderValue(this.option.getValue(), this.option.getDefaultValue());
        this.updateMessage();
    }

    static int sliderValueToInteger(double sliderValue, int minimum, int maximum, int step, int fallback) {
        return new IntegerSliderRange(minimum, maximum, step).fromSliderValue(sliderValue, fallback);
    }

    static double integerToSliderValue(int value, int minimum, int maximum, int step, int fallback) {
        return new IntegerSliderRange(minimum, maximum, step).toSliderValue(value, fallback);
    }

    static double snapIntegerSliderValue(double sliderValue, int minimum, int maximum, int step, int fallback) {
        IntegerSliderRange range = new IntegerSliderRange(minimum, maximum, step);
        return range.toSliderValue(range.fromSliderValue(sliderValue, fallback), fallback);
    }

    static float sliderValueToFloat(double sliderValue, float minimum, float maximum, float step, float fallback) {
        return new FloatSliderRange(minimum, maximum, step).fromSliderValue(sliderValue, fallback);
    }

    static double floatToSliderValue(float value, float minimum, float maximum, float step, float fallback) {
        return new FloatSliderRange(minimum, maximum, step).toSliderValue(value, fallback);
    }

    static double snapFloatSliderValue(double sliderValue, float minimum, float maximum, float step, float fallback) {
        FloatSliderRange range = new FloatSliderRange(minimum, maximum, step);
        return range.toSliderValue(range.fromSliderValue(sliderValue, fallback), fallback);
    }

    @NotNull
    private T getSelectedValue() {
        return this.range.fromSliderValue(this.value, this.option.getDefaultValue());
    }

    private interface SliderRange<T> {

        int stepCount();

        double toSliderValue(@NotNull T value, @NotNull T fallback);

        @NotNull
        T fromSliderValue(double sliderValue, @NotNull T fallback);

    }

    private record IntegerSliderRange(int minimum, int maximum, int step) implements SliderRange<Integer> {

        private IntegerSliderRange {
            if (step <= 0) throw new IllegalArgumentException("Integer slider step must be greater than zero.");
            if (minimum >= maximum) throw new IllegalArgumentException("Integer slider maximum must be greater than its minimum.");
            long span = (long) maximum - minimum;
            if (span % step != 0L) throw new IllegalArgumentException("Integer slider range must divide evenly by its step.");
            if (span / step > Integer.MAX_VALUE) throw new IllegalArgumentException("Integer slider range contains too many steps.");
        }

        @Override
        public int stepCount() {
            return (int) (((long) this.maximum - this.minimum) / this.step);
        }

        @Override
        public double toSliderValue(@NotNull Integer value, @NotNull Integer fallback) {
            int normalizedValue = this.normalize(value);
            return ((long) normalizedValue - this.minimum) / (double) ((long) this.maximum - this.minimum);
        }

        @NotNull
        @Override
        public Integer fromSliderValue(double sliderValue, @NotNull Integer fallback) {
            double safeSliderValue = Double.isFinite(sliderValue) ? sliderValue : this.toSliderValue(fallback, fallback);
            int stepIndex = (int) Math.round(Math.max(0.0D, Math.min(1.0D, safeSliderValue)) * this.stepCount());
            return (int) ((long) this.minimum + (long) stepIndex * this.step);
        }

        private int normalize(int value) {
            int clampedValue = Math.max(this.minimum, Math.min(this.maximum, value));
            int stepIndex = (int) Math.round(((long) clampedValue - this.minimum) / (double) this.step);
            return (int) ((long) this.minimum + (long) stepIndex * this.step);
        }

    }

    private record FloatSliderRange(float minimum, float maximum, float step, int stepCount) implements SliderRange<Float> {

        private FloatSliderRange(float minimum, float maximum, float step) {
            this(minimum, maximum, step, calculateStepCount(minimum, maximum, step));
        }

        private FloatSliderRange {
            if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || !Float.isFinite(step)) throw new IllegalArgumentException("Float slider range values must be finite.");
            if (minimum >= maximum) throw new IllegalArgumentException("Float slider maximum must be greater than its minimum.");
            if (step <= 0.0F) throw new IllegalArgumentException("Float slider step must be greater than zero.");
        }

        @Override
        public double toSliderValue(@NotNull Float value, @NotNull Float fallback) {
            float normalizedValue = this.normalize(value, fallback);
            return ((double) normalizedValue - this.minimum) / ((double) this.maximum - this.minimum);
        }

        @NotNull
        @Override
        public Float fromSliderValue(double sliderValue, @NotNull Float fallback) {
            double safeSliderValue = Double.isFinite(sliderValue) ? sliderValue : this.toSliderValue(fallback, fallback);
            int stepIndex = (int) Math.round(Math.max(0.0D, Math.min(1.0D, safeSliderValue)) * this.stepCount);
            return this.valueAt(stepIndex);
        }

        private float normalize(float value, float fallback) {
            float safeFallback = Float.isFinite(fallback) ? fallback : this.minimum;
            float safeValue = Float.isFinite(value) ? value : safeFallback;
            double sliderValue = ((double) Math.max(this.minimum, Math.min(this.maximum, safeValue)) - this.minimum) / ((double) this.maximum - this.minimum);
            return this.valueAt((int) Math.round(sliderValue * this.stepCount));
        }

        private float valueAt(int stepIndex) {
            return (float) (this.minimum + stepIndex * ((double) this.maximum - this.minimum) / this.stepCount);
        }

        private static int calculateStepCount(float minimum, float maximum, float step) {
            if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || !Float.isFinite(step) || minimum >= maximum || step <= 0.0F) return 0;
            double rawStepCount = ((double) maximum - minimum) / step;
            long roundedStepCount = Math.round(rawStepCount);
            if (roundedStepCount <= 0L || roundedStepCount > Integer.MAX_VALUE || Math.abs(rawStepCount - roundedStepCount) > 0.00001D * Math.max(1.0D, rawStepCount)) {
                throw new IllegalArgumentException("Float slider range must divide evenly by its step.");
            }
            return (int) roundedStepCount;
        }

    }

}
