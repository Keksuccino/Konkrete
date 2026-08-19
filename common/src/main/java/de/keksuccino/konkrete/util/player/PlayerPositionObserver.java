package de.keksuccino.konkrete.util.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Tracks position changes of Minecraft's current camera entity. */
public final class PlayerPositionObserver {

    private static double currentPositionDeltaX;
    private static double currentPositionDeltaY;
    private static double currentPositionDeltaZ;
    private static double lastPositionX;
    private static double lastPositionY;
    private static double lastPositionZ;

    private PlayerPositionObserver() {
    }

    /** Samples one client tick; a missing camera resets deltas and prior position to zero. */
    public static void tick() {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            reset();
            return;
        }

        double currentX = cameraEntity.getX();
        double currentY = cameraEntity.getY();
        double currentZ = cameraEntity.getZ();

        currentPositionDeltaX = currentX - lastPositionX;
        currentPositionDeltaY = currentY - lastPositionY;
        currentPositionDeltaZ = currentZ - lastPositionZ;

        lastPositionX = currentX;
        lastPositionY = currentY;
        lastPositionZ = currentZ;
    }

    /** Returns the most recently sampled X delta. */
    public static double getCurrentPositionDeltaX() {
        return currentPositionDeltaX;
    }

    /** Returns the most recently sampled Y delta. */
    public static double getCurrentPositionDeltaY() {
        return currentPositionDeltaY;
    }

    /** Returns the most recently sampled Z delta. */
    public static double getCurrentPositionDeltaZ() {
        return currentPositionDeltaZ;
    }

    private static void reset() {
        currentPositionDeltaX = 0.0D;
        currentPositionDeltaY = 0.0D;
        currentPositionDeltaZ = 0.0D;
        lastPositionX = 0.0D;
        lastPositionY = 0.0D;
        lastPositionZ = 0.0D;
    }

}
