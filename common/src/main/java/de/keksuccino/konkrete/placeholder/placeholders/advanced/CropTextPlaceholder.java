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

/** Applies the crop text transformation for {@code crop_text}. */
public class CropTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code crop_text} placeholder. */
    public CropTextPlaceholder() {
        super("crop_text");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {

            String input = dps.values.get("text");
            int removeFromStart = SerializationHelper.INSTANCE.deserializeNumber(Integer.class, -1, dps.values.get("remove_from_start"));
            int removeFromEnd = SerializationHelper.INSTANCE.deserializeNumber(Integer.class, -1, dps.values.get("remove_from_end"));

            if (input != null) {
                if (input.length() <= removeFromStart) return "";
                if (input.length() <= removeFromEnd) return "";
                String sub1 = input.substring(removeFromStart);
                if (sub1.length() <= removeFromEnd) return "";
                return sub1.substring(0, sub1.length() - removeFromEnd);
            }

        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to parse 'Crop Text' placeholder!", ex);
        }

        return null;

    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("text");
        l.add("remove_from_start");
        l.add("remove_from_end");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.crop_text");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.crop_text.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("text", "some text");
        m.put("remove_from_start", "0");
        m.put("remove_from_end", "0");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
