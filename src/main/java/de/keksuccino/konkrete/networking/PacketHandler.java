//TODO übernehmen FORGE BUILD
package de.keksuccino.konkrete.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandler {

    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation("konkrete", "main"), () -> NetworkRegistry.ACCEPTVANILLA, NetworkRegistry.ACCEPTVANILLA::equals, NetworkRegistry.ACCEPTVANILLA::equals);

    private static int messageIndex = -1;

    public static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer) {
        messageIndex++;
        INSTANCE.registerMessage(messageIndex, messageType, encoder, decoder, messageConsumer);
    }

    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }

    public static void send(PacketDistributor.PacketTarget target, Object message) {
        INSTANCE.send(target, message);
    }

}
