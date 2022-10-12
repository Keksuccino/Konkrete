package de.keksuccino.konkrete.networking.testpacket;

import de.keksuccino.konkrete.networking.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class TestPacketRegistry {

    public static void registerAll() {

        //Register and handle a packet
        PacketHandler.registerMessage(TestPacketMessage.class, (msg, buf) -> {

            //Write data from message to byte buf
            buf.writeUtf(msg.direction);
            buf.writeUtf(msg.someStringData);

        }, (buf) -> {

            //Write data from byte buf to msg
            TestPacketMessage msg = new TestPacketMessage();
            msg.direction = buf.readUtf();
            msg.someStringData = buf.readUtf();
            return msg;

        }, (msg, context) -> {

            //Handle packet
            context.get().enqueueWork(() -> {
                //Handle on client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    //Handle both sides on client, because integrated server needs handling too
                    if (msg.direction.equals("server")) {
                        ServerboundTestPacket.handle(msg, context.get().getSender());
                    } else if (msg.direction.equals("client")) {
                        ClientboundTestPacket.handle(msg);
                    }
                });
                //Handle on server
                DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
                    ServerboundTestPacket.handle(msg, context.get().getSender());
                });
            });
            context.get().setPacketHandled(true);

        });

    }

}
