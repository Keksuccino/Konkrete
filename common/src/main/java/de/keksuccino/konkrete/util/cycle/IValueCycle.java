package de.keksuccino.konkrete.util.cycle;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.function.Consumer;

/** Cycles through ivalue cycle values. */
@SuppressWarnings("unused")
public interface IValueCycle<T> {

    /** Returns the ordered cycle values. */
    List<T> getValues();

    /** Removes a value and returns this cycle. */
    IValueCycle<T> removeValue(@NotNull T value);

    /**
     * Returns the current value.
     */
    @NotNull
    T current();

    /**
     * Sets the next value as current value and returns it.
     */
    @NotNull
    T next();

    /** Sets the current value and optionally notifies listeners. */
    IValueCycle<T> setCurrentValue(T value, boolean notifyListeners);

    /** Sets the current value and notifies listeners. */
    IValueCycle<T> setCurrentValue(T value);

    /** Sets the current index and optionally notifies listeners. */
    IValueCycle<T> setCurrentValueByIndex(int index, boolean notifyListeners);

    /** Sets the current index and notifies listeners. */
    IValueCycle<T> setCurrentValueByIndex(int index);

    /** Registers a listener for current-value changes. */
    IValueCycle<T> addCycleListener(@NotNull Consumer<T> listener);

    /** Removes all current-value listeners. */
    IValueCycle<T> clearCycleListeners();

}
