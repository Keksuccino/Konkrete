package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.player.PlayerPositionObserver;
import org.jetbrains.annotations.NotNull;

/** Exposes the camera entity's latest Y-axis movement delta. */
public class PlayerPositionDeltaYPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code player_position_delta_y} placeholder. */
    public PlayerPositionDeltaYPlaceholder() {
        super("player_position_delta_y", "player_position_delta_y_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(PlayerPositionObserver.getCurrentPositionDeltaY());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_position_delta_y";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
