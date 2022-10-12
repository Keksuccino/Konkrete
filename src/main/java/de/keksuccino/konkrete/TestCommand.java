package de.keksuccino.konkrete;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.keksuccino.konkrete.command.ClientExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.StringTextComponent;

import java.util.concurrent.CompletableFuture;

public class TestCommand {

    public static void register(CommandDispatcher<CommandSource> d) {
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
        return ISuggestionProvider.suggest(suggestions, suggestionsBuilder);
    }

    private static int openGui(CommandSource stack, String menuIdentifierOrCustomGuiName) {
        ClientExecutor.execute(() -> {
            try {
                stack.sendSuccess(new StringTextComponent("SUCCESS: " + menuIdentifierOrCustomGuiName), false);
                Minecraft.getInstance().setScreen(new IngameMenuScreen(true));
            } catch (Exception e) {
                stack.sendFailure(new StringTextComponent("ERROR WHILE TRYING TO EXECUTE COMMAND! || 2"));
                e.printStackTrace();
            }
        });
        return 1;
    }

}