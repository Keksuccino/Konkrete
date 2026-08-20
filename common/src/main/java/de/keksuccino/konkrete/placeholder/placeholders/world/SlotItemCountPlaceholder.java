package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

/** Reads slot item count state from the current vanilla world/player for {@code slot_item_count}. */
public class SlotItemCountPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code slot_item_count} placeholder. */
    public SlotItemCountPlaceholder() {
        super("slot_item_count");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return "0";

            String slotValue = dps.values.get("slot");
            if (slotValue == null || !MathUtils.isInteger(slotValue)) return "0";

            int slot = Integer.parseInt(slotValue);
            if (slot < 0) return "0";

            Inventory inventory = Minecraft.getInstance().player.getInventory();
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) return "0";
            return "" + stack.getCount();
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("slot");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.slot_item_count");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.slot_item_count.desc"));
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
