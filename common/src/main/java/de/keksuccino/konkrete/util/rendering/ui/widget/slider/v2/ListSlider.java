package de.keksuccino.konkrete.util.rendering.ui.widget.slider.v2;

import de.keksuccino.konkrete.util.ConsumingSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/** Displays and edits list through a draggable slider. */
@SuppressWarnings("all")
public class ListSlider<T> extends AbstractExtendedSlider {

    /** Values selectable by the slider in index order. */
    @NotNull
    protected List<T> listValues;
    /** Converts the selected list value into its displayed label. */
    @NotNull
    protected ConsumingSupplier<T, String> listValueStringSupplier = consumes -> (consumes != null) ? consumes.toString() : "";

    /** Initializes a slider over a non-empty value list and selected index. */
    public ListSlider(int x, int y, int width, int height, Component label, @NotNull List<T> listValues, int preSelectedIndex) {
        super(x, y, width, height, label, 0);
        this.listValues = new ArrayList<>(listValues);
        if (this.listValues.size() < 2) {
            throw new RuntimeException("Not enough list values! At least 2 list values needed!");
        }
        this.setSelectedIndex(preSelectedIndex);
    }

    /** Formats the selected list value with the configured value-string supplier. */
    @Override
    public @NotNull String getValueDisplayText() {
        return this.listValueStringSupplier.get(this.getSelectedListValue());
    }

    /** Returns selected list value. */
    @NotNull
    public T getSelectedListValue() {
        return this.listValues.get(Math.min(this.listValues.size()-1, Math.max(0, this.getSelectedIndex())));
    }

    /** Returns selected index. */
    public int getSelectedIndex() {
        if (!this.listValues.isEmpty()) {
            double minValue = 0;
            double maxValue = this.listValues.size()-1;
            return (int) Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), minValue, maxValue);
        }
        return 0;
    }

    /** Sets selected index for this list slider. */
    public ListSlider<T> setSelectedIndex(double index) {
        if (!this.listValues.isEmpty()) {
            double minValue = 0;
            double maxValue = this.listValues.size()-1;
            this.setValue(((Mth.clamp(index, minValue, maxValue) - minValue) / (maxValue - minValue)));
        }
        return this;
    }

    /** Sets list value string supplier for this list slider. */
    public void setListValueStringSupplier(@NotNull ConsumingSupplier<T, String> supplier) {
        this.listValueStringSupplier = supplier;
    }

}
