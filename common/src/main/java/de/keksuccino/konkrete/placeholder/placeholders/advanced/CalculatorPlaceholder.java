package de.keksuccino.konkrete.placeholder.placeholders.advanced;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Applies the calculator transformation for {@code calc}. */
public class CalculatorPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code calc} placeholder. */
    public CalculatorPlaceholder() {
        super("calc");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String decimalString = dps.values.get("decimal");
        boolean decimal = true;
        if ((decimalString != null) && decimalString.equalsIgnoreCase("false")) {
            decimal = false;
        }
        String ex = dps.values.get("expression");
        if (ex != null) {
            try {
                Expression expression = new ExpressionBuilder(ex).build();
                if (expression.validate().isValid()) {
                    if (decimal) {
                        return "" + expression.evaluate();
                    }
                    return "" + Math.round(expression.evaluate());
                }
            } catch (Exception e) {
                LOGGER.error("[KONKRETE] Failed to evaluate calculator placeholder: {}", dps.placeholderString, e);
                return null;
            }
        }
        LOGGER.error("[KONKRETE] Failed to parse Calculator placeholder: " + dps.placeholderString);
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("expression");
        l.add("decimal");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.calc");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.calc.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("decimal", "true");
        m.put("expression", "2 + 1 - 10");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
