package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/** Reads world players list state from the current vanilla world/player for {@code world_players_list}. */
public class WorldPlayersListPlaceholder extends Placeholder {

    /** Creates the {@code world_players_list} placeholder. */
    public WorldPlayersListPlaceholder() {
        super("world_players_list");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        String separator = dps.values.get("separator");
        if (separator == null) separator = ", ";

        if (level != null) {
            List<AbstractClientPlayer> players = level.players();

            return players.stream()
                    .map(player -> player.getName().getString())
                    .sorted()
                    .collect(Collectors.joining(separator));
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("separator");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.players_list");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.players_list.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("separator", ", ");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
