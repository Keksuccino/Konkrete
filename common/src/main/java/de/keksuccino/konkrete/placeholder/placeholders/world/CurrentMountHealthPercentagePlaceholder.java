package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/** Reads current mount health percentage state from the current vanilla world/player for {@code current_mount_health_percent}. */
public class CurrentMountHealthPercentagePlaceholder extends AbstractWorldPercentagePlaceholder {

    /** Creates the {@code current_mount_health_percent} placeholder. */
    public CurrentMountHealthPercentagePlaceholder() {
        super("current_mount_health_percent");
    }

    @Override
    protected float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        Entity mount = player.getControlledVehicle();
        if (mount instanceof LivingEntity l) return l.getHealth();
        return 0.0F;
    }

    @Override
    protected float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        Entity mount = player.getControlledVehicle();
        if (mount instanceof LivingEntity l) return l.getMaxHealth();
        return 0.0F;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_mount_health_percent";
    }

}
