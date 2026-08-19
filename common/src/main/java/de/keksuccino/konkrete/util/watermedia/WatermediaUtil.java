package de.keksuccino.konkrete.util.watermedia;

import de.keksuccino.konkrete.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Thread-safe capability checks and caller-controlled terminal state for the optional Watermedia bridge. */
public final class WatermediaUtil {

    private static final Logger LOGGER = LogManager.getLogger();
    /** Set by an integration owner after a terminal failure to suppress all later optional-API access. */
    public static volatile boolean WATERMEDIA_CRITICAL_FAILURE = false;
    /** Set once any Watermedia-backed resource has initialized successfully in this client process. */
    public static volatile boolean WATERMEDIA_INITIALIZED = false;
    private static volatile boolean developmentFfmpegLogLevelSuppressed;

    private WatermediaUtil() {}

    /** Returns whether the optional Watermedia API is present and enabled. */
    public static boolean isWatermediaLoaded() {
        if (WATERMEDIA_CRITICAL_FAILURE || !WatermediaIntegrationConfig.permitsWatermedia()) return false;
        return canLoad("org.watermedia.api.media.MRL");
    }

    /** Returns whether Watermedia's optional FFmpeg binaries are present and enabled. */
    public static boolean isWatermediaBinariesLoaded() {
        if (WATERMEDIA_CRITICAL_FAILURE || !WatermediaIntegrationConfig.permitsWatermedia() || !WatermediaIntegrationConfig.permitsBinaries()) return false;
        return canLoad("org.bytedeco.ffmpeg.global.avutil");
    }

    /** Returns the current explicit optional-media availability state. */
    @NotNull
    public static Availability getAvailability() {
        boolean loaded = isWatermediaLoaded();
        if (!loaded) return Availability.UNAVAILABLE;
        return isWatermediaBinariesLoaded() ? Availability.VIDEO_PLAYBACK : Availability.RENDERING_ONLY;
    }

    /** Returns whether Watermedia and its native binaries can play video. */
    public static boolean isWatermediaVideoPlaybackAvailable() {
        return getAvailability() == Availability.VIDEO_PLAYBACK;
    }

    /** Returns whether the Watermedia API can render image-backed media. */
    public static boolean isWatermediaRenderingAvailable() {
        return getAvailability() != Availability.UNAVAILABLE;
    }

    static boolean isWatermediaVideoPlaybackAvailable(boolean watermediaLoaded, boolean binariesLoaded) {
        return isWatermediaRenderingAvailable(watermediaLoaded) && binariesLoaded;
    }

    static boolean isWatermediaRenderingAvailable(boolean watermediaLoaded) {
        return watermediaLoaded;
    }

    /** Reduces native FFmpeg verbosity in a development environment without changing production behavior. */
    public static void trySuppressDevelopmentFfmpegDebugLogs() {
        if (developmentFfmpegLogLevelSuppressed || !Services.PLATFORM.isDevelopmentEnvironment() || !isWatermediaLoaded()) return;
        try {
            Class<?> avutilClass = Class.forName("org.bytedeco.ffmpeg.global.avutil", false, WatermediaIntegrationConfig.getClassLoader());
            Field printLevel = avutilClass.getField("AV_LOG_PRINT_LEVEL");
            Field skipRepeated = avutilClass.getField("AV_LOG_SKIP_REPEATED");
            Field info = avutilClass.getField("AV_LOG_INFO");
            Method setFlags = avutilClass.getMethod("av_log_set_flags", int.class);
            Method setLevel = avutilClass.getMethod("av_log_set_level", int.class);
            setFlags.invoke(null, printLevel.getInt(null) | skipRepeated.getInt(null));
            setLevel.invoke(null, info.getInt(null));
            developmentFfmpegLogLevelSuppressed = true;
            LOGGER.info("[KONKRETE] Reduced Watermedia FFmpeg logging for the development environment");
        } catch (Throwable ignored) {}
    }

    private static boolean canLoad(String className) {
        try {
            Class.forName(className, false, WatermediaIntegrationConfig.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Capability level detected for the optional Watermedia installation. */
    public enum Availability {
        /** The API is absent, disabled, or terminally failed. */
        UNAVAILABLE,
        /** The API can decode images but native video binaries are unavailable. */
        RENDERING_ONLY,
        /** The API and native video binaries are available. */
        VIDEO_PLAYBACK
    }
}
