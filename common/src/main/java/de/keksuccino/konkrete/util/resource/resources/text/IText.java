package de.keksuccino.konkrete.util.resource.resources.text;

import de.keksuccino.konkrete.util.resource.Resource;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Exposes immutable lines from a closeable text resource. */
public interface IText extends Resource {

    /**
     * Due to the nature of some types of {@link IText}, the return value of this method could change
     * asynchronously, so make sure to always cache the value before working with it.
     */
    @Nullable
    List<String> getTextLines();

}
