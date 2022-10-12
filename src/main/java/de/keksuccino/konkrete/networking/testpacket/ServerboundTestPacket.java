package de.keksuccino.konkrete.networking.testpacket;

import de.keksuccino.konkrete.networking.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class ServerboundTestPacket {

    public static void handle(TestPacketMessage msg, ServerPlayer sender) {

        if (msg.someStringData.equals("Hello from client!")) {
            TestPacketMessage msgToClient = new TestPacketMessage();
            msgToClient.direction = "client"; //to client
            msgToClient.someStringData = "Hello from server!";
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> sender), msgToClient);
        }

    }

}
