package de.keksuccino.konkrete.util.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Tracks pitch and yaw changes of Minecraft's current camera entity. */
public final class CameraRotationObserver {

    private static float currentRotationDeltaX;
    private static float currentRotationDeltaY;
    private static float lastRotationX;
    private static float lastRotationY;

    private CameraRotationObserver() {
    }

    /** Samples one client tick; a missing camera resets deltas and prior rotation to zero. */
    public static void tick() {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            reset();
            return;
        }

        float currentRotationX = cameraEntity.getXRot();
        float currentRotationY = cameraEntity.getYRot();

        currentRotationDeltaX = currentRotationX - lastRotationX;
        currentRotationDeltaY = currentRotationY - lastRotationY;

        lastRotationX = currentRotationX;
        lastRotationY = currentRotationY;
    }

    /** Returns the most recently sampled pitch delta. */
    public static float getCurrentRotationDeltaX() {
        return currentRotationDeltaX;
    }

    /** Returns the most recently sampled yaw delta. */
    public static float getCurrentRotationDeltaY() {
        return currentRotationDeltaY;
    }

    private static void reset() {
        currentRotationDeltaX = 0.0F;
        currentRotationDeltaY = 0.0F;
        lastRotationX = 0.0F;
        lastRotationY = 0.0F;
    }

}
