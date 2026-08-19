package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player absorption health state from the current vanilla world/player for {@code current_player_absorption_health}. */
public class CurrentPlayerAbsorptionHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code current_player_absorption_health} placeholder. */
    public CurrentPlayerAbsorptionHealthPlaceholder() {
        super("current_player_absorption_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getAbsorptionAmount();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_absorption_health";
    }

}
