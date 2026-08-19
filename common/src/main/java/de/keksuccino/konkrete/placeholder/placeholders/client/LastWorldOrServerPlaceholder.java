package de.keksuccino.konkrete.placeholder.placeholders.client;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.SerializationHelper;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exposes optional consumer-owned connection history without installing listeners or persistence in Konkrete. */
public class LastWorldOrServerPlaceholder extends Placeholder {

    /** Creates the {@code last_world_server} placeholder. */
    public LastWorldOrServerPlaceholder() {
        super("last_world_server");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        RecentDestination destination = RecentDestinationProviders.get().getRecentDestination();
        if (destination == null) return "";
        String requestedType = dps.values.getOrDefault("type", "both");
        if (requestedType.equals("world") && destination.type() != RecentDestination.Type.WORLD) return "";
        if (requestedType.equals("server") && destination.type() != RecentDestination.Type.SERVER) return "";
        if (!requestedType.equals("both") && !requestedType.equals("world") && !requestedType.equals("server")) return "";
        boolean fullPath = SerializationHelper.INSTANCE.deserializeBoolean(true, dps.values.get("full_world_path"));
        return destination.type() == RecentDestination.Type.WORLD && !fullPath ? destination.displayName() : destination.identifier();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("type", "full_world_path");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.last_world_server");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.last_world_server.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("type", "both");
        values.put("full_world_path", "true");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
