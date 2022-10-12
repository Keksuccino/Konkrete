package de.keksuccino.konkrete.networking;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class TestServer {

    public static final ResourceLocation PACKET_TEST = new ResourceLocation("konkrete", "packet_test");

    public static void init() {

        ServerPlayNetworking.registerGlobalReceiver(PACKET_TEST, (server, player, handler, buf, responseSender) -> {

            String s = buf.readUtf();
            if ((s != null) && s.equals("Message from client!")) {

                FriendlyByteBuf sendBuf = new FriendlyByteBuf(Unpooled.buffer());
                sendBuf.writeUtf("Message from server!");
                ServerPlayNetworking.send(player, PACKET_TEST, sendBuf);

            } else {
                System.out.println("############################### ERROR IN TestServer !!!!!!!!!!!!!");
            }

        });

    }

}
