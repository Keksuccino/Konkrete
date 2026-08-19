package de.keksuccino.konkrete.networking.packets.placeholders.gamerule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServerGameruleValueRequestValidationTest {

    @Test
    void trimsValidNamesAndRejectsMissingOrOversizedNames() {
        assertEquals("keepInventory", ServerSideServerGameruleValueRequestPacketLogic.normalizeGameruleName("  keepInventory  "));
        assertNull(ServerSideServerGameruleValueRequestPacketLogic.normalizeGameruleName(null));
        assertNull(ServerSideServerGameruleValueRequestPacketLogic.normalizeGameruleName("   "));
        assertNull(ServerSideServerGameruleValueRequestPacketLogic.normalizeGameruleName("x".repeat(129)));
    }
}
