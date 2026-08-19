package de.keksuccino.konkrete.util.cycle;

import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.enums.LocalizedEnum;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Adds a localized cycle label and style to non-empty localized-enum values. */
@SuppressWarnings("unused")
public class LocalizedEnumValueCycle<E extends LocalizedEnum<?>> extends ValueCycle<E> implements ILocalizedValueCycle<E> {

    /** Translation key whose argument is the localized current value. */
    protected String cycleLocalizationKey;
    /** Supplies the outer component style from the current enum value. */
    protected ConsumingSupplier<E, Style> cycleStyle = consumes -> Style.EMPTY;

    /** Creates a cycle from an array. */
    @SafeVarargs
    public static <E extends LocalizedEnum<?>> LocalizedEnumValueCycle<E> ofArray(@NotNull String cycleLocalizationKey, @NotNull E... values) {
        Objects.requireNonNull(values);
        List<E> valueList = Arrays.asList(values);
        if (valueList.isEmpty()) {
            throw new IllegalArgumentException("Failed to create LocalizedValueCycle! Value list size too small (empty)!");
        }
        LocalizedEnumValueCycle<E> valueCycle = new LocalizedEnumValueCycle<>(cycleLocalizationKey);
        valueCycle.values.addAll(valueList);
        return valueCycle;
    }

    /** Creates a cycle from a non-empty list while preserving encounter order. */
    @SuppressWarnings("all")
    public static <E extends LocalizedEnum<?>> LocalizedEnumValueCycle<E> ofList(@NotNull String cycleLocalizationKey, @NotNull List<E> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) throw new IllegalArgumentException("Failed to create LocalizedValueCycle! Value list size too small (empty)!");
        LocalizedEnumValueCycle<E> valueCycle = new LocalizedEnumValueCycle<>(cycleLocalizationKey);
        valueCycle.values.addAll(values);
        return valueCycle;
    }

    /** Stores the translation key before factory population of values. */
    protected LocalizedEnumValueCycle(String cycleLocalizationKey) {
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
        return this.current().getValueComponent();
    }

    /** Sets cycle component style supplier. */
    public LocalizedEnumValueCycle<E> setCycleComponentStyleSupplier(@NotNull ConsumingSupplier<E, Style> supplier) {
        this.cycleStyle = supplier;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public LocalizedEnumValueCycle<E> addCycleListener(@NotNull Consumer<E> listener) {
        return (LocalizedEnumValueCycle<E>) super.addCycleListener(listener);
    }

}
