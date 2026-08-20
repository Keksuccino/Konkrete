package de.keksuccino.konkrete.util.rendering;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionfc;

/** Contains stateless helpers for render rotation. */
public final class RenderRotationUtil {

    private static final ThreadLocal<RotationState> ACTIVE_RENDER_ROTATION_KONKRETE = ThreadLocal.withInitial(RotationState::new);

    private RenderRotationUtil() {
    }

    /**
     * Returns the current rotation state accumulated via {@code PoseStack#mulPose(...)} calls.
     * This reflects only additional pose rotations (not the base GUI scale).
     */
    public static RotationState getCurrentAdditionalRenderRotation_Konkrete() {
        return ACTIVE_RENDER_ROTATION_KONKRETE.get();
    }

    /**
     * Returns the forward 2x2 rotation/tilt matrix for GUI space.
     */
    public static Rotation2D getCurrentAdditionalRenderRotation2D() {
        RotationState rotation = ACTIVE_RENDER_ROTATION_KONKRETE.get();
        float x = rotation.x;
        float y = rotation.y;
        float z = rotation.z;
        float w = rotation.w;

        float m00 = 1.0F - 2.0F * y * y - 2.0F * z * z;
        float m01 = 2.0F * x * y - 2.0F * z * w;
        float m10 = 2.0F * x * y + 2.0F * z * w;
        float m11 = 1.0F - 2.0F * x * x - 2.0F * z * z;

        return new Rotation2D(m00, m01, m10, m11);
    }

    /**
     * Returns the 2x2 matrix used for masking rotated/tilted GUI elements.
     * This inverts the rotation/tilt and flips the Z component to match GUI rotation direction.
     */
    public static Rotation2D getCurrentAdditionalRenderMaskRotation2D() {
        RotationState rotation = ACTIVE_RENDER_ROTATION_KONKRETE.get();
        float x = rotation.x;
        float y = rotation.y;
        float z = -rotation.z;
        float w = rotation.w;

        float m00 = 1.0F - 2.0F * y * y - 2.0F * z * z;
        float m01 = 2.0F * x * y - 2.0F * z * w;
        float m10 = 2.0F * x * y + 2.0F * z * w;
        float m11 = 1.0F - 2.0F * x * x - 2.0F * z * z;

        float det = m00 * m11 - m01 * m10;
        if (!Float.isFinite(det) || Math.abs(det) < 1.0E-6F) {
            return Rotation2D.identity();
        }
        float invDet = 1.0F / det;
        return new Rotation2D(m11 * invDet, -m01 * invDet, -m10 * invDet, m00 * invDet);
    }

    /**
     * Returns the inverse 2x2 rotation/tilt matrix for GUI space.
     * This is suitable for mapping screen pixels back into un-rotated element space.
     */
    public static Rotation2D getCurrentAdditionalRenderInverseRotation2D() {
        Rotation2D rotation = getCurrentAdditionalRenderRotation2D();
        float det = rotation.m00() * rotation.m11() - rotation.m01() * rotation.m10();
        if (!Float.isFinite(det) || Math.abs(det) < 1.0E-6F) {
            return Rotation2D.identity();
        }
        float invDet = 1.0F / det;
        return new Rotation2D(rotation.m11() * invDet, -rotation.m01() * invDet, -rotation.m10() * invDet, rotation.m00() * invDet);
    }

    /** Clears active render rotation state. */
    @ApiStatus.Internal
    public static void resetActiveRenderRotation_Konkrete() {
        ACTIVE_RENDER_ROTATION_KONKRETE.get().setIdentity();
    }

    /** Sets active render rotation for this render rotation util. */
    @ApiStatus.Internal
    public static void setActiveRenderRotation_Konkrete(RotationState state) {
        ACTIVE_RENDER_ROTATION_KONKRETE.get().set(state);
    }

    /** Stores a two-dimensional rotation matrix. */
    public record Rotation2D(float m00, float m01, float m10, float m11) {

        /** Creates an identity rotation with no angular offset. */
        public static Rotation2D identity() {
            return new Rotation2D(1.0F, 0.0F, 0.0F, 1.0F);
        }

    }

    /** Captures the immutable state needed for rotation. */
    public static final class RotationState {

        /** Horizontal component of the current transform. */
        public float x = 0.0F;
        /** Vertical component of the current transform. */
        public float y = 0.0F;
        /** Depth component of the current transform. */
        public float z = 0.0F;
        /** Scalar component of the current quaternion. */
        public float w = 1.0F;

        /** Creates an empty rotation state with default state. */
        public RotationState() {
        }

        /** Copies another rotation state's angles and cached transform. */
        public RotationState(RotationState other) {
            this.set(other);
        }

        /** Replaces the value held by this object. */
        public void set(RotationState other) {
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.w = other.w;
        }

        /** Sets identity for this rotation state. */
        public void setIdentity() {
            this.x = 0.0F;
            this.y = 0.0F;
            this.z = 0.0F;
            this.w = 1.0F;
        }

        /** Composes this rotation state with the supplied axis rotations. */
        public void mul(Quaternionfc quaternion) {
            float qx = quaternion.x();
            float qy = quaternion.y();
            float qz = quaternion.z();
            float qw = quaternion.w();

            float nx = this.w * qx + this.x * qw + this.y * qz - this.z * qy;
            float ny = this.w * qy - this.x * qz + this.y * qw + this.z * qx;
            float nz = this.w * qz + this.x * qy - this.y * qx + this.z * qw;
            float nw = this.w * qw - this.x * qx - this.y * qy - this.z * qz;

            this.x = nx;
            this.y = ny;
            this.z = nz;
            this.w = nw;

            float len = (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
            if (len > 0.0F && Float.isFinite(len)) {
                float invLen = 1.0F / len;
                this.x *= invLen;
                this.y *= invLen;
                this.z *= invLen;
                this.w *= invLen;
            }
        }

    }

}
