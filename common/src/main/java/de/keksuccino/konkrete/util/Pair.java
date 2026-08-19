package de.keksuccino.konkrete.util;

import org.jetbrains.annotations.NotNull;

/** A mutable two-value container. */
public class Pair<F, S> {

    /** Mutable first value. */
    protected F key;
    /** Mutable second value. */
    protected S value;

    /** Creates a pair preserving both supplied references. */
    @NotNull
    public static <L, R> Pair<L, R> of(L key, R value) {
        return new Pair<>(key, value);
    }

    /** Initializes both mutable values. */
    protected Pair(F key, S value) {
        this.key = key;
        this.value = value;
    }

    /** Returns the first value. */
    public F getFirst() {
        return this.key;
    }

    /** Replaces the first value. */
    public void setFirst(F key) {
        this.key = key;
    }

    /** Returns the second value. */
    public S getSecond() {
        return this.value;
    }

    /** Replaces the second value. */
    public void setSecond(S value) {
        this.value = value;
    }

}
