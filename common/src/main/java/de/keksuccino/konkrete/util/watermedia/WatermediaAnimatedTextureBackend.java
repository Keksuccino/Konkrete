package de.keksuccino.konkrete.util.watermedia;

import de.keksuccino.konkrete.util.file.FileUtils;
import de.keksuccino.konkrete.util.lifecycle.ClientShutdownHandler;
import de.keksuccino.konkrete.util.rendering.AspectRatio;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileOutputStream;

/**
 * One-shot Watermedia-backed texture controller that owns its MRL, player, frame facade, and temporary source.
 * Initialization may begin off-thread, but rendering, playback control, and terminal {@link #close()} belong on
 * Minecraft's client/render thread. A failed or closed instance is terminal; create another instance to retry.
 */
public class WatermediaAnimatedTextureBackend implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();
    /** Texture-manager identifier for this backend's external frame. */
    @NotNull
    protected final Identifier frameLocation;
    /** Uppercase media type used in diagnostics. */
    @NotNull
    protected final String logTypeName;
    /** Guards player and external-frame creation. */
    @NotNull
    protected final Object playerInitLock = new Object();

    /** External frame texture created after the player exposes a handle. */
    @Nullable
    protected volatile WatermediaFrameTexture frameTexture;
    /** Reflected Watermedia MRL. */
    @Nullable
    protected volatile Object mrl;
    /** Reflected managed Watermedia player. */
    @Nullable
    protected volatile Object mediaPlayer;
    /** Temporary source file owned by this backend. */
    @Nullable
    protected volatile File generatedTempFile;
    /** Active bounded MRL resolution operation. */
    @Nullable
    protected volatile WatermediaMrlResolver.Resolution mrlResolution;

    /** Human-readable source name used in diagnostics. */
    @NotNull
    protected volatile String sourceName = "[Unknown Source]";
    /** Last known decoded width. */
    protected volatile int width = 10;
    /** Last known decoded height. */
    protected volatile int height = 10;
    /** Last known decoded aspect ratio. */
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    /** Whether MRL construction succeeded or reached a terminal fallback. */
    protected volatile boolean ready = false;
    /** Whether MRL resolution completed successfully. */
    protected volatile boolean loadingCompleted = false;
    /** Whether source resolution or player creation failed. */
    protected volatile boolean loadingFailed = false;
    /** Whether Watermedia is unavailable. */
    protected volatile boolean dependencyMissing = false;
    /** Whether playback is requested. */
    protected volatile boolean playRequested = true;
    /** Whether paused playback is requested. */
    protected volatile boolean pausedRequested = false;
    /** Whether this backend has released its resources. */
    protected volatile boolean closed = false;
    private volatile boolean initializationAttempted = false;
    /** Whether a main-thread player creation task is queued. */
    protected volatile boolean playerInitTaskQueued = false;
    /** Whether playback repeats without a loop limit. */
    protected volatile boolean loopForever = true;
    /** Previous playback position used to detect a loop transition. */
    protected volatile long lastPlaybackTimeMs = 0L;
    /** Number of detected loop transitions. */
    protected volatile int completedLoops = 0;
    /** Requested play count, or a non-positive value for infinite looping. */
    protected volatile int maxLoops = -1;

    /** Creates a backend with a stable unique frame identifier and diagnostic media type. */
    public WatermediaAnimatedTextureBackend(@NotNull String uniqueId, @NotNull String typeName) {
        this.logTypeName = typeName.toUpperCase();
        String cleanType = typeName.toLowerCase();
        String cleanId = uniqueId.toLowerCase().replace("-", "");
        this.frameLocation = Identifier.fromNamespaceAndPath("konkrete", "watermedia_" + cleanType + "_frame_" + cleanId);
    }

    /** Starts one initialization from caller bytes copied to an owned temporary file; returns false after any prior attempt or close. */
    public boolean initializeFromBytes(@NotNull byte[] data, @NotNull String extension, @NotNull String sourceName) {
        if (!this.claimInitializationAttempt()) return false;
        if (!WatermediaUtil.isWatermediaRenderingAvailable()) {
            this.onDependencyMissing(sourceName);
            return false;
        }
        File temp = this.writeDataToTempFile(data, extension, sourceName);
        if (temp == null) {
            this.fail("Failed to create temporary source file for Watermedia " + this.logTypeName + " texture: " + sourceName, null);
            return false;
        }
        synchronized (this.playerInitLock) {
            if (this.closed) {
                temp.delete();
                return false;
            }
            this.generatedTempFile = temp;
        }
        return this.initializeClaimedSource(temp.getAbsolutePath(), sourceName);
    }

    /** Starts one initialization from a Watermedia-supported path or URL; returns false after any prior attempt or close. */
    public boolean initializeFromSource(@NotNull String source, @NotNull String sourceName) {
        if (!this.claimInitializationAttempt()) return false;
        if (!WatermediaUtil.isWatermediaRenderingAvailable()) {
            this.onDependencyMissing(sourceName);
            return false;
        }
        return this.initializeClaimedSource(source, sourceName);
    }

    /** Claims the one initialization attempt, creates the owned MRL, and begins bounded asynchronous resolution. */
    protected boolean initializeInternal(@NotNull String source, @NotNull String sourceName) {
        if (!this.claimInitializationAttempt()) return false;
        return this.initializeClaimedSource(source, sourceName);
    }

    private boolean claimInitializationAttempt() {
        synchronized (this.playerInitLock) {
            if (this.closed || this.initializationAttempted) return false;
            this.initializationAttempted = true;
            return true;
        }
    }

    private boolean initializeClaimedSource(@NotNull String source, @NotNull String sourceName) {
        if (this.closed) return false;
        this.sourceName = sourceName;
        try {
            Object cachedMrl = WatermediaReflectionBridge.createMrl(source);
            if (cachedMrl == null) {
                this.fail("Failed to create Watermedia MRL for " + this.logTypeName + " texture source: " + sourceName, null);
                return false;
            }
            synchronized (this.playerInitLock) {
                if (this.closed) return false;
                this.mrl = cachedMrl;
                this.ready = true;
            }
            WatermediaUtil.WATERMEDIA_INITIALIZED = true;
            this.watchMrlStateAsync(sourceName);
            if (this.playRequested) this.queuePlayerInitializationTask();
            return true;
        } catch (Throwable ex) {
            this.fail("Failed to initialize Watermedia " + this.logTypeName + " texture source: " + sourceName, ex);
            return false;
        }
    }

    /** Observes the MRL until it resolves, fails, times out, or is cancelled. */
    protected void watchMrlStateAsync(@NotNull String sourceName) {
        Object cachedMrl = this.mrl;
        if (cachedMrl == null || this.closed) return;
        WatermediaMrlResolver.Resolution resolution = WatermediaMrlResolver.resolve(cachedMrl);
        WatermediaMrlResolver.Resolution previous = this.mrlResolution;
        this.mrlResolution = resolution;
        if (previous != null) previous.close();
        resolution.future().thenAccept(state -> {
            if (this.closed || resolution != this.mrlResolution) return;
            if (state == WatermediaMrlResolver.State.LOADED) {
                this.loadingCompleted = true;
                if (this.playRequested) this.queuePlayerInitializationTask();
            } else if (state == WatermediaMrlResolver.State.FAILED) {
                this.fail("Watermedia MRL failed to resolve " + this.logTypeName + " texture source: " + sourceName, null);
            } else if (state == WatermediaMrlResolver.State.TIMED_OUT) {
                this.fail("Watermedia MRL timed out while resolving " + this.logTypeName + " texture source: " + sourceName, null);
            }
        });
    }

    /** Queues player creation on Minecraft's client thread once. */
    protected void queuePlayerInitializationTask() {
        if (this.closed || this.playerInitTaskQueued) return;
        this.playerInitTaskQueued = true;
        MainThreadTaskExecutor.executeInMainThread(() -> {
            this.playerInitTaskQueued = false;
            this.createPlayerIfPossible();
        }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    /** Creates a player after successful MRL resolution when called on the client thread. */
    protected void createPlayerIfPossible() {
        if (this.closed || this.loadingFailed || this.dependencyMissing) return;
        if (!WatermediaUtil.isWatermediaRenderingAvailable()) {
            this.onDependencyMissing(this.sourceName);
            return;
        }
        if (!Minecraft.getInstance().isSameThread()) {
            this.queuePlayerInitializationTask();
            return;
        }
        if (this.mediaPlayer != null) return;
        Object cachedMrl = this.mrl;
        if (cachedMrl == null) return;
        if (WatermediaReflectionBridge.isMrlResolving(cachedMrl)) return;
        if (!WatermediaReflectionBridge.isMrlLoaded(cachedMrl)) {
            this.fail("Cannot create Watermedia player because MRL is in error state for " + this.logTypeName + " texture", null);
            return;
        }
        synchronized (this.playerInitLock) {
            if (this.mediaPlayer != null || this.closed) return;
            Object createdPlayer = WatermediaReflectionBridge.createPlayer(cachedMrl, Thread.currentThread(), Minecraft.getInstance()::execute, true, false);
            if (createdPlayer == null) {
                this.fail("Failed to create Watermedia player for " + this.logTypeName + " texture", null);
                return;
            }
            this.mediaPlayer = createdPlayer;
            this.applyRepeatMode(createdPlayer);
            this.updateSizeFromPlayer(createdPlayer);
            this.lastPlaybackTimeMs = 0L;
            this.completedLoops = 0;
            if (this.playRequested) {
                if (this.pausedRequested) {
                    WatermediaReflectionBridge.playerStartPaused(createdPlayer);
                    WatermediaReflectionBridge.playerPause(createdPlayer, true);
                } else {
                    WatermediaReflectionBridge.playerStart(createdPlayer);
                    WatermediaReflectionBridge.playerPause(createdPlayer, false);
                }
            } else {
                WatermediaReflectionBridge.playerStop(createdPlayer);
            }
        }
    }

    /** Applies the requested repeat behavior to a player. */
    protected void applyRepeatMode(@Nullable Object player) {
        if (player == null) return;
        boolean repeat = this.loopForever || (this.maxLoops > 1);
        WatermediaReflectionBridge.setPlayerRepeat(player, repeat);
    }

    /** Refreshes decoded dimensions and the registered external texture. */
    protected void updateSizeFromPlayer(@Nullable Object player) {
        if (player == null) return;
        int newWidth = WatermediaReflectionBridge.playerWidth(player);
        int newHeight = WatermediaReflectionBridge.playerHeight(player);
        if ((newWidth > 0) && (newHeight > 0)) {
            this.width = newWidth;
            this.height = newHeight;
            this.aspectRatio = new AspectRatio(newWidth, newHeight);
            WatermediaFrameTexture cachedFrameTexture = this.frameTexture;
            if (cachedFrameTexture != null) {
                cachedFrameTexture.setWidth(newWidth);
                cachedFrameTexture.setHeight(newHeight);
            }
        }
    }

    /** Registers the external frame texture with Minecraft's texture manager. */
    protected boolean ensureFrameTextureRegistered(@NotNull WatermediaFrameTexture frameTexture) {
        var textureManager = Minecraft.getInstance().getTextureManager();
        // TextureManager#getTexture treats an unknown dynamic ID as a file-backed texture and logs a false missing-resource warning. Re-registering the same instance is explicitly identity-safe.
        textureManager.register(this.frameLocation, frameTexture);
        return true;
    }

    /** Returns the lazily-created external frame texture when rendering is available. */
    @Nullable
    protected WatermediaFrameTexture getOrCreateFrameTexture() {
        if (!WatermediaUtil.isWatermediaRenderingAvailable()) return null;
        WatermediaFrameTexture cachedFrameTexture = this.frameTexture;
        if (cachedFrameTexture != null) return cachedFrameTexture;
        synchronized (this.playerInitLock) {
            cachedFrameTexture = this.frameTexture;
            if (cachedFrameTexture == null) {
                cachedFrameTexture = new WatermediaFrameTexture(-1);
                cachedFrameTexture.setWidth(this.width);
                cachedFrameTexture.setHeight(this.height);
                this.frameTexture = cachedFrameTexture;
            }
            return cachedFrameTexture;
        }
    }

    /** Returns the current frame identifier on the render thread, or a transparent fallback until a frame is ready. */
    @Nullable
    public Identifier getResourceLocation() {
        if (this.closed) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
        if (this.dependencyMissing || this.loadingFailed) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
        if (!this.loadingCompleted) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;

        if ((this.mediaPlayer == null) && this.playRequested) {
            if (Minecraft.getInstance().isSameThread()) this.createPlayerIfPossible();
            else this.queuePlayerInitializationTask();
        }

        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
        if (this.handlePlayerError(cachedPlayer)) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;

        this.applyFiniteLoopStop(cachedPlayer);
        this.updateSizeFromPlayer(cachedPlayer);

        long textureHandle = WatermediaReflectionBridge.playerTextureHandle(cachedPlayer);
        if (textureHandle == 0L) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
        WatermediaFrameTexture frameTexture = this.getOrCreateFrameTexture();
        if (frameTexture == null) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
        frameTexture.setHandle(textureHandle);
        this.ensureFrameTextureRegistered(frameTexture);
        return this.frameLocation;
    }

    /** Detects loop transitions and disables repeat before the requested final play. */
    protected void applyFiniteLoopStop(@NotNull Object player) {
        if (this.loopForever || (this.maxLoops <= 1)) return;
        long durationMs = WatermediaReflectionBridge.playerDuration(player);
        if (durationMs <= 0L) return;

        long currentTimeMs = Math.max(0L, WatermediaReflectionBridge.playerTime(player));
        long previousTimeMs = Math.max(0L, this.lastPlaybackTimeMs);
        this.lastPlaybackTimeMs = currentTimeMs;

        if (previousTimeMs > (currentTimeMs + 100L)) {
            this.completedLoops++;
            if (this.completedLoops >= (this.maxLoops - 1)) {
                WatermediaReflectionBridge.setPlayerRepeat(player, false);
            }
        }
    }

    /** Sets the requested number of plays on the client thread, using a non-positive value for infinite looping. */
    public void setLoopCount(int loops) {
        this.maxLoops = loops;
        this.loopForever = loops <= 0;
        this.applyRepeatMode(this.mediaPlayer);
    }

    /** Starts or resumes playback on the client thread; calls after failure or close are ignored. */
    public void play() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        this.playRequested = true;
        this.pausedRequested = false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
            if (statusName.equals("ERROR")) {
                this.handlePlayerError(cachedPlayer);
                return;
            }
            if (statusName.equals("STOPPED") || statusName.equals("ENDED")) {
                WatermediaReflectionBridge.playerStart(cachedPlayer);
            }
            WatermediaReflectionBridge.playerPause(cachedPlayer, false);
        } else {
            this.queuePlayerInitializationTask();
        }
    }

    /** Returns whether playback is active or actively buffering. */
    public boolean isPlaying() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            if (WatermediaReflectionBridge.playerIsPlaying(cachedPlayer)) return true;
            String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
            if (statusName.equals("ERROR")) {
                this.handlePlayerError(cachedPlayer);
                return false;
            }
            return this.playRequested && !this.pausedRequested
                    && (statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING"));
        }
        return this.playRequested && !this.pausedRequested;
    }

    /** Pauses playback on the client thread; calls after failure or close are ignored. */
    public void pause() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        this.pausedRequested = true;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerPause(cachedPlayer, true);
        }
    }

    /** Returns whether paused playback is requested or active. */
    public boolean isPaused() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) return WatermediaReflectionBridge.playerIsPaused(cachedPlayer);
        return this.playRequested && this.pausedRequested;
    }

    /** Restarts playback from the beginning on the client thread; calls after close are ignored. */
    public void stop() {
        if (this.closed) return;
        this.playRequested = true;
        this.pausedRequested = false;
        this.completedLoops = 0;
        this.lastPlaybackTimeMs = 0L;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerStop(cachedPlayer);
            WatermediaReflectionBridge.playerStart(cachedPlayer);
            WatermediaReflectionBridge.playerPause(cachedPlayer, false);
            this.applyRepeatMode(cachedPlayer);
        }
    }

    /** Alias for {@link #stop()}, which restarts playback from the beginning. */
    public void reset() {
        this.stop();
    }

    /** Returns whether initialization reached a usable or terminal fallback state. */
    public boolean isReady() {
        return !this.closed && (this.ready || this.loadingFailed || this.dependencyMissing);
    }

    /** Returns whether source resolution completed successfully. */
    public boolean isLoadingCompleted() {
        return !this.closed && !this.loadingFailed && this.loadingCompleted;
    }

    /** Returns whether source resolution or player creation failed. */
    public boolean isLoadingFailed() {
        return this.loadingFailed;
    }

    /** Returns the last known decoded width. */
    public int getWidth() {
        return this.width;
    }

    /** Returns the last known decoded height. */
    public int getHeight() {
        return this.height;
    }

    /** Returns the last known decoded aspect ratio. */
    @NotNull
    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    /** Returns whether all owned resources have been released. */
    public boolean isClosed() {
        return this.closed;
    }

    /** Returns an explicit snapshot of this backend's lifecycle state. */
    @NotNull
    public State getState() {
        if (this.closed) return State.CLOSED;
        if (this.loadingFailed || this.dependencyMissing) return State.FAILED;
        if (this.mediaPlayer != null) {
            if (this.pausedRequested) return State.PAUSED;
            if (this.playRequested) return State.PLAYING;
        }
        if (this.loadingCompleted) return State.READY;
        if (this.mrl != null) return State.RESOLVING;
        return State.NEW;
    }

    /** Permanently releases every owned resource on the client/render thread; repeated calls are ignored. */
    @Override
    public void close() {
        synchronized (this.playerInitLock) {
            if (this.closed) return;
            this.closed = true;
        }
        this.playRequested = false;
        this.pausedRequested = false;
        Object cachedPlayer = this.mediaPlayer;
        this.mediaPlayer = null;
        this.mrl = null;
        WatermediaMrlResolver.Resolution resolution = this.mrlResolution;
        this.mrlResolution = null;
        if (resolution != null) resolution.close();
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerPause(cachedPlayer, true);
            WatermediaReflectionBridge.playerStop(cachedPlayer);
            if (ClientShutdownHandler.isShuttingDown()) WatermediaReflectionBridge.playerReleaseForShutdown(cachedPlayer);
            else WatermediaReflectionBridge.playerRelease(cachedPlayer);
        }
        this.clearFrameTextureId();
        try {
            Minecraft.getInstance().getTextureManager().release(this.frameLocation);
        } catch (Exception ignored) {}
        File temp = this.generatedTempFile;
        this.generatedTempFile = null;
        if ((temp != null) && temp.isFile()) {
            temp.delete();
        }
    }

    /** Marks the source unavailable so callers can select another decoder. */
    protected void onDependencyMissing(@NotNull String sourceName) {
        this.dependencyMissing = true;
        this.loadingFailed = true;
        this.ready = true;
        LOGGER.warn("[KONKRETE] Watermedia is not loaded, {} source will use fallback decoder: {}", this.logTypeName, sourceName);
    }

    /** Removes the external native texture handle without closing the wrapper. */
    protected void clearFrameTextureId() {
        WatermediaFrameTexture cachedFrameTexture = this.frameTexture;
        if (cachedFrameTexture != null) {
            cachedFrameTexture.setHandle(0L);
        }
    }

    /** Transitions to failure when a player reports an error. */
    protected boolean handlePlayerError(@Nullable Object player) {
        if (player == null) return false;
        if (!WatermediaReflectionBridge.playerStatusName(player).equals("ERROR")) return false;
        this.fail("Watermedia player entered error state for " + this.logTypeName + " texture: " + this.sourceName, null);
        return true;
    }

    /** Records a terminal failure and emits one diagnostic. */
    protected void fail(@NotNull String message, @Nullable Throwable cause) {
        if (this.loadingFailed) return;
        this.loadingFailed = true;
        this.ready = true;
        if (cause != null) LOGGER.error("[KONKRETE] {}", message, cause);
        else LOGGER.error("[KONKRETE] {}", message);
    }

    /** Writes caller bytes to the configured temporary-media directory. */
    @Nullable
    protected File writeDataToTempFile(@NotNull byte[] data, @NotNull String extension, @NotNull String sourceName) {
        String suffix = extension.startsWith(".") ? extension : "." + extension;
        File tempTextureDirectory = FileUtils.createDirectory(WatermediaIntegrationConfig.getDataDirectory().resolve("watermedia_animated_textures").toFile());
        File targetFile = new File(tempTextureDirectory, "watermedia_texture_" + System.nanoTime() + suffix);
        try (FileOutputStream out = new FileOutputStream(targetFile)) {
            out.write(data);
            return targetFile;
        } catch (Exception ex) {
            if (targetFile.isFile()) targetFile.delete();
            LOGGER.error("[KONKRETE] Failed to write {} stream to temporary file: {}", this.logTypeName, sourceName, ex);
        }
        return null;
    }

    /** Snapshot states for the one-shot backend lifecycle. */
    public enum State {
        /** No initialization attempt has started. */
        NEW,
        /** Watermedia is resolving the source. */
        RESOLVING,
        /** The source resolved and may create a player. */
        READY,
        /** Playback is requested on a created player. */
        PLAYING,
        /** Paused playback is requested on a created player. */
        PAUSED,
        /** Dependency detection, resolution, or player creation failed terminally. */
        FAILED,
        /** All owned resources were released terminally. */
        CLOSED
    }

}
