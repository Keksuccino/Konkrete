package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

/** Reads inventory slot food point restore amount state from the current vanilla world/player for {@code inventory_slot_food_point_restore_amount}. */
public class InventorySlotFoodPointRestoreAmountPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code inventory_slot_food_point_restore_amount} placeholder. */
    public InventorySlotFoodPointRestoreAmountPlaceholder() {
        super("inventory_slot_food_point_restore_amount");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            LocalPlayer player = Minecraft.getInstance().player;
            String slotValue = dps.values.get("slot");

            if ((level != null) && (player != null) && (slotValue != null) && MathUtils.isInteger(slotValue)) {
                int slot = Integer.parseInt(slotValue);
                if (slot < 0 || slot >= player.getInventory().getContainerSize()) return "0.0";

                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.isEmpty()) return "0.0";

                FoodProperties foodProperties = stack.get(DataComponents.FOOD);
                if (foodProperties == null) return "0.0";

                return "" + (float) foodProperties.nutrition();
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0.0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("slot");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.inventory_slot_food_point_restore_amount");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.inventory_slot_food_point_restore_amount.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("slot", "0");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
