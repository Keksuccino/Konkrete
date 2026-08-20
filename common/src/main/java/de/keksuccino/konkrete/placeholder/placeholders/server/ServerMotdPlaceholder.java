package de.keksuccino.konkrete.placeholder.placeholders.server;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Exposes one of the first two lines of a server provider's MOTD. */
public final class ServerMotdPlaceholder extends AbstractServerStatusPlaceholder {

    /** Creates the serialized {@code servermotd} placeholder. */
    public ServerMotdPlaceholder() {
        super("servermotd", "servermotd");
    }

    /** Adds the requested one-based line argument. */
    @Override @NotNull public List<String> getValueNames() {
        return List.of("ip", "line");
    }

    /** Returns the requested MOTD line, clamped to the legacy first two lines. */
    @Override protected String getReplacement(@NotNull ServerStatus status, @NotNull DeserializedPlaceholderString placeholder) {
        int requestedLine;
        try {
            requestedLine = Integer.parseInt(placeholder.values.getOrDefault("line", "1"));
        } catch (NumberFormatException ignored) {
            return null;
        }
        int line = Math.clamp(requestedLine, 1, 2) - 1;
        String[] lines = status.motd().split("\\R", -1);
        return line < lines.length ? lines[line] : "";
    }

    /** Returns a representative line query. */
    @Override @NotNull public DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString value = super.getDefaultPlaceholderString();
        value.values.put("line", "1");
        return value;
    }

}
