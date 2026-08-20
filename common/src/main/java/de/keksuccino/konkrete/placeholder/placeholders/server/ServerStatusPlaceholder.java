package de.keksuccino.konkrete.placeholder.placeholders.server;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import org.jetbrains.annotations.NotNull;

/** Exposes a server provider's color-coded online state. */
public final class ServerStatusPlaceholder extends AbstractServerStatusPlaceholder {

    /** Creates the serialized {@code serverstatus} placeholder. */
    public ServerStatusPlaceholder() {
        super("serverstatus", "serverstatus");
    }

    /** Returns a color-coded state matching the legacy serialized contract. */
    @Override protected String getReplacement(@NotNull ServerStatus status, @NotNull DeserializedPlaceholderString placeholder) {
        return status.online() ? "§aOnline" : "§cOffline";
    }

}
