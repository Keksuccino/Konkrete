package de.keksuccino.konkrete;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.keksuccino.konkrete.command.ClientExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public class TestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("konk")
                .then(Commands.argument("test_argument", StringArgumentType.string())
                        .executes((stack) -> {
                            return openGui(stack.getSource(), StringArgumentType.getString(stack, "test_argument"));
                        })
                        .suggests((context, provider) -> {
                            return getStringSuggestions(provider, "this", "is", "a_test");
                        })
                )
        );
    }

    public static CompletableFuture<Suggestions> getStringSuggestions(SuggestionsBuilder suggestionsBuilder, String... suggestions) {
        return SharedSuggestionProvider.suggest(suggestions, suggestionsBuilder);
    }

    private static int openGui(CommandSourceStack stack, String menuIdentifierOrCustomGuiName) {
        ClientExecutor.execute(() -> {
            try {
                stack.sendSuccess(Component.literal("SUCCESS: " + menuIdentifierOrCustomGuiName), false);
                Minecraft.getInstance().setScreen(new PauseScreen(true));
            } catch (Exception e) {
                stack.sendFailure(Component.literal("ERROR WHILE TRYING TO EXECUTE COMMAND! || 2"));
                e.printStackTrace();
            }
        });
        return 1;
    }

}