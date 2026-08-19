package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads player x coordinate state from the current vanilla world/player for {@code player_x_coordinate}. */
public class PlayerXCoordinatePlaceholder extends AbstractWorldIntegerPlaceholder {

    /** Creates the {@code player_x_coordinate} placeholder. */
    public PlayerXCoordinatePlaceholder() {
        super("player_x_coordinate");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.blockPosition().getX();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_x_coordinate";
    }

}
