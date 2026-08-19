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

/** Applies the min number transformation for {@code minnum}. */
public class MinNumberPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code minnum} placeholder. */
    public MinNumberPlaceholder() {
        super("minnum");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String firstNumber = dps.values.get("first");
        String secondNumber = dps.values.get("second");
        if ((firstNumber != null) && (secondNumber != null)) {
            try {
                if (MathUtils.isDouble(firstNumber) && MathUtils.isDouble(secondNumber)) {
                    double first = Double.parseDouble(firstNumber);
                    double second = Double.parseDouble(secondNumber);
                    return (Math.min(first, second) == first) ? firstNumber : secondNumber;
                }
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to evaluate min-number placeholder: {}", dps.placeholderString, ex);
                return null;
            }
        }
        LOGGER.error("[KONKRETE] Failed to parse 'Min Number' placeholder: " + dps.placeholderString);
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("first");
        l.add("second");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.min_number");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.min_number.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("first", "10");
        values.put("second", "15");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
