package de.keksuccino.konkrete.util.reload;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ClientReloadListenerRegistration<L> {

    private final Supplier<? extends L> listenerFactory;
    @Nullable private KonkreteResourceReload.ClientLoader registeredLoader;
    @Nullable private KonkreteResourceReload.ClientLoader registeringLoader;

    ClientReloadListenerRegistration(@NotNull Supplier<? extends L> listenerFactory) {
        this.listenerFactory = Objects.requireNonNull(listenerFactory, "listenerFactory");
    }

    synchronized boolean register(@NotNull KonkreteResourceReload.ClientLoader loader, @NotNull Consumer<? super L> registrar) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(registrar, "registrar");
        if (this.registeredLoader == loader) return false;
        if (this.registeredLoader != null) throw new IllegalStateException("Konkrete's client reload listener is already owned by " + this.registeredLoader + "; " + loader + " cannot register it again");
        // Loader registration mutates a synchronous registry. The monitor and reentrancy guard make factory creation plus ownership commit atomic.
        if (this.registeringLoader != null) throw new IllegalStateException("Konkrete's client reload listener registration is already in progress for " + this.registeringLoader);
        this.registeringLoader = loader;
        try {
            L listener = Objects.requireNonNull(this.listenerFactory.get(), "listenerFactory result");
            registrar.accept(listener);
            this.registeredLoader = loader;
            return true;
        } finally {
            this.registeringLoader = null;
        }
    }

}
