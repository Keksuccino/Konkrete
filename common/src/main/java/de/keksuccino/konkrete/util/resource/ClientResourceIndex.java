package de.keksuccino.konkrete.util.resource;

import de.keksuccino.konkrete.util.MinecraftResourceReloadObserver;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/** Publishes a lazy, reload-generation-safe index of client resource-pack identifiers. */
public final class ClientResourceIndex {

    private static final ClientResourceIndex INSTANCE = new ClientResourceIndex();

    private final Object cacheLock = new Object();
    @Nullable private volatile CacheEntry currentEntry;
    @Nullable private PreparedIndex pendingIndex;

    ClientResourceIndex() {}

    /** Returns the loaded locations used by this resource runtime instance. */
    @NotNull
    public static Set<Identifier> getLoadedLocations(@NotNull ResourceManager resourceManager) {
        return INSTANCE.getForManager(resourceManager);
    }

    /** Prepares resource state for the resource runtime. */
    @ApiStatus.Internal
    @NotNull
    public static PreparedIndex prepare(@NotNull ResourceManager resourceManager) {
        return INSTANCE.prepareForManager(resourceManager);
    }

    /** Publishes the stage state to the resource runtime. */
    @ApiStatus.Internal
    public static void stage(@NotNull PreparedIndex preparedIndex) {
        INSTANCE.stagePrepared(preparedIndex);
    }

    /** Updates resource runtime state when minecraft resource reload occurs. */
    @ApiStatus.Internal
    public static void onMinecraftResourceReload(@NotNull MinecraftResourceReloadObserver.ReloadAction action) {
        INSTANCE.onReload(action);
    }

    @NotNull
    Set<Identifier> getForManager(@NotNull ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        CacheEntry entry = this.currentEntry;
        if (entry != null && (entry.authoritative || entry.resourceManager == resourceManager)) return entry.locations;

        synchronized (this.cacheLock) {
            entry = this.currentEntry;
            if (entry != null && (entry.authoritative || entry.resourceManager == resourceManager)) return entry.locations;
            Set<Identifier> locations = ClientResourceIndexBuilder.build(resourceManager);
            this.currentEntry = new CacheEntry(resourceManager, locations, false);
            return locations;
        }
    }

    @NotNull
    PreparedIndex prepareForManager(@NotNull ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        return new PreparedIndex(ClientResourceIndexBuilder.build(resourceManager));
    }

    void stagePrepared(@NotNull PreparedIndex preparedIndex) {
        synchronized (this.cacheLock) {
            this.pendingIndex = Objects.requireNonNull(preparedIndex, "preparedIndex");
        }
    }

    void onReload(@NotNull MinecraftResourceReloadObserver.ReloadAction action) {
        Objects.requireNonNull(action, "action");
        synchronized (this.cacheLock) {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                this.pendingIndex = null;
                return;
            }
            if (this.pendingIndex != null) {
                // Minecraft exposes a stable ReloadableResourceManager wrapper while each reload uses a new internal manager. Successful lifecycle publication, not wrapper identity, is the generation boundary.
                this.currentEntry = new CacheEntry(null, this.pendingIndex.locations, true);
                this.pendingIndex = null;
            }
        }
    }

    /** Carries an immutable resource-pack index prepared for publication after a successful reload. */
    public static final class PreparedIndex {

        private final Set<Identifier> locations;

        private PreparedIndex(@NotNull Set<Identifier> locations) {
            this.locations = Objects.requireNonNull(locations, "locations");
        }

    }

    private record CacheEntry(@Nullable ResourceManager resourceManager, Set<Identifier> locations, boolean authoritative) {

    }

}
