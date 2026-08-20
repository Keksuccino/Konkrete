package de.keksuccino.konkrete.util.rendering.video.rinku;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.util.rinku.ActionBridge;
import de.keksuccino.konkrete.util.rinku.BrowserHandler;
import de.keksuccino.konkrete.util.rinku.RinkuIntegrationConfig;
import de.keksuccino.konkrete.util.rinku.RinkuUtil;
import de.keksuccino.rinku.Rinku;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Owns Rinku video players, the extracted generic player asset, and browser-scoped JavaScript RPC state. Callers must
 * presence-gate access to this optional-integration class. Player creation is asynchronous and uses the client thread;
 * disposal is terminal for this process.
 */
public final class RinkuVideoManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final RinkuVideoManager INSTANCE = new RinkuVideoManager();
    static final String JS_RESULT_REQUEST_TYPE = "konkrete:video_result";
    private static final ActionBridge.BrowserRequestHandler JS_RESULT_HANDLER = RinkuVideoManager::handleJavaScriptResult;
    private final Map<String, RinkuVideoPlayer> players = new ConcurrentHashMap<>();
    private final Map<BrowserRequestKey, CompletableFuture<String>> pendingJsResults = new ConcurrentHashMap<>();
    private final Object playerLifecycleLock = new Object();
    private final Object initializationLock = new Object();
    private volatile boolean webResourcesRegistered;
    private volatile boolean resultHandlerRegistered;
    private volatile boolean initializing;
    private volatile boolean initialized;
    private volatile boolean shuttingDown;

    private RinkuVideoManager() {}

    /** Returns the process-wide manager; callers must first confirm Rinku availability. */
    @NotNull
    public static RinkuVideoManager getInstance() {
        return INSTANCE;
    }

    /**
     * Installs initialization callbacks without polling. BrowserHandler is registered first so its router and load
     * handler exist before Rinku creates pooled browsers.
     */
    public void initialize() {
        if (!RinkuUtil.isRinkuLoaded()) return;
        synchronized (this.initializationLock) {
            if (this.shuttingDown || this.initialized || this.initializing) return;
            this.initializing = true;
        }
        BrowserHandler.init();
        if (Rinku.isInitialized()) {
            this.scheduleInitializationCompletion(true);
            return;
        }
        Rinku.scheduleForInit(this::scheduleInitializationCompletion);
    }

    /** Returns whether caller overrides and loader detection currently permit optional Rinku playback. */
    public boolean isVideoPlaybackAvailable() {
        return RinkuUtil.isRinkuLoaded();
    }

    /** Creates a client-thread-owned player at default bounds, or null when unavailable or shutting down. */
    @Nullable
    public String createPlayer() {
        return this.createPlayer(0, 0, 200, 200);
    }

    /** Creates and tracks a client-thread-owned player, returning its identifier before its browser is ready. */
    @Nullable
    public String createPlayer(int x, int y, int width, int height) {
        if (this.shuttingDown || !this.isVideoPlaybackAvailable()) return null;
        this.initialize();
        if (!this.ensureWebResources()) return null;
        try {
            String playerId = UUID.randomUUID().toString();
            RinkuVideoPlayer player = new RinkuVideoPlayer(x, y, width, height);
            synchronized (this.playerLifecycleLock) {
                if (this.shuttingDown) {
                    player.dispose();
                    return null;
                }
                this.players.put(playerId, player);
            }
            return playerId;
        } catch (Throwable throwable) {
            LOGGER.error("[KONKRETE] Failed to create a Rinku video player", throwable);
            return null;
        }
    }

    /** Returns a tracked player by identifier. */
    @Nullable
    public RinkuVideoPlayer getPlayer(@NotNull String playerId) {
        return this.shuttingDown ? null : this.players.get(playerId);
    }

    /** Removes and terminally disposes one tracked player on the client/render thread. */
    public void removePlayer(@NotNull String playerId) {
        RinkuVideoPlayer player;
        synchronized (this.playerLifecycleLock) {
            player = this.players.remove(playerId);
        }
        if (player != null) player.dispose();
    }

    /** Permanently disposes every player and pending browser RPC on the client thread for this process. */
    public void disposeAll() {
        this.shuttingDown = true;
        this.initialized = false;
        this.initializing = false;
        if (this.resultHandlerRegistered) {
            ActionBridge.unregisterRequestHandler(JS_RESULT_REQUEST_TYPE, JS_RESULT_HANDLER);
            this.resultHandlerRegistered = false;
        }
        List<RinkuVideoPlayer> snapshot;
        synchronized (this.playerLifecycleLock) {
            snapshot = new ArrayList<>(this.players.values());
            this.players.clear();
        }
        for (RinkuVideoPlayer player : snapshot) {
            try {
                player.dispose();
            } catch (Throwable throwable) {
                LOGGER.error("[KONKRETE] Failed to dispose a Rinku video player", throwable);
            }
        }
        CancellationException cancellation = new CancellationException("Konkrete Rinku video manager was disposed");
        this.pendingJsResults.values().forEach(future -> future.completeExceptionally(cancellation));
        this.pendingJsResults.clear();
    }

    @Nullable
    CompletableFuture<String> registerPendingJsResult(@NotNull CefBrowser browser, @NotNull String requestId) {
        if (this.shuttingDown) return null;
        CompletableFuture<String> future = new CompletableFuture<>();
        BrowserRequestKey key = new BrowserRequestKey(browser, requestId);
        CompletableFuture<String> previous = this.pendingJsResults.putIfAbsent(key, future);
        if (previous != null) return null;
        future.orTimeout(RinkuIntegrationConfig.getJavaScriptQueryTimeout().toNanos(), TimeUnit.NANOSECONDS).whenComplete((ignored, throwable) -> this.pendingJsResults.remove(key, future));
        return future;
    }

    void removePendingJsResult(@NotNull CefBrowser browser, @NotNull String requestId, @NotNull CompletableFuture<String> future) {
        this.pendingJsResults.remove(new BrowserRequestKey(browser, requestId), future);
    }

    @NotNull
    Path getPlayerFile() {
        return RinkuIntegrationConfig.getDataDirectory().resolve("web").resolve("videoplayer").resolve("player.html");
    }

    private void scheduleInitializationCompletion(boolean successful) {
        if (!successful) {
            synchronized (this.initializationLock) {
                this.initializing = false;
            }
            return;
        }
        // Rinku invokes this callback before creating its preload pool. Register the browser-scoped result handler in
        // that same phase so the first pooled browser can never observe a partially initialized bridge.
        this.completeInitialization();
    }

    private void completeInitialization() {
        if (this.shuttingDown) return;
        try {
            if (!ActionBridge.initializeIfNecessary()) throw new IllegalStateException("Rinku JavaScript bridge is unavailable");
            if (!this.resultHandlerRegistered) {
                ActionBridge.registerRequestHandler(JS_RESULT_REQUEST_TYPE, JS_RESULT_HANDLER);
                this.resultHandlerRegistered = true;
            }
            if (!this.ensureWebResources()) throw new IllegalStateException("Rinku player asset could not be extracted");
            synchronized (this.initializationLock) {
                this.initialized = true;
                this.initializing = false;
            }
            LOGGER.info("[KONKRETE] Rinku video manager initialized");
        } catch (Throwable throwable) {
            synchronized (this.initializationLock) {
                this.initializing = false;
            }
            LOGGER.error("[KONKRETE] Failed to initialize the Rinku video manager", throwable);
        }
    }

    private synchronized boolean ensureWebResources() {
        if (this.webResourcesRegistered && Files.isRegularFile(this.getPlayerFile())) return true;
        Path playerFile = this.getPlayerFile();
        try {
            Files.createDirectories(playerFile.getParent());
            try (InputStream stream = Konkrete.class.getResourceAsStream("/assets/konkrete/web/videoplayer/player.html")) {
                if (stream == null) throw new IllegalStateException("Missing /assets/konkrete/web/videoplayer/player.html");
                Files.copy(stream, playerFile, StandardCopyOption.REPLACE_EXISTING);
            }
            this.webResourcesRegistered = true;
            return true;
        } catch (Throwable throwable) {
            this.webResourcesRegistered = false;
            LOGGER.error("[KONKRETE] Failed to extract the Rinku player asset to {}", playerFile, throwable);
            return false;
        }
    }

    private static boolean handleJavaScriptResult(CefBrowser browser, org.cef.browser.CefFrame frame, JsonObject payload, org.cef.callback.CefQueryCallback callback) {
        String requestId = getString(payload, "requestId");
        if (requestId == null || requestId.isBlank()) {
            callback.failure(400, "Missing video result requestId");
            return true;
        }
        RinkuVideoManager manager = getInstance();
        CompletableFuture<String> future = manager.pendingJsResults.remove(new BrowserRequestKey(browser, requestId));
        if (future == null) {
            callback.failure(404, "Unknown or expired video result request");
            return true;
        }
        String error = getString(payload, "error");
        if (error != null) future.completeExceptionally(new IllegalStateException(error));
        else future.complete(getString(payload, "result"));
        callback.success("accepted");
        return true;
    }

    @Nullable
    private static String getString(JsonObject payload, String key) {
        JsonElement element = payload.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static final class BrowserRequestKey {

        private final CefBrowser browser;
        private final String requestId;

        private BrowserRequestKey(CefBrowser browser, String requestId) {
            this.browser = Objects.requireNonNull(browser, "browser");
            this.requestId = Objects.requireNonNull(requestId, "requestId");
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object object) {
            return object instanceof BrowserRequestKey other && this.browser == other.browser && this.requestId.equals(other.requestId);
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(this.browser) + this.requestId.hashCode();
        }

    }

}
