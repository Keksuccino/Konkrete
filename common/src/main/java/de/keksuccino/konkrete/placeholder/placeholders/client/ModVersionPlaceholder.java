package de.keksuccino.konkrete.placeholder.placeholders.client;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** Reads mod version metadata from the active client for {@code modversion}. */
public class ModVersionPlaceholder extends Placeholder {

    /** Creates the {@code modversion} placeholder. */
    public ModVersionPlaceholder() {
        super("modversion");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (!dps.values.containsKey("modid")) {
            return null;
        }
        return getModVersion(dps.values.get("modid"));
    }

    private String getModVersion(String modid) {
        return Services.PLATFORM.getModVersion(modid);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("modid");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.modversion");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.modversion.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("modid", "some_mod_id");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
