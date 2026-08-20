package de.keksuccino.konkrete.util.resource;

import de.keksuccino.konkrete.Konkrete;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Configures the reusable resource runtime and publishes its lifecycle events to consuming mods.
 * Callers should finish configuration before constructing resources because decoder temp folders and identifiers are cached.
 */
public final class ResourceRuntime {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final CopyOnWriteArrayList<AnimatedTextureListener> ANIMATED_TEXTURE_LISTENERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<VideoPlaybackListener> VIDEO_PLAYBACK_LISTENERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> AUDIO_RELOAD_LISTENERS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean();
    private static final AtomicLong NEXT_RESOURCE_ID = new AtomicLong();
    private static volatile String namespace = Konkrete.MOD_ID;
    private static volatile Path temporaryDirectory = Path.of(System.getProperty("java.io.tmpdir"), "konkrete-resource-runtime");
    private static volatile UnaryOperator<String> sourceResolver = UnaryOperator.identity();
    private static volatile Supplier<String> preLoadConfigurationReader = () -> "";
    private static volatile Consumer<String> preLoadConfigurationWriter = ignored -> {};
    private static volatile ResourceSafetyLimits safetyLimits = ResourceSafetyLimits.DEFAULT;

    private ResourceRuntime() {}

    /** Returns the namespace used for generated runtime texture identifiers. */
    @NotNull
    public static String getNamespace() {
        return namespace;
    }

    /** Sets the caller-owned namespace used by resources created after this call. */
    public static void setNamespace(@NotNull String namespace) {
        String checkedNamespace = Objects.requireNonNull(namespace, "namespace").trim();
        Identifier.fromNamespaceAndPath(checkedNamespace, "resource_runtime_validation");
        ResourceRuntime.namespace = checkedNamespace;
    }

    /** Creates a generated identifier in the configured caller namespace. */
    @NotNull
    public static Identifier identifier(@NotNull String path) {
        return Identifier.fromNamespaceAndPath(namespace, Objects.requireNonNull(path, "path"));
    }

    /** Returns a process-unique suffix suitable for dynamic resource identifiers and worker names. */
    @NotNull
    public static String nextResourceId() {
        return Long.toUnsignedString(NEXT_RESOURCE_ID.incrementAndGet(), 36);
    }

    /** Sets the base directory used for temporary decoder and media files. */
    public static void setTemporaryDirectory(@NotNull Path temporaryDirectory) {
        ResourceRuntime.temporaryDirectory = Objects.requireNonNull(temporaryDirectory, "temporaryDirectory").toAbsolutePath().normalize();
    }

    /** Returns an existing runtime-owned child directory below the configured temporary base. */
    @NotNull
    public static File temporaryDirectory(@NotNull String childName) {
        try {
            Path child = temporaryDirectory.resolve(Objects.requireNonNull(childName, "childName")).normalize();
            if (!child.startsWith(temporaryDirectory)) throw new IllegalArgumentException("Temporary resource directory must stay below the configured base");
            return Files.createDirectories(child).toFile();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to prepare the resource runtime temporary directory", ex);
        }
    }

    /** Installs the caller's placeholder or variable resolver for resource source strings. */
    public static void setSourceResolver(@NotNull UnaryOperator<String> sourceResolver) {
        ResourceRuntime.sourceResolver = Objects.requireNonNull(sourceResolver, "sourceResolver");
    }

    /** Resolves a source string through the configured caller hook. */
    @NotNull
    public static String resolveSource(@NotNull String source) {
        return Objects.requireNonNull(sourceResolver.apply(Objects.requireNonNull(source, "source")), "The resource source resolver returned null");
    }

    /** Installs caller-owned persistence hooks for the serialized preload registry. */
    public static void setPreLoadConfiguration(@NotNull Supplier<String> reader, @NotNull Consumer<String> writer) {
        preLoadConfigurationReader = Objects.requireNonNull(reader, "reader");
        preLoadConfigurationWriter = Objects.requireNonNull(writer, "writer");
    }

    /** Reads the serialized preload registry through the configured caller hook. */
    @NotNull
    public static String readPreLoadConfiguration() {
        String serialized = preLoadConfigurationReader.get();
        return serialized != null ? serialized : "";
    }

    /** Persists the serialized preload registry through the configured caller hook. */
    public static void writePreLoadConfiguration(@NotNull String serialized) {
        preLoadConfigurationWriter.accept(Objects.requireNonNull(serialized, "serialized"));
    }

    /** Sets the bounds enforced while decoding untrusted resource containers and images. */
    public static void setSafetyLimits(@NotNull ResourceSafetyLimits safetyLimits) {
        ResourceRuntime.safetyLimits = Objects.requireNonNull(safetyLimits, "safetyLimits");
    }

    /** Returns the currently configured resource decode bounds. */
    @NotNull
    public static ResourceSafetyLimits getSafetyLimits() {
        return safetyLimits;
    }

    /**
     * Registers for animated-texture lifecycle notifications delivered synchronously on the publisher's thread.
     * Listener failures are logged and swallowed so remaining listeners still run.
     */
    @NotNull
    public static Registration addAnimatedTextureListener(@NotNull AnimatedTextureListener listener) {
        return register(ANIMATED_TEXTURE_LISTENERS, Objects.requireNonNull(listener, "listener"));
    }

    /** Returns whether animated-texture playback listeners are currently registered. */
    public static boolean hasAnimatedTextureListeners() {
        return !ANIMATED_TEXTURE_LISTENERS.isEmpty();
    }

    /** Publishes an animated-texture lifecycle event synchronously, logging and swallowing each listener failure. */
    public static void fireAnimatedTextureEvent(@NotNull String source, @NotNull ResourceSourceType sourceType, boolean willRestart, @NotNull AnimatedTextureStatus status) {
        for (AnimatedTextureListener listener : ANIMATED_TEXTURE_LISTENERS) {
            try {
                listener.onPlaybackStatusChanged(source, sourceType, willRestart, status);
            } catch (Throwable throwable) {
                LOGGER.error("[KONKRETE] Animated texture listener failed", throwable);
            }
        }
    }

    /**
     * Registers for video lifecycle notifications delivered synchronously on the publisher's thread.
     * Listener failures are logged and swallowed so remaining listeners still run.
     */
    @NotNull
    public static Registration addVideoPlaybackListener(@NotNull VideoPlaybackListener listener) {
        return register(VIDEO_PLAYBACK_LISTENERS, Objects.requireNonNull(listener, "listener"));
    }

    /** Returns whether video playback listeners are currently registered. */
    public static boolean hasVideoPlaybackListeners() {
        return !VIDEO_PLAYBACK_LISTENERS.isEmpty();
    }

    /** Publishes a video lifecycle event synchronously, logging and swallowing each listener failure. */
    public static void fireVideoPlaybackEvent(@NotNull String source, @NotNull ResourceSourceType sourceType, boolean looping, @NotNull VideoPlaybackStatus status) {
        for (VideoPlaybackListener listener : VIDEO_PLAYBACK_LISTENERS) {
            try {
                listener.onPlaybackStatusChanged(source, sourceType, looping, status);
            } catch (Throwable throwable) {
                LOGGER.error("[KONKRETE] Video playback listener failed", throwable);
            }
        }
    }

    /**
     * Registers a post-audio-reload hook delivered synchronously on the publisher's thread, which integration should keep on the main client thread.
     * Listener failures are logged and swallowed so remaining listeners still run.
     */
    @NotNull
    public static Registration addAudioEngineReloadListener(@NotNull Runnable listener) {
        return register(AUDIO_RELOAD_LISTENERS, Objects.requireNonNull(listener, "listener"));
    }

    /** Publishes audio-reload completion synchronously, logging and swallowing each listener failure. */
    public static void fireAudioEngineReloaded() {
        for (Runnable listener : AUDIO_RELOAD_LISTENERS) {
            try {
                listener.run();
            } catch (Throwable throwable) {
                LOGGER.error("[KONKRETE] Audio engine reload listener failed", throwable);
            }
        }
    }

    /** Marks shutdown as started so asynchronous media work can stop scheduling native operations. */
    public static void markShuttingDown() {
        SHUTTING_DOWN.set(true);
    }

    /** Returns whether resource-runtime shutdown has started. */
    public static boolean isShuttingDown() {
        return SHUTTING_DOWN.get();
    }

    @NotNull
    private static <T> Registration register(@NotNull CopyOnWriteArrayList<T> listeners, @NotNull T listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** A removable callback registration. */
    @FunctionalInterface
    public interface Registration extends AutoCloseable {

        /** Removes the callback; repeated calls are harmless. */
        @Override
        void close();

    }

    /** Receives animated-texture playback lifecycle changes. */
    @FunctionalInterface
    public interface AnimatedTextureListener {

        /** Handles one animated-texture playback lifecycle change. */
        void onPlaybackStatusChanged(@NotNull String source, @NotNull ResourceSourceType sourceType, boolean willRestart, @NotNull AnimatedTextureStatus status);

    }

    /** Receives video playback lifecycle changes. */
    @FunctionalInterface
    public interface VideoPlaybackListener {

        /** Handles one video playback lifecycle change. */
        void onPlaybackStatusChanged(@NotNull String source, @NotNull ResourceSourceType sourceType, boolean looping, @NotNull VideoPlaybackStatus status);

    }

    /** Animated-texture playback lifecycle states. */
    public enum AnimatedTextureStatus {

        /** Indicates that playback has started. */
        STARTED,
        /** Indicates that playback reached its terminal frame. */
        FINISHED

    }

    /** Video playback lifecycle states. */
    public enum VideoPlaybackStatus {

        /** Indicates that playback is actively advancing. */
        PLAYING,
        /** Indicates that playback has stopped and reset. */
        STOPPED,
        /** Indicates that playback is paused without resetting. */
        PAUSED,
        /** Indicates that playback reached its terminal frame. */
        FINISHED

    }

}
