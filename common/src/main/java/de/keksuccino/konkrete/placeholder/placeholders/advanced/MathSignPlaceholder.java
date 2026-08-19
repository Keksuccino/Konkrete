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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/** Applies the math sign transformation for {@code math_sign}. */
public class MathSignPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code math_sign} placeholder. */
    public MathSignPlaceholder() {
        super("math_sign");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String num = dps.values.get("num");
        if (num != null) {
            try {
                if (MathUtils.isDouble(num)) {
                    double numD = Double.parseDouble(num);
                    if (numD > 0) return "1";
                    if (numD < 0) return "-1";
                    return "0";
                }
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to parse 'Sign (Math)' placeholder: " + dps.placeholderString, ex);
            }
        }
        LOGGER.error("[KONKRETE] Failed to parse 'Sign (Math)' placeholder: " + dps.placeholderString);
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
        return I18n.get("konkrete.placeholders.math_sign");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.math_sign.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("num", "-3.14");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
