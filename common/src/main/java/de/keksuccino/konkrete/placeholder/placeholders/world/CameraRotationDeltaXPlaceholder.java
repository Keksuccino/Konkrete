package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.player.CameraRotationObserver;
import org.jetbrains.annotations.NotNull;

/** Exposes the camera pitch change recorded during the latest client tick. */
public class CameraRotationDeltaXPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code camera_rotation_delta_x} placeholder. */
    public CameraRotationDeltaXPlaceholder() {
        super("camera_rotation_delta_x", "camera_rotation_delta_x_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(CameraRotationObserver.getCurrentRotationDeltaX());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.camera_rotation_delta_x";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
