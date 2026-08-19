package de.keksuccino.konkrete.util;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Utility methods for constructing values and evaluating collections. */
@SuppressWarnings("unused")
public class ObjectUtils {

    /** Invokes a non-null builder once and returns its value. */
    public static <T> T build(@NotNull Supplier<T> builder) {
        return builder.get();
    }

    /** Returns whether the predicate accepts every object; empty lists return {@code true}. */
    public static <T> boolean isTrueForAll(List<T> objects, ConsumingSupplier<T, Boolean> checkFor) {
        for (T object : objects) {
            if (!checkFor.get(object)) return false;
        }
        return true;
    }

    /** Returns whether the predicate rejects every object; empty lists return {@code true}. */
    public static <T> boolean isFalseForAll(List<T> objects, ConsumingSupplier<T, Boolean> checkFor) {
        for (T object : objects) {
            if (checkFor.get(object)) return false;
        }
        return true;
    }

    /** Returns whether the predicate produces one uniform result for all objects. */
    public static <T> boolean isTrueOrFalseForAll(List<T> objects, ConsumingSupplier<T, Boolean> checkFor) {
        return isTrueForAll(objects, checkFor) || isFalseForAll(objects, checkFor);
    }


    /**
     * Gets the same field/method value of all given objects and returns them as a list.
     */
    @NotNull
    public static <O, F> List<F> getOfAll(Class<? extends F> getType, List<O> objects, ConsumingSupplier<O, F> getter) {
        List<F> l = new ArrayList<>();
        for (O obj : objects) {
            l.add(getter.get(obj));
        }
        return l;
    }

    /** Applies an untyped getter to every object in encounter order. */
    @NotNull
    public static List<Object> getOfAllUnsafe(List<Object> objects, ConsumingSupplier<Object, Object> getter) {
        return getOfAll(Object.class, objects, getter);
    }

}
