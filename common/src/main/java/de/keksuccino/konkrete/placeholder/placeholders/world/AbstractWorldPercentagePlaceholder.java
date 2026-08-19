package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Serializes a truncated {@code current / maximum * 100} player-state percentage, defaulting to {@code 0}. */
public abstract class AbstractWorldPercentagePlaceholder extends AbstractWorldPlaceholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates a percentage vanilla-state placeholder with the supplied identifier. */
    public AbstractWorldPercentagePlaceholder(@NotNull String identifier) {
        super(identifier);
    }

    /** Reads the current numerator for the percentage calculation. */
    protected abstract float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level);

    /** Reads the maximum denominator for the percentage calculation. */
    protected abstract float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level);

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {
            ClientLevel level = this.getLevel();
            LocalPlayer player = this.getPlayer();
            if ((level != null) && (player != null)) {
                if (this.getMaxFloatValue(player, level) == 0.0F) return "0";
                if (this.getCurrentFloatValue(player, level) == 0.0F) return "0";
                float f = (this.getCurrentFloatValue(player, level) / this.getMaxFloatValue(player, level)) * 100.0F;
                return "" + ((int)f);
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0";

    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
