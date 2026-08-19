package de.keksuccino.konkrete.placeholder.placeholders.advanced;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.MathUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** Applies the math ceil transformation for {@code math_ceil}. */
public class MathCeilPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code math_ceil} placeholder. */
    public MathCeilPlaceholder() {
        super("math_ceil");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String num = dps.values.get("num");
        if (num != null) {
            try {
                if (MathUtils.isDouble(num)) {
                    double numD = Double.parseDouble(num);
                    return MathUtils.formatWholeNumber(Math.ceil(numD));
                }
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to parse 'Ceiling (Math)' placeholder: " + dps.placeholderString, ex);
            }
        }
        LOGGER.error("[KONKRETE] Failed to parse 'Ceiling (Math)' placeholder: " + dps.placeholderString);
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("num");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.math_ceil");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.math_ceil.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("num", "3.14");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
