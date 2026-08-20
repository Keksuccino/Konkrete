package de.keksuccino.konkrete.util.rendering.ui;

import de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser.BrowserAudioSettings;
import de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser.BrowserVideoSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UIConfigurationTest {

    private final UIConfiguration original = UIConfiguration.get();

    @AfterEach
    void restoreConfiguration() {
        UIConfiguration.set(this.original);
    }

    @Test
    void constructorNormalizesTheDataDirectory() {
        UIConfiguration configuration = configuration(Path.of("build", "ui-test", "..", "ui-data"));

        assertEquals(Path.of("build", "ui-data").toAbsolutePath().normalize(), configuration.dataDirectory());
    }

    @Test
    void setAndUpdateInstallCompleteImmutableSnapshots() {
        UIConfiguration initial = configuration(Path.of("build", "initial"));
        UIConfiguration.set(initial);

        UIConfiguration updated = UIConfiguration.update(current -> new UIConfiguration(UIScale.LARGE, current.useMinecraftFont(), current.blurEnabled(), current.blurIntensity(), current.animationsEnabled(), current.clickSoundsEnabled(), current.contextMenuHoverOpenSpeed(), current.pipWindowDockingEnabled(), current.pipWindowDebugEnabled(), current.smoothFontMultilineEnabled(), current.activeThemeIdentifier(), current.dataDirectory()));

        assertSame(updated, UIConfiguration.get());
        assertEquals(UIScale.LARGE, updated.uiScale());
        assertEquals(initial.dataDirectory(), updated.dataDirectory());
    }

    @Test
    void invalidNumericAndThemeValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new UIConfiguration(UIScale.AUTO, false, false, Float.NaN, true, true, 1, true, false, false, "dark", Path.of("config")));
        assertThrows(IllegalArgumentException.class, () -> new UIConfiguration(UIScale.AUTO, false, false, 1.0F, true, true, 0, true, false, false, "dark", Path.of("config")));
        assertThrows(IllegalArgumentException.class, () -> new UIConfiguration(UIScale.AUTO, false, false, 1.0F, true, true, 1, true, false, false, " ", Path.of("config")));
    }

    @Test
    void updateRejectsNullResultsWithoutChangingTheCurrentConfiguration() {
        UIConfiguration initial = configuration(Path.of("build", "unchanged"));
        UIConfiguration.set(initial);

        assertThrows(NullPointerException.class, () -> UIConfiguration.update(current -> null));
        assertSame(initial, UIConfiguration.get());
    }

    @Test
    void browserSettingsResolveAgainstTheCurrentDataDirectory() {
        UIConfiguration.set(configuration(Path.of("build", "first-data-directory")));
        assertEquals(UIConfiguration.get().dataDirectory().resolve("browser_audio_settings.json").toFile(), BrowserAudioSettings.getSettingsFile());
        assertEquals(UIConfiguration.get().dataDirectory().resolve("browser_video_settings.json").toFile(), BrowserVideoSettings.getSettingsFile());

        UIConfiguration.set(configuration(Path.of("build", "second-data-directory")));
        assertEquals(UIConfiguration.get().dataDirectory().resolve("browser_audio_settings.json").toFile(), BrowserAudioSettings.getSettingsFile());
        assertEquals(UIConfiguration.get().dataDirectory().resolve("browser_video_settings.json").toFile(), BrowserVideoSettings.getSettingsFile());
    }

    private static UIConfiguration configuration(Path dataDirectory) {
        return new UIConfiguration(UIScale.SMALL, true, true, 2.0F, true, false, 3, true, false, true, "dark", dataDirectory);
    }

}
