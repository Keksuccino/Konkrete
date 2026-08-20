package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.konkrete.util.lifecycle.ClientShutdownHandler;
import de.keksuccino.konkrete.util.rendering.video.rinku.RinkuVideoManager;
import de.keksuccino.rinku.Rinku;

import java.util.concurrent.atomic.AtomicBoolean;

/** Installs ordered Rinku cleanup without exposing optional Rinku types to generic lifecycle defaults. */
public final class RinkuShutdownIntegration {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private RinkuShutdownIntegration() {}

    /** Registers Rinku cleanup exactly once; it is a no-op until Rinku is both present and initialized. */
    public static void register() {
        if (!RinkuUtil.isRinkuLoaded() || !Rinku.isInitialized() || !REGISTERED.compareAndSet(false, true)) return;
        ClientShutdownHandler.registerCleanup("Rinku video players", ClientShutdownHandler.ORDER_MEDIA + 10, () -> RinkuVideoManager.getInstance().disposeAll());
        ClientShutdownHandler.registerCleanup("Rinku browsers", ClientShutdownHandler.ORDER_MEDIA + 20, BrowserHandler::closeAll);
        ClientShutdownHandler.registerCleanup("Rinku JavaScript bridge", ClientShutdownHandler.ORDER_FINAL + 10, ActionBridge::dispose);
    }

}
