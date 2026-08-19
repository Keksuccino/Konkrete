package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import de.keksuccino.konkrete.util.ScreenUtils;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Opens the reusable AFMA creator screen without depending on a consuming mod's screen registry. */
public final class AfmaCreatorEntryGate {

    private AfmaCreatorEntryGate() {
    }

    /** Opens a new creator state and returns to the supplied parent when the screen closes. */
    public static void open(@NotNull Screen parentScreen) {
        open(parentScreen, new AfmaCreatorState());
    }

    /** Transfers the supplied creator state to a screen that closes it when removed, then returns to the supplied parent. */
    public static void open(@NotNull Screen parentScreen, @NotNull AfmaCreatorState state) {
        ScreenUtils.setScreen(new AfmaCreatorScreen(Objects.requireNonNull(parentScreen, "parentScreen"), Objects.requireNonNull(state, "state")));
    }

}
