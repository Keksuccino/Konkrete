package de.keksuccino.konkrete.util.properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Parser and serializer for Konkrete's compact typed property-container format.
 */
public final class PropertiesParser {

    private static final Logger LOGGER = LogManager.getLogger();

    private PropertiesParser() {}

    /**
     * Reads and parses a complete UTF-8 file.
     *
     * @param filePath file path
     * @return parsed set, or {@code null} for malformed complete content
     * @throws IOException when the file cannot be completely read
     */
    @Nullable
    public static PropertyContainerSet deserializeSetFromFile(@NotNull String filePath) throws IOException {
        return deserializeSetFromString(Files.readString(Path.of(Objects.requireNonNull(filePath, "filePath")), StandardCharsets.UTF_8));
    }

    /**
     * Parses a complete serialized property-container set.
     *
     * @param serializedString complete serialized text
     * @return parsed set, or {@code null} when malformed
     */
    @Nullable
    public static PropertyContainerSet deserializeSetFromString(@NotNull String serializedString) {
        String content = Objects.requireNonNull(serializedString, "serializedString");
        String setType = null;
        PropertyContainerSet result = null;
        PropertyContainer current = null;
        int lineNumber = 0;
        try {
            for (String line : content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (current == null) {
                    int separator = trimmed.indexOf('=');
                    if (separator >= 0 && trimmed.substring(0, separator).trim().equals("type")) {
                        if (setType != null) return malformed("duplicate set type", lineNumber);
                        setType = trimmed.substring(separator + 1).trim();
                        if (setType.isEmpty()) return malformed("empty set type", lineNumber);
                        result = new PropertyContainerSet(setType);
                    } else if (trimmed.endsWith("{")) {
                        if (result == null) return malformed("container before set type", lineNumber);
                        String containerType = trimmed.substring(0, trimmed.length() - 1).trim();
                        if (containerType.isEmpty()) return malformed("empty container type", lineNumber);
                        current = new PropertyContainer(containerType);
                    } else {
                        return malformed("unexpected content outside a container", lineNumber);
                    }
                    continue;
                }
                if (trimmed.equals("}")) {
                    result.putContainer(current);
                    current = null;
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator < 0) return malformed("property without '='", lineNumber);
                String key = line.substring(0, separator).trim();
                if (key.isEmpty()) return malformed("empty property key", lineNumber);
                String value = line.substring(separator + 1);
                if (value.startsWith(" ")) value = value.substring(1);
                current.putProperty(key, value);
            }
            if (current != null) return malformed("unterminated container", lineNumber);
            if (result == null || setType == null) return malformed("missing set type", lineNumber);
            return result;
        } catch (RuntimeException exception) {
            LOGGER.error("[KONKRETE] Failed to parse complete property-container content at line {}", lineNumber, exception);
            return null;
        }
    }

    /**
     * Reads a complete UTF-8 stream without closing the caller-owned stream and then parses it.
     *
     * @param input caller-owned stream
     * @return parsed set, or {@code null} for malformed complete content
     * @throws IOException when the stream cannot be completely read
     */
    @Nullable
    public static PropertyContainerSet deserializeSetFromStream(@NotNull InputStream input) throws IOException {
        byte[] content = Objects.requireNonNull(input, "input").readAllBytes();
        return deserializeSetFromString(new String(content, StandardCharsets.UTF_8));
    }

    /**
     * Serializes a set as UTF-8, creating its parent directory when necessary.
     *
     * @param set set to serialize
     * @param filePath destination path
     * @throws IOException when the complete file cannot be written
     */
    public static void serializeSetToFile(@NotNull PropertyContainerSet set, @NotNull String filePath) throws IOException {
        Path target = Path.of(Objects.requireNonNull(filePath, "filePath"));
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(target, serializeSetToString(set), StandardCharsets.UTF_8);
    }

    /**
     * Serializes one container.
     *
     * @param container container to serialize
     * @return serialized container
     */
    @NotNull
    public static String serializeContainerToString(@NotNull PropertyContainer container) {
        PropertyContainer checkedContainer = Objects.requireNonNull(container, "container");
        StringBuilder result = new StringBuilder(checkedContainer.getType()).append(" {\n");
        checkedContainer.getProperties().forEach((key, value) -> result.append("  ").append(key).append(" = ").append(value).append('\n'));
        return result.append('}').toString();
    }

    /**
     * Serializes a complete set.
     *
     * @param set set to serialize
     * @return serialized text
     */
    @NotNull
    public static String serializeSetToString(@NotNull PropertyContainerSet set) {
        PropertyContainerSet checkedSet = Objects.requireNonNull(set, "set");
        StringBuilder result = new StringBuilder("type = ").append(checkedSet.getType()).append("\n\n");
        for (PropertyContainer container : checkedSet.getContainers()) result.append(serializeContainerToString(container)).append("\n\n");
        return result.toString();
    }

    /**
     * Joins lines with a trailing line break.
     *
     * @param lines lines to join
     * @return joined text
     */
    @NotNull
    public static String buildStringFromList(@NotNull List<String> lines) {
        return String.join("\n", Objects.requireNonNull(lines, "lines")) + "\n";
    }

    /**
     * Escapes structural line and brace tokens for embedding in another property value.
     *
     * @param serializedString text to escape
     * @return escaped text
     */
    @NotNull
    public static String stringify(@NotNull String serializedString) {
        return Objects.requireNonNull(serializedString, "serializedString").replace("\r\n", "\n").replace('\r', '\n').replace("\n", "$prop_line_break$").replace("{", "$prop_brackets_open$").replace("}", "$prop_brackets_close$");
    }

    /**
     * Reverses {@link #stringify(String)}.
     *
     * @param stringified escaped text
     * @return unescaped text
     */
    @NotNull
    public static String unstringify(@NotNull String stringified) {
        return Objects.requireNonNull(stringified, "stringified").replace("$prop_line_break$", "\n").replace("$prop_brackets_open$", "{").replace("$prop_brackets_close$", "}");
    }

    @Nullable
    private static PropertyContainerSet malformed(String detail, int lineNumber) {
        LOGGER.warn("[KONKRETE] Malformed property-container content at line {}: {}", lineNumber, detail);
        return null;
    }

}
