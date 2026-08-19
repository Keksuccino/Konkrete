package de.keksuccino.konkrete.placeholder.placeholders.client;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Provider-backed named Minecraft option placeholder. */
public final class MinecraftOptionValuePlaceholder extends Placeholder {
    /** Creates the provider-backed {@code minecraft_option_value} placeholder. */
    public MinecraftOptionValuePlaceholder() {
        super("minecraft_option_value");
    }

    /** Named option providers commonly access client-only state. */
    @Override public boolean canRunAsync() {
        return false;
    }

    /** Resolves the named option through the configured provider. */
    @Override @Nullable public String getReplacementFor(@NotNull DeserializedPlaceholderString placeholder) {
        String name = placeholder.values.get("name");
        if (name == null) return null;
        String value = MinecraftOptionValueProviders.get().get(name);
        return value == null ? "" : value;
    }

    /** Returns the option-name argument. */
    @Override @NotNull public List<String> getValueNames() {
        return List.of("name");
    }

    /** Resolves the {@code konkrete.placeholders.minecraft_option_value} display-name key. */
    @Override @NotNull public String getDisplayName() {
        return I18n.get("konkrete.placeholders.minecraft_option_value");
    }

    /** Resolves the line-split {@code konkrete.placeholders.minecraft_option_value.desc} description. */
    @Override @NotNull public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.minecraft_option_value.desc"));
    }

    /** Resolves the client-category localization key. */
    @Override @NotNull public String getCategory() {
        return I18n.get("konkrete.requirements.categories.client");
    }

    /** Builds syntax with an explicit {@code name} argument for provider-defined option lookup. */
    @Override @NotNull public DeserializedPlaceholderString getDefaultPlaceholderString() {
        return DeserializedPlaceholderString.build(this.getIdentifier(), Map.of("name", "option_name"));
    }
}
