package de.keksuccino.konkrete.util;

import java.util.Objects;

/**
 * A consumer that accepts three input arguments.
 */
@FunctionalInterface
public interface TripleConsumer<F, S, T> {

    /** Consumes the three supplied values. */
    void accept(F first, S second, T third);

    /** Returns a consumer that invokes this consumer followed by the supplied consumer. */
    default TripleConsumer<F, S, T> andThen(TripleConsumer<? super F, ? super S, ? super T> after) {
        Objects.requireNonNull(after, "after");
        return (first, second, third) -> {
            accept(first, second, third);
            after.accept(first, second, third);
        };
    }

}
