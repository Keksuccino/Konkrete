package de.keksuccino.konkrete.util.rendering.ui.widget.slider;

import de.keksuccino.konkrete.util.MathUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/** Displays and edits range through a draggable slider. */
@SuppressWarnings("all")
public class RangeSlider extends AbstractExtendedSlider {

    /** Model value mapped from normalized slider position zero. */
    protected double minRangeValue;
    /** Model value mapped from normalized slider position one. */
    protected double maxRangeValue;
    /** Whether the current value is formatted without a fractional part. */
    protected boolean showAsInteger = false;
    /** Decimal places retained in mapped values, or a negative value to disable rounding. */
    protected int roundingDecimalPlace = 2;

    /** Maps a normalized slider across the supplied numeric range and preset. */
    public RangeSlider(int x, int y, int width, int height, Component label, double minRangeValue, double maxRangeValue, double preSelectedRangeValue) {
        super(x, y, width, height, label, 0);
        this.minRangeValue = minRangeValue;
        this.maxRangeValue = maxRangeValue;
        this.setRangeValue(preSelectedRangeValue);
    }

    /** Formats the mapped range value using the configured integer or decimal mode. */
    @Override
    public @NotNull String getValueDisplayText() {
        if (this.showAsInteger()) return "" + this.getIntegerRangeValue();
        return "" + this.getRangeValue();
    }

    /** Returns integer range value. */
    public int getIntegerRangeValue() {
        return (int) this.getRangeValue();
    }

    /** Returns range value. */
    public double getRangeValue() {
        double d = Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), this.minRangeValue, this.maxRangeValue);
        if (this.roundingDecimalPlace < 0) return d;
        return MathUtils.round(d, this.roundingDecimalPlace);
    }

    /** Sets range value for this range slider. */
    public RangeSlider setRangeValue(double rangeValue) {
        rangeValue = Math.min(this.maxRangeValue, Math.max(this.minRangeValue, rangeValue));
        if (rangeValue == this.maxRangeValue) {
            this.setValue(1.0D);
        } else if (rangeValue == this.minRangeValue) {
            this.setValue(0.0D);
        } else {
            this.setValue(((Mth.clamp(rangeValue, this.minRangeValue, this.maxRangeValue) - this.minRangeValue) / (this.maxRangeValue - this.minRangeValue)));
        }
        return this;
    }

    /** Returns min range value. */
    public double getMinRangeValue() {
        return this.minRangeValue;
    }

    /** Sets min range value for this range slider. */
    public RangeSlider setMinRangeValue(double minRangeValue) {
        this.minRangeValue = minRangeValue;
        return this;
    }

    /** Returns max range value. */
    public double getMaxRangeValue() {
        return this.maxRangeValue;
    }

    /** Sets max range value for this range slider. */
    public RangeSlider setMaxRangeValue(double maxRangeValue) {
        this.maxRangeValue = maxRangeValue;
        return this;
    }

    /** Opens as integer. */
    public boolean showAsInteger() {
        return this.showAsInteger;
    }

    /** Sets show as integer for this range slider. */
    public RangeSlider setShowAsInteger(boolean showAsInteger) {
        this.showAsInteger = showAsInteger;
        return this;
    }

    /** Returns rounding decimal place. */
    public int getRoundingDecimalPlace() {
        return this.roundingDecimalPlace;
    }

    /** Sets rounding decimal place for this range slider. */
    public RangeSlider setRoundingDecimalPlace(int decimalPlace) {
        this.roundingDecimalPlace = decimalPlace;
        return this;
    }

}
