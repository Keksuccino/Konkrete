package de.keksuccino.konkrete.placeholder.remote;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemotePlaceholderProvidersTest {

    @AfterEach
    void resetProvider() {
        RemotePlaceholderProviders.reset();
    }

    @Test
    void defaultProviderReportsUnavailableWithoutProductPackets() {
        assertFalse(RemotePlaceholderProviders.isConfigured());
        assertNull(RemotePlaceholderProviders.get().resolve("gamerule", "keepInventory", Map.of()));
    }

    @Test
    void overrideReceivesProviderRequestAndArguments() {
        RemotePlaceholderProviders.set((providerKey, requestKey, arguments) -> providerKey + ":" + requestKey + ":" + arguments.get("path"));

        assertTrue(RemotePlaceholderProviders.isConfigured());
        assertEquals("nbt:player:Inventory[0]", RemotePlaceholderProviders.get().resolve("nbt", "player", Map.of("path", "Inventory[0]")));
    }

    @Test
    void resetRestoresStableUnavailablePolicy() {
        RemotePlaceholderProviders.set((providerKey, requestKey, arguments) -> "value");

        RemotePlaceholderProviders.reset();

        assertFalse(RemotePlaceholderProviders.isConfigured());
        assertNull(RemotePlaceholderProviders.get().resolve("nbt", "player", Map.of()));
    }
}
