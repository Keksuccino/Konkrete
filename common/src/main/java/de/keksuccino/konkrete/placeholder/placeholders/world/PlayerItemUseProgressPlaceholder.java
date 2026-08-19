package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Exposes normalized use progress for the local player's active item. */
public class PlayerItemUseProgressPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code player_item_use_progress} placeholder. */
    public PlayerItemUseProgressPlaceholder() {
        super("player_item_use_progress", "player_item_use_progress_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        LocalPlayer player = this.getPlayer();
        if (player == null || !player.isUsingItem()) return "0.0";
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty()) return "0.0";
        int totalDuration = stack.getUseDuration(player);
        if (totalDuration <= 0) return "0.0";
        float progress = (totalDuration - player.getUseItemRemainingTicks()) / (float) totalDuration;
        if (stack.getItem() instanceof BowItem) progress = Math.min((progress * progress + progress * 2.0F) / 3.0F, 1.0F);
        return String.valueOf(progress);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_item_use_progress";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
