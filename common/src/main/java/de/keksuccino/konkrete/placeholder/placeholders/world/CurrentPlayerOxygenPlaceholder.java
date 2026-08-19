package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player oxygen state from the current vanilla world/player for {@code current_player_oxygen}. */
public class CurrentPlayerOxygenPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code current_player_oxygen} placeholder. */
    public CurrentPlayerOxygenPlaceholder() {
        super("current_player_oxygen");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getAirSupply();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_oxygen";
    }

}
