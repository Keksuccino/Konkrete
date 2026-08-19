package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player health state from the current vanilla world/player for {@code current_player_health}. */
public class CurrentPlayerHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code current_player_health} placeholder. */
    public CurrentPlayerHealthPlaceholder() {
        super("current_player_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getHealth();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_health";
    }

}
