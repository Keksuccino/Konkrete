package de.keksuccino.konkrete.util.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextValidatorsTest {

    @Test
    void acceptsSupportedRgbAndRgbaHexForms() {
        assertTrue(TextValidators.HEX_COLOR_TEXT_VALIDATOR.get("#12aBcF"));
        assertTrue(TextValidators.HEX_COLOR_TEXT_VALIDATOR.get("12ABCDEF"));
        assertTrue(TextValidators.HEX_COLOR_TEXT_VALIDATOR.get(" #12 AB cd "));
    }

    @Test
    void rejectsEmptyMalformedAndShorthandColors() {
        assertFalse(TextValidators.HEX_COLOR_TEXT_VALIDATOR.get(null));
        assertFalse(TextValidators.HEX_COLOR_TEXT_VALIDATOR.get(""));
        assertFalse(TextValidators.HEX_COLOR_TEXT_VALIDATOR.get("#fff"));
        assertFalse(TextValidators.HEX_COLOR_TEXT_VALIDATOR.get("#12345g"));
        assertFalse(TextValidators.HEX_COLOR_TEXT_VALIDATOR.get("#123456789"));
    }
}
