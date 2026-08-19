package de.keksuccino.konkrete.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Utility methods for command. */
public class CommandUtils {

    /** Builds the string suggestions list. */
    public static CompletableFuture<Suggestions> buildStringSuggestionsList(@NotNull CommandContext<CommandSourceStack> context, @NotNull String... suggestions) {
        return CompletableFuture.supplyAsync(() -> {
            List<Suggestion> l = new ArrayList<>();
            Arrays.asList(suggestions).forEach(s -> l.add(new Suggestion(context.getRange(), s)));
            return new Suggestions(context.getRange(), l);
        });
    }

    /** Returns the string suggestions. */
    public static CompletableFuture<Suggestions> getStringSuggestions(@NotNull SuggestionsBuilder builder, @NotNull String... suggestions) {
        Arrays.asList(suggestions).forEach(builder::suggest);
        return builder.buildFuture();
    }

}
