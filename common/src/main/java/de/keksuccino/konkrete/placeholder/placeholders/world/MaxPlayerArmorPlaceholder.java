package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads max player armor state from the current vanilla world/player for {@code max_player_armor}. */
public class MaxPlayerArmorPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code max_player_armor} placeholder. */
    public MaxPlayerArmorPlaceholder() {
        super("max_player_armor");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return 20; //hardcoded max value for players
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.max_player_armor";
    }

}
