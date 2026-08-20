package de.keksuccino.konkrete.placeholder.placeholders.server;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/** Shared provider access and metadata for server-status placeholders. */
public abstract class AbstractServerStatusPlaceholder extends Placeholder {

    private final String localizationName;

    /** Creates a provider-backed server placeholder with an identifier and localization-key suffix. */
    protected AbstractServerStatusPlaceholder(@NotNull String identifier, @NotNull String localizationName) {
        super(identifier);
        this.localizationName = localizationName;
    }

    /** Maps a provider snapshot to the replacement string. */
    @Nullable protected abstract String getReplacement(@NotNull ServerStatus status, @NotNull DeserializedPlaceholderString placeholder);

    /** Resolves an address through the injected provider. */
    @Override
    @Nullable public final String getReplacementFor(@NotNull DeserializedPlaceholderString placeholder) {
        String address = placeholder.values.get("ip");
        if (address == null || address.isBlank()) return null;
        ServerStatus status = ServerStatusProviders.get().getStatus(address);
        return status != null ? this.getReplacement(status, placeholder) : null;
    }

    /** Returns the address argument. */
    @Override
    @NotNull public List<String> getValueNames() {
        return List.of("ip");
    }

    /** Resolves the display-name key for this server-status subtype. */
    @Override
    @NotNull public String getDisplayName() {
        return I18n.get("konkrete.placeholders." + this.localizationName);
    }

    /** Resolves and line-splits the description key for this server-status subtype. */
    @Override
    @NotNull public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders." + this.localizationName + ".desc"));
    }

    /** Resolves the server-category localization key. */
    @Override
    @NotNull public String getCategory() {
        return I18n.get("konkrete.requirements.categories.server");
    }

    /** Builds syntax with the required {@code ip} provider lookup argument. */
    @Override
    @NotNull public DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString value = new DeserializedPlaceholderString(this.getIdentifier(), null, "");
        value.values.put("ip", "example.com:25565");
        return value;
    }

}
