package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.player.CameraRotationObserver;
import org.jetbrains.annotations.NotNull;

/** Exposes the camera yaw change recorded during the latest client tick. */
public class CameraRotationDeltaYPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code camera_rotation_delta_y} placeholder. */
    public CameraRotationDeltaYPlaceholder() {
        super("camera_rotation_delta_y", "camera_rotation_delta_y_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(CameraRotationObserver.getCurrentRotationDeltaY());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.camera_rotation_delta_y";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
