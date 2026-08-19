package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads player attack strength percentage state from the current vanilla world/player for {@code player_attack_strength}. */
public class PlayerAttackStrengthPercentagePlaceholder extends AbstractWorldIntegerPlaceholder {

    /** Creates the {@code player_attack_strength} placeholder. */
    public PlayerAttackStrengthPercentagePlaceholder() {
        super("player_attack_strength");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return (int)(player.getAttackStrengthScale(0.0F) * 100.0F);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_attack_strength";
    }

}
