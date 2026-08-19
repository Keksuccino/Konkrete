package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Serializes an integer read from the current player and client level, defaulting to {@code 0}. */
public abstract class AbstractWorldIntegerPlaceholder extends AbstractWorldPlaceholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates an integer vanilla-state placeholder with the supplied identifier. */
    public AbstractWorldIntegerPlaceholder(@NotNull String identifier) {
        super(identifier);
    }

    /** Reads the integer serialized by the shared evaluator. */
    protected abstract int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level);

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {
            ClientLevel level = this.getLevel();
            LocalPlayer player = this.getPlayer();
            if ((level != null) && (player != null)) {
                return "" + this.getIntegerValue(player, level);
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
