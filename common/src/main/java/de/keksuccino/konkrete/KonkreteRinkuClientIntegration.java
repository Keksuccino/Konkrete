package de.keksuccino.konkrete;

import de.keksuccino.konkrete.util.rendering.video.rinku.RinkuVideoManager;
import de.keksuccino.konkrete.util.rinku.BrowserHandler;
import de.keksuccino.konkrete.util.rinku.RinkuIntegrationConfig;
import de.keksuccino.melody.resources.audio.MinecraftSoundSettingsObserver;

/**
 * Isolated bridge for optional Rinku classes. Callers must verify that the {@code rinku} mod is loaded before resolving this class.
 */
final class KonkreteRinkuClientIntegration {

    private static boolean initialized;

    private KonkreteRinkuClientIntegration() {}

    static synchronized void init() {
        if (initialized) return;
        RinkuIntegrationConfig.setVolumeListenerRegistrar(listener -> {
            long listenerId = MinecraftSoundSettingsObserver.registerVolumeListener((source, volume) -> listener.onVolumeChanged(source, volume));
            return () -> MinecraftSoundSettingsObserver.unregisterVolumeListener(listenerId);
        });
        BrowserHandler.init();
        RinkuVideoManager.getInstance().initialize();
        initialized = true;
    }

}
