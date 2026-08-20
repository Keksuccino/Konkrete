package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.KonkreteFabric;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricDedicatedServerNetworkingLinkageTest {

    @Test
    void mainEntrypointAndNetworkingBootstrapHaveNoClientOnlyClassLinks() throws IOException {
        assertDedicatedServerSafe(KonkreteFabric.class);
        assertDedicatedServerSafe(PacketsFabric.class);
    }

    @Test
    void clientOnlyNetworkingLinksRemainInTheClientBootstrap() throws IOException {
        String classFile = classFileText(PacketsFabricClient.class);

        assertTrue(classFile.contains("net/minecraft/client/"));
        assertTrue(classFile.contains("net/fabricmc/fabric/api/client/"));
    }

    private static void assertDedicatedServerSafe(Class<?> type) throws IOException {
        String classFile = classFileText(type);
        assertFalse(classFile.contains("net/minecraft/client/"));
        assertFalse(classFile.contains("net/fabricmc/fabric/api/client/"));
        assertFalse(classFile.contains("PacketsFabricClient"));
    }

    private static String classFileText(Class<?> type) throws IOException {
        String resourceName = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resourceName)) {
            if (input == null) throw new IOException("Missing compiled class resource: " + resourceName);
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

}
