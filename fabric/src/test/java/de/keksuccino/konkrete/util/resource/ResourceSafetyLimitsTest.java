package de.keksuccino.konkrete.util.resource;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceSafetyLimitsTest {

    @Test
    void validatesDimensionFrameAndConfigurationBounds() {
        ResourceSafetyLimits limits = new ResourceSafetyLimits(32, 16, 256L, 4, 8, 64L, 128L);

        assertDoesNotThrow(() -> limits.validateImageDimensions(16, 16, "image"));
        assertThrows(IllegalArgumentException.class, () -> limits.validateImageDimensions(32, 16, "image"));
        assertThrows(IllegalArgumentException.class, () -> limits.validateFrameCount(5L, "animation"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceSafetyLimits(0, 1, 1L, 1, 1, 1L, 1L));
    }

    @Test
    void boundedInputHelpersAcceptBoundaryAndRejectOverflow() throws Exception {
        byte[] boundary = new byte[] {1, 2, 3, 4};
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResourceInputLimits.copy(new ByteArrayInputStream(boundary), output, boundary.length, "test");

        assertArrayEquals(boundary, output.toByteArray());
        assertThrows(IOException.class, () -> ResourceInputLimits.readAllBytes(new ByteArrayInputStream(new byte[5]), 4L, "test"));
        assertThrows(IOException.class, () -> ResourceInputLimits.bounded(new ByteArrayInputStream(new byte[5]), 4L, "test").readAllBytes());
        assertThrows(IllegalArgumentException.class, () -> ResourceInputLimits.bounded(new ByteArrayInputStream(boundary), -1L, "test"));
    }

}
