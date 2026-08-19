package de.keksuccino.konkrete.util.properties;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyTest {

    @Test
    void codecsProcessorsAndListenersComposeInStorageOrder() {
        AtomicReference<String> transition = new AtomicReference<>();
        Property.IntegerProperty property = Property.integerProperty("count", 2, "count");
        property.setValueSetProcessor(value -> Math.max(0, value));
        property.addValueSetListener((oldValue, newValue) -> transition.set(oldValue + "->" + newValue));
        PropertyContainer serialized = new PropertyContainer("test");

        property.set(-5).serialize(serialized);

        assertEquals(0, property.getInteger());
        assertEquals("2->0", transition.get());
        assertEquals("0", serialized.getValue("count"));
    }

    @Test
    void manualInputResolutionIsCallerOwnedAndSurvivesRoundTrip() {
        Property.DoubleProperty property = Property.doubleProperty("scale", 1.0D, "scale");
        property.setManualInputResolver(value -> value.replace("${scale}", "2.5"));
        property.setManualInput("${scale}");
        PropertyContainer serialized = new PropertyContainer("test");

        property.serialize(serialized);
        Property.DoubleProperty restored = Property.doubleProperty("scale", 1.0D, "scale");
        restored.setManualInputResolver(value -> value.replace("${scale}", "2.5"));
        restored.deserialize(serialized);

        assertEquals(2.5D, restored.getDouble());
        assertEquals("${scale}", restored.getManualInput());
        assertFalse(restored.isDefault());
    }

    @Test
    void clonePreservesConfigurationWithoutSharingStoredState() {
        Property.StringProperty original = Property.stringProperty("label", "default", false, false, "label");
        original.setValueSetProcessor(String::trim);

        Property<String> clone = original.clone();
        clone.set(" changed ");

        assertNotSame(original, clone);
        assertEquals("default", original.get());
        assertEquals("changed", clone.get());
        assertTrue(original.isDefault());
    }

}
