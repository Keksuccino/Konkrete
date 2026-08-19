package de.keksuccino.konkrete.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Keeps the first displayed frame of a streamed texture available for non-blocking playback resets.
 * Streamed textures retain their last GPU contents while their decoder asynchronously seeks back to frame zero,
 * so resetting only the playback indices would briefly expose that stale frame when the texture becomes visible again.
 */
public final class AnimatedTextureResetFrame implements AutoCloseable {

    @Nullable
    private NativeImage snapshot;
    private long requestedRestoreVersion = 0L;
    private long completedRestoreVersion = 0L;

    /** Captures the first owned frame once for later texture restoration. */
    public synchronized void captureIfAbsent(@NotNull NativeImage source) {
        if (this.snapshot != null) return;
        NativeImage captured = new NativeImage(source.format(), source.getWidth(), source.getHeight(), false);
        captured.copyFrom(source);
        this.snapshot = captured;
    }

    /** Requests the restore from the texture resource. */
    public synchronized boolean requestRestore() {
        if (this.snapshot != null) {
            this.requestedRestoreVersion++;
            return true;
        }
        return false;
    }

    /** Restores the retained frame into the supplied texture manager entry. */
    public boolean restoreTo(@Nullable DynamicTexture texture) {
        long restoreVersion;
        synchronized (this) {
            if (!this.isRestorePending() || (this.snapshot == null) || (texture == null)) return false;
            NativeImage target = texture.getPixels();
            if ((target == null) || !hasSameLayout(this.snapshot, target)) return false;
            target.copyFrom(this.snapshot);
            restoreVersion = this.requestedRestoreVersion;
        }
        // Keep the helper monitor away from GPU work. A concurrent newer request remains pending because it has a higher version.
        texture.upload();
        this.markRestored(restoreVersion);
        return true;
    }

    /** Copies the retained pixels only when a newer restore generation requested them. */
    @Nullable
    public synchronized RequestedFrame copyForRequestedRestore() {
        if (!this.isRestorePending() || (this.snapshot == null)) return null;
        NativeImage copy = new NativeImage(this.snapshot.format(), this.snapshot.getWidth(), this.snapshot.getHeight(), false);
        copy.copyFrom(this.snapshot);
        return new RequestedFrame(copy, this.requestedRestoreVersion);
    }

    /** Marks the restored state for the texture resource. */
    public synchronized void markRestored(long restoreVersion) {
        this.completedRestoreVersion = Math.max(this.completedRestoreVersion, restoreVersion);
    }

    /** Cancels every pending restore request without discarding the retained image. */
    public synchronized void cancelRestore() {
        this.completedRestoreVersion = this.requestedRestoreVersion;
    }

    /** Clears the retained state from this texture resource component. */
    public synchronized void clear() {
        NativeImage previous = this.snapshot;
        this.snapshot = null;
        this.completedRestoreVersion = this.requestedRestoreVersion;
        if (previous != null) {
            previous.close();
        }
    }

    private boolean isRestorePending() {
        return this.requestedRestoreVersion > this.completedRestoreVersion;
    }

    private static boolean hasSameLayout(@NotNull NativeImage first, @NotNull NativeImage second) {
        return (first.format() == second.format()) && (first.getWidth() == second.getWidth()) && (first.getHeight() == second.getHeight());
    }

    /** Closes the retained reset image and cancels pending restoration; repeated calls are harmless. */
    @Override
    public void close() {
        this.clear();
    }

    /** Carries {@code RequestedFrame} data between validated stages of the texture resource. */
    public static final class RequestedFrame implements AutoCloseable {

        @Nullable
        private NativeImage image;
        private final long restoreVersion;

        private RequestedFrame(@NotNull NativeImage image, long restoreVersion) {
            this.image = image;
            this.restoreVersion = restoreVersion;
        }

        /** Retrieves the image from the texture resource. */
        @NotNull
        public NativeImage takeImage() {
            NativeImage result = Objects.requireNonNull(this.image, "The requested reset frame image was already transferred");
            this.image = null;
            return result;
        }

        /** Restores the version state for the texture resource. */
        public long restoreVersion() {
            return this.restoreVersion;
        }

        /** Closes the copied image unless {@link #takeImage()} already transferred it; repeated calls are harmless. */
        @Override
        public void close() {
            NativeImage previous = this.image;
            this.image = null;
            if (previous != null) {
                previous.close();
            }
        }

    }

}
