package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.placeholder.PlaceholderParser;
import de.keksuccino.konkrete.placeholder.PlaceholderRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Resolves bridge requests against Konkrete's shared placeholder registry on the bridge callback executor. */
final class KonkreteRinkuPlaceholderAdapter {

    private KonkreteRinkuPlaceholderAdapter() {}

    @NotNull
    static ActionBridge.PlaceholderResult resolve(@NotNull String identifier, @NotNull Map<String, String> variables) {
        Placeholder placeholder = PlaceholderRegistry.getPlaceholder(identifier);
        if (placeholder == null && !identifier.equals(identifier.toLowerCase(Locale.ROOT))) placeholder = PlaceholderRegistry.getPlaceholder(identifier.toLowerCase(Locale.ROOT));
        if (placeholder == null) return ActionBridge.PlaceholderResult.failure(404, "NOT_FOUND", "Unknown placeholder: " + identifier);

        List<String> requiredNames = placeholder.getValueNames();
        if (requiredNames != null) {
            for (String requiredName : requiredNames) {
                if (!variables.containsKey(requiredName)) return ActionBridge.PlaceholderResult.failure(400, "MISSING_VARIABLE", "Missing placeholder variable: " + requiredName);
            }
        }
        if (!placeholder.checkExecutionContext()) return ActionBridge.PlaceholderResult.failure(403, "EXECUTION_DENIED", "Placeholder cannot run on the configured bridge callback thread");

        LinkedHashMap<String, String> resolvedVariables = new LinkedHashMap<>();
        variables.forEach((name, value) -> resolvedVariables.put(name, PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(value)));
        DeserializedPlaceholderString deserialized = DeserializedPlaceholderString.build(placeholder.getIdentifier(), resolvedVariables);
        deserialized.placeholderString = deserialized.toString();
        String replacement = placeholder.getReplacementFor(deserialized);
        return ActionBridge.PlaceholderResult.success(replacement);
    }

}
