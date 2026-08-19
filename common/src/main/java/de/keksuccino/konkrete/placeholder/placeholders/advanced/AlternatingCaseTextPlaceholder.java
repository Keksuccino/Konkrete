package de.keksuccino.konkrete.placeholder.placeholders.advanced;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.TextCaseUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** Applies the alternating case text transformation for {@code alternating_case_text}. */
public class AlternatingCaseTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code alternating_case_text} placeholder. */
    public AlternatingCaseTextPlaceholder() {
        super("alternating_case_text");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            String input = dps.values.get("text");
            if (input != null) {
                return TextCaseUtils.toAlternatingCase(input);
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to parse 'Alternating Case Text' placeholder!", ex);
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
        return I18n.get("konkrete.placeholders.alternating_case_text");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.alternating_case_text.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("text", "alternating case");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
