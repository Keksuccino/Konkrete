package de.keksuccino.konkrete.placeholder.placeholders.client;

import org.jetbrains.annotations.Nullable;

/** Supplies connection history owned by the embedding mod without requiring Konkrete persistence or listeners. */
@FunctionalInterface
public interface RecentDestinationProvider {

    /** Returns the most recent destination snapshot, or {@code null} when no history is available. */
    @Nullable RecentDestination getRecentDestination();
}
