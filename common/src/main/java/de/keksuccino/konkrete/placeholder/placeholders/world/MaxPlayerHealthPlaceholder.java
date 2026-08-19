package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads max player health state from the current vanilla world/player for {@code max_player_health}. */
public class MaxPlayerHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code max_player_health} placeholder. */
    public MaxPlayerHealthPlaceholder() {
        super("max_player_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getMaxHealth();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.max_player_health";
    }

}
