package de.keksuccino.konkrete.placeholder.placeholders.client;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

/** Reads mod loader version metadata from the active client for {@code loaderver}. */
public class ModLoaderVersionPlaceholder extends Placeholder {

    /** Creates the {@code loaderver} placeholder. */
    public ModLoaderVersionPlaceholder() {
        super("loaderver");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return Services.PLATFORM.getLoaderVersion();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.loader_version");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.loader_version.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
