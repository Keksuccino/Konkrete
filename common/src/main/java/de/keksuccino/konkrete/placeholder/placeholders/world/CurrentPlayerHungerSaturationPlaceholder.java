package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player hunger saturation state from the current vanilla world/player for {@code current_player_hunger_saturation}. */
public class CurrentPlayerHungerSaturationPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code current_player_hunger_saturation} placeholder. */
    public CurrentPlayerHungerSaturationPlaceholder() {
        super("current_player_hunger_saturation");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getFoodData().getSaturationLevel();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_hunger_saturation";
    }
}
