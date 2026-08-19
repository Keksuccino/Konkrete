package de.keksuccino.konkrete.placeholder.placeholders.server;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import org.jetbrains.annotations.NotNull;

/** Exposes a server provider's formatted online and maximum player counts. */
public final class ServerPlayerCountPlaceholder extends AbstractServerStatusPlaceholder {
    /** Creates the serialized {@code serverplayercount} placeholder. */
    public ServerPlayerCountPlaceholder() {
        super("serverplayercount", "serverplayercount");
    }

    /** Returns the provider's formatted online/max count. */
    @Override protected String getReplacement(@NotNull ServerStatus status, @NotNull DeserializedPlaceholderString placeholder) {
        return status.playerCount();
    }
}
