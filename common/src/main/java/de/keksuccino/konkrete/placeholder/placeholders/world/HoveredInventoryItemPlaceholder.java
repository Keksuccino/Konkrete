package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinAbstractContainerScreen;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.ScreenUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

/** Exposes the registry identifier of the item beneath the active container cursor. */
public class HoveredInventoryItemPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code hovered_inventory_item} placeholder. */
    public HoveredInventoryItemPlaceholder() {
        super("hovered_inventory_item");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Screen screen = ScreenUtils.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> container)) return "";
        Slot hovered = ((AccessorMixinAbstractContainerScreen) container).get_hoveredSlot_Konkrete();
        if (hovered == null || !hovered.hasItem()) return "";
        Identifier itemKey = Services.PLATFORM.getItemKey(hovered.getItem().getItem());
        return itemKey != null ? itemKey.toString() : "";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.hovered_inventory_item";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
