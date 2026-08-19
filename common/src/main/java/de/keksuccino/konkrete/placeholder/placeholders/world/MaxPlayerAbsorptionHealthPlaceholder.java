package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads max player absorption health state from the current vanilla world/player for {@code max_player_absorption_health}. */
public class MaxPlayerAbsorptionHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code max_player_absorption_health} placeholder. */
    public MaxPlayerAbsorptionHealthPlaceholder() {
        super("max_player_absorption_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getMaxAbsorption();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.max_player_absorption_health";
    }

}
