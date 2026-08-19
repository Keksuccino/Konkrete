package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads player y coordinate state from the current vanilla world/player for {@code player_y_coordinate}. */
public class PlayerYCoordinatePlaceholder extends AbstractWorldIntegerPlaceholder {

    /** Creates the {@code player_y_coordinate} placeholder. */
    public PlayerYCoordinatePlaceholder() {
        super("player_y_coordinate");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.blockPosition().getY();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_y_coordinate";
    }

}
