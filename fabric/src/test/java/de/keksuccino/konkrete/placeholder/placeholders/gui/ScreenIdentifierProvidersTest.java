package de.keksuccino.konkrete.placeholder.placeholders.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenIdentifierProvidersTest {

    @AfterEach
    void resetProvider() {
        ScreenIdentifierProviders.reset();
    }

    @Test
    void overrideIsVisibleAndResetRestoresClassNamePolicy() {
        ScreenIdentifierProvider defaultProvider = ScreenIdentifierProviders.get();
        ScreenIdentifierProvider override = screen -> "third_party:screen";

        ScreenIdentifierProviders.set(override);
        assertSame(override, ScreenIdentifierProviders.get());

        ScreenIdentifierProviders.reset();
        assertSame(defaultProvider, ScreenIdentifierProviders.get());
        assertNotSame(override, ScreenIdentifierProviders.get());
    }
}
