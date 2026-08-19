package de.keksuccino.konkrete.networking;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base type for logical Konkrete packets transported through the optional bridge channel.
 */
public abstract class Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Processes this packet on the logical main thread.
     *
     * @param sender the sending player for serverbound packets, or {@code null} for clientbound packets
     * @return whether processing completed successfully
     */
    public abstract boolean processPacket(@Nullable ServerPlayer sender);

    /**
     * Processes a clientbound packet for the exact connection that received it.
     *
     * @param connection the receiving play connection
     * @return whether processing completed successfully
     */
    public boolean processClientPacket(@NotNull Connection connection) {
        return this.processPacket(null);
    }

    /**
     * Adds local packet feedback to the client chat when a world is active.
     *
     * @param message the feedback component
     * @param failure whether to render the component as an error
     */
    public void sendChatFeedback(@NotNull MutableComponent message, boolean failure) {
        try {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
                MutableComponent styledMessage = failure ? message.withStyle(ChatFormatting.RED) : message;
                Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(styledMessage);
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to send packet chat feedback.", ex);
        }
    }
}
