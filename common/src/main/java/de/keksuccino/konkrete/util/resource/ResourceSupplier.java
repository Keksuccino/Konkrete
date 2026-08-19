package de.keksuccino.konkrete.util.resource;

import de.keksuccino.konkrete.util.file.type.FileMediaType;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import de.keksuccino.konkrete.util.resource.resources.text.IText;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.resource.resources.video.IVideo;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Resolves a source on demand and caches the resource borrowed from its typed handler.
 * Instances are intentionally not thread-safe; configuration and {@link #get()} calls must be serialized by the owning caller.
 */
@SuppressWarnings("unused")
public class ResourceSupplier<R extends Resource> {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Unresolved serialized source supplied by the caller. */
    @NotNull
    protected String source;
    /** Runtime type promised by this supplier. */
    @NotNull
    protected Class<R> resourceType;
    @NotNull
    FileMediaType mediaType;
    /** Resource currently borrowed from its handler, or null before dispatch or after invalidation. */
    @Nullable
    protected R current;
    /** Most recent expanded source, used to detect resolver changes. */
    @Nullable
    protected String lastGetterSource;
    /** Optional synchronous callback that receives the old borrowed resource before replacement. */
    @Nullable
    protected Consumer<R> onUpdateCurrent = null;
    /** Whether this supplier is the non-dispatching empty sentinel. */
    protected boolean empty = false;

    /**
     * Returns a dummy-like {@link ResourceSupplier} that will never return a {@link Resource}, it will only return NULL.<br>
     * Empty {@link ResourceSupplier}s can be identified by calling {@link ResourceSupplier#isEmpty()}.
     */
    @NotNull
    public static <R extends Resource> ResourceSupplier<R> empty(@NotNull Class<R> resourceType, @NotNull FileMediaType mediaType) {
        ResourceSupplier<R> supplier = new ResourceSupplier<>(resourceType, mediaType, "");
        supplier.empty = true;
        return supplier;
    }

    /**
     * Returns a new {@link ResourceSupplier} for an image source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<ITexture> image(@NotNull String source) {
        return new ResourceSupplier<>(ITexture.class, FileMediaType.IMAGE, source);
    }

    /**
     * Returns a new {@link ResourceSupplier} for an audio source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<IAudio> audio(@NotNull String source) {
        return new ResourceSupplier<>(IAudio.class, FileMediaType.AUDIO, source)
                .setOnUpdateResourceTask(PlayableResource::stop);
    }

    /**
     * Returns a new {@link ResourceSupplier} for a video source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<IVideo> video(@NotNull String source) {
        return new ResourceSupplier<>(IVideo.class, FileMediaType.VIDEO, source)
                .setOnUpdateResourceTask(PlayableResource::stop);
    }

    /**
     * Returns a new {@link ResourceSupplier} for a text source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<IText> text(@NotNull String source) {
        return new ResourceSupplier<>(IText.class, FileMediaType.TEXT, source);
    }

    /** Initializes a new {@code ResourceSupplier} for resource runtime use. */
    public ResourceSupplier(@NotNull Class<R> resourceType, @NotNull FileMediaType mediaType, @NotNull String source) {
        this.source = Objects.requireNonNull(source);
        this.resourceType = Objects.requireNonNull(resourceType);
        this.mediaType = Objects.requireNonNull(mediaType);
    }

    /**
     * Returns the handler-owned resource for the currently expanded source, dispatching only when the cached source changed or closed.
     * The replacement callback runs synchronously on the calling thread before the old reference is cleared and may stop or close that old resource.
     */
    @SuppressWarnings("all")
    @Nullable
    public R get() {
        if (this.empty) return null;
        if ((this.current != null) && this.current.isClosed()) {
            this.current = null;
        }
        String getterSource = ResourceRuntime.resolveSource(this.source);
        if (!getterSource.equals(this.lastGetterSource)) {
            if ((this.onUpdateCurrent != null) && (this.current != null)) {
                this.onUpdateCurrent.accept(this.current);
            }
            this.current = null;
        }
        this.lastGetterSource = getterSource;
        if (this.current == null) {
            // Identifier accepts an empty path as minecraft:, which is also TextureManager's intentional-missing marker.
            // Dispatching a temporarily empty placeholder result would register that marker as a reloadable file texture,
            // so a later resource reload could try to decode a namespace directory as an image and fail the whole reload.
            if (!ResourceSource.isDispatchable(getterSource)) return null;
            ResourceSource resourceSource = ResourceSource.of(getterSource);
            try {
                ResourceHandler<?,?> handler = this.getResourceHandler();
                if (handler != null) {
                    this.current = (R) handler.get(resourceSource);
                }
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] ResourceSupplier failed to get resource: " + resourceSource + " (" + this.source + ")", ex);
            }
        }
        return this.current;
    }

    /** Returns the resource handler, or {@code null} when it is not available. */
    @Nullable
    public ResourceHandler<?,?> getResourceHandler() {
        if (this.mediaType == FileMediaType.IMAGE) return ResourceHandlers.getImageHandler();
        if (this.mediaType == FileMediaType.AUDIO) return ResourceHandlers.getAudioHandler();
        if (this.mediaType == FileMediaType.VIDEO) return ResourceHandlers.getVideoHandler();
        if (this.mediaType == FileMediaType.TEXT) return ResourceHandlers.getTextHandler();
        return null;
    }

    /**
     * Only works if this {@link ResourceSupplier}'s resource type is a {@link RenderableResource}.<br>
     * The {@link BiConsumer}'s {@link Resource} and {@link Identifier} is never NULL!<br><br>
     *
     * The {@link BiConsumer}'s {@link Identifier} is the {@link RenderableResource}'s
     * current {@link Identifier} ({@link RenderableResource#getResourceLocation()}).
     * You should always use the provided location instead of calling {@link RenderableResource#getResourceLocation()},
     * because some types of resources asynchronously change that method's return value.
     */
    public void forRenderable(@NotNull BiConsumer<R, Identifier> task) {
        R resource = this.get();
        if (resource instanceof RenderableResource r) {
            Identifier loc = r.getResourceLocation();
            if (loc != null) task.accept(resource, loc);
        }
    }

    /** Returns the resource type used by this resource runtime instance. */
    @NotNull
    public Class<R> getResourceType() {
        return this.resourceType;
    }

    /** Returns the media type used by this resource runtime instance. */
    @NotNull
    public FileMediaType getMediaType() {
        return this.mediaType;
    }

    /** Returns the source type used by this resource runtime instance. */
    @NotNull
    public ResourceSourceType getSourceType() {
        if (this.empty) return ResourceSourceType.LOCAL;
        return ResourceSourceType.getSourceTypeOf(ResourceRuntime.resolveSource(this.source));
    }

    /**
     * The source without its {@link ResourceSourceType} prefix.<br>
     * Should <b>NOT</b> be used for saving/serializing the source! For saving, use {@link ResourceSupplier#getSourceWithPrefix()} instead!
     */
    @NotNull
    public String getSourceWithoutPrefix() {
        if (this.empty) return "";
        return ResourceSourceType.getWithoutSourcePrefix(this.source);
    }

    /**
     * The source with its {@link ResourceSourceType} prefix.<br>
     * This should be used for saving/serializing the source.
     */
    @NotNull
    public String getSourceWithPrefix() {
        if (this.empty) return "";
        if (ResourceSourceType.hasSourcePrefix(this.source)) return this.source;
        return this.getSourceType().getSourcePrefix() + this.source;
    }

    /** Replaces the unresolved source; the next {@link #get()} call performs invalidation and dispatch. */
    public void setSource(@NotNull String source) {
        if (this.empty) return;
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Sets the callback invoked synchronously by {@link #get()} before an old resource is replaced.
     * The callback receives a handler-owned resource and may stop or close it; exceptions propagate from {@link #get()}.
     */
    public ResourceSupplier<R> setOnUpdateResourceTask(@Nullable Consumer<R> oldResourceConsumer) {
        this.onUpdateCurrent = oldResourceConsumer;
        return this;
    }

    /** Returns whether empty. */
    public boolean isEmpty() {
        return this.empty;
    }

}
