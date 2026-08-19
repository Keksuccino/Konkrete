package de.keksuccino.konkrete.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Mutable nullable reference wrapper for values shared with callbacks. */
public class ObjectHolder<T> {

    private T object;

    /** Wraps the supplied nullable reference. */
    @NotNull
    public static <T> ObjectHolder<T> of(@Nullable T object) {
        return new ObjectHolder<>(object);
    }

    /** Initializes the wrapper with a nullable reference. */
    protected ObjectHolder(@Nullable T object) {
        this.object = object;
    }

    /** Returns the current nullable reference. */
    public T get() {
        return this.object;
    }

    /** Replaces the current reference, including with {@code null}. */
    public void set(@Nullable T object) {
        this.object = object;
    }

}
