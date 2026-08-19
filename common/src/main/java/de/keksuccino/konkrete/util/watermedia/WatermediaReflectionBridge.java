package de.keksuccino.konkrete.util.watermedia;

import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.threading.KonkreteThreads;
import de.keksuccino.konkrete.util.watermedia.vulkan.WatermediaVulkanInterop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reflection-only adapter for Watermedia 3, safe to load when the optional dependency is absent.
 * Created player wrappers own Watermedia's player and supplied engines and must be released exactly once. Video
 * controls and normal release belong on the render thread designated during player creation.
 */
public final class WatermediaReflectionBridge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long SHUTDOWN_PLAYER_RELEASE_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(2L);
    private static final AtomicLong SHUTDOWN_PLAYER_RELEASE_DEADLINE_NANOS = new AtomicLong();
    private static final AtomicBoolean SHUTDOWN_PLAYER_RELEASE_TIMEOUT_LOGGED = new AtomicBoolean();
    private static volatile boolean WATERMEDIA_unsupported_gl_texture_handle_logged = false;
    private static final Map<MethodKey, Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Set<ManagedModernPlayer> MANAGED_PLAYERS = ConcurrentHashMap.newKeySet();

    private WatermediaReflectionBridge() {}

    static void clearReflectionCache() {
        METHOD_CACHE.clear();
        CLASS_CACHE.clear();
    }

    /** Creates a Watermedia-owned MRL, or returns null when the optional API is absent or incompatible. */
    @Nullable
    public static Object createMrl(@NotNull String source) {
        if (!WatermediaUtil.isWatermediaLoaded()) return null;
        try {
            Class<?> mediaApiClass = requireClass("org.watermedia.api.media.MediaAPI");
            Method createMrl = requireMethod(mediaApiClass, "mrl", String.class);
            return createMrl.invoke(null, source);
        } catch (Throwable ex) {
            LOGGER.error("[KONKRETE] Failed to create Watermedia MRL via MediaAPI#mrl for source: {}", source, ex);
        }

        return null;
    }

    /** Decodes an image through the optional codecs API, or returns null when unavailable or incompatible. */
    @Nullable
    public static Object decodeImage(@NotNull byte[] data) {
        if (!WatermediaUtil.isWatermediaLoaded()) return null;
        try {
            Class<?> codecsApiClass = requireClass("org.watermedia.api.codecs.CodecsAPI");
            Method decodeImage = requireMethod(codecsApiClass, "decodeImage", byte[].class);
            return decodeImage.invoke(null, (Object) data);
        } catch (Throwable ex) {
            LOGGER.error("[KONKRETE] Failed to decode image with Watermedia CodecsAPI", ex);
        }
        return null;
    }

    /** Returns a decoded image width, or zero when unavailable. */
    public static int imageWidth(@Nullable Object image) {
        return invokeInt(image, "width", 0);
    }

    /** Returns a decoded image height, or zero when unavailable. */
    public static int imageHeight(@Nullable Object image) {
        return invokeInt(image, "height", 0);
    }

    /** Returns the decoded image repeat count. */
    public static int imageRepeat(@Nullable Object image) {
        return invokeInt(image, "repeat", 0);
    }

    /** Returns decoded frame delays when exposed by Watermedia. */
    @Nullable
    public static long[] imageDelay(@Nullable Object image) {
        Object result = invoke(image, "delay", 0);
        if (result instanceof long[] delays) return delays;
        return null;
    }

    /** Returns decoded frame buffers when exposed by Watermedia. */
    @Nullable
    public static ByteBuffer[] imageFrames(@Nullable Object image) {
        Object result = invoke(image, "frames", 0);
        if (result instanceof ByteBuffer[] frameBuffers) return frameBuffers;
        return null;
    }

    /** Returns whether an MRL is still fetching. */
    public static boolean isMrlResolving(@Nullable Object mrl) {
        return mrlStatusName(mrl).equals("FETCHING");
    }

    /** Returns whether an MRL resolved successfully. */
    public static boolean isMrlLoaded(@Nullable Object mrl) {
        return mrlStatusName(mrl).equals("LOADED");
    }

    /** Returns whether an MRL reached a terminal failure state. */
    public static boolean isMrlFailed(@Nullable Object mrl) {
        String statusName = mrlStatusName(mrl);
        return statusName.equals("ERROR")
                || statusName.equals("BLOCKED")
                || statusName.equals("EXPIRED")
                || statusName.equals("FORGOTTEN");
    }

    /** Returns the Watermedia MRL status name or {@code UNKNOWN}. */
    @NotNull
    public static String mrlStatusName(@Nullable Object mrl) {
        Object status = invoke(mrl, "status", 0);
        return (status != null) ? status.toString() : "UNKNOWN";
    }

    /**
     * Creates an owned reflected player, or null when Watermedia is unavailable or incompatible.
     * Video callers must supply the actual render thread and an executor that dispatches back to that same thread;
     * Vulkan interop must already expose a live device when Vulkan is active.
     */
    @Nullable
    public static Object createPlayer(@Nullable Object mrl, @NotNull Thread renderThread, @NotNull Executor renderThreadExecutor, boolean video, boolean audio) {
        if (mrl == null || !WatermediaUtil.isWatermediaLoaded()) return null;
        if (video && !WatermediaUtil.isWatermediaRenderingAvailable()) return null;
        WatermediaShutdownIntegration.register();

        WatermediaUtil.trySuppressDevelopmentFfmpegDebugLogs();

        try {
            Object player = createModernPlayer(mrl, renderThread, renderThreadExecutor, video, audio);
            if (player != null) {
                return player;
            }
        } catch (Throwable ex) {
            LOGGER.error("[KONKRETE] Failed to create Watermedia player via media API", ex);
        }
        return null;
    }

    /** Starts a player. */
    public static void playerStart(@Nullable Object player) {
        invoke(player, "start", 0);
    }

    /** Starts a player in paused state. */
    public static void playerStartPaused(@Nullable Object player) {
        invoke(player, "startPaused", 0);
    }

    /** Changes a player's pause state. */
    public static void playerPause(@Nullable Object player, boolean paused) {
        invoke(player, "pause", 1, paused);
    }

    /** Stops a player. */
    public static void playerStop(@Nullable Object player) {
        invoke(player, "stop", 0);
    }

    /** Releases a player through its ownership wrapper; callers must use the player's render-thread release path. */
    public static void playerRelease(@Nullable Object player) {
        if (player instanceof ManagedModernPlayer managedModernPlayer) {
            managedModernPlayer.release();
            return;
        }
        invoke(player, "release", 0);
    }

    /**
     * Releases a player without allowing Watermedia's unbounded native thread joins to consume Minecraft's shutdown-watchdog window.
     * Modern players share one short wait budget so their GL engine can still be released on the render thread when native cleanup finishes promptly.
     */
    public static void playerReleaseForShutdown(@Nullable Object player) {
        if (player == null) return;
        if (player instanceof ManagedModernPlayer managedModernPlayer) {
            managedModernPlayer.releaseForShutdown();
            return;
        }
        if (RenderingUtils.isVulkanActive()) {
            invoke(player, "release", 0);
        } else {
            KonkreteThreads.startDaemonThread(() -> invoke(player, "release", 0), "Watermedia-PlayerRelease");
        }
    }

    /** Synchronously releases every registered Vulkan player on the device thread before device destruction. */
    public static void releaseVulkanPlayersBeforeDeviceClose() {
        for (ManagedModernPlayer player : MANAGED_PLAYERS.toArray(ManagedModernPlayer[]::new)) {
            if (player.graphicsBackend == GraphicsBackend.VULKAN) player.release();
        }
    }

    /** Returns whether a player reports active playback. */
    public static boolean playerIsPlaying(@Nullable Object player) {
        return invokeBoolean(player, "playing", false);
    }

    /** Returns whether a player reports paused playback. */
    public static boolean playerIsPaused(@Nullable Object player) {
        return invokeBoolean(player, "paused", false);
    }

    /** Returns the player status name or {@code UNKNOWN}. */
    @NotNull
    public static String playerStatusName(@Nullable Object player) {
        Object status = invoke(player, "status", 0);
        return (status != null) ? status.toString() : "UNKNOWN";
    }

    /** Returns the backend-specific texture handle without narrowing Vulkan handles. */
    public static long playerTextureHandle(@Nullable Object player) {
        Number textureHandle = invokeNumber(player, "texture", 0);
        return textureHandle != null ? textureHandle.longValue() : 0L;
    }

    /** Returns the decoded media width. */
    public static int playerWidth(@Nullable Object player) {
        return invokeInt(player, "width", 0);
    }

    /** Returns the decoded media height. */
    public static int playerHeight(@Nullable Object player) {
        return invokeInt(player, "height", 0);
    }

    /** Returns media duration in milliseconds. */
    public static long playerDuration(@Nullable Object player) {
        return invokeLong(player, "duration", 0L);
    }

    /** Returns current media time in milliseconds. */
    public static long playerTime(@Nullable Object player) {
        return invokeLong(player, "time", 0L);
    }

    /** Seeks a player in milliseconds and reports acceptance. */
    public static boolean playerSeek(@Nullable Object player, long timeMs) {
        Object result = invoke(player, "seek", 1, timeMs);
        if (result instanceof Boolean b) return b;
        return false;
    }

    /** Returns the player's repeat setting. */
    public static boolean playerRepeat(@Nullable Object player) {
        return invokeBoolean(player, "repeat", false);
    }

    /** Changes the player's repeat setting. */
    public static void setPlayerRepeat(@Nullable Object player, boolean repeat) {
        invoke(player, "repeat", 1, repeat);
    }

    /** Sets player volume in integer percent. */
    public static void setPlayerVolume(@Nullable Object player, int volumePercent) {
        invoke(player, "volume", 1, volumePercent);
    }

    @Nullable
    private static Object createModernPlayer(@NotNull Object mrl, @NotNull Thread renderThread, @NotNull Executor renderThreadExecutor, boolean video, boolean audio) throws Throwable {
        Class<?> mediaApiClass = requireClass("org.watermedia.api.media.MediaAPI");
        Class<?> mrlClass = requireClass("org.watermedia.api.media.MRL");
        GraphicsBackend graphicsBackend = video ? graphicsBackend(RenderingUtils.isVulkanActive()) : GraphicsBackend.OPENGL;

        Object gfxEngine = null;
        Object sfxEngine = null;
        AtomicBoolean gfxSupplied = new AtomicBoolean();
        AtomicBoolean sfxSupplied = new AtomicBoolean();

        try {
            gfxEngine = video ? buildModernGfxEngine(renderThread, renderThreadExecutor, graphicsBackend) : null;
            sfxEngine = audio ? buildModernSfxEngine() : null;
            Object preparedGfxEngine = gfxEngine;
            Object preparedSfxEngine = sfxEngine;
            Supplier<Object> gfxSupplier = () -> {
                gfxSupplied.set(true);
                return preparedGfxEngine;
            };
            Supplier<Object> sfxSupplier = () -> {
                sfxSupplied.set(true);
                return preparedSfxEngine;
            };
            Method createPlayer = requireMethod(mediaApiClass, "createPlayer", mrlClass, int.class, Supplier.class, Supplier.class);
            Object player = createPlayer.invoke(null, mrl, 0, gfxSupplier, sfxSupplier);

            // WaterMedia 3.0.0.22 owns an engine as soon as it invokes its supplier. Its player owns supplied engines on success, and MediaAPI releases them after a caught construction failure. Engines whose suppliers were never invoked remain Konkrete's responsibility.
            if (!gfxSupplied.get()) releaseModernResource(gfxEngine);
            if (!sfxSupplied.get()) releaseModernResource(sfxEngine);
            if (player == null) return null;

            ManagedModernPlayer managedPlayer = new ManagedModernPlayer(player, graphicsBackend);
            MANAGED_PLAYERS.add(managedPlayer);
            return managedPlayer;
        } catch (Throwable ex) {
            // Errors deliberately propagate out of MediaAPI without its construction-failure cleanup, while reflection can fail before ownership transfer. Best-effort cleanup is therefore required for every pre-created engine on this path.
            releaseModernResource(gfxEngine);
            releaseModernResource(sfxEngine);
            throw ex;
        }
    }

    @NotNull
    private static Object buildModernGfxEngine(@NotNull Thread renderThread, @NotNull Executor renderThreadExecutor, @NotNull GraphicsBackend graphicsBackend) throws Throwable {
        Class<?> mediaApiClass = requireClass("org.watermedia.api.media.MediaAPI");
        if (graphicsBackend == GraphicsBackend.VULKAN) {
            Class<?> vkContextClass = requireClass("org.watermedia.api.media.engines.vk.VKContext");
            Object vkContext = WatermediaVulkanInterop.context();
            if (vkContext == null || !vkContextClass.isInstance(vkContext)) {
                throw new IllegalStateException("Minecraft's Vulkan device is not available through the Watermedia VKContext bridge");
            }
            Method createVkEngine = requireMethod(mediaApiClass, "vkEngine", vkContextClass);
            return createVkEngine.invoke(null, vkContext);
        }
        // WaterMedia 3.0.0.22 removed the callback-based builder; its factory engine now preserves the host's exact GL state itself, including state cached by Minecraft, Sodium, and Iris.
        Method createGlEngine = requireMethod(mediaApiClass, "glEngine", Thread.class, Executor.class);
        return createGlEngine.invoke(null, renderThread, renderThreadExecutor);
    }

    @NotNull
    static GraphicsBackend graphicsBackend(boolean vulkanActive) {
        return vulkanActive ? GraphicsBackend.VULKAN : GraphicsBackend.OPENGL;
    }

    static int openGlTextureId(long textureHandle) {
        if (textureHandle > 0L && textureHandle <= Integer.MAX_VALUE) return (int) textureHandle;
        if (textureHandle != 0L && !WATERMEDIA_unsupported_gl_texture_handle_logged) {
            WATERMEDIA_unsupported_gl_texture_handle_logged = true;
            LOGGER.warn("[KONKRETE] Watermedia returned an unsupported OpenGL texture handle: {}", textureHandle);
        }
        return 0;
    }

    @NotNull
    private static Object buildModernSfxEngine() throws Throwable {
        Class<?> mediaApiClass = requireClass("org.watermedia.api.media.MediaAPI");
        Method createAlEngine = requireMethod(mediaApiClass, "alEngine");
        return createAlEngine.invoke(null);
    }

    @Nullable
    private static Object invoke(@Nullable Object target, @NotNull String methodName, int parameterCount, Object... args) {
        Object invocationTarget = unwrapInvocationTarget(target);
        if (invocationTarget == null) return null;
        try {
            Method method = findMethod(invocationTarget.getClass(), methodName, parameterCount);
            if (method != null) {
                return method.invoke(invocationTarget, args);
            }
        } catch (Throwable ex) {
            LOGGER.error("[KONKRETE] Failed to invoke Watermedia method '{}'", methodName, ex);
        }
        return null;
    }

    private static boolean invokeBoolean(@Nullable Object target, @NotNull String methodName, boolean fallback) {
        Object result = invoke(target, methodName, 0);
        if (result instanceof Boolean b) return b;
        return fallback;
    }

    @Nullable
    private static Number invokeNumber(@Nullable Object target, @NotNull String methodName, int parameterCount, Object... args) {
        Object result = invoke(target, methodName, parameterCount, args);
        if (result instanceof Number number) return number;
        return null;
    }

    private static int invokeInt(@Nullable Object target, @NotNull String methodName, int fallback) {
        Object result = invoke(target, methodName, 0);
        if (result instanceof Number n) return n.intValue();
        return fallback;
    }

    private static long invokeLong(@Nullable Object target, @NotNull String methodName, long fallback) {
        Object result = invoke(target, methodName, 0);
        if (result instanceof Number n) return n.longValue();
        return fallback;
    }

    @Nullable
    private static Object unwrapInvocationTarget(@Nullable Object target) {
        if (target instanceof ManagedModernPlayer managedModernPlayer) {
            return managedModernPlayer.player;
        }
        return target;
    }

    private static void releaseModernResource(@Nullable Object resource) {
        if (resource == null) return;
        try {
            Method release = findMethod(resource.getClass(), "release", 0);
            if (release != null) {
                release.invoke(resource);
            }
        } catch (Throwable ex) {
            LOGGER.error("[KONKRETE] Failed to release Watermedia engine resource", ex);
        }
    }

    @NotNull
    private static Class<?> requireClass(@NotNull String className) throws ClassNotFoundException {
        Optional<Class<?>> cached = CLASS_CACHE.computeIfAbsent(className, name -> {
            try {
                return Optional.of(Class.forName(name, false, WatermediaIntegrationConfig.getClassLoader()));
            } catch (ClassNotFoundException | LinkageError ignored) {
                return Optional.empty();
            }
        });
        if (cached.isEmpty()) throw new ClassNotFoundException(className);
        return cached.get();
    }

    @NotNull
    private static Method requireMethod(@NotNull Class<?> owner, @NotNull String name, @NotNull Class<?>... parameterTypes) throws NoSuchMethodException {
        MethodKey key = new MethodKey(owner, name, List.of(parameterTypes));
        Optional<Method> cached = METHOD_CACHE.computeIfAbsent(key, ignored -> {
            try {
                return Optional.of(owner.getMethod(name, parameterTypes));
            } catch (NoSuchMethodException | SecurityException exception) {
                return Optional.empty();
            }
        });
        if (cached.isEmpty()) throw new NoSuchMethodException(owner.getName() + "#" + name);
        return cached.get();
    }

    private static boolean awaitShutdownPlayerRelease(@NotNull Thread releaseThread) {
        long now = System.nanoTime();
        long deadline = SHUTDOWN_PLAYER_RELEASE_DEADLINE_NANOS.updateAndGet(current -> current == 0L ? now + SHUTDOWN_PLAYER_RELEASE_BUDGET_NANOS : current);
        long remainingNanos = Math.max(0L, deadline - now);
        if (remainingNanos > 0L) {
            long remainingMillis = remainingNanos / 1_000_000L;
            int additionalNanos = (int) (remainingNanos % 1_000_000L);
            try {
                releaseThread.join(remainingMillis, additionalNanos);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        boolean completed = !releaseThread.isAlive();
        if (!completed && SHUTDOWN_PLAYER_RELEASE_TIMEOUT_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("[KONKRETE] Watermedia player release exceeded the shared client-shutdown time budget; remaining native cleanup will finish on a daemon thread.");
        }
        return completed;
    }

    @Nullable
    private static Method findMethod(@NotNull Class<?> type, @NotNull String name, int parameterCount) {
        MethodKey key = new MethodKey(type, name, parameterCount);
        return METHOD_CACHE.computeIfAbsent(key, ignored -> {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) return Optional.of(method);
            }
            return Optional.empty();
        }).orElse(null);
    }

    private static final class ManagedModernPlayer {
        // WaterMedia 3.0.0.22 players own and release both supplied engines. Retaining either engine here would make every successful player release it twice.
        private final Object player;
        private final GraphicsBackend graphicsBackend;
        private final AtomicBoolean released = new AtomicBoolean();

        private ManagedModernPlayer(@NotNull Object player, @NotNull GraphicsBackend graphicsBackend) {
            this.player = player;
            this.graphicsBackend = graphicsBackend;
        }

        private void release() {
            if (!this.released.compareAndSet(false, true)) return;

            this.releasePlayer();
        }

        private void releaseForShutdown() {
            if (!this.released.compareAndSet(false, true)) return;
            if (this.graphicsBackend == GraphicsBackend.VULKAN) {
                this.releasePlayer();
            } else {
                Thread releaseThread = KonkreteThreads.startDaemonThread(this::releasePlayer, "Watermedia-NativePlayerRelease");
                awaitShutdownPlayerRelease(releaseThread);
            }
        }

        private void releasePlayer() {
            try {
                Method release = findMethod(this.player.getClass(), "release", 0);
                if (release != null) {
                    release.invoke(this.player);
                }
            } catch (Throwable ex) {
                LOGGER.error("[KONKRETE] Failed to release Watermedia player", ex);
            } finally {
                MANAGED_PLAYERS.remove(this);
            }
        }
    }

    private record MethodKey(@NotNull Class<?> owner, @NotNull String name, int parameterCount, @Nullable List<Class<?>> parameterTypes) {
        private MethodKey(Class<?> owner, String name, int parameterCount) {
            this(owner, name, parameterCount, null);
        }

        private MethodKey(Class<?> owner, String name, List<Class<?>> parameterTypes) {
            this(owner, name, parameterTypes.size(), parameterTypes);
        }
    }

    enum GraphicsBackend {
        OPENGL,
        VULKAN
    }

}
