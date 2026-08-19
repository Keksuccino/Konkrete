package de.keksuccino.konkrete.util.rendering.ui.theme;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UIColorThemeRegistryTest {

    private List<UITheme> originalThemes;
    private String originalActiveTheme;
    private Consumer<UITheme> listener;

    @BeforeEach
    void captureRegistry() {
        this.originalThemes = UIColorThemeRegistry.getThemes();
        this.originalActiveTheme = UIColorThemeRegistry.getActiveTheme().getIdentifier();
        UIColorThemeRegistry.clearThemes();
    }

    @AfterEach
    void restoreRegistry() {
        if (this.listener != null) UIColorThemeRegistry.removeThemeChangeListener(this.listener);
        UIColorThemeRegistry.clearThemes();
        for (UITheme theme : this.originalThemes) UIColorThemeRegistry.register(theme);
        UIColorThemeRegistry.setActiveTheme(this.originalActiveTheme);
    }

    @Test
    void listenersReceiveTheResolvedActiveThemeAndCanBeRemoved() {
        UITheme theme = new UITheme("test", "Test Theme");
        List<UITheme> notifications = new ArrayList<>();
        this.listener = notifications::add;
        UIColorThemeRegistry.register(theme);
        UIColorThemeRegistry.addThemeChangeListener(this.listener);

        UIColorThemeRegistry.setActiveTheme("test");
        assertSame(theme, UIColorThemeRegistry.getActiveTheme());
        assertEquals(List.of(theme), notifications);

        assertTrue(UIColorThemeRegistry.removeThemeChangeListener(this.listener));
        this.listener = null;
        UIColorThemeRegistry.setActiveTheme("test");
        assertEquals(List.of(theme), notifications);
    }
}
