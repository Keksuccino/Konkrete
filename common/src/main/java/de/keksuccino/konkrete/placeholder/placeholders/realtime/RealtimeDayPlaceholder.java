package de.keksuccino.konkrete.placeholder.placeholders.realtime;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.List;

/** Formats wall-clock realtime day for {@code realtimeday}. */
public class RealtimeDayPlaceholder extends AbstractRealtimePlaceholder {

    /** Creates the {@code realtimeday} placeholder. */
    public RealtimeDayPlaceholder() {
        super("realtimeday");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = this.getCalendar(dps);
        return formatTwoDigitDateTimePart(c.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return getTimezoneValueNames();
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.realtime_day");
    }

    @Override
    public List<String> getDescription() {
        return null;
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return this.buildDefaultPlaceholderString();
    }

}
