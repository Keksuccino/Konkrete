package de.keksuccino.konkrete.placeholder.placeholders.realtime;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

/** Resolves wall-clock fields in either the system timezone or a serialized timezone override. */
abstract class AbstractRealtimePlaceholder extends Placeholder {

    /** Serialized value name accepting a {@link ZoneId}. */
    protected static final String TIMEZONE_VALUE_NAME = "timezone";
    /** Serialized value selecting the JVM's current default timezone. */
    protected static final String TIMEZONE_DEFAULT_VALUE = "system";

    /** Creates a realtime placeholder with the supplied namespace-local identifier. */
    protected AbstractRealtimePlaceholder(@NotNull String id) {
        super(id);
    }

    /** Creates a calendar in the serialized timezone, falling back to the system timezone for invalid input. */
    protected final @NotNull Calendar getCalendar(@NotNull DeserializedPlaceholderString dps) {
        return Calendar.getInstance(this.getTimeZone(dps));
    }

    /** Builds default syntax containing the system-timezone argument. */
    protected final @NotNull DeserializedPlaceholderString buildDefaultPlaceholderString() {
        return this.buildDefaultPlaceholderString(new LinkedHashMap<>());
    }

    /** Adds the system-timezone default without overwriting a caller-supplied timezone. */
    protected final @NotNull DeserializedPlaceholderString buildDefaultPlaceholderString(@NotNull LinkedHashMap<String, String> values) {
        LinkedHashMap<String, String> defaultValues = new LinkedHashMap<>(values);
        defaultValues.putIfAbsent(TIMEZONE_VALUE_NAME, TIMEZONE_DEFAULT_VALUE);
        return new DeserializedPlaceholderString(this.getIdentifier(), defaultValues, "");
    }

    /** Returns the serialized timezone value contract shared by realtime placeholders. */
    protected static @NotNull List<String> getTimezoneValueNames() {
        return List.of(TIMEZONE_VALUE_NAME);
    }

    /** Formats a day, month, hour, minute, or second as at least two decimal digits. */
    protected static @NotNull String formatTwoDigitDateTimePart(int in) {
        String s = Integer.toString(in);
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.realtime");
    }

    private @NotNull TimeZone getTimeZone(@NotNull DeserializedPlaceholderString dps) {
        String timezoneId = dps.values.get(TIMEZONE_VALUE_NAME);
        if ((timezoneId == null) || timezoneId.isBlank() || TIMEZONE_DEFAULT_VALUE.equalsIgnoreCase(timezoneId)) {
            return TimeZone.getDefault();
        }
        try {
            return TimeZone.getTimeZone(ZoneId.of(timezoneId.trim()));
        } catch (Exception ignored) {
            return TimeZone.getDefault();
        }
    }

}
