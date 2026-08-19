package de.keksuccino.konkrete.util.rendering.video.rinku;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.konkrete.util.lifecycle.ClientShutdownHandler;
import de.keksuccino.konkrete.util.rinku.RinkuIntegrationConfig;
import de.keksuccino.konkrete.util.rinku.RinkuUtil;
import de.keksuccino.konkrete.util.rinku.WrappedRinkuBrowser;
import de.keksuccino.konkrete.util.threading.KonkreteExecutors;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import de.keksuccino.rinku.Rinku;
import org.cef.browser.CefBrowser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Browser-backed video surface using Rinku and Konkrete's generic HTML player. Rinku must be installed, though
 * construction may precede its asynchronous initialization; operations queued before a successful load retain order.
 * Rendering belongs on Minecraft's render thread; page calls use the configurable browser executor, whose default is
 * the client thread. This object owns its browser, and disposal is terminal. Browser-backed synchronous getters wait
 * for the configured query deadline and must stay off hot paths.
 */
public class RinkuVideoPlayer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final ScheduledExecutorService PLAYBACK_LISTENER_EXECUTOR = KonkreteExecutors.newSingleThreadScheduledExecutor("Konkrete-RinkuVideoPlayer-PlaybackListener");
    private static final long PLAYBACK_LISTENER_TICK_MS = 250L;
    private static final double PLAYBACK_END_EPSILON_SECONDS = 0.12D;
    private static final double LOOP_RESTART_WINDOW_SECONDS = 0.40D;

    /** Browser wrapper owned by this player after initialization. */
    protected volatile WrappedRinkuBrowser browser;
    /** Desired media volume in the inclusive zero-to-one range. */
    protected volatile float volume = 1.0f;
    /** Whether media looping is enabled. */
    protected volatile boolean looping = false;
    /** Whether the video should cover its browser viewport. */
    protected volatile boolean fillScreen = false;
    /** Whether video aspect ratio should be preserved. */
    protected volatile boolean preserveAspectRatio = true;
    /** Current browser-ready video source. */
    protected volatile String currentVideoPath = null;
    /** Desired player mute state. */
    protected volatile boolean isMuted = false;
    /** Player X coordinate in GUI space. */
    protected volatile int posX = 0;
    /** Player Y coordinate in GUI space. */
    protected volatile int posY = 0;
    /** Player width in GUI space. */
    protected volatile int width = 200;
    /** Player height in GUI space. */
    protected volatile int height = 200;
    /** Whether the browser player page completed initialization. */
    protected volatile boolean initialized = false;
    /** Whether listeners currently consider a playback cycle active. */
    protected volatile boolean listenerPlaybackCycleActive = false;
    /** Whether the current playback cycle already emitted its finish event. */
    protected volatile boolean listenerFinishedEventEmittedForCycle = false;
    /** Last sampled playback time used for loop-transition detection. */
    protected volatile double listenerLastKnownPlaybackTimeSeconds = 0.0D;
    /** Last listener-facing pause state. */
    protected volatile boolean listenerPausedState = false;
    /** Whether Rinku owns a deferred initialization callback for this player. */
    protected volatile boolean initializationScheduled = false;
    /** Whether this player has released its browser and pending work. */
    protected volatile boolean disposed = false;
    /** Periodic listener sampler, when active. */
    @Nullable
    protected volatile ScheduledFuture<?> playbackListenerTicker = null;
    private final Object pendingTaskLock = new Object();
    private final AtomicBoolean playbackSamplePending = new AtomicBoolean();
    private final List<Runnable> pendingInitializationTasks = new ArrayList<>();
    private final List<PlaybackListener> playbackListeners = new CopyOnWriteArrayList<>();
    private final String instanceId = UUID.randomUUID().toString();

    /** Creates a default-size player and starts retryable asynchronous Rinku initialization. */
    public RinkuVideoPlayer() {
        initialize();
    }

    /** Creates a bounded player and starts retryable asynchronous Rinku initialization. */
    public RinkuVideoPlayer(int x, int y, int width, int height) {
        this.posX = x;
        this.posY = y;
        this.width = width;
        this.height = height;
        initialize();
    }

    /** Starts or retries initialization on the client thread; stale failed browsers are closed, while disposal is terminal. */
    public void initialize() {
        if (this.disposed || this.initialized || this.initializationScheduled) return;
        if (!RinkuUtil.isRinkuLoaded()) {
            LOGGER.error("[KONKRETE] Failed to initialize Rinku video player because Rinku is unavailable");
            return;
        }
        if (!Rinku.isInitialized()) {
            this.initializationScheduled = true;
            Rinku.scheduleForInit(success -> {
                this.initializationScheduled = false;
                if (success && !this.disposed) MainThreadTaskExecutor.executeInMainThread(this::initialize, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            });
            return;
        }
        RinkuVideoManager.getInstance().initialize();

        WrappedRinkuBrowser staleBrowser = this.browser;
        if (staleBrowser != null) {
            try {
                staleBrowser.close();
            } catch (Exception exception) {
                LOGGER.error("[KONKRETE] Failed to close a stale browser before retrying player initialization", exception);
            }
            this.browser = null;
        }

        try {
            String playerUrl = buildPlayerUrl();

            File playerFile = RinkuVideoManager.getInstance().getPlayerFile().toFile();
            if (!playerFile.exists()) {
                LOGGER.warn("[KONKRETE] Player HTML file not found. Attempting to extract resources.");
                LOGGER.error("[KONKRETE] Rinku player asset does not exist at {}", playerFile.getAbsolutePath());
                return;
            }

            // The manager, rather than BrowserHandler's inactivity timeout, owns this browser.
            this.browser = WrappedRinkuBrowser.build(playerUrl, false, false, posX, posY, width, height, success -> {
                if (success) {
                    initialized = true;
                    this.runPendingInitializationTasks();
                    this.startPlaybackListenerTicker();
                } else {
                    LOGGER.error("[KONKRETE] Failed to initialize RinkuVideoPlayer for browser with ID: " + (this.browser != null ? this.browser.getIdentifier() : "unknown"));
                    initialized = false;
                }
            });

            if (this.browser != null) {
                // The bundled page API owns autoplay and mute state for this specialized wrapper.
                this.browser.setAutoPlayAllVideosOnLoad(false);
                this.browser.setMuted(false);
            } else {
                LOGGER.error("[KONKRETE] RinkuVideoPlayer: Browser was not created successfully. Player will not function.");
                initialized = false;
            }

        } catch (Exception e) {
            LOGGER.error("[KONKRETE] Failed to initialize RinkuVideoPlayer [{}]", instanceId, e);
            initialized = false;
        }
    }

    /** Builds the bundled local-player URL with encoded initial settings. */
    protected String buildPlayerUrl() {
        File playerFile = RinkuVideoManager.getInstance().getPlayerFile().toFile();
        String basePath = playerFile.toURI().toString();

        Map<String, String> params = new HashMap<>();
        params.put("volume", String.valueOf(this.volume));
        params.put("loop", String.valueOf(this.looping));
        params.put("fillScreen", String.valueOf(this.fillScreen));
        params.put("preserveAspectRatio", String.valueOf(this.preserveAspectRatio));
        params.put("autoPlay", "false");

        return basePath + "?" + buildQueryString(params);
    }

    /** Encodes player parameters as an application/x-www-form-urlencoded query. */
    protected String buildQueryString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (result.length() > 0) {
                result.append("&");
            }

            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        return result.toString();
    }

    /**
     * Helper to ensure initialization and run on executor
     */
    private void executeWhenInitialized(@NotNull Runnable task) {
        if (this.disposed) return;
        if (this.initialized && this.browser != null) {
            dispatchBrowserTask(task);
            return;
        }
        synchronized (this.pendingTaskLock) {
            if (this.disposed) return;
            if (this.initialized && this.browser != null) {
                dispatchBrowserTask(task);
            } else {
                this.pendingInitializationTasks.add(task);
            }
        }
    }

    private void runPendingInitializationTasks() {
        List<Runnable> tasks;
        synchronized (this.pendingTaskLock) {
            tasks = new ArrayList<>(this.pendingInitializationTasks);
            this.pendingInitializationTasks.clear();
        }
        for (Runnable task : tasks) {
            dispatchBrowserTask(task);
        }
    }

    @Nullable
    static Throwable dispatchBrowserTask(@NotNull Runnable task) {
        try {
            RinkuIntegrationConfig.BrowserTaskDispatcher dispatcher = RinkuIntegrationConfig.getBrowserTaskDispatcher();
            if (dispatcher.isCurrentThread()) task.run();
            else dispatcher.execute(task);
            return null;
        } catch (Throwable throwable) {
            LOGGER.error("[KONKRETE] Failed to dispatch a Rinku browser operation", throwable);
            return throwable;
        }
    }

    /** Extracts the current browser frame into GUI render state on Minecraft's render thread. */
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (browser != null && initialized) {
            browser.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Queues a caller-supplied local-file URI or network URL; the value is JSON-encoded before script injection. */
    public void loadVideo(@NotNull String videoPath) {
        executeWhenInitialized(() -> {
            boolean sourceChanged = !Objects.equals(this.currentVideoPath, videoPath);
            if (sourceChanged) {
                this.resetVideoPlaybackListenerState();
            }
            this.currentVideoPath = videoPath;
            String script = String.format("if(window.videoPlayerAPI && window.videoPlayerAPI.loadVideo) { window.videoPlayerAPI.loadVideo(%s); } else { console.error('[KONKRETE] videoPlayerAPI.loadVideo not found!'); }", GSON.toJson(videoPath));
            executeJavaScriptDirect(script);
        });
    }

    /** Queues client-thread playback and begins a listener-visible playback cycle. */
    public void play() {
        executeWhenInitialized(() -> {
            boolean wasPaused = this.listenerPausedState;
            this.listenerPausedState = false;
            boolean startedNow = this.maybeEmitVideoStartedEvent();
            if (startedNow || wasPaused) {
                this.maybeEmitVideoPlaybackStatusChanged(PlaybackStatus.PLAYING);
            }
            executeJavaScriptDirect("if(window.videoPlayerAPI && window.videoPlayerAPI.play) { window.videoPlayerAPI.play(); } else { console.error('[KONKRETE] videoPlayerAPI.play not found!'); }");
        });
    }

    /** Queues a client-thread pause without ending the listener-visible playback cycle. */
    public void pause() {
        executeWhenInitialized(() -> {
            boolean wasPaused = this.listenerPausedState;
            this.listenerPausedState = true;
            if (!wasPaused && this.listenerPlaybackCycleActive) {
                this.maybeEmitVideoPlaybackStatusChanged(PlaybackStatus.PAUSED);
            }
            executeJavaScriptDirect("if(window.videoPlayerAPI && window.videoPlayerAPI.pause) { window.videoPlayerAPI.pause(); } else { console.error('[KONKRETE] videoPlayerAPI.pause not found!'); }");
        });
    }

    /** Queues a client-thread play/pause toggle. */
    public void togglePlayPause() {
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI && window.videoPlayerAPI.togglePlayPause) { window.videoPlayerAPI.togglePlayPause(); } else { console.error('[KONKRETE] videoPlayerAPI.togglePlayPause not found!'); }");
        });
    }

    /** Queues a client-thread stop, resets time, and ends the listener-visible playback cycle. */
    public void stop() {
        executeWhenInitialized(() -> {
            boolean hadActivePlaybackState = this.listenerPlaybackCycleActive || this.listenerPausedState;
            if (hadActivePlaybackState) {
                this.maybeEmitVideoPlaybackStatusChanged(PlaybackStatus.STOPPED);
            }
            this.resetVideoPlaybackListenerState();
            executeJavaScriptDirect("if(window.videoPlayerAPI && window.videoPlayerAPI.stop) { window.videoPlayerAPI.stop(); } else { console.error('[KONKRETE] videoPlayerAPI.stop not found!'); }");
        });
    }

    /** Sets the desired page-player mute state, retaining it across asynchronous initialization. */
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI) { window.videoPlayerAPI.setMuted(" + muted + "); }");
        });
    }

    /** Returns the desired page-player mute state without querying Chromium. */
    public boolean getMuted() {
        return this.isMuted;
    }

    /** Toggles the desired page-player mute state. */
    public void toggleMuted() {
        setMuted(!getMuted());
    }

    /** Sets desired page-player volume, clamped to the inclusive zero-to-one range. */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI) { window.videoPlayerAPI.setVolume(" + this.volume + "); }");
        });
    }

    /** Returns desired page-player volume without querying Chromium. */
    public float getVolume() {
        return this.volume;
    }

    /** Sets desired looping and retains it across asynchronous initialization. */
    public void setLooping(boolean looping) {
        this.looping = looping;
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI) { window.videoPlayerAPI.setLoop(" + looping + "); }");
        });
    }

    /** Returns desired looping without querying Chromium. */
    public boolean isLooping() {
        return this.looping;
    }

    /** Sets whether video covers the browser viewport. */
    public void setFillScreen(boolean fillScreen) {
        this.fillScreen = fillScreen;
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI) { window.videoPlayerAPI.setFillScreen(" + fillScreen + "); }");
        });
    }

    /** Returns whether video is configured to cover the browser viewport. */
    public boolean isFillScreen() {
        return this.fillScreen;
    }

    /** Sets whether video preserves its source aspect ratio inside the browser viewport. */
    public void setPreserveAspectRatio(boolean preserveAspectRatio) {
        this.preserveAspectRatio = preserveAspectRatio;
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI) { window.videoPlayerAPI.setPreserveAspectRatio(" + preserveAspectRatio + "); }");
        });
    }

    /** Returns whether source aspect ratio preservation is configured. */
    public boolean isPreserveAspectRatio() {
        return this.preserveAspectRatio;
    }

    /** Queries duration in seconds, blocking at most the configured deadline and returning zero on failure. */
    public double getDuration() {
        if (!initialized) return 0;
        String jsCode = "(function() { try { var dur = window.videoPlayerAPI.getDuration(); return (dur === undefined || dur === null || isNaN(dur)) ? 0 : dur; } catch(e) { console.error('Error in getDuration:', e); return 0; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return 0;
        try {
            return Double.parseDouble(result);
        } catch (NumberFormatException e) {
            LOGGER.warn("[KONKRETE] Player [{}]: Could not parse duration from JS result: '{}'. JS Code: {}", instanceId, result, jsCode);
            return 0;
        }
    }

    /** Queries duration in milliseconds under the same bounded semantics as {@link #getDuration()}. */
    public long getDurationMillis() {
        return (long)(getDuration() * 1000);
    }

    /** Queries playback time in seconds, blocking at most the configured deadline and returning zero on failure. */
    public double getCurrentTime() {
        if (!initialized) return 0;
        String jsCode = "(function() { try { var time = window.videoPlayerAPI.getCurrentTime(); return (time === undefined || time === null || isNaN(time)) ? 0 : time; } catch(e) { console.error('Error in getCurrentTime:', e); return 0; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return 0;
        try {
            return Double.parseDouble(result);
        } catch (NumberFormatException e) {
            LOGGER.warn("[KONKRETE] Player [{}]: Could not parse current time from JS result: '{}'. JS Code: {}", instanceId, result, jsCode);
            return 0;
        }
    }

    /** Queries playback time in milliseconds under the same bounded semantics as {@link #getCurrentTime()}. */
    public long getCurrentTimeMillis() {
        return (long)(getCurrentTime() * 1000);
    }

    /** Queues an absolute seek in seconds, clamping negative positions to zero. */
    public void setCurrentTime(double seconds) {
        final double secondsFinal = Math.max(0, seconds);
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI) { window.videoPlayerAPI.setCurrentTime(" + secondsFinal + "); }");
        });
    }

    /** Queues an absolute seek in milliseconds. */
    public void setCurrentTimeMillis(long milliseconds) {
        setCurrentTime(milliseconds / 1000.0);
    }

    /** Queues a relative forward seek bounded by media duration. */
    public void seekForward(double seconds) {
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI) { var currentTime = window.videoPlayerAPI.getCurrentTime(); var duration = window.videoPlayerAPI.getDuration(); window.videoPlayerAPI.setCurrentTime(Math.min(currentTime + " + seconds + ", duration)); }");
        });
    }

    /** Queues a relative backward seek bounded by zero. */
    public void seekBackward(double seconds) {
        executeWhenInitialized(() -> {
            executeJavaScriptDirect("if(window.videoPlayerAPI) { var currentTime = window.videoPlayerAPI.getCurrentTime(); window.videoPlayerAPI.setCurrentTime(Math.max(currentTime - " + seconds + ", 0)); }");
        });
    }

    /** Returns the bounded current-time query formatted as unbounded minutes and seconds. */
    public String getFormattedCurrentTime() {
        return formatTime(getCurrentTime());
    }

    /** Returns the bounded duration query formatted as unbounded minutes and seconds. */
    public String getFormattedDuration() {
        return formatTime(getDuration());
    }

    /** Returns the bounded current-time query with an hours component only when non-zero. */
    public String getDetailedFormattedCurrentTime() {
        return formatTimeDetailed(getCurrentTime());
    }

    /** Returns the bounded duration query with an hours component only when non-zero. */
    public String getDetailedFormattedDuration() {
        return formatTimeDetailed(getDuration());
    }

    /** Returns compact current-time and duration queries separated by a slash. */
    public String getFormattedTimeInfo() {
        return getFormattedCurrentTime() + " / " + getFormattedDuration();
    }

    /** Returns detailed current-time and duration queries separated by a slash. */
    public String getDetailedFormattedTimeInfo() {
        return getDetailedFormattedCurrentTime() + " / " + getDetailedFormattedDuration();
    }

    /** Returns bounded-query playback progress as an integer percentage, or zero for unknown duration. */
    public int getProgressPercentage() {
        double duration = getDuration();
        if (duration <= 0) return 0;
        return (int)((getCurrentTime() / duration) * 100);
    }

    /** Formats seconds as unbounded minutes plus two-digit seconds. */
    protected String formatTime(double seconds) {
        int totalSeconds = (int)Math.floor(seconds);
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    /** Formats seconds with an hours component only when non-zero. */
    protected String formatTimeDetailed(double seconds) {
        int totalSeconds = (int)Math.floor(seconds);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int remainingSeconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, remainingSeconds);
        } else {
            return String.format("%d:%02d", minutes, remainingSeconds);
        }
    }

    /** Queries active playback, blocking at most the configured deadline and returning false on failure. */
    public boolean isPlaying() {
        if (!initialized) return false;
        String jsCode = "(function() { try { return !!(window.videoPlayerAPI && window.videoPlayerAPI.isPlaying()); } catch(e) { console.error('Error in isPlaying:', e); return false; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return false;
        try {
            return Boolean.parseBoolean(result);
        } catch (Exception e) {
            LOGGER.warn("[KONKRETE] Player [{}]: Could not parse isPlaying state from JS result: '{}'. JS Code: {}", instanceId, result, jsCode, e);
            return false;
        }
    }

    /** Queries natural video width, blocking at most the configured deadline and returning zero on failure. */
    public int getVideoWidth() {
        if (!initialized) return 0;
        String jsCode = "(function() { try { var w = window.videoPlayerAPI.getVideoWidth(); return (w === undefined || w === null || isNaN(w)) ? 0 : w; } catch(e) { console.error('Error in getVideoWidth:', e); return 0; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return 0;
        try {
            return (int) Double.parseDouble(result);
        } catch (NumberFormatException e) {
            LOGGER.warn("[KONKRETE] Player [{}]: Could not parse video width from JS result: '{}'. JS Code: {}", instanceId, result, jsCode);
            return 0;
        }
    }

    /** Queries natural video height, blocking at most the configured deadline and returning zero on failure. */
    public int getVideoHeight() {
        if (!initialized) return 0;
        String jsCode = "(function() { try { var h = window.videoPlayerAPI.getVideoHeight(); return (h === undefined || h === null || isNaN(h)) ? 0 : h; } catch(e) { console.error('Error in getVideoHeight:', e); return 0; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return 0;
        try {
            return (int) Double.parseDouble(result);
        } catch (NumberFormatException e) {
            LOGGER.warn("[KONKRETE] Player [{}]: Could not parse video height from JS result: '{}'. JS Code: {}", instanceId, result, jsCode);
            return 0;
        }
    }

    /** Sets GUI-space browser bounds on the client thread, queueing the change until initialization succeeds. */
    public void setPosition(int x, int y) {
        this.posX = x;
        this.posY = y;
        // Use the executeWhenInitialized pattern to ensure it gets applied
        // even if called before the browser is ready
        executeWhenInitialized(() -> {
            browser.setPosition(x, y);
        });
    }

    /** Returns desired GUI-space X without querying Chromium. */
    public int getX() {
        return this.posX;
    }

    /** Returns desired GUI-space Y without querying Chromium. */
    public int getY() {
        return this.posY;
    }

    /** Sets GUI-space browser size on the client thread, queueing the change until initialization succeeds. */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        // Use the executeWhenInitialized pattern to ensure it gets applied
        // even if called before the browser is ready
        executeWhenInitialized(() -> {
            browser.setSize(width, height);
        });
    }

    /** Returns desired GUI-space width without querying Chromium. */
    public int getWidth() {
        return this.width;
    }

    /** Returns desired GUI-space height without querying Chromium. */
    public int getHeight() {
        return this.height;
    }

    /** Sets render opacity, clamped to zero-to-one and queued until initialization succeeds. */
    public void setOpacity(float opacity) {
        final float finalOpacity = Math.max(0.0F, Math.min(1.0F, opacity));
        executeWhenInitialized(() -> {
            if (browser != null) {
                browser.setOpacity(finalOpacity);
            }
        });
    }

    /** Returns the owned browser after creation, or null before creation and after disposal. */
    @Nullable
    public WrappedRinkuBrowser getBrowser() {
        return browser;
    }

    /** Registers an instance listener delivered on the configured callback executor. */
    public void addPlaybackListener(@NotNull PlaybackListener listener) {
        this.playbackListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Removes an instance listener; already queued callback snapshots may still include it. */
    public void removePlaybackListener(@NotNull PlaybackListener listener) {
        this.playbackListeners.remove(listener);
    }

    /** Dispatches page code through the configured browser target and executes it only when the browser is ready. */
    protected void executeJavaScript(@NotNull String code) {
        dispatchBrowserTask(() -> this.executeJavaScriptDirect(code));
    }

    /** Direct primitive for operations already admitted by {@link #dispatchBrowserTask(Runnable)}. */
    private void executeJavaScriptDirect(@NotNull String code) {
        if ((browser != null) && browser.getBrowser() != null && initialized) {
            try {
                browser.getBrowser().executeJavaScript(code, browser.getUrl(), 0);
            } catch (Exception e) {
                LOGGER.error("[KONKRETE] Player [{}]: Error executing JavaScript: {}", instanceId, e.getMessage(), e);
            }
        } else {
            String reason = (browser == null) ? "browser is null" : (!initialized ? "initialized is false" : "underlying Rinku browser is null");
            LOGGER.warn("[KONKRETE] Player [{}]: Attempted to execute JS when not ready. Reason: {}. Code: {}",
                instanceId, reason, code.substring(0, Math.min(50, code.length())));
        }
    }

    /** Dispatches a browser-scoped query through the configured browser target; its future expires at the configured deadline. */
    @NotNull
    protected CompletableFuture<String> executeJavaScriptWithResultAsync(@NotNull String jsCodeToEvaluate) {
        WrappedRinkuBrowser wrappedBrowser = this.browser;
        if (wrappedBrowser == null || !this.initialized) return CompletableFuture.failedFuture(new IllegalStateException("Rinku player is not ready"));
        CefBrowser cefBrowser = wrappedBrowser.getBrowser();
        String requestId = UUID.randomUUID().toString();
        RinkuVideoManager manager = RinkuVideoManager.getInstance();
        CompletableFuture<String> resultFuture = manager.registerPendingJsResult(cefBrowser, requestId);
        if (resultFuture == null) return CompletableFuture.failedFuture(new IllegalStateException("Rinku video manager is shutting down or the request identifier collided"));

        String script = "try {" +
                "var value=(" + jsCodeToEvaluate + ");" +
                "var serialized=JSON.stringify(value);" +
                "window.cefQuery({request:JSON.stringify({type:'" + RinkuVideoManager.JS_RESULT_REQUEST_TYPE + "',requestId:'" + requestId + "',result:serialized===undefined?null:serialized}),onSuccess:function(){},onFailure:function(code,message){console.error('[Konkrete] Video result query failed',code,message);}});" +
                "} catch(error) {" +
                "window.cefQuery({request:JSON.stringify({type:'" + RinkuVideoManager.JS_RESULT_REQUEST_TYPE + "',requestId:'" + requestId + "',error:String(error)}),onSuccess:function(){},onFailure:function(){}});" +
                "}";
        Throwable dispatchFailure = dispatchBrowserTask(() -> this.executeJavaScriptWithResultDirect(wrappedBrowser, cefBrowser, requestId, script, manager, resultFuture));
        if (dispatchFailure != null) {
            manager.removePendingJsResult(cefBrowser, requestId, resultFuture);
            resultFuture.completeExceptionally(dispatchFailure);
        }
        resultFuture.whenComplete((ignored, throwable) -> manager.removePendingJsResult(cefBrowser, requestId, resultFuture));
        return resultFuture;
    }

    /** Direct query primitive for the single target-thread task created by the protected asynchronous API. */
    private void executeJavaScriptWithResultDirect(@NotNull WrappedRinkuBrowser wrappedBrowser, @NotNull CefBrowser cefBrowser, @NotNull String requestId, @NotNull String script, @NotNull RinkuVideoManager manager, @NotNull CompletableFuture<String> resultFuture) {
        if (this.disposed || !this.initialized || this.browser != wrappedBrowser || wrappedBrowser.isClosed()) {
            manager.removePendingJsResult(cefBrowser, requestId, resultFuture);
            resultFuture.completeExceptionally(new IllegalStateException("Rinku player was no longer ready when its browser query reached the target thread"));
            return;
        }
        try {
            cefBrowser.executeJavaScript(script, wrappedBrowser.getUrl(), 0);
        } catch (Throwable throwable) {
            manager.removePendingJsResult(cefBrowser, requestId, resultFuture);
            resultFuture.completeExceptionally(throwable);
        }
    }

    /**
     * Executes JavaScript and waits no longer than the configured browser-query deadline.
     * Prefer {@link #executeJavaScriptWithResultAsync(String)} in non-legacy integrations.
     */
    @Nullable
    protected String executeJavaScriptWithResult(String jsCodeToEvaluate) {
        CompletableFuture<String> resultFuture = this.executeJavaScriptWithResultAsync(jsCodeToEvaluate);
        long timeoutNanos = RinkuIntegrationConfig.getJavaScriptQueryTimeout().toNanos();
        try {
            return resultFuture.get(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            resultFuture.cancel(true);
            LOGGER.warn("[KONKRETE] Player [{}]: Timed out after {} waiting for a JavaScript result", instanceId, RinkuIntegrationConfig.getJavaScriptQueryTimeout());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[KONKRETE] Player [{}]: Interrupted while waiting for a JavaScript result", instanceId, e);
            return null;
        } catch (ExecutionException e) {
            LOGGER.warn("[KONKRETE] Player [{}]: JavaScript result failed", instanceId, e.getCause());
            return null;
        }
    }

    /**
     * Permanently cancels queued work and releases the owned browser on Minecraft's client/render thread.
     */
    public void dispose() {
        if (this.disposed) return;
        this.disposed = true;
        // The managed scheduler is already stopped during client teardown, and closing the browser below terminates playback directly.
        if (!ClientShutdownHandler.isShuttingDown()) this.stop();
        this.stopPlaybackListenerTicker();
        this.resetVideoPlaybackListenerState();
        synchronized (this.pendingTaskLock) {
            this.pendingInitializationTasks.clear();
        }
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception e) {
                LOGGER.error("[KONKRETE] Player [{}]: Error closing RinkuVideoPlayer browser", instanceId, e);
            }
        }
        browser = null;
        initialized = false;
    }

    /** Starts one off-thread playback sampler; listener delivery uses the caller-configured executor. */
    protected void startPlaybackListenerTicker() {
        if (this.playbackListenerTicker != null && !this.playbackListenerTicker.isCancelled()) return;
        this.playbackListenerTicker = PLAYBACK_LISTENER_EXECUTOR.scheduleAtFixedRate(this::updatePlaybackListenerState, 0L, PLAYBACK_LISTENER_TICK_MS, TimeUnit.MILLISECONDS);
    }

    /** Cancels this player's off-thread playback sampler. */
    protected void stopPlaybackListenerTicker() {
        ScheduledFuture<?> ticker = this.playbackListenerTicker;
        if (ticker != null) {
            ticker.cancel(true);
        }
        this.playbackListenerTicker = null;
    }

    /** Starts at most one browser-scoped asynchronous snapshot and emits boundary transitions. */
    protected void updatePlaybackListenerState() {
        if (!this.hasVideoPlaybackListeners()) {
            this.resetVideoPlaybackListenerState();
            return;
        }
        if (!this.initialized) return;
        if (this.currentVideoPath == null || this.currentVideoPath.isBlank()) return;
        if (!this.listenerPlaybackCycleActive && !this.listenerFinishedEventEmittedForCycle) return;
        if (!this.playbackSamplePending.compareAndSet(false, true)) return;

        String snapshotScript = "(function(){try{var api=window.videoPlayerAPI;var duration=Number(api&&api.getDuration?api.getDuration():0);var currentTime=Number(api&&api.getCurrentTime?api.getCurrentTime():0);return {duration:Number.isFinite(duration)?duration:0,currentTime:Number.isFinite(currentTime)?currentTime:0,playing:!!(api&&api.isPlaying&&api.isPlaying())};}catch(error){return {duration:0,currentTime:0,playing:false};}})()";
        this.executeJavaScriptWithResultAsync(snapshotScript).whenComplete((result, throwable) -> {
            this.playbackSamplePending.set(false);
            if (throwable != null || result == null || this.disposed) return;
            try {
                JsonObject snapshot = JsonParser.parseString(result).getAsJsonObject();
                this.processPlaybackListenerSnapshot(snapshot.get("duration").getAsDouble(), snapshot.get("currentTime").getAsDouble(), snapshot.get("playing").getAsBoolean());
            } catch (Throwable parseFailure) {
                LOGGER.debug("[KONKRETE] Ignored an invalid Rinku playback-state response for player {}", this.instanceId, parseFailure);
            }
        });
    }

    private void processPlaybackListenerSnapshot(double sampledDuration, double sampledCurrentTime, boolean playing) {
        double duration = Math.max(0.0D, sampledDuration);
        double currentTime = Math.max(0.0D, sampledCurrentTime);
        double previousTime = Math.max(0.0D, this.listenerLastKnownPlaybackTimeSeconds);

        if (this.listenerPlaybackCycleActive && !this.listenerFinishedEventEmittedForCycle) {
            boolean nearEnd = duration > PLAYBACK_END_EPSILON_SECONDS && currentTime >= Math.max(0.0D, duration - PLAYBACK_END_EPSILON_SECONDS);
            if (!this.looping && nearEnd && !playing && !this.listenerPausedState) {
                if (this.maybeEmitVideoFinishedEvent(false)) {
                    this.maybeEmitVideoPlaybackStatusChanged(PlaybackStatus.FINISHED);
                }
            } else if (this.looping) {
                boolean wrappedAround = duration > PLAYBACK_END_EPSILON_SECONDS
                        && previousTime >= Math.max(0.0D, duration - PLAYBACK_END_EPSILON_SECONDS)
                        && currentTime <= LOOP_RESTART_WINDOW_SECONDS
                        && playing;
                if (wrappedAround) {
                    if (this.maybeEmitVideoFinishedEvent(true)) {
                        this.maybeEmitVideoPlaybackStatusChanged(PlaybackStatus.FINISHED);
                    }
                    if (this.maybeEmitVideoStartedEvent()) {
                        this.maybeEmitVideoPlaybackStatusChanged(PlaybackStatus.PLAYING);
                    }
                } else if (nearEnd && !playing && !this.listenerPausedState) {
                    if (this.maybeEmitVideoFinishedEvent(true)) {
                        this.maybeEmitVideoPlaybackStatusChanged(PlaybackStatus.FINISHED);
                    }
                }
            }
        } else if (this.looping && this.listenerFinishedEventEmittedForCycle) {
            boolean restarted = playing && (currentTime <= LOOP_RESTART_WINDOW_SECONDS || (previousTime > (currentTime + LOOP_RESTART_WINDOW_SECONDS)));
            if (restarted) {
                if (this.maybeEmitVideoStartedEvent()) {
                    this.maybeEmitVideoPlaybackStatusChanged(PlaybackStatus.PLAYING);
                }
            }
        }

        this.listenerLastKnownPlaybackTimeSeconds = currentTime;
    }

    /** Marks a new listener-visible playback cycle and reports whether it changed state. */
    protected boolean maybeEmitVideoStartedEvent() {
        if (!this.hasVideoPlaybackListeners()) return false;
        if (this.listenerPlaybackCycleActive) return false;
        if (this.currentVideoPath == null || this.currentVideoPath.isBlank()) return false;
        this.listenerPlaybackCycleActive = true;
        this.listenerFinishedEventEmittedForCycle = false;
        this.listenerLastKnownPlaybackTimeSeconds = 0.0D;
        return true;
    }

    /** Marks the current listener-visible cycle finished and reports whether it changed state. */
    protected boolean maybeEmitVideoFinishedEvent(boolean willRestart) {
        if (!this.hasVideoPlaybackListeners()) return false;
        if (!this.listenerPlaybackCycleActive || this.listenerFinishedEventEmittedForCycle) return false;
        this.listenerPlaybackCycleActive = false;
        this.listenerFinishedEventEmittedForCycle = true;
        return true;
    }

    /** Clears listener transition-tracking state. */
    protected void resetVideoPlaybackListenerState() {
        this.listenerPlaybackCycleActive = false;
        this.listenerFinishedEventEmittedForCycle = false;
        this.listenerLastKnownPlaybackTimeSeconds = 0.0D;
        this.listenerPausedState = false;
    }

    /** Returns whether this player has registered playback listeners. */
    protected boolean hasVideoPlaybackListeners() {
        return !this.playbackListeners.isEmpty();
    }

    /** Emits a playback transition to every listener, isolating listener failures. */
    protected void maybeEmitVideoPlaybackStatusChanged(@NotNull PlaybackStatus status) {
        if (this.playbackListeners.isEmpty()) return;
        PlaybackEvent event = new PlaybackEvent(this.resolveVideoSourceForListener(), this.resolveVideoSourceTypeForListener(), this.looping, status);
        List<PlaybackListener> listeners = List.copyOf(this.playbackListeners);
        try {
            RinkuIntegrationConfig.getPlaybackListenerExecutor().execute(() -> {
                for (PlaybackListener listener : listeners) {
                    try {
                        listener.onPlaybackChanged(event);
                    } catch (Throwable throwable) {
                        LOGGER.error("[KONKRETE] Rinku playback listener failed for player {}", this.instanceId, throwable);
                    }
                }
            });
        } catch (Throwable throwable) {
            LOGGER.error("[KONKRETE] Failed to schedule Rinku playback listeners for player {}", this.instanceId, throwable);
        }
    }

    /** Returns a normalized listener-facing source string. */
    @NotNull
    protected String resolveVideoSourceForListener() {
        String source = this.currentVideoPath;
        if (source == null || source.isBlank()) return "ERROR";
        if (source.startsWith("file:/")) {
            try {
                return new File(URI.create(source)).getAbsolutePath().replace("\\", "/");
            } catch (Exception ignored) {
            }
        }
        return source;
    }

    /** Returns a generic listener-facing source category. */
    @NotNull
    protected String resolveVideoSourceTypeForListener() {
        String source = this.currentVideoPath;
        if (source == null || source.isBlank()) return "UNKNOWN";
        String trimmed = source.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) return "WEB";
        if (lower.startsWith("file:/")) return "LOCAL";
        if (Identifier.tryParse(trimmed) != null) return "RESOURCE_LOCATION";
        return "LOCAL";
    }

    /** Browser-independent playback transitions exposed to caller listeners. */
    public enum PlaybackStatus {
        /** A playback cycle started or resumed. */
        PLAYING,
        /** The active playback cycle paused. */
        PAUSED,
        /** The active playback cycle stopped explicitly. */
        STOPPED,
        /** The active playback cycle reached its end. */
        FINISHED
    }

    /** Immutable event delivered to instance-scoped playback listeners. */
    public record PlaybackEvent(@NotNull String source, @NotNull String sourceType, boolean looping, @NotNull PlaybackStatus status) {}

    /** Receives state changes from one Rinku video player. */
    @FunctionalInterface
    public interface PlaybackListener {
        /** Receives one playback transition. */
        void onPlaybackChanged(@NotNull PlaybackEvent event);
    }

}
