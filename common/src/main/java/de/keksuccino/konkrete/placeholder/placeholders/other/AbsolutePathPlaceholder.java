package de.keksuccino.konkrete.placeholder.placeholders.other;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.file.LocalSourcePathResolver;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves a game-relative or documented Minecraft-relative path to a confined absolute path. */
public class AbsolutePathPlaceholder extends Placeholder {

    /** Creates the {@code absolute_path} placeholder. */
    public AbsolutePathPlaceholder() {
        super("absolute_path");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String path = dps.values.get("short_path");
        if (path == null || path.isBlank()) return "";
        try {
            return LocalSourcePathResolver.createForGameAndMinecraftDirectories().resolve(path).path().toString();
        } catch (Exception exception) {
            return "";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("short_path");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.absolute_path");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.absolute_path.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("short_path", "config/example.txt");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
