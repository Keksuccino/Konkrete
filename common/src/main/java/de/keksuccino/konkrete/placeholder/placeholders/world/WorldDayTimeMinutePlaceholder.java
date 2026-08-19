package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Reads world day time minute state from the current vanilla world/player for {@code world_daytime_minute}. */
public class WorldDayTimeMinutePlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code world_daytime_minute} placeholder. */
    public WorldDayTimeMinutePlaceholder() {
        super("world_daytime_minute");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {
            return getDayTimeMinutes();
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0";

    }

    private static long getDayTime() {
        ClientLevel w = Minecraft.getInstance().level;
        if (w != null) {
            return w.getDefaultClockTime();
        }
        return 0L;
    }

    private static String getDayTimeMinutes() {
        String minString = "00";
        long i = getDayTime() / 1000;
        long i2 = getDayTime() - (i * 1000);
        if (i2 <= 0) {
            return minString;
        }
        long min = (long)((float)i2 / 16.6F);
        if (min > 59) {
            min = 0;
        }
        minString = "" + min;
        if (minString.length() < 2) {
            minString = "0" + minString;
        }
        return minString;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.world_day_time_minute");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.world_day_time_minute.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
