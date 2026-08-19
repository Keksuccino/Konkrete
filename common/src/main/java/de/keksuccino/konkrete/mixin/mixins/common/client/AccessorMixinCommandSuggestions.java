package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public interface AccessorMixinCommandSuggestions {

    @Accessor("allowSuggestions") boolean get_allowSuggestions_Konkrete();

    @Accessor("keepSuggestions") boolean get_keepSuggestions_Konkrete();

    @Accessor("currentParse") @Nullable ParseResults<ClientSuggestionProvider> get_currentParse_Konkrete();

    @Accessor("currentParse") void set_currentParse_Konkrete(ParseResults<ClientSuggestionProvider> currentParse);

    @Accessor("pendingSuggestions") @Nullable CompletableFuture<Suggestions> get_pendingSuggestions_Konkrete();

    @Accessor("pendingSuggestions") void set_pendingSuggestions_Konkrete(CompletableFuture<Suggestions> pendingSuggestions);

    @Accessor("commandUsage") List<FormattedCharSequence> get_commandUsage_Konkrete();

    @Accessor("suggestions") @Nullable CommandSuggestions.SuggestionsList get_suggestions_Konkrete();

    @Accessor("suggestions") void set_suggestions_Konkrete(CommandSuggestions.SuggestionsList suggestions);

    @Invoker("updateUsageInfo") void invoke_updateUsageInfo_Konkrete(ParseResults<ClientSuggestionProvider> currentParse, Suggestions suggestions);

    @Invoker("sortSuggestions") List<Suggestion> invoke_sortSuggestions_Konkrete(Suggestions suggestions);

}
