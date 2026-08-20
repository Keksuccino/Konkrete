package de.keksuccino.konkrete.util.rendering;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RenderingUtilsTest {

    @Test
    void parsesRgbColorsWithOrWithoutHashPrefix() {
        assertEquals(new Color(0x12, 0x34, 0x56), RenderingUtils.getColorFromHexString("123456"));
        assertEquals(new Color(0xAB, 0xCD, 0xEF), RenderingUtils.getColorFromHexString("#ABCDEF"));
    }

    @Test
    void parsesRgbaColors() {
        assertEquals(new Color(0x12, 0x34, 0x56, 0x78), RenderingUtils.getColorFromHexString("12345678"));
    }

    @Test
    void rejectsUnsupportedHexLengths() {
        assertNull(RenderingUtils.getColorFromHexString("12345"));
        assertNull(RenderingUtils.getColorFromHexString("123456789"));
    }

}
