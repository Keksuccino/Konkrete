package de.keksuccino.konkrete.command;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public class ClientCommandSourceStack extends CommandSource {

    public ClientCommandSourceStack(ICommandSource source, Vector3d position, Vector2f rotation, int permission, String plainTextName, ITextComponent displayName, Entity executing) {
        super(source, position, rotation, null, permission, plainTextName, displayName, null, executing);
    }

    @Override
    public void sendSuccess(ITextComponent message, boolean sendToAdmins) {
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
    public Set<RegistryKey<World>> levels() {
        return Minecraft.getInstance().getConnection().levels();
    }

    @Override
    public DynamicRegistries registryAccess() {
        return Minecraft.getInstance().getConnection().registryAccess();
    }

    @Override
    public MinecraftServer getServer() {
        throw new UnsupportedOperationException("Getting the server is not allowed in a client-only command");
    }

    @Override
    public ServerWorld getLevel() {
        throw new UnsupportedOperationException("Getting the server level is not allowed in a client-only command");
    }

}
