package de.keksuccino.konkrete.placeholder.placeholders.server;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import org.jetbrains.annotations.NotNull;

/** Exposes a server provider's last measured latency in milliseconds. */
public final class ServerPingPlaceholder extends AbstractServerStatusPlaceholder {

    /** Creates the serialized {@code serverping} placeholder. */
    public ServerPingPlaceholder() {
        super("serverping", "serverping");
    }

    /** Returns ping milliseconds. */
    @Override protected String getReplacement(@NotNull ServerStatus status, @NotNull DeserializedPlaceholderString placeholder) {
        return Long.toString(status.pingMillis());
    }

}
