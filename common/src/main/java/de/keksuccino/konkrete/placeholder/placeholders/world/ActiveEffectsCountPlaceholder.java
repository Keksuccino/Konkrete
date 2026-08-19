package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads active effects count state from the current vanilla world/player for {@code effects_count}. */
public class ActiveEffectsCountPlaceholder extends AbstractWorldIntegerPlaceholder {

    /** Creates the {@code effects_count} placeholder. */
    public ActiveEffectsCountPlaceholder() {
        super("effects_count");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getActiveEffects().size();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.effects_count";
    }

}
