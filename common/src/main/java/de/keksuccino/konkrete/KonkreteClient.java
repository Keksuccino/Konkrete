package de.keksuccino.konkrete;

import de.keksuccino.konkrete.placeholder.placeholders.BuiltinPlaceholders;
import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.file.type.types.FileTypes;
import de.keksuccino.konkrete.util.lifecycle.ClientShutdownHandler;
import de.keksuccino.konkrete.util.rendering.text.color.colors.TextColorFormatters;
import de.keksuccino.konkrete.util.rendering.text.smooth.SmoothFontManager;
import de.keksuccino.konkrete.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.konkrete.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.konkrete.util.rendering.ui.theme.themes.UIThemes;
import de.keksuccino.konkrete.util.resource.resources.audio.AudioEngineReloadHandler;
import de.keksuccino.konkrete.util.rinku.RinkuUtil;
import de.keksuccino.konkrete.util.watermedia.WatermediaIntegrationConfig;
import de.keksuccino.konkrete.util.window.WindowHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Coordinates Konkrete's client-only registries and lifecycle without exposing client classes to dedicated-server entrypoints.
 */
public final class KonkreteClient {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final InitializationPhase INITIALIZATION = new InitializationPhase("client initialization");
    private static final InitializationPhase LATE_INITIALIZATION = new InitializationPhase("late client initialization");

    private KonkreteClient() {}

    /** Initializes reusable client registries, reload observers, and optional-media availability once. */
    public static void init() {
        INITIALIZATION.runOnce(KonkreteClient::initializeClientSystems);
    }

    /** Completes window-dependent client setup once Minecraft has finished construction. */
    public static void onGameInitCompleted() {
        init();
        LATE_INITIALIZATION.runOnce(KonkreteClient::initializeWindowDependentSystems);
    }

    /** Advances client-thread services that require one update near the start of every client tick. */
    public static void onPreClientTick() {
        ScreenOverlayHandler.INSTANCE.tick();
        CursorHandler.tick();
    }

    /** Releases client-owned services while Minecraft's render, audio, and window infrastructure is still alive. */
    public static void shutdown() {
        ClientShutdownHandler.shutdown();
    }

    private static void initializeClientSystems() {
        LOGGER.info("[KONKRETE] Initializing reusable client systems..");
        FileTypes.registerAll();
        BuiltinPlaceholders.registerAll();
        TextColorFormatters.registerAll();
        SmoothFontManager.registerReloadListener();
        MaterialIcons.registerReloadListener();
        AudioEngineReloadHandler.register();
        configureWatermediaAvailability();
        initializeOptionalRinkuIntegration();
    }

    private static void initializeWindowDependentSystems() {
        LOGGER.info("[KONKRETE] Starting late client initialization phase..");
        UIThemes.registerAll();
        WindowHandler.updateCustomWindowIcon();
        WindowHandler.handleForceFullscreen();
        CursorHandler.init();
    }

    private static void configureWatermediaAvailability() {
        WatermediaIntegrationConfig.setAvailabilityOverride(() -> Services.PLATFORM.isModLoaded("watermedia"));
        WatermediaIntegrationConfig.setBinariesAvailabilityOverride(() -> Services.PLATFORM.isModLoaded("watermedia_binaries"));
    }

    private static void initializeOptionalRinkuIntegration() {
        if (!RinkuUtil.isRinkuLoaded()) return;
        try {
            KonkreteRinkuClientIntegration.init();
        } catch (LinkageError | RuntimeException exception) {
            LOGGER.error("[KONKRETE] Failed to initialize optional Rinku integration", exception);
        }
    }

    /** Serializes one initialization phase and prevents reentrant or duplicate execution. */
    static final class InitializationPhase {

        private final String name;
        private State state = State.NEW;
        private Throwable failure;

        InitializationPhase(@NotNull String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        synchronized boolean runOnce(@NotNull Runnable action) {
            Objects.requireNonNull(action, "action");
            if (this.state == State.COMPLETE || this.state == State.RUNNING) return false;
            if (this.state == State.FAILED) throw new IllegalStateException("Konkrete " + this.name + " previously failed", this.failure);
            this.state = State.RUNNING;
            try {
                action.run();
                this.state = State.COMPLETE;
                return true;
            } catch (RuntimeException | Error throwable) {
                this.failure = throwable;
                this.state = State.FAILED;
                throw throwable;
            }
        }

        private enum State {

            NEW,
            RUNNING,
            COMPLETE,
            FAILED

        }

    }

}
