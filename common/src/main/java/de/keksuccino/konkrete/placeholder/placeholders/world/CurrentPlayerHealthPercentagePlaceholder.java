package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player health percentage state from the current vanilla world/player for {@code current_player_health_percent}. */
public class CurrentPlayerHealthPercentagePlaceholder extends AbstractWorldPercentagePlaceholder {

    /** Creates the {@code current_player_health_percent} placeholder. */
    public CurrentPlayerHealthPercentagePlaceholder() {
        super("current_player_health_percent");
    }

    @Override
    protected float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getHealth();
    }

    @Override
    protected float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getMaxHealth();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_health_percent";
    }

}
