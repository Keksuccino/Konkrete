package de.keksuccino.konkrete.placeholder.placeholders.server;

import de.keksuccino.konkrete.util.lifecycle.ClientShutdownHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Lazy ownership and caller-override lifecycle for server-status placeholders. */
public final class ServerStatusProviders {

    private static final ServerStatusProvider UNAVAILABLE = address -> null;
    private static final ProviderLifecycle LIFECYCLE = new ProviderLifecycle(VanillaServerStatusProvider::new, cleanup -> ClientShutdownHandler.registerCleanup("placeholder server-status pinger", ClientShutdownHandler.ORDER_NETWORK + 10, cleanup), ClientShutdownHandler::isShuttingDown);

    private ServerStatusProviders() {
    }

    /**
     * Atomically installs a caller-owned provider. Konkrete closes its prior built-in pinger but never closes this
     * override, including when another override is installed or {@link #reset()} is called. Installation after client
     * shutdown fails with {@link IllegalStateException}.
     */
    public static void set(@NotNull ServerStatusProvider newProvider) {
        LIFECYCLE.set(Objects.requireNonNull(newProvider, "newProvider"));
    }

    /** Lazily creates the bounded vanilla provider unless an override is active; after shutdown, returns an unavailable provider. */
    @NotNull public static ServerStatusProvider get() {
        return LIFECYCLE.get();
    }

    /** Drops the caller-owned override without closing it; before shutdown, the bounded vanilla provider is recreated lazily. */
    public static void reset() {
        LIFECYCLE.reset();
    }

    interface OwnedProvider extends ServerStatusProvider, AutoCloseable {
        @Override
        void close();
    }

    static final class ProviderLifecycle {

        private final Supplier<? extends OwnedProvider> ownedFactory;
        private final Consumer<Runnable> shutdownRegistrar;
        private final BooleanSupplier shutdownState;
        private OwnedProvider ownedProvider;
        private ServerStatusProvider overrideProvider;
        private boolean shutdownRegistered;
        private boolean shutdown;

        ProviderLifecycle(@NotNull Supplier<? extends OwnedProvider> ownedFactory, @NotNull Consumer<Runnable> shutdownRegistrar, @NotNull BooleanSupplier shutdownState) {
            this.ownedFactory = Objects.requireNonNull(ownedFactory, "ownedFactory");
            this.shutdownRegistrar = Objects.requireNonNull(shutdownRegistrar, "shutdownRegistrar");
            this.shutdownState = Objects.requireNonNull(shutdownState, "shutdownState");
        }

        @NotNull
        synchronized ServerStatusProvider get() {
            if (this.shutdown || this.shutdownState.getAsBoolean()) return UNAVAILABLE;
            if (this.overrideProvider != null) return this.overrideProvider;
            if (this.ownedProvider == null) this.ownedProvider = Objects.requireNonNull(this.ownedFactory.get(), "ownedFactory returned null");
            if (!this.shutdownRegistered) {
                try {
                    this.shutdownRegistrar.accept(this::shutdown);
                    this.shutdownRegistered = true;
                } catch (RuntimeException | Error exception) {
                    OwnedProvider failed = this.ownedProvider;
                    this.ownedProvider = null;
                    if (failed != null) failed.close();
                    throw exception;
                }
            }
            return this.ownedProvider != null ? this.ownedProvider : UNAVAILABLE;
        }

        synchronized void set(@NotNull ServerStatusProvider provider) {
            if (this.shutdown || this.shutdownState.getAsBoolean()) throw new IllegalStateException("Server-status providers cannot be installed after client shutdown");
            Objects.requireNonNull(provider, "provider");
            if (provider == this.ownedProvider || provider == this.overrideProvider) return;
            this.overrideProvider = provider;
            this.closeOwned();
        }

        synchronized void reset() {
            this.overrideProvider = null;
        }

        synchronized void shutdown() {
            if (this.shutdown) return;
            this.shutdown = true;
            this.closeOwned();
        }

        private void closeOwned() {
            OwnedProvider closing = this.ownedProvider;
            this.ownedProvider = null;
            if (closing != null) closing.close();
        }
    }
}
