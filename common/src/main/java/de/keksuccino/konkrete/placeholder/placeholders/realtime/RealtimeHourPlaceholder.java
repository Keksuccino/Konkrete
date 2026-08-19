package de.keksuccino.konkrete.placeholder.placeholders.realtime;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.SerializationHelper;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

/** Formats wall-clock realtime hour for {@code realtimehour}. */
public class RealtimeHourPlaceholder extends AbstractRealtimePlaceholder {

    /** Creates the {@code realtimehour} placeholder. */
    public RealtimeHourPlaceholder() {
        super("realtimehour");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = this.getCalendar(dps);
        boolean twelveHourFormat = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("twelve_hour_format"));
        int hour = c.get(twelveHourFormat ? Calendar.HOUR : Calendar.HOUR_OF_DAY);
        if (twelveHourFormat && hour == 0) {
            hour = 12;
        }
        return formatTwoDigitDateTimePart(hour);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("twelve_hour_format", TIMEZONE_VALUE_NAME);
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.realtime_hour");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.realtime_hour.desc"));
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("twelve_hour_format", "false");
        return this.buildDefaultPlaceholderString(values);
    }

}
