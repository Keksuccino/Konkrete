package de.keksuccino.konkrete.placeholder.placeholders.gui;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

/** Maps an active screen to a stable identifier without imposing an editor or layout system. */
@FunctionalInterface
public interface ScreenIdentifierProvider {

    /** Returns the identifier exposed by {@code screenid}; the result must not be {@code null}. */
    @NotNull String identify(@NotNull Screen screen);
}
