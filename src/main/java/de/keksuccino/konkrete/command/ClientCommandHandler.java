//TODO übernehmen 1.4.0
package de.keksuccino.konkrete.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientCommandRegistrationEvent;
import de.keksuccino.konkrete.events.client.ClientPlayerLoginEvent;
import de.keksuccino.konkrete.mixin.mixins.client.IMixinClientPacketListener;
import de.keksuccino.konkrete.mixin.mixins.client.IMixinLocalPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

public class ClientCommandHandler {

    private static final Logger LOGGER = LogManager.getLogger("konkrete/ClientCommandHandler");

    private static CommandDispatcher<CommandSourceStack> dispatcher;

    public static void init() {

        Konkrete.getEventHandler().registerEventsFrom(new ClientCommandHandler());
        ClientExecutor.init();

    }

    @SubscribeEvent
    public void onPlayerLogin(ClientPlayerLoginEvent e)  {

        CommandDispatcher<SharedSuggestionProvider> d = mergeWithServerCommands(new CommandDispatcher<>(), new CommandBuildContext(e.player.connection.registryAccess()));
        if (e.connection.getPacketListener() instanceof ClientPacketListener listener) {
            ((IMixinClientPacketListener)listener).setCommandsKonkrete(d);
        }

    }

    public static CommandDispatcher<SharedSuggestionProvider> mergeWithServerCommands(CommandDispatcher<SharedSuggestionProvider> serverCommands, CommandBuildContext buildContext) {

        CommandDispatcher<CommandSourceStack> commandsTemp = new CommandDispatcher<>();
        Konkrete.getEventHandler().callEventsFor(new ClientCommandRegistrationEvent(commandsTemp, buildContext));

        dispatcher = new CommandDispatcher<>();
        copy(commandsTemp.getRoot(), dispatcher.getRoot());

        RootCommandNode<SharedSuggestionProvider> serverCommandsRoot = serverCommands.getRoot();
        CommandDispatcher<SharedSuggestionProvider> newServerCommands = new CommandDispatcher<>();
        copy(serverCommandsRoot, newServerCommands.getRoot());

        mergeCommandNode(dispatcher.getRoot(), newServerCommands.getRoot(), new IdentityHashMap<>(), getSource(), (context) -> 0, (suggestions) -> {
            SuggestionProvider<SharedSuggestionProvider> suggestionProvider = SuggestionProviders
                    .safelySwap((SuggestionProvider<SharedSuggestionProvider>) (SuggestionProvider<?>) suggestions);
            if (suggestionProvider == SuggestionProviders.ASK_SERVER)
            {
                suggestionProvider = (context, builder) -> {
                    ClientCommandSourceStack source = getSource();
                    StringReader reader = new StringReader(context.getInput());
                    if (reader.canRead() && reader.peek() == '/')
                    {
                        reader.skip();
                    }

                    ParseResults<CommandSourceStack> parse = dispatcher.parse(reader, source);
                    return dispatcher.getCompletionSuggestions(parse);
                };
            }
            return suggestionProvider;
        });

        return newServerCommands;

    }

    private static <S, T> void mergeCommandNode(CommandNode<S> sourceNode, CommandNode<T> resultNode, Map<CommandNode<S>, CommandNode<T>> sourceToResult, S canUse, Command<T> execute, Function<SuggestionProvider<S>, SuggestionProvider<T>> sourceToResultSuggestion) {
        sourceToResult.put(sourceNode, resultNode);
        for (CommandNode<S> sourceChild : sourceNode.getChildren()) {
            if (sourceChild.canUse(canUse)) {
                resultNode.addChild(toResult(sourceChild, sourceToResult, canUse, execute, sourceToResultSuggestion));
            }
        }
    }

    private static <S, T> CommandNode<T> toResult(CommandNode<S> sourceNode, Map<CommandNode<S>, CommandNode<T>> sourceToResult, S canUse, Command<T> execute, Function<SuggestionProvider<S>, SuggestionProvider<T>> sourceToResultSuggestion) {

        if (sourceToResult.containsKey(sourceNode))
            return sourceToResult.get(sourceNode);

        ArgumentBuilder<T, ?> resultBuilder;
        if (sourceNode instanceof ArgumentCommandNode<?, ?>) {

            ArgumentCommandNode<S, ?> sourceArgument = (ArgumentCommandNode<S, ?>) sourceNode;
            RequiredArgumentBuilder<T, ?> resultArgumentBuilder = RequiredArgumentBuilder.argument(sourceArgument.getName(), sourceArgument.getType());
            if (sourceArgument.getCustomSuggestions() != null) {
                resultArgumentBuilder.suggests(sourceToResultSuggestion.apply(sourceArgument.getCustomSuggestions()));
            }
            resultBuilder = resultArgumentBuilder;

        } else if (sourceNode instanceof LiteralCommandNode<?>) {

            LiteralCommandNode<S> sourceLiteral = (LiteralCommandNode<S>) sourceNode;
            resultBuilder = LiteralArgumentBuilder.literal(sourceLiteral.getLiteral());

        } else if (sourceNode instanceof RootCommandNode<?>) {

            CommandNode<T> resultNode = new RootCommandNode<>();
            mergeCommandNode(sourceNode, resultNode, sourceToResult, canUse, execute, sourceToResultSuggestion);
            return resultNode;

        } else {
            throw new IllegalStateException("Node type " + sourceNode + " is not a standard node type");
        }

        if (sourceNode.getCommand() != null) {
            resultBuilder.executes(execute);
        }

        if (sourceNode.getRedirect() != null) {
            resultBuilder.redirect(toResult(sourceNode.getRedirect(), sourceToResult, canUse, execute, sourceToResultSuggestion));
        }

        CommandNode<T> resultNode = resultBuilder.build();
        mergeCommandNode(sourceNode, resultNode, sourceToResult, canUse, execute, sourceToResultSuggestion);

        return resultNode;

    }

    public static CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    public static ClientCommandSourceStack getSource() {
        LocalPlayer player = Minecraft.getInstance().player;
        return new ClientCommandSourceStack(player, player.position(), player.getRotationVector(), ((IMixinLocalPlayer)player).getPermissionLevelKonkrete(), player.getName().getString(), player.getDisplayName(), player);
    }

    private static <S> void copy(CommandNode<S> sourceNode, CommandNode<S> resultNode) {
        Map<CommandNode<S>, CommandNode<S>> newNodes = new IdentityHashMap<>();
        newNodes.put(sourceNode, resultNode);
        for (CommandNode<S> child : sourceNode.getChildren()) {
            CommandNode<S> copy = newNodes.computeIfAbsent(child, innerChild -> {
                ArgumentBuilder<S, ?> builder = innerChild.createBuilder();
                CommandNode<S> innerCopy = builder.build();
                copy(innerChild, innerCopy);
                return innerCopy;
            });
            resultNode.addChild(copy);
        }
    }

    public static boolean executeClientCommand(String command) {

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        StringReader reader = new StringReader(command);
        ClientCommandSourceStack source = getSource();

        try {
            dispatcher.execute(reader, source);
        } catch (CommandRuntimeException ex) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("").append(ex.getComponent()).withStyle(ChatFormatting.RED));
        }
        catch (CommandSyntaxException ex) {
            if (ex.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand() || ex.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()) {
                return false;
            }
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("").append(ComponentUtils.fromMessage(ex.getRawMessage())).withStyle(ChatFormatting.RED));
        } catch (Exception ex) {
            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("command.failed").withStyle(ChatFormatting.RED));
            LOGGER.error("Error while trying to execute client-only command '" + command + "'!");
        }

        return true;

    }

}
