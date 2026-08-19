package de.keksuccino.konkrete.placeholder.placeholders.realtime;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.List;

/** Formats wall-clock realtime minute for {@code realtimeminute}. */
public class RealtimeMinutePlaceholder extends AbstractRealtimePlaceholder {

    /** Creates the {@code realtimeminute} placeholder. */
    public RealtimeMinutePlaceholder() {
        super("realtimeminute");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = this.getCalendar(dps);
        return formatTwoDigitDateTimePart(c.get(Calendar.MINUTE));
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return getTimezoneValueNames();
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.realtime_minute");
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
