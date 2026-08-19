package de.keksuccino.konkrete.placeholder.placeholders.realtime;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

/** Formats wall-clock unix timestamp for {@code unix_time}. */
public class UnixTimestampPlaceholder extends Placeholder {

    /** Creates the {@code unix_time} placeholder. */
    public UnixTimestampPlaceholder() {
        super("unix_time");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return "" + System.currentTimeMillis();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.unix_time");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.unix_time"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.realtime");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
