package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.rinku.Rinku;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Multiplexes Rinku's single global load handler across owned browsers. Listener callbacks run on
 * CEF's load-handler thread and are removed after one terminal main-frame event unless registered as persistent.
 */
public final class BrowserLoadEventListenerManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final BrowserLoadEventListenerManager INSTANCE = new BrowserLoadEventListenerManager();

    // Maps browser IDs to their initialization futures
    private final Map<String, List<BrowserLoadListener>> browserMap = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    // The single load handler that will be registered with CefClient
    private final CefLoadHandlerAdapter globalHandler = new CefLoadHandlerAdapter() {

        /** {@inheritDoc} */
        @Override
        public void onLoadStart(CefBrowser cefBrowser, CefFrame frame, CefRequest.TransitionType transitionType) {
            if (!frame.isMain()) return;

            String browserId = getIdByCefBrowser(cefBrowser);
            if (browserId == null) return;

            List<BrowserLoadListener> loadListeners = browserMap.get(browserId);
            if (loadListeners == null) return;
            synchronized (loadListeners) {
                if (loadListeners.isEmpty()) return;
                loadListeners.get(0).getBrowser().onMainFrameLoadStartedForTracking(frame.getURL());
            }
        }

        /** {@inheritDoc} */
        @Override
        public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
            if (!frame.isMain()) return; // Only care about main frame loads

            String browserId = getIdByCefBrowser(cefBrowser);
            if (browserId == null) return;

            List<BrowserLoadListener> loadListeners = browserMap.get(browserId);
            if (loadListeners != null) {
                synchronized (loadListeners) {
                    if (loadListeners.isEmpty()) return;
                    if (isStalePreloadedPage(frame.getURL(), loadListeners.get(0).getBrowser().getExpectedMainFrameUrlForTracking())) return;
                    boolean success = isSuccessfulLoad(frame.getURL(), httpStatusCode);
                    processListeners(loadListeners, success);
                }
            } else {
                LOGGER.warn("[KONKRETE] onLoadEnd: No load listeners found for browser ID: {}", browserId);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void onLoadError(CefBrowser cefBrowser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
            if (!frame.isMain()) return;

            String browserId = getIdByCefBrowser(cefBrowser);
            if (browserId == null) return;

            List<BrowserLoadListener> loadListeners = browserMap.get(browserId);
            if (loadListeners != null) {
                synchronized (loadListeners) {
                    if (loadListeners.isEmpty()) return;
                    if (isStalePreloadedPage(failedUrl, loadListeners.get(0).getBrowser().getExpectedMainFrameUrlForTracking())) return;
                    LOGGER.error("[KONKRETE] Browser [ID:{}] load error: {}, {}, URL: {}", browserId, errorCode, errorText, failedUrl);
                    processListeners(loadListeners, false);
                }
            } else {
                LOGGER.warn("[KONKRETE] onLoadError: No load listeners found for browser ID: {}", browserId);
            }
        }
    };

    private BrowserLoadEventListenerManager() {
        // Private constructor for singleton
    }

    /** Returns the process-wide load-listener multiplexer. */
    public static BrowserLoadEventListenerManager getInstance() {
        return INSTANCE;
    }

    /** Returns the single handler installed during Rinku's pre-browser initialization phase. */
    public CefLoadHandler getGlobalHandler() {
        return globalHandler;
    }

    /**
     * Registers the single global load handler after Rinku is ready and before it creates any browsers. Rinku fans this handler out itself,
     * so adding the same instance once per Konkrete browser would process every load event multiple times.
     */
    public synchronized void initialize() {
        if (this.initialized) return;
        if (!RinkuUtil.isRinkuLoaded() || !Rinku.isInitialized()) throw new IllegalStateException("Rinku must be ready before its global load handler is registered");
        Rinku.getClient().addLoadHandler(this.globalHandler);
        this.initialized = true;
    }

    /** Registers a one-shot terminal main-frame listener that runs on CEF's load-handler thread. */
    public void registerListenerForBrowser(@NotNull WrappedRinkuBrowser browser, @NotNull Consumer<Boolean> onLoadListener) {
        registerListenerForBrowserInternal(browser, onLoadListener, false);
    }

    /** Registers a persistent terminal main-frame listener that runs on CEF's load-handler thread per navigation. */
    public void registerPersistentListenerForBrowser(@NotNull WrappedRinkuBrowser browser, @NotNull Consumer<Boolean> onLoadListener) {
        registerListenerForBrowserInternal(browser, onLoadListener, true);
    }

    /** Removes every listener for a closing browser; safe from inside its own callback. */
    public void unregisterAllListenersForBrowser(String browserId) {
        List<BrowserLoadListener> loadListeners = browserMap.remove(browserId);
        if (loadListeners == null) return;
        synchronized (loadListeners) {
            loadListeners.clear();
        }
    }

    /** Returns the wrapper identifier whose native browser is exactly the supplied instance. */
    @Nullable
    public String getIdByCefBrowser(@NotNull CefBrowser cefBrowser) {
        for (Map.Entry<String, List<BrowserLoadListener>> m : this.browserMap.entrySet()) {
            List<BrowserLoadListener> loadListeners = m.getValue();
            synchronized (loadListeners) {
                if (loadListeners.isEmpty()) continue;
                BrowserLoadListener listener1 = loadListeners.get(0);
                if (listener1.getBrowser().getBrowser() == cefBrowser) return m.getKey();
            }
        }
        return null;
    }

    private static class BrowserLoadListener {

        private final Consumer<Boolean> onLoadCompleted;
        private final WrappedRinkuBrowser browser;
        private final boolean persistent;
        private volatile boolean handled = false;

        private BrowserLoadListener(WrappedRinkuBrowser browser, Consumer<Boolean> onLoadCompleted, boolean persistent) {
            this.onLoadCompleted = onLoadCompleted;
            this.browser = browser;
            this.persistent = persistent;
        }

        private Consumer<Boolean> getOnLoadCompletedTask() {
            return this.onLoadCompleted;
        }

        private WrappedRinkuBrowser getBrowser() {
            return this.browser;
        }

        private boolean isHandled() {
            return this.handled;
        }

        private void setHandled(boolean handled) {
            this.handled = handled;
        }

        private boolean isPersistent() {
            return this.persistent;
        }

    }

    private void registerListenerForBrowserInternal(@NotNull WrappedRinkuBrowser browser, @NotNull Consumer<Boolean> onLoadListener, boolean persistent) {
        if (browser.isClosed()) return;
        List<BrowserLoadListener> listeners = browserMap.computeIfAbsent(browser.getIdentifier(), id -> new ArrayList<>());
        synchronized (listeners) {
            if (browser.isClosed()) {
                browserMap.remove(browser.getIdentifier(), listeners);
                return;
            }
            listeners.add(new BrowserLoadListener(browser, onLoadListener, persistent));
        }
    }

    private void processListeners(@NotNull List<BrowserLoadListener> loadListeners, boolean success) {
        // Listener callbacks may close their own browser and clear this list reentrantly, so iterate over a snapshot.
        for (BrowserLoadListener loadListener : new ArrayList<>(loadListeners)) {
            if (loadListener.getBrowser().isClosed() || processListener(loadListener, success)) loadListeners.remove(loadListener);
        }
    }

    private boolean processListener(@NotNull BrowserLoadListener loadListener, boolean success) {
        if (!loadListener.isHandled()) {
            loadListener.setHandled(true);
            loadListener.getOnLoadCompletedTask().accept(success);
        }
        if (loadListener.isPersistent()) {
            loadListener.setHandled(false);
            return false;
        }
        return true;
    }

    static boolean isSuccessfulLoad(@Nullable String url, int httpStatusCode) {
        if ((httpStatusCode >= 200) && (httpStatusCode < 300)) return true;
        if (httpStatusCode != 0 || url == null) return false;
        return url.startsWith("file:") || url.equalsIgnoreCase("about:blank");
    }

    static boolean isStalePreloadedPage(@Nullable String eventUrl, @Nullable String expectedMainFrameUrl) {
        return (eventUrl != null) && eventUrl.equalsIgnoreCase("about:blank") && (expectedMainFrameUrl != null) && !expectedMainFrameUrl.equalsIgnoreCase("about:blank");
    }

}
