package de.keksuccino.konkrete.util;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Provides null-safe access to screen titles. */
public class ScreenTitleUtils {

    /** Returns the screen title, falling back to an empty component for non-conforming screens. */
    @SuppressWarnings("all")
    public static Component getTitleOfScreen(@NotNull Screen screen) {
        Component c = screen.getTitle();
        if (c == null) return Component.empty();
        return c;
    }

    /** Returns the screen title's translation key, or {@code null} for a literal title. */
    @Nullable
    public static String getTitleLocalizationKeyOfScreen(@NotNull Screen screen) {
        Component title = ScreenTitleUtils.getTitleOfScreen(screen);
        if (title instanceof MutableComponent) {
            ComponentContents cc = title.getContents();
            if (cc instanceof TranslatableContents t) {
                return t.getKey();
            }
        }
        return null;
    }

    /** Replaces the vanilla screen title through the loader access widener/transformer. */
    public static void setScreenTitle(@NotNull Screen screen, @NotNull Component title) {
        screen.title = title;
    }

}
