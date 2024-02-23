package de.keksuccino.konkrete.gui.content.scrollarea;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Deprecated(forRemoval = true)
public class LegacyScrollAreaCompat {

    private static final List<MouseScrollCallback> SCROLL_CALLBACKS = new ArrayList<>();

    public static void notifyCallbacks(float scrollDelta) {
        SCROLL_CALLBACKS.forEach(callback -> callback.scroll(scrollDelta));
    }

    protected static void registerScrollCallback(@NotNull MouseScrollCallback callback) {
        SCROLL_CALLBACKS.add(Objects.requireNonNull(callback));
    }

    @FunctionalInterface
    protected interface MouseScrollCallback {
        void scroll(float scrollDelta);
    }

}
