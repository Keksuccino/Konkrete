package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

/** Reads slot item durability state from the current vanilla world/player for {@code slot_item_durability}. */
public class SlotItemDurabilityPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code slot_item_durability} placeholder. */
    public SlotItemDurabilityPlaceholder() {
        super("slot_item_durability");
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
            if (level == null || player == null) return "0";

            String slotValue = dps.values.get("slot");
            if (slotValue == null || !MathUtils.isInteger(slotValue)) return "0";

            int slot = Integer.parseInt(slotValue);
            Inventory inventory = player.getInventory();
            if (slot < 0 || slot >= inventory.getContainerSize()) return "0";

            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) return "0";

            int max = stack.getMaxDamage();
            int damage = stack.getDamageValue();
            int current = Math.max(0, max - damage);

            String format = dps.values.get("format");
            if (format == null) format = "current";

            return switch (format.toLowerCase()) {
                case "max" -> "" + max;
                case "damage" -> "" + damage;
                case "percentage", "percent" -> {
                    if (max <= 0) {
                        yield "0";
                    }
                    int percent = Math.round((current / (float) max) * 100.0F);
                    yield "" + percent;
                }
                case "current", "remaining" -> "" + current;
                default -> "" + current;
            };
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("slot", "format");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.slot_item_durability");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.slot_item_durability.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("slot", "0");
        values.put("format", "current");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
