package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/** Reads inventory item count state from the current vanilla world/player for {@code inventory_item_count}. */
public class InventoryItemCountPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code inventory_item_count} placeholder. */
    public InventoryItemCountPlaceholder() {
        super("inventory_item_count");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return "0";

            Inventory inventory = Minecraft.getInstance().player.getInventory();
            String itemKey = dps.values.get("item");

            if (itemKey == null || itemKey.trim().isEmpty()) {
                int total = 0;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.isEmpty()) total += stack.getCount();
                }
                return "" + total;
            }

            Identifier itemId = Identifier.tryParse(itemKey.trim());
            if (itemId == null) return "0";

            Optional<Item> itemOptional = BuiltInRegistries.ITEM.getOptional(itemId);
            if (itemOptional.isEmpty()) return "0";
            Item targetItem = itemOptional.get();

            int total = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && stack.getItem() == targetItem) {
                    total += stack.getCount();
                }
            }
            return "" + total;
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("item");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.inventory_item_count");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.inventory_item_count.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("item", "minecraft:stone");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
