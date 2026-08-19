package de.keksuccino.konkrete.util.rendering.text.color;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** Registers and resolves text color formatter values by stable identifiers. */
public class TextColorFormatterRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, TextColorFormatter> FORMATTERS = new HashMap<>();

    /** Registers the supplied value for later lookup. */
    public static void register(@NotNull String identifier, @NotNull TextColorFormatter formatter) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(formatter);
        if (FORMATTERS.containsKey(identifier)) {
            LOGGER.warn("[KONKRETE] TextColorFormatter with identifier '" + identifier + "' already exists! Overriding formatter!");
        }
        FORMATTERS.put(identifier, formatter);
    }

    /** Returns formatter. */
    @Nullable
    public static TextColorFormatter getFormatter(@NotNull String identifier) {
        return FORMATTERS.get(identifier);
    }

    /** Returns by code. */
    @Nullable
    public static TextColorFormatter getByCode(char code) {
        for (TextColorFormatter f : getFormatters()) {
            if (("" + f.getCode()).equals("" + code)) return f;
        }
        return null;
    }

    /** Returns formatters. */
    @NotNull
    public static List<TextColorFormatter> getFormatters() {
        return new ArrayList<>(FORMATTERS.values());
    }

}
