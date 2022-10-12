package de.keksuccino.konkrete.networking.testpacket;

import de.keksuccino.fancymenu.commands.ClientExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientboundTestPacket {

    public static void handle(TestPacketMessage msg) {

        ClientExecutor.execute(() -> {
            if (Minecraft.getInstance().level != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(msg.someStringData));
            }
        });

    }

}
