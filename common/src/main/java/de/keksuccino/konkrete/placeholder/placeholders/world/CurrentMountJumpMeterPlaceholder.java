package de.keksuccino.konkrete.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.PlayerRideableJumping;
import org.jetbrains.annotations.NotNull;

/** Reads current mount jump meter state from the current vanilla world/player for {@code current_mount_jump_meter}. */
public class CurrentMountJumpMeterPlaceholder extends AbstractWorldIntegerPlaceholder {

    /** Creates the {@code current_mount_jump_meter} placeholder. */
    public CurrentMountJumpMeterPlaceholder() {
        super("current_mount_jump_meter");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        PlayerRideableJumping mount = player.jumpableVehicle();
        if (mount != null) return (int)(player.getJumpRidingScale() * 100.0F);
        return 0;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_mount_jump_meter";
    }

}
