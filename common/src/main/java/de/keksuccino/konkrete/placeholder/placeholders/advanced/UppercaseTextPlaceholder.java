package de.keksuccino.konkrete.placeholder.placeholders.advanced;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Applies the uppercase text transformation for {@code uppercase_text}. */
public class UppercaseTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code uppercase_text} placeholder. */
    public UppercaseTextPlaceholder() {
        super("uppercase_text");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {

            String input = dps.values.get("text");

            if (input != null) {
                return input.toUpperCase();
            }

        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to parse 'Uppercase Text' placeholder!", ex);
        }

        return null;

    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("text");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.uppercase_text");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.uppercase_text.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("text", "Hello World");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
