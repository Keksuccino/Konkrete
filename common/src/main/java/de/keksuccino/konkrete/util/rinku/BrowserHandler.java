package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.konkrete.util.Pair;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns auto-managed wrapped browsers and coordinates their client-thread ticking, input, and terminal shutdown.
 * Initialization is presence-gated and must be scheduled before Rinku creates its first pooled browser.
 */
public final class BrowserHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashMap<String, Pair<WrappedRinkuBrowser, Long>> BROWSERS = new HashMap<>();

    @Nullable private static volatile RinkuIntegrationConfig.VolumeListenerRegistration volumeListenerRegistration;
    private static volatile boolean shuttingDown = false;
    private static volatile boolean initializing = false;
    /** Indicates whether Konkrete's Rinku handlers completed initialization. */
    public static volatile boolean initialized = false;

    private BrowserHandler() {}

    /** Schedules handler installation in Rinku's pre-browser initialization phase; safe to call repeatedly. */
    public static void init() {
        if (!RinkuUtil.isRinkuLoaded()) return;
        synchronized (BrowserHandler.class) {
            if (shuttingDown || initialized || initializing) return;
            initializing = true;
        }

        LOGGER.info("[KONKRETE] Starting Rinku browser-handler initialization");

        if (Rinku.isInitialized()) {
            completeInitialization(true);
            return;
        }

        LOGGER.info("[KONKRETE] Waiting for Rinku's initialization phase");

        // Rinku invokes these callbacks after creating its client but before creating its preloaded browsers. The
        // message router must exist before those browser contexts or the first pooled browser never receives cefQuery.
        Rinku.scheduleForInit(BrowserHandler::completeInitialization);

        // Rinku and both loader startup paths initialize on the client thread. Keep an idempotent recheck as a defensive
        // fallback for integrations that report Rinku ready immediately after listener registration.
        if (Rinku.isInitialized()) completeInitialization(true);

    }

    private static synchronized void completeInitialization(boolean successful) {
        if (shuttingDown || initialized || !initializing) return;
        if (!successful) {
            RinkuUtil.RINKU_CRITICAL_FAILURE = true;
            RinkuUtil.RINKU_INITIALIZED = false;
            initializing = false;
            LOGGER.error("[KONKRETE] Rinku initialization failed before browser handlers were installed");
            return;
        }

        RinkuShutdownIntegration.register();
        try {
            // These native client integrations must be installed synchronously in Rinku's init callback. Rinku creates
            // its browser preload pool immediately after the callback returns.
            if (!ActionBridge.initializeIfNecessary()) throw new IllegalStateException("Failed to initialize the Rinku action bridge");
            BrowserLoadEventListenerManager.getInstance().initialize();

            volumeListenerRegistration = RinkuIntegrationConfig.registerVolumeListener(BrowserHandler::onVolumeUpdated);
            RinkuUtil.RINKU_INITIALIZED = true;
            initialized = true;
            initializing = false;
            LOGGER.info("[KONKRETE] Rinku browser handlers initialized");
        } catch (Throwable ex) {
            RinkuUtil.RINKU_CRITICAL_FAILURE = true;
            RinkuUtil.RINKU_INITIALIZED = false;
            initializing = false;
            LOGGER.error("[KONKRETE] Failed to initialize Rinku browser handlers", ex);
        }
    }

    /** Refreshes or registers an auto-managed browser under a caller identifier from the client/render thread. */
    public static void notifyHandler(@NotNull String identifier, @NotNull WrappedRinkuBrowser browser) {
        long now = System.currentTimeMillis();
        WrappedRinkuBrowser staleBrowser = null;
        synchronized (BROWSERS) {
            if (shuttingDown) {
                staleBrowser = browser;
            } else {
                Pair<WrappedRinkuBrowser, Long> cached = BROWSERS.get(identifier);
                if ((cached == null) || (cached.getFirst() != browser)) {
                    if ((cached != null) && (cached.getFirst() != null) && (cached.getFirst() != browser) && !cached.getFirst().isClosed()) {
                        staleBrowser = cached.getFirst();
                    }
                    BROWSERS.put(identifier, Pair.of(browser, now));
                } else {
                    cached.setSecond(now);
                }
            }
        }
        closeBrowserQuietly(staleBrowser, "stale");
    }

    /** Returns the auto-managed browser registered under an identifier, if any. */
    @Nullable
    public static WrappedRinkuBrowser get(@NotNull String identifier) {
        if (shuttingDown) return null;
        synchronized (BROWSERS) {
            Pair<WrappedRinkuBrowser, Long> browser = BROWSERS.get(identifier);
            return (browser != null) ? browser.getFirst() : null;
        }
    }

    /** Removes an auto-managed browser and optionally closes it on the client/render thread. */
    public static void remove(@NotNull String identifier, boolean close) {
        Pair<WrappedRinkuBrowser, Long> browser;
        synchronized (BROWSERS) {
            browser = BROWSERS.remove(identifier);
        }
        if (close && (browser != null)) closeBrowserQuietly(browser.getFirst(), "removed");
    }

    /** Expires auto-managed browsers that have not rendered for five seconds; call once per client tick. */
    public static void tick() {
        if (shuttingDown) return;
        long now = System.currentTimeMillis();
        List<WrappedRinkuBrowser> garbageCollect = new ArrayList<>();
        synchronized (BROWSERS) {
            List<String> staleIdentifiers = new ArrayList<>();
            for (Map.Entry<String, Pair<WrappedRinkuBrowser, Long>> entry : BROWSERS.entrySet()) {
                //Close browser after 5 seconds of inactivity
                if ((entry.getValue().getSecond() + 5000) < now) {
                    staleIdentifiers.add(entry.getKey());
                    garbageCollect.add(entry.getValue().getFirst());
                }
            }
            staleIdentifiers.forEach(BROWSERS::remove);
        }
        garbageCollect.forEach(browser -> closeBrowserQuietly(browser, "inactive"));
    }

    /** Forwards a global pointer move to all auto-managed browsers on the client thread. */
    public static void mouseMoved(double mouseX, double mouseY) {
        if (shuttingDown) return;
        getBrowserSnapshot().forEach(browser -> browser.mouseMoved(mouseX, mouseY));
    }

    /** Reapplies browser media volume after a client-thread sound-category update. */
    public static void onVolumeUpdated(SoundSource soundSource, float newVolume) {
        if (shuttingDown) return;
        getBrowserSnapshot().forEach(browser -> browser.onVolumeUpdated(soundSource, newVolume));
    }

    /** Permanently closes all managed browsers on the client thread and unregisters optional adapters. */
    public static void closeAll() {
        RinkuIntegrationConfig.VolumeListenerRegistration registration;
        synchronized (BrowserHandler.class) {
            shuttingDown = true;
            initialized = false;
            initializing = false;
            registration = volumeListenerRegistration;
            volumeListenerRegistration = null;
        }

        List<WrappedRinkuBrowser> browsers;
        synchronized (BROWSERS) {
            browsers = new ArrayList<>(BROWSERS.size());
            BROWSERS.values().forEach(browser -> browsers.add(browser.getFirst()));
            BROWSERS.clear();
        }
        browsers.forEach(browser -> closeBrowserQuietly(browser, "client shutdown"));

        if (registration != null) {
            try {
                registration.close();
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to unregister the Rinku browser volume listener during client shutdown", ex);
            }
        }
    }

    @NotNull
    private static List<WrappedRinkuBrowser> getBrowserSnapshot() {
        synchronized (BROWSERS) {
            List<WrappedRinkuBrowser> browsers = new ArrayList<>(BROWSERS.size());
            BROWSERS.values().forEach(browser -> browsers.add(browser.getFirst()));
            return browsers;
        }
    }

    private static void closeBrowserQuietly(@Nullable WrappedRinkuBrowser browser, @NotNull String reason) {
        if ((browser == null) || browser.isClosed()) return;
        try {
            browser.close();
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to close {} Rinku browser", reason, ex);
        }
    }

}
