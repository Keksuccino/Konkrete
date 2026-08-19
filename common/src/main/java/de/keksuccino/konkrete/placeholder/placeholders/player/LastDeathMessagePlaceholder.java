package de.keksuccino.konkrete.placeholder.placeholders.player;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.SerializationHelper;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exposes the most recently captured local-player death message as plain text or component JSON. */
public class LastDeathMessagePlaceholder extends Placeholder {

    /** Creates the {@code lastdeathmessage} placeholder. */
    public LastDeathMessagePlaceholder() {
        super("lastdeathmessage");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        boolean asJson = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("as_json_component"));
        String message = asJson ? LastDeathMessageTracker.getJson() : LastDeathMessageTracker.getPlainText();
        return message != null ? message : "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("as_json_component");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.last_death_message");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.last_death_message.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.player");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("as_json_component", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
