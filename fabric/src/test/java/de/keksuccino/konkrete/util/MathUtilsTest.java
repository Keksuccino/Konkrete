package de.keksuccino.konkrete.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MathUtilsTest {

    @Test
    void evaluatesNumbersAndExpressions() {
        assertEquals(1000.0D, MathUtils.calculateFromString("1e3"));
        assertEquals(14.0D, MathUtils.calculateFromString("2 + 3 * 4"));
        assertEquals(512.0D, MathUtils.calculateFromString("2^3^2"));
        assertEquals(3.0D, MathUtils.calculateFromString("sqrt 9"));
    }

    @Test
    void identifiesInvalidExpressionsWithoutChangingLegacyFallback() {
        assertFalse(MathUtils.isCalculateableString(null));
        assertFalse(MathUtils.isCalculateableString("2 +"));
        assertFalse(MathUtils.isCalculateableString("unknown 1"));
        assertEquals(0.0D, MathUtils.calculateFromString("2 +"));
    }

    @Test
    void preservesNonFiniteDoubleParsing() {
        assertTrue(MathUtils.isCalculateableString("NaN"));
        assertTrue(Double.isNaN(MathUtils.calculateFromString("NaN")));
        assertEquals(Double.POSITIVE_INFINITY, MathUtils.calculateFromString("Infinity"));
    }

    @Test
    void roundsFiniteValuesAndRejectsNegativeScale() {
        assertEquals(1.24D, MathUtils.round(1.235D, 2));
        assertEquals(Double.POSITIVE_INFINITY, MathUtils.round(Double.POSITIVE_INFINITY, 2));
        assertThrows(IllegalArgumentException.class, () -> MathUtils.round(1.0D, -1));
    }

    @Test
    void inclusiveRandomRangeHandlesDegenerateAndMaximumBounds() {
        assertEquals(4, MathUtils.getRandomNumberInRange(4, 4));
        assertEquals(5, MathUtils.getRandomNumberInRange(5, 4));
        assertDoesNotThrow(() -> MathUtils.getRandomNumberInRange(Integer.MIN_VALUE, Integer.MAX_VALUE));
        for (int iteration = 0; iteration < 100; iteration++) {
            int value = MathUtils.getRandomNumberInRange(-2, 2);
            assertTrue(value >= -2 && value <= 2);
        }
    }
}
