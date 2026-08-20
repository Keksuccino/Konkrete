package de.keksuccino.konkrete.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Mutable serialized-placeholder model retained for source compatibility with existing placeholder implementations. */
public class DeserializedPlaceholderString {

    /** Placeholder identifier found in the serialized object. */
    @NotNull public String placeholderIdentifier;
    /** Ordered serialized values. */
    @NotNull public HashMap<String, String> values = new LinkedHashMap<>();
    /** Original serialized source, or an empty string for newly built values. */
    @NotNull public String placeholderString;

    /** Builds a new serialized placeholder model. */
    @NotNull
    public static DeserializedPlaceholderString build(@NotNull String placeholderIdentifier, @Nullable Map<String, String> values) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        if (values != null) copy.putAll(values);
        return new DeserializedPlaceholderString(placeholderIdentifier, copy, "");
    }

    /** Creates an empty model. */
    public DeserializedPlaceholderString() {
        this("", null, "");
    }

    /** Creates a model from parsed values. */
    public DeserializedPlaceholderString(@NotNull String placeholderIdentifier, @Nullable Map<String, String> values, @NotNull String placeholderString) {
        this.placeholderIdentifier = Objects.requireNonNull(placeholderIdentifier, "placeholderIdentifier");
        if (values != null) this.values.putAll(values);
        this.placeholderString = Objects.requireNonNull(placeholderString, "placeholderString");
    }

    /** Serializes this model using the placeholder JSON syntax. */
    @Override
    @NotNull
    public String toString() {
        StringBuilder out = new StringBuilder("{\"placeholder\":\"").append(escape(this.placeholderIdentifier)).append('"');
        if (!this.values.isEmpty()) {
            out.append(",\"values\":{");
            boolean first = true;
            for (Map.Entry<String, String> value : this.values.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append('"').append(escape(value.getKey())).append("\":\"").append(escape(value.getValue())).append('"');
            }
            out.append('}');
        }
        return out.append('}').toString();
    }

    private static String escape(@NotNull String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("%n%");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

}
