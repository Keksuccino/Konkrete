package de.keksuccino.konkrete.side;

import de.keksuccino.konkrete.platform.Services;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public class ServerUtils {

    /**
     * Gets the active dedicated or integrated server instance.
     *
     * @return The active server, or {@code null} when no server is running.
     */
    @Nullable
    public static MinecraftServer getServer() {
        return Services.PLATFORM.getServer();
    }

    public static void assertIsOnServer() {
        if (Services.PLATFORM.isOnClient()) throw new RuntimeException("Wrong side! Should be server-side, but was client instead!");
    }

}
