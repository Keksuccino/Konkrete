package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

/** Reads player z coordinate state from the current vanilla world/player for {@code player_z_coordinate}. */
public class PlayerZCoordinatePlaceholder extends AbstractWorldIntegerPlaceholder {

    /** Creates the {@code player_z_coordinate} placeholder. */
    public PlayerZCoordinatePlaceholder() {
        super("player_z_coordinate");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.blockPosition().getZ();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_z_coordinate";
    }

}
