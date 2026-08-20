package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/** Exposes the active camera's wrapped pitch in degrees. */
public class CameraRotationXPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code camera_rotation_x} placeholder. */
    public CameraRotationXPlaceholder() {
        super("camera_rotation_x", "camera_rotation_x_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return camera != null ? String.valueOf(Mth.wrapDegrees(camera.getXRot())) : "0";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.camera_rotation_x";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
