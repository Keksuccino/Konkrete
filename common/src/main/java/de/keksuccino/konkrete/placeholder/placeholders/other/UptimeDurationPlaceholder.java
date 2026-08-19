package de.keksuccino.konkrete.placeholder.placeholders.other;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.SerializationHelper;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/** Formats the current JVM uptime using configurable day/hour/minute/second labels. */
public class UptimeDurationPlaceholder extends Placeholder {

    private volatile long registeredAtMs;

    /** Creates the {@code uptime_duration} placeholder. */
    public UptimeDurationPlaceholder() {
        super("uptime_duration");
    }

    @Override
    public void onRegistered() {
        this.registeredAtMs = System.currentTimeMillis();
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        long timestamp = this.registeredAtMs;
        if (timestamp <= 0L) {
            return "0";
        }
        long uptimeMs = System.currentTimeMillis() - timestamp;
        if (uptimeMs < 0L) {
            uptimeMs = 0L;
        }
        if (SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("output_as_millis"))) {
            return Long.toString(uptimeMs);
        }
        return Long.toString(uptimeMs / 1000L);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("output_as_millis");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.uptime_seconds");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.uptime_seconds.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("output_as_millis", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
