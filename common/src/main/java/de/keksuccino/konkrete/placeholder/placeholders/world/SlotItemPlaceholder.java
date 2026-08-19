package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;

/** Reads slot item state from the current vanilla world/player for {@code slot_item}. */
public class SlotItemPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code slot_item} placeholder. */
    public SlotItemPlaceholder() {
        super("slot_item");
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
                ItemStack stack = player.getInventory().getItem(Integer.parseInt(slotValue));
                return "" + Services.PLATFORM.getItemKey(stack.getItem());
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "";

    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("slot");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world.slot_item");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world.slot_item.desc"));
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
