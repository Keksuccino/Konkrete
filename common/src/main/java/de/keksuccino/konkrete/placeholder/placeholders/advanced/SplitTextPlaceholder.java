package de.keksuccino.konkrete.placeholder.placeholders.advanced;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.SerializationHelper;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Applies the split text transformation for {@code split_text}. */
public class SplitTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code split_text} placeholder. */
    public SplitTextPlaceholder() {
        super("split_text");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {

            String input = dps.values.get("input");
            String regex = dps.values.get("regex");
            int maxParts = SerializationHelper.INSTANCE.deserializeNumber(Integer.class, -1, dps.values.get("max_parts"));
            int splitIndex = SerializationHelper.INSTANCE.deserializeNumber(Integer.class, -1, dps.values.get("split_index"));

            if ((maxParts > 0) && (maxParts-1 < splitIndex)) {
                LOGGER.error("[KONKRETE] Failed to parse 'Split Text' placeholder! Max_parts is smaller than split_index!");
                return "Failed to parse! Max_parts is smaller than split_index!";
            }

            if ((input != null) && (regex != null)) {
                String[] inSplit;
                if (maxParts <= 0) {
                    inSplit = input.split(regex);
                } else {
                    inSplit = input.split(regex, maxParts);
                }
                if (inSplit.length-1 < splitIndex) {
                    LOGGER.error("[KONKRETE] Failed to parse 'Split Text' placeholder! There is no part with index " + splitIndex + "! Only " + inSplit.length + " parts found! Keep in mind first part index is 0!");
                    return "Failed to parse! There is no part with index " + splitIndex + "! Only " + inSplit.length + " parts found! Keep in mind first part index is 0!";
                }
                return inSplit[splitIndex];
            }

        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to parse 'Split Text' placeholder!", ex);
        }

        return null;

    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("input");
        l.add("regex");
        l.add("max_parts");
        l.add("split_index");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.split_text");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.split_text.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("input", "some text");
        m.put("regex", "e");
        m.put("max_parts", "2");
        m.put("split_index", "0");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
