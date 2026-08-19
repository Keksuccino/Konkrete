package de.keksuccino.konkrete.util;

/**
 * This supplier consumes (first class parameter) an object and returns (second class parameter) another one.
 */
@FunctionalInterface
public interface ConsumingSupplier<C, R> {

    /** Consumes the input and returns the computed value. */
    R get(C consumes);

}
