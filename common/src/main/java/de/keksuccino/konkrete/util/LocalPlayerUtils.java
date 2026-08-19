package de.keksuccino.konkrete.util;

import net.minecraft.client.player.LocalPlayer;

/** Utility methods for local player. */
public class LocalPlayerUtils {

    /** Sends player command. */
    public static void sendPlayerCommand(LocalPlayer player, String command) {
        player.connection.sendCommand(command);
    }

    /** Sends player chat message. */
    public static void sendPlayerChatMessage(LocalPlayer player, String message) {
        player.connection.sendChat(message);
    }

}
