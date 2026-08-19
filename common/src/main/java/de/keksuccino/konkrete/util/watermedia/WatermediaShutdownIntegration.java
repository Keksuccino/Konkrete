package de.keksuccino.konkrete.util.watermedia;

import de.keksuccino.konkrete.util.lifecycle.ClientShutdownHandler;

import java.util.concurrent.atomic.AtomicBoolean;

/** Installs ordered Watermedia cleanup without exposing optional media types to generic lifecycle defaults. */
public final class WatermediaShutdownIntegration {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private WatermediaShutdownIntegration() {}

    /** Registers deferred-player cleanup exactly once; absent Watermedia is a safe no-op and does not load its API. */
    public static void register() {
        if (!WatermediaUtil.isWatermediaLoaded() || !REGISTERED.compareAndSet(false, true)) return;
        ClientShutdownHandler.registerCleanup("deferred Watermedia players", ClientShutdownHandler.ORDER_MEDIA, WatermediaDeferredPlayerReleaseTracker::shutdown);
    }
}
