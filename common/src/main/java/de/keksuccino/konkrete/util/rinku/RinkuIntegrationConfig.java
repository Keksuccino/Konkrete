package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Holds caller-configurable paths, deadlines, callback execution, and optional adapters for Rinku.
 * Configure it before creating browsers; changes affect future extraction, queries, and callback dispatch only.
 */
public final class RinkuIntegrationConfig {

    private static volatile Path dataDirectory = Path.of(System.getProperty("java.io.tmpdir"), "konkrete-media").toAbsolutePath().normalize();
    private static volatile Duration javaScriptQueryTimeout = Duration.ofSeconds(1L);
    private static final BrowserTaskDispatcher DEFAULT_BROWSER_TASK_DISPATCHER = new BrowserTaskDispatcher() {
        /** {@inheritDoc} */
        @Override
        public boolean isCurrentThread() {
            return Minecraft.getInstance().isSameThread();
        }

        /** {@inheritDoc} */
        @Override
        public void execute(@NotNull Runnable command) {
            MainThreadTaskExecutor.executeInMainThread(command, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }
    };
    private static volatile BrowserTaskDispatcher browserTaskDispatcher = DEFAULT_BROWSER_TASK_DISPATCHER;
    private static volatile Executor playbackListenerExecutor = command -> MainThreadTaskExecutor.executeInMainThread(command, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    private static volatile VolumeListenerRegistrar volumeListenerRegistrar = listener -> () -> {};

    private RinkuIntegrationConfig() {}

    /** Returns the directory used for extracted browser-player assets. */
    @NotNull
    public static Path getDataDirectory() {
        return dataDirectory;
    }

    /** Sets the directory used for extracted browser-player assets. */
    public static void setDataDirectory(@NotNull Path directory) {
        dataDirectory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
    }

    /** Returns the maximum lifetime of a browser-scoped JavaScript result query. */
    @NotNull
    public static Duration getJavaScriptQueryTimeout() {
        return javaScriptQueryTimeout;
    }

    /** Sets the positive maximum lifetime of a browser-scoped JavaScript result query. */
    public static void setJavaScriptQueryTimeout(@NotNull Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        try {
            timeout.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("timeout is too large", exception);
        }
        javaScriptQueryTimeout = timeout;
    }

    /** Sets an off-thread page-call executor that never assumes its caller is already on the target thread. */
    public static void setBrowserExecutor(@NotNull Executor executor) {
        Executor checkedExecutor = Objects.requireNonNull(executor, "executor");
        browserTaskDispatcher = new BrowserTaskDispatcher() {
            /** {@inheritDoc} */
            @Override
            public boolean isCurrentThread() {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public void execute(@NotNull Runnable command) {
                checkedExecutor.execute(command);
            }
        };
    }

    /** Sets a dispatcher that can run page calls inline when already on its target thread. */
    public static void setBrowserTaskDispatcher(@NotNull BrowserTaskDispatcher dispatcher) {
        browserTaskDispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    /** Returns the dispatcher used for future page calls. */
    @NotNull
    public static BrowserTaskDispatcher getBrowserTaskDispatcher() {
        return browserTaskDispatcher;
    }

    /** Sets the executor used to deliver playback listener callbacks; the default is Minecraft's client thread. */
    public static void setPlaybackListenerExecutor(@NotNull Executor executor) {
        playbackListenerExecutor = Objects.requireNonNull(executor, "executor");
    }

    /** Returns the executor used to deliver playback listener callbacks. */
    @NotNull
    public static Executor getPlaybackListenerExecutor() {
        return playbackListenerExecutor;
    }

    /** Installs an optional sound-volume observer adapter. */
    public static void setVolumeListenerRegistrar(@NotNull VolumeListenerRegistrar registrar) {
        volumeListenerRegistrar = Objects.requireNonNull(registrar, "registrar");
    }

    @NotNull
    static VolumeListenerRegistration registerVolumeListener(@NotNull VolumeListener listener) throws Exception {
        return Objects.requireNonNull(volumeListenerRegistrar.register(listener), "volume listener registration");
    }

    /** Receives a changed Minecraft sound-category volume. */
    @FunctionalInterface
    public interface VolumeListener {

        /** Receives one category-volume update. */
        void onVolumeChanged(@NotNull SoundSource source, float volume);

    }

    /** Registers a volume callback and returns its unregister handle. */
    @FunctionalInterface
    public interface VolumeListenerRegistrar {

        /** Registers one callback. */
        @NotNull VolumeListenerRegistration register(@NotNull VolumeListener listener) throws Exception;

    }

    /** Handle that unregisters a previously installed volume callback. */
    @FunctionalInterface
    public interface VolumeListenerRegistration extends AutoCloseable {

        /** Unregisters the callback. */
        @Override void close() throws Exception;

    }

    /** Dispatches browser calls without self-queueing when the caller already owns the target thread. */
    public interface BrowserTaskDispatcher extends Executor {

        /** Returns whether the caller already runs on this dispatcher's target thread. */
        boolean isCurrentThread();

    }

}
