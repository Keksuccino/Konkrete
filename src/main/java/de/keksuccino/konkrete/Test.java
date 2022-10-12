package de.keksuccino.konkrete;

import de.keksuccino.konkrete.networking.PacketHandler;
import de.keksuccino.konkrete.networking.testpacket.TestPacketMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Test {

    private static boolean packetSent = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {

        Screen s = Minecraft.getInstance().screen;
        if ((s != null) && (s instanceof PauseScreen)) {
            if (!packetSent) {
                TestPacketMessage msg = new TestPacketMessage();
                msg.direction = "server"; //to server
                msg.someStringData = "Hello from client!";
                PacketHandler.sendToServer(msg);
                packetSent = true;
            }
        } else {
            packetSent = false;
        }

    }

}
