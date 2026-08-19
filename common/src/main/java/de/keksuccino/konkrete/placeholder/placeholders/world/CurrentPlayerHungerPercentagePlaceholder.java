package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player hunger percentage state from the current vanilla world/player for {@code current_player_hunger_percent}. */
public class CurrentPlayerHungerPercentagePlaceholder extends AbstractWorldPercentagePlaceholder {

    /** Creates the {@code current_player_hunger_percent} placeholder. */
    public CurrentPlayerHungerPercentagePlaceholder() {
        super("current_player_hunger_percent");
    }

    @Override
    protected float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getFoodData().getFoodLevel();
    }

    @Override
    protected float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return 20; //20 is the hardcoded max food level for players
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_hunger_percent";
    }

}
