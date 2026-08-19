package de.keksuccino.konkrete.util.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param <E> The enum type.
 */
public interface NamedEnum<E> {

    /** Returns this enum value's stable serialized name. */
    @NotNull
    String getName();

    /** Returns all values in this enum family. */
    @NotNull
    E[] getValues();

    /** Returns the value matching a stable name, or {@code null} when absent. */
    @Nullable
    E getByNameInternal(@NotNull String name);

}
