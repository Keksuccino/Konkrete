package de.keksuccino.konkrete.placeholder.placeholders.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class MinecraftOptionValueProvidersTest {

    @AfterEach
    void resetProvider() {
        MinecraftOptionValueProviders.reset();
    }

    @Test
    void overrideIsVisibleAndResetRestoresVanillaRegistryProvider() {
        MinecraftOptionValueProvider vanilla = MinecraftOptionValueProviders.get();
        MinecraftOptionValueProvider override = name -> "override:" + name;

        MinecraftOptionValueProviders.set(override);
        assertSame(override, MinecraftOptionValueProviders.get());
        assertEquals("override:fov", MinecraftOptionValueProviders.get().get("fov"));

        MinecraftOptionValueProviders.reset();
        assertSame(vanilla, MinecraftOptionValueProviders.get());
        assertNotSame(override, MinecraftOptionValueProviders.get());
    }

}
