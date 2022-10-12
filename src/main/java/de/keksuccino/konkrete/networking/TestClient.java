package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.command.ClientExecutor;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TestClient {

    private static boolean packetSent = false;

    public static final ResourceLocation PACKET_TEST = new ResourceLocation("konkrete", "packet_test");

    public static void init() {

        Konkrete.getEventHandler().registerEventsFrom(TestClient.class);

        ClientPlayNetworking.registerGlobalReceiver(PACKET_TEST, (client, handler, buf, responseSender) -> {

            //Always read bytebuf data in packet thread, not main thread
            String s = buf.readUtf();

            ClientExecutor.execute(() -> {
                try {
                    if (client.level != null) {
                        client.player.sendSystemMessage(Component.literal("" + s));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        });

    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre e) {

        Screen s = Minecraft.getInstance().screen;
        if ((s != null) && (s instanceof PauseScreen)) {
            if (!packetSent) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeUtf("Message from client!");
                ClientPlayNetworking.send(PACKET_TEST, buf);
                packetSent = true;
            }
        } else {
            packetSent = false;
        }

    }

}
