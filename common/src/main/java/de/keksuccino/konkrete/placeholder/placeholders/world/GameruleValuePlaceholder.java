package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.networking.packets.placeholders.ServerPlaceholderRequests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;

/** Integrated-server or packet-backed gamerule placeholder. */
public final class GameruleValuePlaceholder extends AbstractWorldPlaceholder {
    /** Creates the local-or-packet-backed {@code gamerule_value} placeholder. */
    public GameruleValuePlaceholder() {
        super("gamerule_value");
    }

    /** Resolves an integrated-server rule directly or refreshes a multiplayer value without blocking. */
    @Override @NotNull public String getReplacementFor(@NotNull DeserializedPlaceholderString placeholder) {
        String name = normalize(placeholder.values.get("name"));
        if (name == null) return "";
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            String value = findRule(server.getGameRules(), name);
            return value == null ? "" : value;
        }
        return ServerPlaceholderRequests.resolveGamerule(name);
    }

    /** Returns the gamerule-name argument. */
    @Override @NotNull public List<String> getValueNames() {
        return List.of("name");
    }

    /** Resolves the world-category localization key. */
    @Override @NotNull protected String getLocalizationBase() {
        return "konkrete.placeholders.world.gamerule_value";
    }

    /** Builds syntax containing the required serialized gamerule {@code name}. */
    @Override @NotNull public DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("name", "doDaylightCycle");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    @Nullable private static String findRule(@NotNull GameRules rules, @NotNull String name) {
        String[] result = new String[1];
        rules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override public <T> void visit(GameRule<T> rule) {
                if (result[0] == null && rule.id().equalsIgnoreCase(name)) result[0] = rules.getAsString(rule);
            }
        });
        return result[0];
    }

    @Nullable private static String normalize(@Nullable String name) {
        if (name == null || name.isBlank()) return null;
        return name.trim();
    }
}
