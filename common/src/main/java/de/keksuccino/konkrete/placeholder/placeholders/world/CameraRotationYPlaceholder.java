package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/** Exposes the active camera's wrapped yaw in degrees. */
public class CameraRotationYPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code camera_rotation_y} placeholder. */
    public CameraRotationYPlaceholder() {
        super("camera_rotation_y", "camera_rotation_y_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return camera != null ? String.valueOf(Mth.wrapDegrees(camera.getYRot())) : "0";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.camera_rotation_y";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
