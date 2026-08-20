package de.keksuccino.konkrete.networking.packets.placeholders.gamerule;

import de.keksuccino.konkrete.networking.PacketHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Resolves gamerule requests on the server main thread. */
final class ServerSideServerGameruleValueRequestPacketLogic {

    private static final int MAX_GAMERULE_NAME_LENGTH = 128;

    private ServerSideServerGameruleValueRequestPacketLogic() {
    }

    static boolean handle(@NotNull ServerPlayer sender, @NotNull ServerGameruleValueRequestPacket packet) {
        if (packet.requestId() <= 0) return false;
        String gameruleName = normalizeGameruleName(packet.gamerule());
        if (gameruleName == null) return false;
        MinecraftServer server = sender.level().getServer();
        String value = server == null ? null : getGameruleValue(server.getGameRules(), gameruleName);
        return PacketHandler.sendToClient(sender, new ServerGameruleValueResponsePacket(packet.requestId(), value)) == de.keksuccino.konkrete.networking.PacketSendResult.SENT;
    }

    static @Nullable String getGameruleValue(@NotNull GameRules gameRules, @NotNull String gameruleName) {
        String[] value = new String[1];
        gameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(GameRule<T> gameRule) {
                if (value[0] == null && gameRule.id().equalsIgnoreCase(gameruleName)) value[0] = gameRules.getAsString(gameRule);
            }
        });
        return value[0];
    }

    static @Nullable String normalizeGameruleName(@Nullable String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        return trimmed.isEmpty() || trimmed.length() > MAX_GAMERULE_NAME_LENGTH ? null : trimmed;
    }

}
