package de.keksuccino.konkrete.util.rendering.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityRotationTest {

    @Test
    void preservesRadianValues() {
        EntityRotation rotation = EntityRotation.radians(0.25F, -0.5F, 1.5F);

        assertEquals(0.25F, rotation.x());
        assertEquals(-0.5F, rotation.y());
        assertEquals(1.5F, rotation.z());
    }

    @Test
    void convertsDegreeFactoriesToRadians() {
        EntityRotation rotation = EntityRotation.degrees(90.0F, -180.0F, 45.0F);

        assertEquals((float) (Math.PI / 2.0), rotation.x(), 0.00001F);
        assertEquals((float) -Math.PI, rotation.y(), 0.00001F);
        assertEquals((float) (Math.PI / 4.0), rotation.z(), 0.00001F);
    }

    @Test
    void exposesDegreeViewsOfStoredRadians() {
        EntityRotation rotation = EntityRotation.radians((float) Math.PI, (float) (Math.PI / 2.0), (float) (-Math.PI / 4.0));

        assertEquals(180.0F, rotation.xDegrees(), 0.0001F);
        assertEquals(90.0F, rotation.yDegrees(), 0.0001F);
        assertEquals(-45.0F, rotation.zDegrees(), 0.0001F);
    }

}
