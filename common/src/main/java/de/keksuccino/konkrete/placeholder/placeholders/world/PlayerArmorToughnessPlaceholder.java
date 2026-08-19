package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/** Reads player armor toughness state from the current vanilla world/player for {@code player_armor_toughness}. */
public class PlayerArmorToughnessPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code player_armor_toughness} placeholder. */
    public PlayerArmorToughnessPlaceholder() {
        super("player_armor_toughness");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.player_armor_toughness";
    }
}
