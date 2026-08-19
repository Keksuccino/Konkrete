package de.keksuccino.konkrete.util.resource.resources.texture.afma.creator;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfmaCreatorScreenValueParsingTest {

    @Test
    void emptyPathFieldsRemainUnset() {
        assertNull(AfmaCreatorScreen.pathToDirectory(null));
        assertNull(AfmaCreatorScreen.pathToDirectory("  "));
        assertNull(AfmaCreatorScreen.pathToFile(""));
        assertEquals("", AfmaCreatorScreen.fileToPath(null));
    }

    @Test
    void pathFieldsNormalizeSerializedSeparators() {
        assertEquals("frames/main", AfmaCreatorScreen.pathToDirectory("frames\\main").getPath());
        assertEquals("output/demo.afma", AfmaCreatorScreen.pathToFile("output\\demo.afma").getPath());
        assertEquals("output/demo.afma", AfmaCreatorScreen.fileToPath(new File("output/demo.afma")));
    }

    @Test
    void integerValidationAcceptsOnlyCompleteLongValues() {
        assertTrue(AfmaCreatorScreen.isInteger(" -42 "));
        assertTrue(AfmaCreatorScreen.isInteger("+17"));
        assertFalse(AfmaCreatorScreen.isInteger("12.5"));
        assertFalse(AfmaCreatorScreen.isInteger("9223372036854775808"));
        assertFalse(AfmaCreatorScreen.isInteger(null));
        assertTrue(AfmaCreatorScreen.isInt("2147483647"));
        assertFalse(AfmaCreatorScreen.isInt("2147483648"));
    }

    @Test
    void numericParsingUsesFallbackOnlyForBlankOrMalformedValues() {
        assertEquals(-42L, AfmaCreatorScreen.parseLongOrDefault(" -42 ", 7L));
        assertEquals(7L, AfmaCreatorScreen.parseLongOrDefault("4.2", 7L));
        assertEquals(12, AfmaCreatorScreen.parseIntOrDefault("12", 7));
        assertEquals(7, AfmaCreatorScreen.parseIntOrDefault("2147483648", 7));
        assertEquals(0.125D, AfmaCreatorScreen.parseDoubleOrDefault(" 0.125 ", 2.0D));
        assertEquals(2.0D, AfmaCreatorScreen.parseDoubleOrDefault("nope", 2.0D));
    }
}
