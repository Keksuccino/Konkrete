package de.keksuccino.konkrete.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Interfaces annotated with {@link ClassExtender} get applied to classes via Mixin to add new methods and other features.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface ClassExtender {

    /** Lists interfaces that should be applied to the annotated type. */
    public Class<?>[] value();

}
