//TODO übernehmen 1.5.1
package de.keksuccino.konkrete.command;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public class ClientCommandSourceStack extends CommandSourceStack {

    public ClientCommandSourceStack(CommandSource source, Vec3 position, Vec2 rotation, int permission, String plainTextName, Component displayName, Entity executing) {
        super(source, position, rotation, null, permission, plainTextName, displayName, null, executing);
    }

    @Override
    public void sendSuccess(Component message, boolean sendToAdmins) {
        Minecraft.getInstance().player.sendMessage(message, Util.NIL_UUID);
    }

    @Override
    public Collection<String> getAllTeams() {
        return Minecraft.getInstance().level.getScoreboard().getTeamNames();
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        Collection<String> l = new ArrayList<>();
        Minecraft.getInstance().getConnection().getOnlinePlayers().stream().forEach((player) -> {
            l.add(player.getProfile().getName());
        });
        return l;
    }

    @Override
    public Stream<ResourceLocation> getRecipeNames() {
        return Minecraft.getInstance().getConnection().getRecipeManager().getRecipeIds();
    }

    @Override
    public Set<ResourceKey<Level>> levels() {
        return Minecraft.getInstance().getConnection().levels();
    }

    @Override
    public RegistryAccess registryAccess() {
        return Minecraft.getInstance().getConnection().registryAccess();
    }

    @Override
    public MinecraftServer getServer() {
        throw new UnsupportedOperationException("Getting the server is not allowed in a client-only command");
    }

    @Override
    public ServerLevel getLevel() {
        throw new UnsupportedOperationException("Getting the server level is not allowed in a client-only command");
    }

}
