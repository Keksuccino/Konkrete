package de.keksuccino.konkrete.networking.packets.placeholders.nbt;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerNbtDataRequestValidationTest {

    @Test
    void placeholderValuesNormalizeIntoAnImmutableQuery() {
        ServerNbtQuery query = ServerNbtQuery.fromPlaceholderValues(Map.of("source_type", " entity ", "entity_selector", " @s ", "nbt_path", " foodLevel ", "scale", "2.5", "return_type", " value "));

        assertEquals("entity", query.sourceType());
        assertEquals("@s", query.entitySelector());
        assertEquals("foodLevel", query.nbtPath());
        assertEquals("value", query.returnType());
        assertEquals(2.5D, query.scale());
        assertNull(query.blockPosition());
        assertNull(query.storageId());
    }

    @Test
    void rejectsUnboundedStringsAndNonFiniteScale() {
        ServerNbtQuery valid = new ServerNbtQuery("entity", "@s", null, null, "Inventory[0]", "value", 1.0D);
        assertTrue(ServerSideServerNbtDataRequestPacketLogic.isBounded(valid));
        assertFalse(ServerSideServerNbtDataRequestPacketLogic.isBounded(new ServerNbtQuery("entity", "x".repeat(1025), null, null, null, null, null)));
        assertFalse(ServerSideServerNbtDataRequestPacketLogic.isBounded(new ServerNbtQuery("entity", "@s", null, null, "x".repeat(2049), null, null)));
        assertFalse(ServerSideServerNbtDataRequestPacketLogic.isBounded(new ServerNbtQuery("entity", "@s", null, null, null, null, Double.NaN)));
        assertFalse(ServerSideServerNbtDataRequestPacketLogic.isBounded(new ServerNbtQuery("entity", "@s", null, null, null, null, Double.POSITIVE_INFINITY)));
    }
}
