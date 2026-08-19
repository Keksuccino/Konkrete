package de.keksuccino.konkrete.util;

import org.jetbrains.annotations.NotNull;

/** A mutable three-value container. */
public class Trio<F, S, T> {

    /** Mutable first value. */
    protected F first;
    /** Mutable second value. */
    protected S second;
    /** Mutable third value. */
    protected T third;

    /** Creates a trio preserving all supplied references. */
    @NotNull
    public static <F, S, T> Trio<F, S, T> of(F first, S second, T third) {
        return new Trio<>(first, second, third);
    }

    /** Initializes all three mutable values. */
    protected Trio(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /** Returns the first value. */
    public F getFirst() {
        return this.first;
    }

    /** Replaces the first value. */
    public void setFirst(F first) {
        this.first = first;
    }

    /** Returns the second value. */
    public S getSecond() {
        return this.second;
    }

    /** Replaces the second value. */
    public void setSecond(S second) {
        this.second = second;
    }

    /** Returns the third value. */
    public T getThird() {
        return this.third;
    }

    /** Replaces the third value. */
    public void setThird(T third) {
        this.third = third;
    }

}
