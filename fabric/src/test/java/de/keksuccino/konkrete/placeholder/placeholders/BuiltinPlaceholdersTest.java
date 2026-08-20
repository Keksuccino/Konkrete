package de.keksuccino.konkrete.placeholder.placeholders;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.placeholder.PlaceholderRegistry;
import de.keksuccino.konkrete.placeholder.placeholders.client.RecentDestinationProviders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderRegistry global state")
class BuiltinPlaceholdersTest {

    @AfterEach
    void clearRegistry() {
        PlaceholderRegistry.clear();
        RecentDestinationProviders.reset();
    }

    @Test
    void registerAllIsIdempotentAndPreservesDeclaredOrder() {
        BuiltinPlaceholders.registerAll();
        BuiltinPlaceholders.registerAll();

        assertEquals(BuiltinPlaceholders.all(), PlaceholderRegistry.getPlaceholders());
        assertEquals(BuiltinPlaceholders.all().size(), PlaceholderRegistry.getRegistrations(PlaceholderRegistry.KONKRETE_NAMESPACE).size());
        for (Placeholder placeholder : BuiltinPlaceholders.all()) assertSame(placeholder, PlaceholderRegistry.getPlaceholder("konkrete:" + placeholder.getIdentifier()));
    }

    @Test
    void allCoversEveryPublicStaticBuiltinExactlyOnce() throws ReflectiveOperationException {
        Set<Placeholder> declared = new HashSet<>();
        for (Field field : BuiltinPlaceholders.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Placeholder.class.isAssignableFrom(field.getType())) continue;
            declared.add((Placeholder) field.get(null));
        }

        List<Placeholder> managed = new ArrayList<>(BuiltinPlaceholders.all());
        managed.addAll(BuiltinPlaceholders.recentDestinationBacked());
        assertEquals(declared, new HashSet<>(managed));
        assertEquals(BuiltinPlaceholders.all().size(), new HashSet<>(BuiltinPlaceholders.all()).size());
        assertEquals(BuiltinPlaceholders.all().size(), BuiltinPlaceholders.all().stream().map(Placeholder::getIdentifier).distinct().count());
        assertTrue(BuiltinPlaceholders.all().size() >= 155, "Generic built-in coverage unexpectedly shrank");
    }

    @Test
    void newReusableStateIdentifiersPreserveSourceCategoryAndRelativeOrder() {
        List<String> identifiers = BuiltinPlaceholders.all().stream().map(Placeholder::getIdentifier).toList();
        List<String> expected = List.of("world_load_progress", "screenid", "clicks_per_second", "lastdeathmessage", "slot_item_display_name", "hovered_inventory_item", "current_boss_health", "boss_name", "boss_count", "action_bar_message", "action_bar_message_time", "highlighted_item_time", "current_title", "camera_rotation_x", "camera_rotation_y", "camera_rotation_delta_x", "camera_rotation_delta_y", "player_position_delta_x", "player_position_delta_y", "player_position_delta_z", "player_item_use_progress", "absolute_path");
        Set<String> expectedSet = new HashSet<>(expected);

        assertTrue(identifiers.containsAll(expected));
        assertEquals(expected, identifiers.stream().filter(expectedSet::contains).toList());
    }

    @Test
    void renamedStateIdentifiersResolveTheirSerializedCompatibilityAliases() {
        BuiltinPlaceholders.registerAll();

        assertSame(BuiltinPlaceholders.ACTION_BAR_MESSAGE, PlaceholderRegistry.getPlaceholder("action_bar_message_fm"));
        assertSame(BuiltinPlaceholders.ACTION_BAR_MESSAGE_TIME, PlaceholderRegistry.getPlaceholder("action_bar_message_time_fm"));
        assertSame(BuiltinPlaceholders.CAMERA_ROTATION_X, PlaceholderRegistry.getPlaceholder("camera_rotation_x_fm"));
        assertSame(BuiltinPlaceholders.CAMERA_ROTATION_Y, PlaceholderRegistry.getPlaceholder("camera_rotation_y_fm"));
        assertSame(BuiltinPlaceholders.CAMERA_ROTATION_DELTA_X, PlaceholderRegistry.getPlaceholder("camera_rotation_delta_x_fm"));
        assertSame(BuiltinPlaceholders.CAMERA_ROTATION_DELTA_Y, PlaceholderRegistry.getPlaceholder("camera_rotation_delta_y_fm"));
        assertSame(BuiltinPlaceholders.HIGHLIGHTED_ITEM_TIME, PlaceholderRegistry.getPlaceholder("highlighted_item_time_fm"));
        assertSame(BuiltinPlaceholders.PLAYER_ITEM_USE_PROGRESS, PlaceholderRegistry.getPlaceholder("player_item_use_progress_fm"));
        assertSame(BuiltinPlaceholders.PLAYER_POSITION_DELTA_X, PlaceholderRegistry.getPlaceholder("player_position_delta_x_fm"));
        assertSame(BuiltinPlaceholders.PLAYER_POSITION_DELTA_Y, PlaceholderRegistry.getPlaceholder("player_position_delta_y_fm"));
        assertSame(BuiltinPlaceholders.PLAYER_POSITION_DELTA_Z, PlaceholderRegistry.getPlaceholder("player_position_delta_z_fm"));
        assertSame(BuiltinPlaceholders.SLOT_ITEM_DISPLAY_NAME, PlaceholderRegistry.getPlaceholder("slot_item_display_name_fm"));
    }

    @Test
    void clientRenderAndWindowBuiltinsDeclareMainThreadConfinement() {
        List<Placeholder> confined = List.of(BuiltinPlaceholders.LOADED_MODS, BuiltinPlaceholders.TOTAL_MODS, BuiltinPlaceholders.WORLD_LOAD_PROGRESS, BuiltinPlaceholders.MINECRAFT_OPTION_VALUE, BuiltinPlaceholders.SCREEN_WIDTH, BuiltinPlaceholders.SCREEN_HEIGHT, BuiltinPlaceholders.CURRENT_SCREEN_IDENTIFIER, BuiltinPlaceholders.MOUSE_POS_X, BuiltinPlaceholders.MOUSE_POS_Y, BuiltinPlaceholders.GUI_SCALE, BuiltinPlaceholders.NBT_DATA_GET, BuiltinPlaceholders.NBT_DATA_GET_SERVER, BuiltinPlaceholders.GAMERULE_VALUE, BuiltinPlaceholders.LAST_WORLD_OR_SERVER, BuiltinPlaceholders.GPU_INFO, BuiltinPlaceholders.OPEN_GL_VERSION, BuiltinPlaceholders.FPS, BuiltinPlaceholders.TEXT_WIDTH, BuiltinPlaceholders.CLIPBOARD_CONTENT);

        for (Placeholder placeholder : confined) assertFalse(placeholder.canRunAsync(), placeholder.getIdentifier());
        assertTrue(BuiltinPlaceholders.CLICKS_PER_SECOND.canRunAsync());
    }

    @Test
    void packetBackedBuiltinsRegisterByDefaultWhileHistoryStillRequiresItsProvider() {
        BuiltinPlaceholders.registerAll();
        assertSame(BuiltinPlaceholders.NBT_DATA_GET_SERVER, PlaceholderRegistry.getPlaceholder("nbt_data_get_server"));
        assertSame(BuiltinPlaceholders.GAMERULE_VALUE, PlaceholderRegistry.getPlaceholder("gamerule_value"));
        assertSame(null, PlaceholderRegistry.getPlaceholder("last_world_server"));

        RecentDestinationProviders.set(() -> null);
        BuiltinPlaceholders.registerRecentDestinationBacked();
        BuiltinPlaceholders.registerRecentDestinationBacked();

        assertSame(BuiltinPlaceholders.LAST_WORLD_OR_SERVER, PlaceholderRegistry.getPlaceholder("last_world_server"));
    }

    @Test
    void unregisterAllLeavesThirdPartyNamespacesUntouched() {
        Placeholder thirdParty = new StaticPlaceholder("example");
        PlaceholderRegistry.register("third_party", thirdParty);
        BuiltinPlaceholders.registerAll();

        BuiltinPlaceholders.unregisterAll();

        assertEquals(List.of(thirdParty), PlaceholderRegistry.getPlaceholders());
        assertSame(thirdParty, PlaceholderRegistry.getPlaceholder("third_party:example"));
        assertEquals(List.of(), PlaceholderRegistry.getRegistrations(PlaceholderRegistry.KONKRETE_NAMESPACE));
    }

    private static final class StaticPlaceholder extends Placeholder {

        private StaticPlaceholder(String identifier) {
            super(identifier);
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString placeholder) {
            return "example";
        }

        @Override
        public List<String> getValueNames() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "Example";
        }

        @Override
        public List<String> getDescription() {
            return List.of();
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public DeserializedPlaceholderString getDefaultPlaceholderString() {
            return DeserializedPlaceholderString.build(this.getIdentifier(), null);
        }

    }

}
