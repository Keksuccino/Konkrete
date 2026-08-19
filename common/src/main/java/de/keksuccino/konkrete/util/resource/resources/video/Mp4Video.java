package de.keksuccino.konkrete.util.resource.resources.video;

import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.file.FileUtils;
import de.keksuccino.konkrete.util.input.TextValidators;
import de.keksuccino.konkrete.util.rendering.AspectRatio;
import de.keksuccino.konkrete.util.resource.ResourceInputLimits;
import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import de.keksuccino.konkrete.util.threading.KonkreteThreads;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.util.watermedia.WatermediaDeferredPlayerReleaseTracker;
import de.keksuccino.konkrete.util.watermedia.WatermediaFrameTexture;
import de.keksuccino.konkrete.util.watermedia.WatermediaReflectionBridge;
import de.keksuccino.konkrete.util.watermedia.WatermediaUtil;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Owns MP4 stream spooling, native playback, audio synchronization, and deferred texture release. */
@SuppressWarnings("unused")
public class Mp4Video implements IVideo {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final File TEMP_VIDEO_DIR = ResourceRuntime.temporaryDirectory("watermedia_videos");
    private static final int HARD_STOP_DEFER_TICKS = 2;
    private static final int CLOSE_RELEASE_MAX_DEFER_TICKS = 16;
    private static final int INITIAL_UNPAUSE_DEFER_TICKS = 1;
    private static final int INITIAL_UNPAUSE_STATUS_WAIT_TICKS = 40;
    private static final long PLAY_REQUEST_GRACE_MS = 2200L;
    private static final long PREMATURE_END_START_WINDOW_MS = 1000L;
    private static final double PLAYBACK_END_EPSILON_SECONDS = 0.12D;
    private static final double LOOP_RESTART_WINDOW_SECONDS = 0.40D;

    /** Current mrl state for this video resource instance. */
    @Nullable
    protected volatile Object mrl;
    /** Current media player state for this video resource instance. */
    @Nullable
    protected volatile Object mediaPlayer;
    /** Original resource-pack identifier, or null when another source kind is used. */
    @Nullable
    protected Identifier sourceLocation;
    /** Holds the sourceFile handle whose lifecycle follows this video resource instance. */
    @Nullable
    protected File sourceFile;
    /** Original web URL, or null when another source kind is used. */
    @Nullable
    protected String sourceURL;
    /** Holds the generatedTempFile handle whose lifecycle follows this video resource instance. */
    @Nullable
    protected volatile File generatedTempFile;
    /** Decoded canvas width in pixels for this video resource instance. */
    protected volatile int width = 10;
    /** Decoded canvas height in pixels for this video resource instance. */
    protected volatile int height = 10;
    /** Aspect ratio derived from the decoded width and height. */
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    /** Current volume state for this video resource instance. */
    protected volatile float volume = 1.0F;
    /** Whether looping currently applies to this video resource instance. */
    protected volatile boolean looping = false;
    /** Current source name state for this video resource instance. */
    @NotNull
    protected volatile String sourceName = "[Unknown Source]";
    /** Process-unique suffix used for this video resource instance's runtime identifiers. */
    protected final String uniqueId = ResourceRuntime.nextResourceId();
    /** Current frame location state for this video resource instance. */
    protected final Identifier frameLocation = ResourceRuntime.identifier("watermedia_video_frame_" + this.uniqueId);
    /** Owned frame texture state for this video resource instance. */
    @Nullable
    protected volatile WatermediaFrameTexture frameTexture;
    /** Whether ready currently applies to this video resource instance. */
    protected volatile boolean ready = false;
    /** Whether loading completed currently applies to this video resource instance. */
    protected volatile boolean loadingCompleted = false;
    /** Whether loading failed currently applies to this video resource instance. */
    protected volatile boolean loadingFailed = false;
    /** Whether dependency missing currently applies to this video resource instance. */
    protected volatile boolean dependencyMissing = false;
    /** Whether play requested currently applies to this video resource instance. */
    protected volatile boolean playRequested = false;
    /** Whether paused requested currently applies to this video resource instance. */
    protected volatile boolean pausedRequested = false;
    /** Current seek requested ms state for this video resource instance. */
    protected volatile long seekRequestedMs = -1L;
    /** Whether closed currently applies to this video resource instance. */
    protected volatile boolean closed = false;
    /** Whether player init task queued currently applies to this video resource instance. */
    protected volatile boolean playerInitTaskQueued = false;
    /** Current stop request version state for this video resource instance. */
    protected volatile long stopRequestVersion = 0L;
    /** Current close release request version state for this video resource instance. */
    protected volatile long closeReleaseRequestVersion = 0L;
    /** Whether restart from beginning on next play currently applies to this video resource instance. */
    protected volatile boolean restartFromBeginningOnNextPlay = false;
    /** Current last play request timestamp ms state for this video resource instance. */
    protected volatile long lastPlayRequestTimestampMs = -1L;
    /** Whether frame presented currently applies to this video resource instance. */
    protected volatile boolean framePresented = false;
    /** Whether premature end logged currently applies to this video resource instance. */
    protected volatile boolean prematureEndLogged = false;
    /** Whether listener playback cycle active currently applies to this video resource instance. */
    protected volatile boolean listenerPlaybackCycleActive = false;
    /** Whether listener finished event emitted for cycle currently applies to this video resource instance. */
    protected volatile boolean listenerFinishedEventEmittedForCycle = false;
    /** Current listener last known playback time seconds state for this video resource instance. */
    protected volatile double listenerLastKnownPlaybackTimeSeconds = 0.0D;
    /** Lock guarding player init transitions in this video resource instance. */
    protected final Object playerInitLock = new Object();

    /** Creates the location video resource variant. */
    @NotNull
    public static Mp4Video location(@NotNull Identifier location) {
        return location(location, null);
    }

    /** Creates the location video resource variant. */
    @NotNull
    public static Mp4Video location(@NotNull Identifier location, @Nullable Mp4Video writeTo) {

        Objects.requireNonNull(location);
        Mp4Video video = (writeTo != null) ? writeTo : new Mp4Video();
        video.sourceLocation = location;
        video.initializeAsync(location.toString());

        return video;

    }

    /** Creates the local video resource variant. */
    @NotNull
    public static Mp4Video local(@NotNull File videoFile) {
        return local(videoFile, null);
    }

    /** Creates the local video resource variant. */
    @NotNull
    public static Mp4Video local(@NotNull File videoFile, @Nullable Mp4Video writeTo) {

        Objects.requireNonNull(videoFile);
        Mp4Video video = (writeTo != null) ? writeTo : new Mp4Video();
        video.sourceFile = videoFile;

        if (!videoFile.isFile()) {
            video.fail("Failed to read MP4 video from file! File not found: " + videoFile.getPath(), null);
            return video;
        }

        video.initializeAsync(videoFile.getPath());

        return video;

    }

    /** Creates the web video resource variant. */
    @NotNull
    public static Mp4Video web(@NotNull String videoURL) {
        return web(videoURL, null);
    }

    /** Creates the web video resource variant. */
    @NotNull
    public static Mp4Video web(@NotNull String videoURL, @Nullable Mp4Video writeTo) {

        Objects.requireNonNull(videoURL);
        Mp4Video video = (writeTo != null) ? writeTo : new Mp4Video();
        video.sourceURL = videoURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(videoURL)) {
            video.fail("Failed to read MP4 video from URL! Invalid URL: " + videoURL, null);
            return video;
        }

        video.initializeAsync(videoURL);

        return video;

    }

    /** Builds a resource value for the video resource. */
    @NotNull
    public static Mp4Video of(@NotNull InputStream in, @Nullable String videoName, @Nullable Mp4Video writeTo) {

        Objects.requireNonNull(in);
        Mp4Video video = (writeTo != null) ? writeTo : new Mp4Video();
        String sourceName = (videoName != null) ? videoName : "[Generic InputStream Source]";

        KonkreteThreads.startDaemonThread(() -> {
            File temp = video.writeInputStreamToTempFile(in, sourceName);
            if (temp == null) {
                video.fail("Failed to decode MP4 video from input stream: " + sourceName, null);
                return;
            }
            video.generatedTempFile = temp;
            video.sourceFile = temp;
            video.initializeInternal(sourceName);
        }, "Mp4Video-InputStreamLoader");

        return video;

    }

    /** Builds a resource value for the video resource. */
    @NotNull
    public static Mp4Video of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    /** Initializes a new {@code Mp4Video} for video resource use. */
    protected Mp4Video() {
        Mp4VideoSoundEngineReloadHandler.register(this);
    }

    /** Initializes the async for the video resource. */
    protected void initializeAsync(@NotNull String sourceName) {
        KonkreteThreads.startDaemonThread(() -> this.initializeInternal(sourceName), "Mp4Video-Initializer");
    }

    /** Initializes the internal for the video resource. */
    protected void initializeInternal(@NotNull String sourceName) {
        if (this.closed) return;
        this.sourceName = sourceName;
        if (!WatermediaUtil.isWatermediaVideoPlaybackAvailable()) {
            this.onDependencyMissing(sourceName);
            return;
        }
        try {
            String backendSource = this.resolveBackendSource(sourceName);
            if (backendSource == null) {
                if (!this.loadingFailed) this.fail("Failed to prepare MP4 video source for Watermedia: " + sourceName, null);
                return;
            }
            Object cachedMrl = WatermediaReflectionBridge.createMrl(backendSource);
            if (cachedMrl == null) {
                this.fail("Failed to create Watermedia MRL for MP4 video source: " + sourceName, null);
                return;
            }
            this.mrl = cachedMrl;
            this.ready = true;
            WatermediaUtil.WATERMEDIA_INITIALIZED = true;
            this.watchMrlStateAsync();
            if (this.playRequested) this.queuePlayerInitializationTask();
        } catch (Throwable ex) {
            this.fail("An error occurred while initializing MP4 video source: " + sourceName, ex);
        }
    }

    /** Resolves the backend source for the video resource. */
    @Nullable
    protected String resolveBackendSource(@NotNull String sourceName) {
        if (this.sourceURL != null) {
            return this.sourceURL;
        }
        File cachedSourceFile = this.sourceFile;
        if (cachedSourceFile != null) {
            if (!cachedSourceFile.isFile()) {
                this.fail("MP4 video source file does not exist: " + cachedSourceFile.getPath(), null);
                return null;
            }
            return cachedSourceFile.getAbsolutePath();
        }
        Identifier cachedSourceLocation = this.sourceLocation;
        if (cachedSourceLocation != null) {
            try {
                InputStream in = Minecraft.getInstance().getResourceManager().open(cachedSourceLocation);
                File temp = this.writeInputStreamToTempFile(in, sourceName);
                if (temp == null) {
                    this.fail("Failed to cache MP4 video Identifier source into a temporary file: " + cachedSourceLocation, null);
                    return null;
                }
                this.generatedTempFile = temp;
                this.sourceFile = temp;
                return temp.getAbsolutePath();
            } catch (Exception ex) {
                this.fail("Failed to open MP4 video resource location: " + cachedSourceLocation, ex);
                return null;
            }
        }
        return null;
    }

    /** Watches the mrl state async asynchronously for the video resource. */
    protected void watchMrlStateAsync() {
        KonkreteThreads.startDaemonThread(() -> {
            long waitStart = System.currentTimeMillis();
            while (!this.closed) {
                Object cachedMrl = this.mrl;
                if (cachedMrl == null) return;
                if (WatermediaReflectionBridge.isMrlLoaded(cachedMrl)) {
                    this.loadingCompleted = true;
                    if (this.playRequested) this.queuePlayerInitializationTask();
                    return;
                }
                if (WatermediaReflectionBridge.isMrlFailed(cachedMrl)) {
                    this.fail("Watermedia MRL failed to resolve media source for MP4 video", null);
                    return;
                }
                if ((waitStart + 30000L) < System.currentTimeMillis()) {
                    this.fail("Watermedia MRL timed out while resolving MP4 video source", null);
                    return;
                }
                try {
                    Thread.sleep(25);
                } catch (Exception ignored) {}
            }
        }, "Mp4Video-MediaResolver");
    }

    /** Schedules the player initialization task on the required video resource lifecycle boundary. */
    protected void queuePlayerInitializationTask() {
        if (this.closed || this.playerInitTaskQueued) return;
        this.playerInitTaskQueued = true;
        MainThreadTaskExecutor.executeInMainThread(() -> {
            this.playerInitTaskQueued = false;
            this.createPlayerIfPossible();
        }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    /** Builds the player if possible for the video resource. */
    protected void createPlayerIfPossible() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        if (!WatermediaUtil.isWatermediaVideoPlaybackAvailable()) {
            this.onDependencyMissing(this.sourceName);
            return;
        }
        if (!Minecraft.getInstance().isSameThread()) {
            this.queuePlayerInitializationTask();
            return;
        }
        if (!this.playRequested || Mp4VideoSoundEngineReloadHandler.isSoundEngineReloading()) return;
        boolean openAlReady = this.isOpenAlReadyForPlayerCreation();
        // The first sound reload creates Minecraft's OpenAL capabilities. Calling Watermedia's ALEngine before that
        // point permanently poisons LWJGL's write-once capability holder, so initial unavailability must stay retryable.
        if (!openAlReady && !Mp4VideoSoundEngineReloadHandler.hasSoundEngineReloadCompleted()) return;
        if (this.mediaPlayer != null) return;
        Object cachedMrl = this.mrl;
        if (cachedMrl == null) return;
        if (WatermediaReflectionBridge.isMrlResolving(cachedMrl)) return;
        if (!WatermediaReflectionBridge.isMrlLoaded(cachedMrl)) {
            this.fail("Cannot create MP4 video player because Watermedia MRL is in error state", null);
            return;
        }
        synchronized (this.playerInitLock) {
            if (this.mediaPlayer != null || this.closed) return;
            Object createdPlayer = WatermediaReflectionBridge.createPlayer(cachedMrl, Thread.currentThread(), Minecraft.getInstance()::execute, true, openAlReady);
            if (createdPlayer == null) {
                this.fail("Failed to create Watermedia media player for MP4 video source", null);
                return;
            }
            this.mediaPlayer = createdPlayer;
            this.resetFramePresentationStateForNewPlayer();
            this.applyVolumeToPlayer();
            this.applyLoopingToPlayer();
            if (this.playRequested) {
                this.startPlayerForCurrentRequest(createdPlayer);
            } else {
                WatermediaReflectionBridge.playerStop(createdPlayer);
            }
            this.tryApplyQueuedSeekToPlayer(createdPlayer);
        }
    }

    /** Applies the volume to player to the video resource. */
    protected void applyVolumeToPlayer() {
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            int volumePercent = (int) Math.max(0, Math.min(100, Math.round(this.volume * 100.0F)));
            WatermediaReflectionBridge.setPlayerVolume(cachedPlayer, volumePercent);
        }
    }

    /** Applies the looping to player to the video resource. */
    protected void applyLoopingToPlayer() {
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.setPlayerRepeat(cachedPlayer, this.looping);
        }
    }

    /** Updates the size from player for the video resource. */
    protected void updateSizeFromPlayer(@NotNull Object player) {
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

    /** Ensures the frame texture registered is valid for the video resource. */
    protected boolean ensureFrameTextureRegistered(@NotNull WatermediaFrameTexture frameTexture) {
        var textureManager = Minecraft.getInstance().getTextureManager();
        // TextureManager#getTexture treats an unknown dynamic ID as a file-backed texture and logs a false missing-resource warning. Re-registering the same instance is explicitly identity-safe.
        textureManager.register(this.frameLocation, frameTexture);
        return true;
    }

    /** Returns the or create frame texture, or {@code null} when it is not available. */
    @Nullable
    protected WatermediaFrameTexture getOrCreateFrameTexture() {
        if (!WatermediaUtil.isWatermediaVideoPlaybackAvailable()) return null;
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

    /** Returns the resource location, or {@code null} when it is not available. */
    @Nullable
    @Override
    public Identifier getResourceLocation() {
        if (this.closed) return FULLY_TRANSPARENT_TEXTURE;
        if (this.dependencyMissing || this.loadingFailed) return MISSING_TEXTURE_LOCATION;
        if (!this.playRequested || !this.loadingCompleted) return FULLY_TRANSPARENT_TEXTURE;
        if ((this.mediaPlayer == null) && this.playRequested) {
            if (Minecraft.getInstance().isSameThread()) this.createPlayerIfPossible();
            else this.queuePlayerInitializationTask();
        }
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return FULLY_TRANSPARENT_TEXTURE;
        String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
        this.updateVideoPlaybackListenerStateFromPlayer(cachedPlayer, statusName);
        if (this.shouldRecoverPrematureEnd(cachedPlayer, statusName)) {
            this.recoverFromPrematureEnd(cachedPlayer);
            return FULLY_TRANSPARENT_TEXTURE;
        }
        if (!this.shouldPresentFrame(statusName)) return FULLY_TRANSPARENT_TEXTURE;
        this.tryApplyQueuedSeekToPlayer(cachedPlayer);
        this.updateSizeFromPlayer(cachedPlayer);
        long textureHandle = WatermediaReflectionBridge.playerTextureHandle(cachedPlayer);
        if (textureHandle == 0L) return FULLY_TRANSPARENT_TEXTURE;
        WatermediaFrameTexture frameTexture = this.getOrCreateFrameTexture();
        if (frameTexture == null) return FULLY_TRANSPARENT_TEXTURE;
        frameTexture.setHandle(textureHandle);
        this.framePresented = true;
        this.ensureFrameTextureRegistered(frameTexture);
        return this.frameLocation;
    }

    /** Returns the width used by this video resource instance. */
    @Override
    public int getWidth() {
        return this.width;
    }

    /** Returns the height used by this video resource instance. */
    @Override
    public int getHeight() {
        return this.height;
    }

    /** Returns the aspect ratio used by this video resource instance. */
    @Override
    public @NotNull AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    /** Starts playback for this video resource. */
    @Override
    public void play() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        this.stopRequestVersion++;
        this.lastPlayRequestTimestampMs = System.currentTimeMillis();
        this.playRequested = true;
        Object cachedPlayer = this.mediaPlayer;
        boolean wasPaused = this.pausedRequested;
        if (cachedPlayer != null) {
            if (WatermediaReflectionBridge.playerIsPaused(cachedPlayer) || WatermediaReflectionBridge.playerStatusName(cachedPlayer).equals("PAUSED")) {
                wasPaused = true;
            }
        }
        this.pausedRequested = false;
        if (wasPaused) {
            // Resuming from pause should re-trigger "Video Started Playing".
            this.resetVideoPlaybackListenerState();
        }
        if (this.maybeEmitVideoStartedEvent()) {
            this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.PLAYING);
        }
        boolean restartFromBeginning = this.restartFromBeginningOnNextPlay;
        this.restartFromBeginningOnNextPlay = false;
        if (cachedPlayer != null) {
            String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
            if (this.isTerminalPlayerStatus(statusName)) {
                // STOPPED/ENDED/ERROR players can keep stale state; release and recreate before autoplay.
                this.recreatePlayerForAutoplay(cachedPlayer);
                return;
            }
            if (restartFromBeginning) {
                this.seekPlayerToBeginning(cachedPlayer);
            }
            if (WatermediaReflectionBridge.playerIsPaused(cachedPlayer) || statusName.equals("PAUSED")) {
                WatermediaReflectionBridge.playerPause(cachedPlayer, false);
                this.tryApplyQueuedSeekToPlayer(cachedPlayer);
                return;
            }
            // Watermedia restarts FFMediaPlayer when start() is called while its thread is still active.
            // Keep WAITING/LOADING/BUFFERING alive and only start from real terminal/idle states.
            if (statusName.equals("PLAYING") || this.isLoadingPlayerStatus(statusName)) {
                // stop() first pauses and then hard-stops asynchronously. If we hit this branch in the
                // transition window, force-clear pause state so playback actually resumes.
                WatermediaReflectionBridge.playerPause(cachedPlayer, false);
                this.tryApplyQueuedSeekToPlayer(cachedPlayer);
                return;
            }
            WatermediaReflectionBridge.playerStart(cachedPlayer);
            WatermediaReflectionBridge.playerPause(cachedPlayer, false);
            this.tryApplyQueuedSeekToPlayer(cachedPlayer);
        } else {
            this.queuePlayerInitializationTask();
        }
    }

    /** Returns whether playing. */
    @Override
    public boolean isPlaying() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            if (WatermediaReflectionBridge.playerIsPlaying(cachedPlayer)) return true;
            String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
            if (this.playRequested && !this.pausedRequested) {
                if (statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING") || statusName.equals("PAUSED")) {
                    return true;
                }
                if (statusName.equals("STOPPED") || statusName.equals("ENDED") || statusName.equals("ERROR")) {
                    long requestTimestamp = this.lastPlayRequestTimestampMs;
                    if ((requestTimestamp > 0L) && ((requestTimestamp + PLAY_REQUEST_GRACE_MS) > System.currentTimeMillis())) {
                        return true;
                    }
                }
            }
            if (statusName.equals("STOPPED") || statusName.equals("ENDED") || statusName.equals("ERROR")) {
                if (!this.playRequested || this.pausedRequested) {
                    this.lastPlayRequestTimestampMs = -1L;
                }
                return false;
            }
            return false;
        }
        return this.playRequested && !this.pausedRequested;
    }

    /** Pauses playback without releasing this video resource. */
    @Override
    public void pause() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        boolean wasPaused = this.pausedRequested;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            if (WatermediaReflectionBridge.playerIsPaused(cachedPlayer) || WatermediaReflectionBridge.playerStatusName(cachedPlayer).equals("PAUSED")) {
                wasPaused = true;
            }
        }
        this.pausedRequested = true;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerPause(cachedPlayer, true);
        }
        if (!wasPaused && (this.playRequested || this.listenerPlaybackCycleActive)) {
            this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.PAUSED);
        }
    }

    /** Returns whether paused. */
    @Override
    public boolean isPaused() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) return WatermediaReflectionBridge.playerIsPaused(cachedPlayer);
        return this.playRequested && this.pausedRequested;
    }

    /** Stops playback and resets this video resource. */
    @Override
    public void stop() {
        boolean shouldEmitStoppedStatus = this.playRequested || this.pausedRequested || this.listenerPlaybackCycleActive;
        this.playRequested = false;
        this.pausedRequested = false;
        this.seekRequestedMs = -1L;
        this.restartFromBeginningOnNextPlay = true;
        this.lastPlayRequestTimestampMs = -1L;
        if (shouldEmitStoppedStatus) {
            this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.STOPPED);
        }
        this.resetVideoPlaybackListenerState();
        this.clearFrameTextureId();
        long stopVersion = ++this.stopRequestVersion;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            // stop() should behave like a real reset, so move back to the beginning immediately.
            this.seekPlayerToBeginning(cachedPlayer);
            // Queue a deferred hard-stop so render-thread upload tasks can drain before FFmpeg cleanup.
            WatermediaReflectionBridge.playerPause(cachedPlayer, true);
            this.queueDeferredHardStop(cachedPlayer, stopVersion, HARD_STOP_DEFER_TICKS);
        }
        this.framePresented = false;
    }

    /** Sets the volume used by subsequent video resource operations. */
    @Override
    public void setVolume(float volume) {
        this.volume = Math.max(0.0F, Math.min(1.0F, volume));
        this.applyVolumeToPlayer();
    }

    /** Returns the volume used by this video resource instance. */
    @Override
    public float getVolume() {
        return this.volume;
    }

    /** Returns the duration used by this video resource instance. */
    @Override
    public float getDuration() {
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return 0.0F;
        long durationMs = WatermediaReflectionBridge.playerDuration(cachedPlayer);
        if (durationMs <= 0L) return 0.0F;
        return (durationMs / 1000.0F);
    }

    /** Returns the play time used by this video resource instance. */
    @Override
    public float getPlayTime() {
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return 0.0F;
        long timeMs = WatermediaReflectionBridge.playerTime(cachedPlayer);
        if (timeMs <= 0L) return 0.0F;
        return (timeMs / 1000.0F);
    }

    /** Sets the play time used by subsequent video resource operations. */
    @Override
    public void setPlayTime(float playTime) {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        this.seekRequestedMs = Math.max(0L, (long)Math.floor(playTime * 1000.0F));
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            this.tryApplyQueuedSeekToPlayer(cachedPlayer);
        } else if (this.playRequested) {
            this.queuePlayerInitializationTask();
        }
    }

    /** Returns whether ended. */
    @Override
    public boolean isEnded() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return false;
        String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
        this.updateVideoPlaybackListenerStateFromPlayer(cachedPlayer, statusName);
        if (this.shouldRecoverPrematureEnd(cachedPlayer, statusName)) {
            this.recoverFromPrematureEnd(cachedPlayer);
            return false;
        }
        boolean ended = this.isNaturalEndStatus(cachedPlayer, statusName);
        if (ended && this.playRequested && !this.pausedRequested) {
            if (this.maybeEmitVideoFinishedEvent(this.looping)) {
                this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.FINISHED);
            }
        } else if (this.looping && this.playRequested && !this.pausedRequested && this.listenerFinishedEventEmittedForCycle) {
            if (this.maybeEmitVideoStartedEvent()) {
                this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.PLAYING);
            }
        }
        return ended;
    }

    /** Sets the looping used by subsequent video resource operations. */
    @Override
    public void setLooping(boolean looping) {
        this.looping = looping;
        this.applyLoopingToPlayer();
    }

    /** Returns whether looping. */
    @Override
    public boolean isLooping() {
        return this.looping;
    }

    /** Opens the resource for the video resource. */
    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL, WebUtils.WebResourceType.VIDEO);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    /** Returns whether ready. */
    @Override
    public boolean isReady() {
        return !this.closed && (this.ready || this.loadingFailed || this.dependencyMissing);
    }

    /** Returns whether loading completed. */
    @Override
    public boolean isLoadingCompleted() {
        return !this.closed && !this.loadingFailed && this.loadingCompleted;
    }

    /** Returns whether loading failed. */
    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed;
    }

    /** Restores initial video resource state without transferring ownership. */
    @Override
    public void reset() {
        this.stop();
    }

    /**
     * Stops and releases the media player, removes the frame texture, and deletes generated input data.
     * Player release is deferred outside shutdown so already-queued frame uploads can drain; repeated calls are harmless.
     */
    @Override
    public void close() {
        if (this.closed) return;
        this.stopRequestVersion++;
        long closeReleaseVersion = ++this.closeReleaseRequestVersion;
        this.closed = true;
        this.playRequested = false;
        this.pausedRequested = false;
        this.seekRequestedMs = -1L;
        this.restartFromBeginningOnNextPlay = false;
        this.lastPlayRequestTimestampMs = -1L;
        this.looping = false;
        this.resetVideoPlaybackListenerState();
        Object cachedPlayer = this.mediaPlayer;
        this.mediaPlayer = null;
        this.mrl = null;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerPause(cachedPlayer, true);
            if (ResourceRuntime.isShuttingDown()) {
                // No later client tick is guaranteed once shutdown starts, so the normal future-tick release sequence could never execute.
                WatermediaReflectionBridge.playerStop(cachedPlayer);
                WatermediaReflectionBridge.playerReleaseForShutdown(cachedPlayer);
            } else if (WatermediaDeferredPlayerReleaseTracker.track(cachedPlayer)) {
                // FFmpeg can still enqueue render-thread upload tasks right after stop.
                // Run stop/release in a deferred sequence so queued uploads can drain first.
                this.queueDeferredHardStopAndReleaseForClose(cachedPlayer, closeReleaseVersion, HARD_STOP_DEFER_TICKS);
            } else {
                // Shutdown can begin between the global-state check and tracker registration; ownership then stays on this close call.
                WatermediaReflectionBridge.playerStop(cachedPlayer);
                WatermediaReflectionBridge.playerReleaseForShutdown(cachedPlayer);
            }
        }
        this.clearFrameTextureId();
        try {
            Minecraft.getInstance().getTextureManager().release(this.frameLocation);
        } catch (Exception ignored) {}
        File temp = this.generatedTempFile;
        if ((temp != null) && temp.isFile()) {
            temp.delete();
        }
        this.ready = false;
    }

    /** Returns whether closed. */
    @Override
    public boolean isClosed() {
        return this.closed;
    }

    /** Writes the input stream to temp file to the video resource output. */
    @Nullable
    protected File writeInputStreamToTempFile(@NotNull InputStream in, @NotNull String sourceName) {
        File targetFile = new File(TEMP_VIDEO_DIR, "mp4_video_" + this.uniqueId.toLowerCase().replace("-", "") + "_" + System.nanoTime() + ".mp4");
        try (InputStream input = in; FileOutputStream out = new FileOutputStream(targetFile)) {
            ResourceInputLimits.copy(input, out, ResourceRuntime.getSafetyLimits().maxArchiveBytes(), "MP4 stream");
            return targetFile;
        } catch (Exception ex) {
            if (targetFile.isFile()) targetFile.delete();
            LOGGER.error("[KONKRETE] Failed to write MP4 video stream to temporary file: {}", sourceName, ex);
        }
        return null;
    }

    /** Applies a queued positive seek after duration metadata becomes available, leaving unavailable seeks pending. */
    protected void tryApplyQueuedSeekToPlayer(@Nullable Object player) {
        if (player == null) return;
        long requestedMs = this.seekRequestedMs;
        if (requestedMs < 0L) return;
        if (requestedMs <= 0L) {
            this.seekRequestedMs = -1L;
            return;
        }
        String statusName = WatermediaReflectionBridge.playerStatusName(player);
        if (statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING")) return;
        // FFMediaPlayer.seek(..) clamps against duration(). If we seek before metadata is ready, duration is 0 and the target collapses to 0ms.
        long durationMs = WatermediaReflectionBridge.playerDuration(player);
        if (durationMs <= 0L) return;
        requestedMs = Math.min(requestedMs, durationMs);
        if (requestedMs <= 0L) {
            this.seekRequestedMs = -1L;
            return;
        }
        if (WatermediaReflectionBridge.playerSeek(player, requestedMs)) {
            this.seekRequestedMs = -1L;
        }
    }

    /** Updates video resource state when dependency missing occurs. */
    protected void onDependencyMissing(@NotNull String sourceName) {
        this.dependencyMissing = true;
        this.loadingFailed = true;
        this.ready = true;
        this.playRequested = false;
        this.lastPlayRequestTimestampMs = -1L;
        this.resetVideoPlaybackListenerState();
        this.framePresented = false;
        LOGGER.warn("[KONKRETE] Watermedia V3 and/or Watermedia Binaries are not loaded, MP4 source will render as missing texture: {}", sourceName);
    }

    /** Clears the frame texture id from this video resource component. */
    protected void clearFrameTextureId() {
        WatermediaFrameTexture cachedFrameTexture = this.frameTexture;
        if (cachedFrameTexture != null) {
            cachedFrameTexture.setHandle(0L);
        }
    }

    /** Returns whether open al ready for player creation. */
    protected boolean isOpenAlReadyForPlayerCreation() {
        try {
            return ALUtils.isOpenAlReady();
        } catch (RuntimeException ignored) {
            // A very early PRE_CLIENT_TICK can run before Minecraft has exposed its SoundManager through Melody's accessors.
            return false;
        }
    }

    /**
     * Detaches this player while Minecraft's old OpenAL context is still current. Playback intent is deliberately kept
     * so the same resource can recreate its player after the reload without element-specific replacement logic.
     */
    void releasePlayerBeforeSoundEngineReload() {
        if (this.closed) return;
        Object cachedPlayer;
        synchronized (this.playerInitLock) {
            cachedPlayer = this.mediaPlayer;
            if (cachedPlayer == null) return;
            if (this.playRequested && (this.seekRequestedMs < 0L)) {
                // An explicit seek that is still waiting for metadata takes precedence over the last reported time.
                long playTimeMs = WatermediaReflectionBridge.playerTime(cachedPlayer);
                if (playTimeMs > 0L) this.seekRequestedMs = playTimeMs;
            }
            this.stopRequestVersion++;
            this.mediaPlayer = null;
            this.resetFramePresentationStateForNewPlayer();
        }
        WatermediaReflectionBridge.playerPause(cachedPlayer, true);
        WatermediaReflectionBridge.playerRelease(cachedPlayer);
    }

    void retryPlayerAfterSoundEngineReload() {
        if (this.closed || !this.playRequested || this.mediaPlayer != null) return;
        this.queuePlayerInitializationTask();
    }

    /** Marks the video resource as failed and records the fail condition. */
    protected void fail(@NotNull String message, @Nullable Throwable cause) {
        this.loadingFailed = true;
        this.ready = true;
        this.playRequested = false;
        this.lastPlayRequestTimestampMs = -1L;
        this.resetVideoPlaybackListenerState();
        this.framePresented = false;
        if (cause != null) LOGGER.error("[KONKRETE] {}", message, cause);
        else LOGGER.error("[KONKRETE] {}", message);
    }

    /** Returns whether loading player status. */
    protected boolean isLoadingPlayerStatus(@NotNull String statusName) {
        return statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING");
    }

    /** Returns whether terminal player status. */
    protected boolean isTerminalPlayerStatus(@NotNull String statusName) {
        return statusName.equals("STOPPED") || statusName.equals("ENDED") || statusName.equals("ERROR");
    }

    /** Returns whether present frame. */
    protected boolean shouldPresentFrame(@NotNull String statusName) {
        if (!this.playRequested) return false;
        if (this.isLoadingPlayerStatus(statusName)) {
            // Watermedia keeps its last GL texture alive while repeat seeks and ordinary seeks temporarily enter BUFFERING. Preserve that frame once one was presented so renderers do not expose their fallback between decoded frames.
            return this.framePresented;
        }
        return !this.isTerminalPlayerStatus(statusName);
    }

    /** Starts the player for current request for the video resource. */
    protected void startPlayerForCurrentRequest(@NotNull Object player) {
        WatermediaReflectionBridge.playerStartPaused(player);
        if (this.pausedRequested) {
            WatermediaReflectionBridge.playerPause(player, true);
            return;
        }
        long startVersion = this.stopRequestVersion;
        this.queueDeferredInitialUnpause(player, startVersion, INITIAL_UNPAUSE_DEFER_TICKS, INITIAL_UNPAUSE_STATUS_WAIT_TICKS);
    }

    /** Resets the frame presentation state for new player to its initial video resource state. */
    protected void resetFramePresentationStateForNewPlayer() {
        this.framePresented = false;
        WatermediaFrameTexture cachedFrameTexture = this.frameTexture;
        if (cachedFrameTexture != null) {
            cachedFrameTexture.setHandle(0L);
        }
    }

    /** Schedules the deferred initial unpause on the required video resource lifecycle boundary. */
    protected void queueDeferredInitialUnpause(@NotNull Object player, long startVersion, int ticksToWait, int statusWaitTicks) {
        MainThreadTaskExecutor.executeInMainThread(() -> this.runDeferredInitialUnpause(player, startVersion, ticksToWait, statusWaitTicks), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    /** Runs the deferred initial unpause for the video resource. */
    protected void runDeferredInitialUnpause(@NotNull Object player, long startVersion, int ticksToWait, int statusWaitTicks) {
        if (!this.shouldExecuteDeferredInitialUnpause(player, startVersion)) return;
        if (ticksToWait > 0) {
            this.queueDeferredInitialUnpause(player, startVersion, ticksToWait - 1, statusWaitTicks);
            return;
        }

        String statusName = WatermediaReflectionBridge.playerStatusName(player);
        if (statusName.equals("WAITING") || statusName.equals("LOADING")) {
            if (statusWaitTicks > 0) {
                this.queueDeferredInitialUnpause(player, startVersion, 0, statusWaitTicks - 1);
            }
            return;
        }

        if (this.shouldRecoverPrematureEnd(player, statusName)) {
            this.recoverFromPrematureEnd(player);
            return;
        }
        if (this.isTerminalPlayerStatus(statusName)) return;

        this.tryApplyQueuedSeekToPlayer(player);
        WatermediaReflectionBridge.playerPause(player, false);
        this.tryApplyQueuedSeekToPlayer(player);
    }

    /** Returns whether execute deferred initial unpause. */
    protected boolean shouldExecuteDeferredInitialUnpause(@NotNull Object player, long startVersion) {
        if (this.closed || !this.playRequested || this.pausedRequested) return false;
        if (this.stopRequestVersion != startVersion) return false;
        return this.mediaPlayer == player;
    }

    /** Returns whether recover premature end. */
    protected boolean shouldRecoverPrematureEnd(@NotNull Object player, @NotNull String statusName) {
        if (!statusName.equals("ENDED")) return false;
        if (!this.playRequested || this.pausedRequested || this.framePresented) return false;

        long durationMs = WatermediaReflectionBridge.playerDuration(player);
        if (durationMs <= PREMATURE_END_START_WINDOW_MS) return false;

        long timeMs = Math.max(0L, WatermediaReflectionBridge.playerTime(player));
        return timeMs < Math.min(PREMATURE_END_START_WINDOW_MS, Math.max(0L, durationMs - Math.round(PLAYBACK_END_EPSILON_SECONDS * 1000.0D)));
    }

    /** Returns whether natural end status. */
    protected boolean isNaturalEndStatus(@NotNull Object player, @NotNull String statusName) {
        if (!statusName.equals("ENDED")) return false;

        long durationMs = WatermediaReflectionBridge.playerDuration(player);
        if (durationMs <= 0L) return this.framePresented;

        long timeMs = Math.max(0L, WatermediaReflectionBridge.playerTime(player));
        long endEpsilonMs = Math.max(1L, Math.round(PLAYBACK_END_EPSILON_SECONDS * 1000.0D));
        return timeMs >= Math.max(0L, durationMs - endEpsilonMs);
    }

    /** Recovers a prematurely ended native player while preserving the requested playback state. */
    protected void recoverFromPrematureEnd(@NotNull Object player) {
        if (!this.prematureEndLogged) {
            this.prematureEndLogged = true;
            LOGGER.warn("[KONKRETE] Watermedia reported MP4 playback as ended before Konkrete received a video frame. Recreating the player to work around the early-end state. source: {}", this.resolveVideoSourceForListener());
        }
        this.recreatePlayerForAutoplay(player);
    }

    /** Recreates the player for autoplay for the video resource. */
    protected void recreatePlayerForAutoplay(@NotNull Object player) {
        Runnable recreateTask = () -> {
            if (this.closed || !this.playRequested || this.pausedRequested) return;
            synchronized (this.playerInitLock) {
                if (this.mediaPlayer != player) return;
                this.mediaPlayer = null;
                WatermediaReflectionBridge.playerRelease(player);
            }
            this.resetFramePresentationStateForNewPlayer();
            this.createPlayerIfPossible();
        };
        if (Minecraft.getInstance().isSameThread()) {
            recreateTask.run();
        } else {
            MainThreadTaskExecutor.executeInMainThread(recreateTask, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }
    }

    /** Schedules the deferred hard stop on the required video resource lifecycle boundary. */
    protected void queueDeferredHardStop(@NotNull Object player, long stopVersion, int ticksToWait) {
        MainThreadTaskExecutor.executeInMainThread(() -> this.runDeferredHardStop(player, stopVersion, ticksToWait), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    /** Runs the deferred hard stop for the video resource. */
    protected void runDeferredHardStop(@NotNull Object player, long stopVersion, int ticksToWait) {
        if (!this.shouldExecuteDeferredHardStop(player, stopVersion)) return;
        if (ticksToWait > 0) {
            this.queueDeferredHardStop(player, stopVersion, ticksToWait - 1);
            return;
        }
        WatermediaReflectionBridge.playerStop(player);
    }

    /** Returns whether execute deferred hard stop. */
    protected boolean shouldExecuteDeferredHardStop(@NotNull Object player, long stopVersion) {
        if (this.closed) return false;
        if (this.stopRequestVersion != stopVersion) return false;
        if (this.playRequested) return false;
        return this.mediaPlayer == player;
    }

    /** Schedules the deferred player release on the required video resource lifecycle boundary. */
    protected void queueDeferredPlayerRelease(@NotNull Object player, long closeReleaseVersion, int ticksToWait) {
        MainThreadTaskExecutor.executeInMainThread(() -> this.runDeferredPlayerRelease(player, closeReleaseVersion, ticksToWait), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    /** Schedules the deferred hard stop and release for close on the required video resource lifecycle boundary. */
    protected void queueDeferredHardStopAndReleaseForClose(@NotNull Object player, long closeReleaseVersion, int ticksToWait) {
        MainThreadTaskExecutor.executeInMainThread(() -> this.runDeferredHardStopAndReleaseForClose(player, closeReleaseVersion, ticksToWait), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    /** Runs the deferred hard stop and release for close for the video resource. */
    protected void runDeferredHardStopAndReleaseForClose(@NotNull Object player, long closeReleaseVersion, int ticksToWait) {
        if (!this.shouldExecuteDeferredPlayerRelease(closeReleaseVersion)) return;
        if (ticksToWait > 0) {
            this.queueDeferredHardStopAndReleaseForClose(player, closeReleaseVersion, ticksToWait - 1);
            return;
        }
        WatermediaReflectionBridge.playerStop(player);
        this.queueDeferredPlayerRelease(player, closeReleaseVersion, CLOSE_RELEASE_MAX_DEFER_TICKS);
    }

    /** Runs the deferred player release for the video resource. */
    protected void runDeferredPlayerRelease(@NotNull Object player, long closeReleaseVersion, int ticksToWait) {
        if (!this.shouldExecuteDeferredPlayerRelease(closeReleaseVersion)) return;
        if ((ticksToWait > 0) && !this.isReadyForDeferredPlayerRelease(player)) {
            this.queueDeferredPlayerRelease(player, closeReleaseVersion, ticksToWait - 1);
            return;
        }
        WatermediaDeferredPlayerReleaseTracker.release(player);
    }

    /** Returns whether execute deferred player release. */
    protected boolean shouldExecuteDeferredPlayerRelease(long closeReleaseVersion) {
        return this.closeReleaseRequestVersion == closeReleaseVersion;
    }

    /** Returns whether ready for deferred player release. */
    protected boolean isReadyForDeferredPlayerRelease(@NotNull Object player) {
        String statusName = WatermediaReflectionBridge.playerStatusName(player);
        if (statusName.equals("UNKNOWN")) return true;
        return this.isTerminalPlayerStatus(statusName);
    }

    /** Seeks the player to beginning for the video resource. */
    protected void seekPlayerToBeginning(@NotNull Object player) {
        String statusName = WatermediaReflectionBridge.playerStatusName(player);
        if (statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING")) {
            this.seekRequestedMs = 1L;
            return;
        }
        if (WatermediaReflectionBridge.playerSeek(player, 0L)) {
            this.seekRequestedMs = -1L;
            return;
        }
        this.seekRequestedMs = 1L;
    }

    /** Conditionally updates the emit video started event for the video resource. */
    protected boolean maybeEmitVideoStartedEvent() {
        if (!this.hasVideoPlaybackListeners()) return false;
        if (this.listenerPlaybackCycleActive) return false;
        this.listenerPlaybackCycleActive = true;
        this.listenerFinishedEventEmittedForCycle = false;
        this.listenerLastKnownPlaybackTimeSeconds = 0.0D;
        return true;
    }

    /** Conditionally updates the emit video finished event for the video resource. */
    protected boolean maybeEmitVideoFinishedEvent(boolean willRestart) {
        if (!this.hasVideoPlaybackListeners()) return false;
        if (!this.listenerPlaybackCycleActive || this.listenerFinishedEventEmittedForCycle) return false;
        this.listenerPlaybackCycleActive = false;
        this.listenerFinishedEventEmittedForCycle = true;
        return true;
    }

    /** Resets the video playback listener state to its initial video resource state. */
    protected void resetVideoPlaybackListenerState() {
        this.listenerPlaybackCycleActive = false;
        this.listenerFinishedEventEmittedForCycle = false;
        this.listenerLastKnownPlaybackTimeSeconds = 0.0D;
    }

    /** Returns whether video playback listeners. */
    protected boolean hasVideoPlaybackListeners() {
        return ResourceRuntime.hasVideoPlaybackListeners();
    }

    /** Conditionally updates the emit video playback status changed for the video resource. */
    protected void maybeEmitVideoPlaybackStatusChanged(@NotNull ResourceRuntime.VideoPlaybackStatus status) {
        ResourceRuntime.fireVideoPlaybackEvent(this.resolveVideoSourceForListener(), this.resolveVideoSourceTypeForListener(), this.looping, status);
    }

    /** Updates the video playback listener state from player for the video resource. */
    protected void updateVideoPlaybackListenerStateFromPlayer(@NotNull Object player, @NotNull String statusName) {
        if (!this.hasVideoPlaybackListeners()) {
            this.resetVideoPlaybackListenerState();
            return;
        }
        if (!this.playRequested || this.pausedRequested) return;
        if (!this.listenerPlaybackCycleActive && !this.listenerFinishedEventEmittedForCycle) return;

        long durationMs = WatermediaReflectionBridge.playerDuration(player);
        long timeMs = WatermediaReflectionBridge.playerTime(player);
        double duration = Math.max(0.0D, durationMs / 1000.0D);
        double currentTime = Math.max(0.0D, timeMs / 1000.0D);
        double previousTime = Math.max(0.0D, this.listenerLastKnownPlaybackTimeSeconds);
        boolean playing = WatermediaReflectionBridge.playerIsPlaying(player) || statusName.equals("PLAYING");

        if (this.listenerPlaybackCycleActive && !this.listenerFinishedEventEmittedForCycle) {
            boolean nearEnd = duration > PLAYBACK_END_EPSILON_SECONDS && currentTime >= Math.max(0.0D, duration - PLAYBACK_END_EPSILON_SECONDS);
            if (!this.looping && nearEnd && !playing) {
                if (this.maybeEmitVideoFinishedEvent(false)) {
                    this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.FINISHED);
                }
            } else if (this.looping) {
                boolean wrappedAround = duration > PLAYBACK_END_EPSILON_SECONDS
                        && previousTime >= Math.max(0.0D, duration - PLAYBACK_END_EPSILON_SECONDS)
                        && currentTime <= LOOP_RESTART_WINDOW_SECONDS
                        && playing;
                if (wrappedAround) {
                    if (this.maybeEmitVideoFinishedEvent(true)) {
                        this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.FINISHED);
                    }
                    if (this.maybeEmitVideoStartedEvent()) {
                        this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.PLAYING);
                    }
                } else if (nearEnd && !playing) {
                    if (this.maybeEmitVideoFinishedEvent(true)) {
                        this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.FINISHED);
                    }
                }
            }
        } else if (this.looping && this.listenerFinishedEventEmittedForCycle) {
            boolean restarted = playing && (currentTime <= LOOP_RESTART_WINDOW_SECONDS || (previousTime > (currentTime + LOOP_RESTART_WINDOW_SECONDS)));
            if (restarted) {
                if (this.maybeEmitVideoStartedEvent()) {
                    this.maybeEmitVideoPlaybackStatusChanged(ResourceRuntime.VideoPlaybackStatus.PLAYING);
                }
            }
        }

        this.listenerLastKnownPlaybackTimeSeconds = currentTime;
    }

    /** Resolves the video source for listener for the video resource. */
    @NotNull
    protected String resolveVideoSourceForListener() {
        if (this.sourceURL != null) return this.sourceURL;
        if (this.sourceLocation != null) return this.sourceLocation.toString();
        if (this.sourceFile != null) return this.sourceFile.getPath();
        return "ERROR";
    }

    /** Resolves the video source type for listener for the video resource. */
    @NotNull
    protected ResourceSourceType resolveVideoSourceTypeForListener() {
        if (this.sourceURL != null) return ResourceSourceType.WEB;
        if (this.sourceLocation != null) return ResourceSourceType.LOCATION;
        return ResourceSourceType.LOCAL;
    }

}
