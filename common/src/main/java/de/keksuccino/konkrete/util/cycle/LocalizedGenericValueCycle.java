package de.keksuccino.konkrete.util.cycle;

import de.keksuccino.konkrete.util.ConsumingSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Adds configurable labels and styles to an arbitrary non-empty value cycle. */
@SuppressWarnings("unused")
public class LocalizedGenericValueCycle<T> extends ValueCycle<T> implements ILocalizedValueCycle<T> {

    /** Translation key whose argument is the current value component. */
    protected String cycleLocalizationKey;
    /** Supplies the outer component style from the current value. */
    protected ConsumingSupplier<T, Style> cycleStyle = consumes -> Style.EMPTY;
    /** Supplies the inner value style from the current value. */
    protected ConsumingSupplier<T, Style> valueStyle = consumes -> Style.EMPTY;
    /** Converts each current value to its literal label. */
    protected ConsumingSupplier<T, String> valueNameSupplier = Object::toString;

    /** Creates an instance from the supplied values. */
    @SafeVarargs
    public static <T> LocalizedGenericValueCycle<T> of(@NotNull String cycleLocalizationKey, @NotNull T... values) {
        Objects.requireNonNull(values);
        List<T> valueList = Arrays.asList(values);
        if (valueList.isEmpty()) {
            throw new IllegalArgumentException("Failed to create LocalizedGenericValueCycle! Value list size too small (empty)!");
        }
        LocalizedGenericValueCycle<T> valueCycle = new LocalizedGenericValueCycle<>(cycleLocalizationKey);
        valueCycle.values.addAll(valueList);
        return valueCycle;
    }

    /** Stores the translation key before factory population of values. */
    protected LocalizedGenericValueCycle(String cycleLocalizationKey) {
        this.cycleLocalizationKey = cycleLocalizationKey;
    }

    /** Returns the cycle localization key. */
    @NotNull
    public String getCycleLocalizationKey() {
        return this.cycleLocalizationKey;
    }

    /** Returns the cycle component. */
    public MutableComponent getCycleComponent() {
        return Component.translatable(this.getCycleLocalizationKey(), this.getCurrentValueComponent()).withStyle(this.cycleStyle.get(this.current()));
    }

    /** Returns the current value component. */
    public MutableComponent getCurrentValueComponent() {
        return Component.literal(this.valueNameSupplier.get(this.current())).withStyle(this.valueStyle.get(this.current()));
    }

    /** Sets value name supplier. */
    public LocalizedGenericValueCycle<T> setValueNameSupplier(@NotNull ConsumingSupplier<T, String> supplier) {
        this.valueNameSupplier = supplier;
        return this;
    }

    /** Sets cycle component style supplier. */
    public LocalizedGenericValueCycle<T> setCycleComponentStyleSupplier(@NotNull ConsumingSupplier<T, Style> supplier) {
        this.cycleStyle = supplier;
        return this;
    }

    /** Sets value component style supplier. */
    public LocalizedGenericValueCycle<T> setValueComponentStyleSupplier(@NotNull ConsumingSupplier<T, Style> supplier) {
        this.valueStyle = supplier;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public LocalizedGenericValueCycle<T> addCycleListener(@NotNull Consumer<T> listener) {
        return (LocalizedGenericValueCycle<T>) super.addCycleListener(listener);
    }

}
