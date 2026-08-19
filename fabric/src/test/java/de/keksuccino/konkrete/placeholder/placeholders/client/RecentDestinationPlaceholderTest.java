package de.keksuccino.konkrete.placeholder.placeholders.client;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentDestinationPlaceholderTest {

    private final LastWorldOrServerPlaceholder placeholder = new LastWorldOrServerPlaceholder();

    @AfterEach
    void resetProvider() {
        RecentDestinationProviders.reset();
    }

    @Test
    void defaultProviderReportsNoHistory() {
        assertFalse(RecentDestinationProviders.isConfigured());
        assertEquals("", this.resolve("both", true));
    }

    @Test
    void worldHistorySupportsIdentifierDisplayNameAndTypeFiltering() {
        RecentDestinationProviders.set(() -> new RecentDestination(RecentDestination.Type.WORLD, "/saves/example", "Example World"));

        assertTrue(RecentDestinationProviders.isConfigured());
        assertEquals("/saves/example", this.resolve("both", true));
        assertEquals("Example World", this.resolve("world", false));
        assertEquals("", this.resolve("server", true));
    }

    @Test
    void serverHistoryUsesItsConnectionIdentifier() {
        RecentDestinationProviders.set(() -> new RecentDestination(RecentDestination.Type.SERVER, "example.test:25565", "Example Server"));

        assertEquals("example.test:25565", this.resolve("server", false));
        assertEquals("", this.resolve("world", true));
    }

    private String resolve(String type, boolean fullWorldPath) {
        return this.placeholder.getReplacementFor(new DeserializedPlaceholderString(this.placeholder.getIdentifier(), Map.of("type", type, "full_world_path", Boolean.toString(fullWorldPath)), ""));
    }
}
