package de.keksuccino.konkrete.util.cycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Maintains a non-empty ordered value list with wraparound navigation and change listeners. */
public class ValueCycle<T> implements IValueCycle<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Ordered values available for selection. */
    protected List<T> values = new ArrayList<>();
    /** Index of the current value. */
    protected int currentIndex = 0;
    /** Listeners notified in registration order after an explicit or navigated change. */
    protected List<Consumer<T>> cycleListeners = new ArrayList<>();

    /**
     * A value toggle.<br>
     * <b>The value list needs at least one entry!</b>
     */
    public static <T> ValueCycle<T> fromList(@NotNull List<T> values) {
        Objects.requireNonNull(values);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Failed to create ValueCycle! Value list size too small (empty)!");
        }
        ValueCycle<T> valueCycle = new ValueCycle<>();
        valueCycle.values.addAll(values);
        return valueCycle;
    }

    /**
     * A value toggle.<br>
     * <b>The value array needs at least one entry!</b>
     */
    @SafeVarargs
    public static <T> ValueCycle<T> fromArray(@NotNull T... values) {
        Objects.requireNonNull(values);
        return fromList(Arrays.asList(values));
    }

    /** Creates an empty instance for validated factory/subclass population. */
    protected ValueCycle() {
    }

    /** Returns a mutable copy of the ordered values. */
    public List<T> getValues() {
        return new ArrayList<>(this.values);
    }

    /** Removes a value when at least one other remains, then resets selection to index zero. */
    public ValueCycle<T> removeValue(@NotNull T value) {
        if (this.values.size() <= 1) {
            LOGGER.error("Unable to remove value! At least 1 value needed!");
            return this;
        }
        this.values.remove(value);
        this.setCurrentValueByIndex(0, false);
        return this;
    }

    /**
     * Returns the current value.
     */
    @NotNull
    public T current() {
        return this.values.get(this.currentIndex);
    }

    /**
     * Sets the next value as current value and returns it.
     */
    @NotNull
    public T next() {
        if (this.currentIndex >= this.values.size()-1) {
            this.currentIndex = 0;
        } else {
            this.currentIndex++;
        }
        this.notifyListeners();
        return this.current();
    }

    /** Selects a present value and optionally notifies listeners; absent values are ignored. */
    public ValueCycle<T> setCurrentValue(T value, boolean notifyListeners) {
        int i = this.values.indexOf(value);
        if (i != -1) {
            this.currentIndex = i;
            if (notifyListeners) this.notifyListeners();
        }
        return this;
    }

    /** Selects a present value and notifies listeners. */
    public ValueCycle<T> setCurrentValue(T value) {
        return this.setCurrentValue(value, true);
    }

    /** Selects a valid index and optionally notifies listeners; invalid indices are ignored. */
    public ValueCycle<T> setCurrentValueByIndex(int index, boolean notifyListeners) {
        if ((index >= 0) && (index < this.values.size())) {
            this.currentIndex = index;
            if (notifyListeners) this.notifyListeners();
        }
        return this;
    }

    /** Selects a valid index and notifies listeners. */
    public ValueCycle<T> setCurrentValueByIndex(int index) {
        return this.setCurrentValueByIndex(index, true);
    }

    /** Appends a non-null listener to notification order. */
    public ValueCycle<T> addCycleListener(@NotNull Consumer<T> listener) {
        this.cycleListeners.add(listener);
        return this;
    }

    /** Removes all listeners without changing values or selection. */
    public ValueCycle<T> clearCycleListeners() {
        this.cycleListeners.clear();
        return this;
    }

    /** Invokes listeners in registration order with the current value; failures propagate. */
    protected void notifyListeners() {
        for (Consumer<T> listener : this.cycleListeners) {
            listener.accept(this.current());
        }
    }

}
