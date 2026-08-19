package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player exp progress state from the current vanilla world/player for {@code current_player_exp_progress}. */
public class CurrentPlayerExpProgressPlaceholder extends AbstractWorldIntegerPlaceholder {

    /** Creates the {@code current_player_exp_progress} placeholder. */
    public CurrentPlayerExpProgressPlaceholder() {
        super("current_player_exp_progress");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return (int)(player.experienceProgress * 100);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_exp_progress";
    }

}
