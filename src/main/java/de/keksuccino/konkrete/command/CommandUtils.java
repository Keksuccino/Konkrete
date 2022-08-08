//TODO übernehmen 1.5.1
package de.keksuccino.konkrete.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class CommandUtils {

    public static CompletableFuture<Suggestions> getStringSuggestions(SuggestionsBuilder suggestionsBuilder, String... suggestions) {
        return SharedSuggestionProvider.suggest(suggestions, suggestionsBuilder);
    }

}
