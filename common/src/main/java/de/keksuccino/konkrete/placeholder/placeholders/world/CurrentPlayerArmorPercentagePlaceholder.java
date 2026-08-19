package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads current player armor percentage state from the current vanilla world/player for {@code current_player_armor_percent}. */
public class CurrentPlayerArmorPercentagePlaceholder extends AbstractWorldPercentagePlaceholder {

    /** Creates the {@code current_player_armor_percent} placeholder. */
    public CurrentPlayerArmorPercentagePlaceholder() {
        super("current_player_armor_percent");
    }

    @Override
    protected float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getArmorValue();
    }

    @Override
    protected float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return 20; //hardcoded max value
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_player_armor_percent";
    }

}
