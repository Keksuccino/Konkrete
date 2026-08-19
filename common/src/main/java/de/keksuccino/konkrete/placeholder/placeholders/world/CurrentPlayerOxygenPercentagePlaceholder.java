package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player oxygen percentage state from the current vanilla world/player for {@code current_player_oxygen_percent}. */
public class CurrentPlayerOxygenPercentagePlaceholder extends AbstractWorldPercentagePlaceholder {

    /** Creates the {@code current_player_oxygen_percent} placeholder. */
    public CurrentPlayerOxygenPercentagePlaceholder() {
        super("current_player_oxygen_percent");
    }

    @Override
    protected float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getAirSupply();
    }

    @Override
    protected float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getMaxAirSupply();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_oxygen_percent";
    }

}
