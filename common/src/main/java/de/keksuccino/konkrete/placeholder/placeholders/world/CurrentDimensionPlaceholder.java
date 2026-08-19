package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.locale.Language;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/** Reads current dimension state from the current vanilla world/player for {@code current_dimension}. */
public class CurrentDimensionPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code current_dimension} placeholder. */
    public CurrentDimensionPlaceholder() {
        super("current_dimension");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = this.getLevel();
        if (level == null) return "";

        Identifier dimensionId = level.dimension().identifier();
        String asKeyString = dps.values.get("as_key");
        boolean asKey = true;
        if ((asKeyString != null) && asKeyString.equalsIgnoreCase("false")) {
            asKey = false;
        }

        if (!asKey) {
            // Vanilla doesn't ship dimension name translations in the language files,
            // so we provide our own for the vanilla dimensions.
            if ("minecraft".equals(dimensionId.getNamespace())) {
                if ("overworld".equals(dimensionId.getPath()) || "the_nether".equals(dimensionId.getPath()) || "the_end".equals(dimensionId.getPath())) {
                    String localizationKey = "konkrete.dimensions.minecraft." + dimensionId.getPath();
                    if (Language.getInstance().has(localizationKey)) {
                        return I18n.get(localizationKey);
                    }
                }
            }

            // Fallback: if some environment provides dimension translations, use them.
            String fallbackTranslationKey = "dimension." + dimensionId.getNamespace() + "." + dimensionId.getPath();
            if (Language.getInstance().has(fallbackTranslationKey)) {
                return I18n.get(fallbackTranslationKey);
            }
        }

        return dimensionId.toString();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("as_key");
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_dimension";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("as_key", "true");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
