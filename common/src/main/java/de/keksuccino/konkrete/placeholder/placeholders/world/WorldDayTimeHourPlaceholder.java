package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.SerializationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Reads world day time hour state from the current vanilla world/player for {@code world_daytime_hour}. */
public class WorldDayTimeHourPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code world_daytime_hour} placeholder. */
    public WorldDayTimeHourPlaceholder() {
        super("world_daytime_hour");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        boolean twelveHourFormat = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("twelve_hour_format"));

        try {
            return getDayTimeHours(twelveHourFormat);
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

    private static String getDayTimeHours(boolean twelveHourFormat) {
        String hString = "00";
        long dt = Math.floorMod(getDayTime(), 24000L);
        long h = 0;
        if (dt < 18000) {
            h = (dt / 1000) + 6;
        } else {
            h = (dt / 1000) - 18;
        }
        if (twelveHourFormat) {
            h %= 12;
            if (h == 0) {
                h = 12;
            }
        }
        hString = "" + h;
        if (hString.length() < 2) {
            hString = "0" + hString;
        }
        return hString;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("twelve_hour_format"); // true/false - if true returns the hour in 12-hour format (01-12)
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.world_day_time_hour");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.world_day_time_hour.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("twelve_hour_format", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
