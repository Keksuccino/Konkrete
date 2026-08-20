package de.keksuccino.konkrete.placeholder.placeholders.server;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import org.jetbrains.annotations.NotNull;

/** Exposes a server provider's advertised version string. */
public final class ServerVersionPlaceholder extends AbstractServerStatusPlaceholder {

    /** Creates the serialized {@code serverversion} placeholder. */
    public ServerVersionPlaceholder() {
        super("serverversion", "serverversion");
    }

    /** Returns the advertised version. */
    @Override protected String getReplacement(@NotNull ServerStatus status, @NotNull DeserializedPlaceholderString placeholder) {
        return status.version();
    }

}
