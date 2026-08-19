package de.keksuccino.konkrete.util.input;

import de.keksuccino.konkrete.util.ConsumingSupplier;

/** Reusable validators for numeric, URL, color, and non-empty text input. */
@SuppressWarnings("all")
public class TextValidators {

    private static final CharacterFilter INTEGER_CHARACTER_FILTER = CharacterFilter.buildIntegerFilter();
    private static final CharacterFilter DOUBLE_CHARACTER_FILTER = CharacterFilter.buildDecimalFiler();

    /** Accepts non-null text containing at least one non-space character. */
    public static final ConsumingSupplier<String, Boolean> NO_EMPTY_STRING_TEXT_VALIDATOR = consumes -> {
        return (consumes != null) && !consumes.replace(" ", "").isEmpty();
    };
    /** Accepts every non-null, non-empty string, including whitespace-only input. */
    public static final ConsumingSupplier<String, Boolean> NO_EMPTY_STRING_SPACES_ALLOWED_TEXT_VALIDATOR = consumes -> {
        return (consumes != null) && !consumes.isEmpty();
    };
    /** Accepts basic HTTP(S) URLs that contain a dot and no normalization requirement. */
    public static final ConsumingSupplier<String, Boolean> BASIC_URL_TEXT_VALIDATOR = consumes -> {
        if ((consumes != null) && !consumes.replace(" ", "").isEmpty()) {
            if ((consumes.startsWith("http://") || consumes.startsWith("https://")) && consumes.contains(".")) return true;
        }
        return false;
    };
    /** Accepts six- or eight-digit hexadecimal colors with an optional leading hash. */
    public static final ConsumingSupplier<String, Boolean> HEX_COLOR_TEXT_VALIDATOR = consumes -> {
        return (consumes != null) && consumes.replace(" ", "").matches("(?i)#?(?:[0-9a-f]{6}|[0-9a-f]{8})");
    };
    /** Accepts text composed only of signed-integer characters. */
    public static final ConsumingSupplier<String, Boolean> INTEGER_TEXT_VALIDATOR = consumes -> INTEGER_CHARACTER_FILTER.isAllowedText(consumes);
    /** Accepts text composed only of signed-decimal characters. */
    public static final ConsumingSupplier<String, Boolean> DOUBLE_TEXT_VALIDATOR = consumes -> DOUBLE_CHARACTER_FILTER.isAllowedText(consumes);

}
