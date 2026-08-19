package de.keksuccino.konkrete.util;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Provides localization helpers with an optional integration hook for enumerating translation keys. */
public class LocalizationUtils {

    private static volatile Supplier<? extends Collection<String>> localizationKeySupplier = List::of;

    /** Configures the integration-owned source used to enumerate localization keys. */
    public static void setLocalizationKeySupplier(@NotNull Supplier<? extends Collection<String>> supplier) {
        localizationKeySupplier = Objects.requireNonNull(supplier, "supplier");
    }

    /** Restores the empty default localization-key source. */
    public static void resetLocalizationKeySupplier() {
        localizationKeySupplier = List::of;
    }

    /** Splits a localized value into literal components at newline boundaries. */
    @NotNull
    public static Component[] splitLocalizedLines(@NotNull String localizationKey, String... placeholderReplacements) {
        List<Component> l = new ArrayList<>();
        for (String s : splitLocalizedStringLines(localizationKey, placeholderReplacements)) {
            l.add(Component.literal(s));
        }
        return l.toArray(new Component[]{});
    }

    /** Splits a localized value into strings at newline boundaries. */
    @NotNull
    public static String[] splitLocalizedStringLines(@NotNull String localizationKey, String... placeholderReplacements) {
        return I18n.get(localizationKey, (Object[]) placeholderReplacements).split("\\R", -1);
    }

    /** Returns a defensive snapshot of keys published by the configured integration. */
    @NotNull
    public static List<String> getLocalizationKeys() {
        Collection<String> keys = localizationKeySupplier.get();
        return keys == null ? new ArrayList<>() : new ArrayList<>(keys);
    }

    /** Returns whether the active language contains the supplied localization key. */
    public static boolean isLocalizationKey(String key) {
        if (key == null) return false;
        return Language.getInstance().has(key);
    }

    /** Returns a translatable component's localization key, or {@code null} for other component types. */
    @Nullable
    public static String getComponentLocalizationKey(@NotNull Component component) {
        if (component instanceof MutableComponent m) {
            ComponentContents cc = m.getContents();
            if (cc instanceof TranslatableContents t) {
                return t.getKey();
            }
        }
        return null;
    }

}
