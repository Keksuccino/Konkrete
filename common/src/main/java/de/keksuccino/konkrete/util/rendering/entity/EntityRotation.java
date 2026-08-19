package de.keksuccino.konkrete.util.rendering.entity;

/**
 * Immutable three-axis entity-model rotation stored in radians.
 *
 * @param x rotation around the horizontal axis in radians
 * @param y rotation around the vertical axis in radians
 * @param z rotation around the depth axis in radians
 */
public record EntityRotation(float x, float y, float z) {

    /** Creates a rotation from radian values. */
    public static EntityRotation radians(float x, float y, float z) {
        return new EntityRotation(x, y, z);
    }

    /** Creates a rotation from degree values. */
    public static EntityRotation degrees(float x, float y, float z) {
        return new EntityRotation((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }

    /** Returns the horizontal-axis rotation in degrees. */
    public float xDegrees() {
        return (float) Math.toDegrees(this.x);
    }

    /** Returns the vertical-axis rotation in degrees. */
    public float yDegrees() {
        return (float) Math.toDegrees(this.y);
    }

    /** Returns the depth-axis rotation in degrees. */
    public float zDegrees() {
        return (float) Math.toDegrees(this.z);
    }

}
