package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.player.PlayerPositionObserver;
import org.jetbrains.annotations.NotNull;

/** Exposes the camera entity's latest Z-axis movement delta. */
public class PlayerPositionDeltaZPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code player_position_delta_z} placeholder. */
    public PlayerPositionDeltaZPlaceholder() {
        super("player_position_delta_z", "player_position_delta_z_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(PlayerPositionObserver.getCurrentPositionDeltaZ());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_position_delta_z";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
