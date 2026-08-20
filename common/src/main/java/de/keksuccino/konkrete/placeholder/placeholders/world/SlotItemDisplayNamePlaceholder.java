package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinSpectatorGui;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.SerializationHelper;
import de.keksuccino.konkrete.util.MathUtils;
import de.keksuccino.konkrete.util.rendering.text.ComponentParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exposes an inventory slot's styled display name as component JSON, including spectator-menu slots. */
public class SlotItemDisplayNamePlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code slot_item_display_name} placeholder. */
    public SlotItemDisplayNamePlaceholder() {
        super("slot_item_display_name", "slot_item_display_name_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String slotValue = dps.values.get("slot");
        if (!MathUtils.isInteger(slotValue) || Minecraft.getInstance().player == null) return "";
        int slot = Integer.parseInt(slotValue);
        boolean ignoreSpectator = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("ignore_spectator"));
        if (Minecraft.getInstance().player.isSpectator() && slot >= 0 && slot <= 8 && !ignoreSpectator) {
            SpectatorMenu menu = ((AccessorMixinSpectatorGui) Minecraft.getInstance().gui.hud.getSpectatorGui()).get_menu_Konkrete();
            if (menu == null) return "";
            SpectatorMenuItem selected = menu.getSelectedItem();
            Component name = selected == SpectatorMenu.EMPTY_SLOT ? menu.getSelectedCategory().getPrompt() : selected.getName();
            return ComponentParser.toJson(name);
        }
        if (slot < 0 || slot >= Minecraft.getInstance().player.getInventory().getContainerSize()) return "";
        ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
        if (stack.isEmpty()) return "";
        return ComponentParser.toJson(Component.empty().append(stack.getHoverName()).withStyle(stack.getRarity().color()));
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("slot", "ignore_spectator");
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.slot_item_display_name";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("slot", "0");
        values.put("ignore_spectator", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
