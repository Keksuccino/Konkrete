package de.keksuccino.konkrete.util.mod;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UniversalModContainerTest {

    @Test
    void normalizesMissingMetadata() {
        UniversalModContainer mod = new UniversalModContainer(null, null, null, null, null);

        assertEquals("", mod.id());
        assertEquals("", mod.name());
        assertEquals("", mod.description());
        assertEquals("", mod.license());
        assertEquals(List.of(), mod.authors());
    }

    @Test
    void snapshotsAuthorsAndDiscardsNullEntries() {
        List<String> authors = new ArrayList<>();
        authors.add("Keksuccino");
        authors.add(null);

        UniversalModContainer mod = new UniversalModContainer("konkrete", null, "Core library", "Apache-2.0", authors);
        authors.add("Later mutation");

        assertEquals("konkrete", mod.name());
        assertEquals(List.of("Keksuccino"), mod.authors());
        assertThrows(UnsupportedOperationException.class, () -> mod.authors().add("Mutation"));
    }

}
