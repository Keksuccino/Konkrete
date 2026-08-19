package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads max player oxygen state from the current vanilla world/player for {@code max_player_oxygen}. */
public class MaxPlayerOxygenPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code max_player_oxygen} placeholder. */
    public MaxPlayerOxygenPlaceholder() {
        super("max_player_oxygen");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getMaxAirSupply();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.max_player_oxygen";
    }

}
