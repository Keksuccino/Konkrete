package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.player.PlayerPositionObserver;
import org.jetbrains.annotations.NotNull;

/** Exposes the camera entity's latest X-axis movement delta. */
public class PlayerPositionDeltaXPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code player_position_delta_x} placeholder. */
    public PlayerPositionDeltaXPlaceholder() {
        super("player_position_delta_x", "player_position_delta_x_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(PlayerPositionObserver.getCurrentPositionDeltaX());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_position_delta_x";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
