package de.keksuccino.konkrete.config.v2;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the legacy Konkrete text format without loading or depending on Konkrete.
 */
final class LegacyConfigReader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pattern ENTRY_START = Pattern.compile("^\\s*([ISBLDF]):([^=]+)=\\s*'(.*)$");

    private LegacyConfigReader() {
    }

    @NotNull
    static Result read(@NotNull Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        JsonObject values = new JsonObject();
        Set<String> importedKeys = new HashSet<>();
        String category = null;
        boolean complete = true;

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("//")) continue;

            if (trimmedLine.startsWith("##[") && trimmedLine.endsWith("]")) {
                category = trimmedLine.substring(3, trimmedLine.length() - 1);
                continue;
            }
            if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) continue;

            Matcher matcher = ENTRY_START.matcher(line);
            if (!matcher.matches()) {
                LOGGER.warn("Ignoring unrecognized line {} while converting legacy config {}.", lineIndex + 1, file);
                complete = false;
                continue;
            }

            char type = matcher.group(1).charAt(0);
            String key = matcher.group(2).replace(" ", "").trim();
            StringBuilder rawValue = new StringBuilder(matcher.group(3));
            while (findTerminator(rawValue) < 0 && lineIndex + 1 < lines.size()) {
                rawValue.append('\n').append(lines.get(++lineIndex));
            }

            int terminator = findTerminator(rawValue);
            if (category == null || category.isEmpty() || key.isEmpty() || terminator < 0) {
                LOGGER.warn("Ignoring incomplete legacy value '{}' while converting {}.", key, file);
                complete = false;
                continue;
            }
            if (!importedKeys.add(key)) {
                // Konkrete keyed entries globally even though it displayed them in categories, so the first occurrence is authoritative.
                continue;
            }

            JsonPrimitive parsedValue = parseValue(type, rawValue.substring(0, terminator), key, file);
            if (parsedValue == null) {
                complete = false;
                continue;
            }
            JsonObject section = values.has(category) && values.get(category).isJsonObject() ? values.getAsJsonObject(category) : new JsonObject();
            section.add(key, parsedValue);
            values.add(category, section);
        }

        return new Result(values, complete);
    }

    private static int findTerminator(@NotNull CharSequence value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1)) && value.charAt(end - 1) != '\n') {
            end--;
        }
        return end >= 2 && value.charAt(end - 2) == '\'' && value.charAt(end - 1) == ';' ? end - 2 : -1;
    }

    @Nullable
    private static JsonPrimitive parseValue(char type, @NotNull String rawValue, @NotNull String key, @NotNull Path file) {
        try {
            switch (type) {
                case 'I':
                    return new JsonPrimitive(Integer.parseInt(rawValue));
                case 'S':
                    return new JsonPrimitive(rawValue);
                case 'B':
                    if (!rawValue.equalsIgnoreCase("true") && !rawValue.equalsIgnoreCase("false")) throw new IllegalArgumentException("Invalid boolean");
                    return new JsonPrimitive(Boolean.parseBoolean(rawValue));
                case 'L':
                    return new JsonPrimitive(Long.parseLong(rawValue));
                case 'D':
                    double doubleValue = Double.parseDouble(rawValue);
                    if (!Double.isFinite(doubleValue)) throw new IllegalArgumentException("Non-finite double");
                    return new JsonPrimitive(doubleValue);
                case 'F':
                    float floatValue = Float.parseFloat(rawValue);
                    if (!Float.isFinite(floatValue)) throw new IllegalArgumentException("Non-finite float");
                    return new JsonPrimitive(floatValue);
                default:
                    return null;
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Ignoring invalid legacy value '{}' while converting {}.", key, file, ex);
            return null;
        }
    }

    static final class Result {

        private final JsonObject values;
        private final boolean complete;

        private Result(@NotNull JsonObject values, boolean complete) {
            this.values = values;
            this.complete = complete;
        }

        @NotNull
        JsonObject getValues() {
            return this.values;
        }

        boolean isComplete() {
            return this.complete;
        }

    }

}
