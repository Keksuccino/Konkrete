package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/** Reads max mount health state from the current vanilla world/player for {@code max_mount_health}. */
public class MaxMountHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    /** Creates the {@code max_mount_health} placeholder. */
    public MaxMountHealthPlaceholder() {
        super("max_mount_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        Entity mount = player.getControlledVehicle();
        if (mount instanceof LivingEntity l) return l.getMaxHealth();
        return 0.0F;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.max_mount_health";
    }

}
