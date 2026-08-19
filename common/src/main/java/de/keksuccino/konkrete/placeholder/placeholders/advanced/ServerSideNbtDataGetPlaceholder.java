package de.keksuccino.konkrete.placeholder.placeholders.advanced;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.placeholder.remote.RemotePlaceholderProviders;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/** Provider-backed server NBT query placeholder. */
public final class ServerSideNbtDataGetPlaceholder extends Placeholder {
    /** Creates the provider-backed {@code nbt_data_get_server} placeholder. */
    public ServerSideNbtDataGetPlaceholder() {
        super("nbt_data_get_server");
    }

    /** Resolves the query through the configured remote provider. */
    @Override @Nullable public String getReplacementFor(@NotNull DeserializedPlaceholderString placeholder) {
        String key = placeholder.placeholderString.isEmpty() ? placeholder.toString() : placeholder.placeholderString;
        String value = RemotePlaceholderProviders.get().resolve("server_nbt", key, java.util.Map.copyOf(placeholder.values));
        return value == null ? "" : value;
    }

    /** Returns supported vanilla data-command source arguments. */
    @Override @NotNull public List<String> getValueNames() {
        return List.of("source_type", "entity_selector", "block_pos", "storage_id", "nbt_path", "scale", "return_type");
    }

    /** Resolves the server-NBT display-name localization key. */
    @Override @NotNull public String getDisplayName() {
        return I18n.get("konkrete.placeholders.nbt_data_get.server");
    }

    /** Resolves and line-splits the server-NBT description key. */
    @Override @NotNull public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.nbt_data_get.server.desc"));
    }

    /** Resolves the advanced-category localization key. */
    @Override @NotNull public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    /** Builds syntax containing source, target, path, scale, and return-format arguments. */
    @Override @NotNull public DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("source_type", "entity");
        values.put("entity_selector", "@s");
        values.put("block_pos", "");
        values.put("storage_id", "minecraft:storage_key");
        values.put("nbt_path", "foodLevel");
        values.put("scale", "1.0");
        values.put("return_type", "value");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
