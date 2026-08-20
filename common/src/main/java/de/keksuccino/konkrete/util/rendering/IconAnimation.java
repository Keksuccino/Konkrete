package de.keksuccino.konkrete.util.rendering;

import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Tracks the timing and transforms of icon. */
public final class IconAnimation {

    private final float durationMs;
    @NotNull
    private final OffsetProvider offsetProvider;

    /** Pairs an animation duration with its per-frame offset provider. */
    public IconAnimation(float durationMs, @NotNull OffsetProvider offsetProvider) {
        this.durationMs = Math.max(1.0F, durationMs);
        this.offsetProvider = Objects.requireNonNull(offsetProvider);
    }

    /** Returns duration ms. */
    public float getDurationMs() {
        return this.durationMs;
    }

    /** Creates independent playback state for this immutable animation. */
    @NotNull
    public Instance createInstance() {
        return new Instance(this);
    }

    /** Returns the sampled animation translation. */
    @NotNull
    public Offset getOffset(float elapsedMs) {
        if (elapsedMs <= 0.0F || elapsedMs >= this.durationMs) {
            return Offset.ZERO;
        }
        float t = elapsedMs / this.durationMs;
        return this.offsetProvider.getOffset(t);
    }

    /** Returns offset x. */
    public float getOffsetX(float elapsedMs) {
        return this.getOffset(elapsedMs).x();
    }

    /** Returns offset y. */
    public float getOffsetY(float elapsedMs) {
        return this.getOffset(elapsedMs).y();
    }

    /** Returns rotation degrees. */
    public float getRotationDegrees(float elapsedMs) {
        return this.getOffset(elapsedMs).rotationDegrees();
    }

    /** Returns width offset. */
    public float getWidthOffset(float elapsedMs) {
        return this.getOffset(elapsedMs).widthOffset();
    }

    /** Returns height offset. */
    public float getHeightOffset(float elapsedMs) {
        return this.getOffset(elapsedMs).heightOffset();
    }

    /** Returns animation. */
    @NotNull
    public IconAnimation getAnimation() {
        return this;
    }

    /** Supplies offset values on demand. */
    @FunctionalInterface
    public interface OffsetProvider {

        /** Returns offset for this widget. */
        @NotNull
        Offset getOffset(float t);

    }

    /** Tracks playback state for one use of an icon animation. */
    public static final class Instance {

        @NotNull
        private final IconAnimation animation;
        private long startMs = -1L;
        private boolean looping = false;
        private float loopDelayMs = 0.0F;
        private int loopCount = 0;

        private Instance(@NotNull IconAnimation animation) {
            this.animation = animation;
        }

        /** Starts this animation from its initial keyframe. */
        public void start() {
            this.startMs = Util.getMillis();
        }

        /** Restores this instance to its initial state. */
        public void reset() {
            this.startMs = -1L;
        }

        /** Returns whether running. */
        public boolean isRunning() {
            if (this.startMs < 0L) {
                return false;
            }
            float elapsedMs = (float) (Util.getMillis() - this.startMs);
            if (!this.looping) {
                if (elapsedMs >= this.animation.durationMs) {
                    this.startMs = -1L;
                    return false;
                }
                return true;
            }
            float lastPlayEnd = getLastPlayEndMs();
            if (lastPlayEnd > 0.0F && elapsedMs >= lastPlayEnd) {
                this.startMs = -1L;
                return false;
            }
            return true;
        }

        /** Returns the sampled animation translation. */
        @NotNull
        public Offset getOffset() {
            if (this.startMs < 0L) {
                return Offset.ZERO;
            }
            float elapsedMs = (float) (Util.getMillis() - this.startMs);
            if (!this.looping) {
                Offset offset = this.animation.getOffset(elapsedMs);
                if (elapsedMs >= this.animation.durationMs) {
                    this.startMs = -1L;
                }
                return offset;
            }
            float lastPlayEnd = getLastPlayEndMs();
            if (lastPlayEnd > 0.0F && elapsedMs >= lastPlayEnd) {
                this.startMs = -1L;
                return Offset.ZERO;
            }
            float cycleDuration = this.animation.durationMs + this.loopDelayMs;
            if (cycleDuration <= 0.0F) {
                return Offset.ZERO;
            }
            float timeInCycle = elapsedMs % cycleDuration;
            if (timeInCycle >= this.animation.durationMs) {
                return Offset.ZERO;
            }
            return this.animation.getOffset(timeInCycle);
        }

        /** Returns offset x. */
        public float getOffsetX() {
            return this.getOffset().x();
        }

        /** Returns offset y. */
        public float getOffsetY() {
            return this.getOffset().y();
        }

        /** Returns rotation degrees. */
        public float getRotationDegrees() {
            return this.getOffset().rotationDegrees();
        }

        /** Returns width offset. */
        public float getWidthOffset() {
            return this.getOffset().widthOffset();
        }

        /** Returns height offset. */
        public float getHeightOffset() {
            return this.getOffset().heightOffset();
        }

        /** Sets looping for this instance. */
        @NotNull
        public Instance setLooping(boolean looping) {
            this.looping = looping;
            return this;
        }

        /** Sets loop delay ms for this instance. */
        @NotNull
        public Instance setLoopDelayMs(float loopDelayMs) {
            this.loopDelayMs = Math.max(0.0F, loopDelayMs);
            return this;
        }

        /** Sets loop count for this instance. */
        @NotNull
        public Instance setLoopCount(int loopCount) {
            this.loopCount = loopCount;
            return this;
        }

        /** Returns animation. */
        @NotNull
        public IconAnimation getAnimation() {
            return this.animation;
        }

        private float getLastPlayEndMs() {
            if (this.loopCount <= 0) {
                return -1.0F;
            }
            float cycleDuration = this.animation.durationMs + this.loopDelayMs;
            return ((this.loopCount - 1) * cycleDuration) + this.animation.durationMs;
        }

    }

    /** Stores an icon animation's translation, rotation, and size offsets. */
    public static final class Offset {

        /** Zero-transform offset shared by all icon animations. */
        public static final Offset ZERO = new Offset(0.0F, 0.0F);

        private final float x;
        private final float y;
        private final float rotationDegrees;
        private final float widthOffset;
        private final float heightOffset;

        /** Stores translation, rotation, and optional size deltas for one animation sample. */
        public Offset(float x, float y) {
            this(x, y, 0.0F);
        }

        /** Stores translation, rotation, and optional size deltas for one animation sample. */
        public Offset(float x, float y, float rotationDegrees) {
            this(x, y, rotationDegrees, 0.0F, 0.0F);
        }

        /** Stores translation, rotation, and optional size deltas for one animation sample. */
        public Offset(float x, float y, float rotationDegrees, float widthOffset, float heightOffset) {
            this.x = x;
            this.y = y;
            this.rotationDegrees = rotationDegrees;
            this.widthOffset = widthOffset;
            this.heightOffset = heightOffset;
        }

        /** Returns the horizontal offset in GUI units. */
        public float x() {
            return this.x;
        }

        /** Returns the vertical offset in GUI units. */
        public float y() {
            return this.y;
        }

        /** Returns the configured rotation in degrees. */
        public float rotationDegrees() {
            return this.rotationDegrees;
        }

        /** Returns the width adjustment in GUI units. */
        public float widthOffset() {
            return this.widthOffset;
        }

        /** Returns the height adjustment in GUI units. */
        public float heightOffset() {
            return this.heightOffset;
        }

    }

}
