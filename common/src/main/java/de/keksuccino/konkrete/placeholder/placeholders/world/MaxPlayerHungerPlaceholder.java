package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads max player hunger state from the current vanilla world/player for {@code max_player_hunger}. */
public class MaxPlayerHungerPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code max_player_hunger} placeholder. */
    public MaxPlayerHungerPlaceholder() {
        super("max_player_hunger");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return 20; //20 is the hardcoded max food level for players
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.max_player_hunger";
    }

}
