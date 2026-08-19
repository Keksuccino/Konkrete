package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.LevelData;
import de.keksuccino.konkrete.util.WorldUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/** Serializes the matching local world-save summary as JSON for {@code level_save_data}. */
public class WorldSaveDataPlaceholder extends Placeholder {

    /** Creates the client-thread-only {@code level_save_data} placeholder. */
    public WorldSaveDataPlaceholder() {
        super("level_save_data");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String levelName = dps.values.get("level_name");
        if (levelName == null) {
            return "";
        }

        List<LevelData> levelsData = WorldUtils.getLevelsAsData();

        for (LevelData data : levelsData) {
            if (data.settings_level_name.equals(levelName)) {
                return data.serialize();
            }
        }

        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("level_name");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.level_save_data");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.level_save_data.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("level_name", "World");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
