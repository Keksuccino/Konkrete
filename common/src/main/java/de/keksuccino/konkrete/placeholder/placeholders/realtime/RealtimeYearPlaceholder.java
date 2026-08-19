package de.keksuccino.konkrete.placeholder.placeholders.realtime;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.List;

/** Formats wall-clock realtime year for {@code realtimeyear}. */
public class RealtimeYearPlaceholder extends AbstractRealtimePlaceholder {

    /** Creates the {@code realtimeyear} placeholder. */
    public RealtimeYearPlaceholder() {
        super("realtimeyear");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = this.getCalendar(dps);
        return "" + c.get(Calendar.YEAR);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return getTimezoneValueNames();
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.realtime_year");
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
