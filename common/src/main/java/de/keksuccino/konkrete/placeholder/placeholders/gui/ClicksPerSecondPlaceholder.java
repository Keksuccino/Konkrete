package de.keksuccino.konkrete.placeholder.placeholders.gui;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.input.ClicksPerSecondTracker;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exposes left- or right-button clicks recorded within the latest rolling second. */
public class ClicksPerSecondPlaceholder extends Placeholder {

    /** Creates the {@code clicks_per_second} placeholder. */
    public ClicksPerSecondPlaceholder() {
        super("clicks_per_second");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String mouseButton = dps.values.getOrDefault("mouse_button", "left");
        return Integer.toString(ClicksPerSecondTracker.getClicksPerSecond(mouseButton.equalsIgnoreCase("right")));
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("mouse_button");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.clicks_per_second");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.clicks_per_second.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("mouse_button", "left");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
