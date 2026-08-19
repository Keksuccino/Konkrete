package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player hunger state from the current vanilla world/player for {@code current_player_hunger}. */
public class CurrentPlayerHungerPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code current_player_hunger} placeholder. */
    public CurrentPlayerHungerPlaceholder() {
        super("current_player_hunger");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getFoodData().getFoodLevel();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_hunger";
    }

}
