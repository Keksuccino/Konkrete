package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player level state from the current vanilla world/player for {@code current_player_level}. */
public class CurrentPlayerLevelPlaceholder extends AbstractWorldIntegerPlaceholder {

    /** Creates the {@code current_player_level} placeholder. */
    public CurrentPlayerLevelPlaceholder() {
        super("current_player_level");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.experienceLevel;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_level";
    }

}
