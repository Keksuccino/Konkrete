package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player experience state from the current vanilla world/player for {@code current_player_exp}. */
public class CurrentPlayerExperiencePlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code current_player_exp} placeholder. */
    public CurrentPlayerExperiencePlaceholder() {
        super("current_player_exp");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.totalExperience;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_experience";
    }

}
